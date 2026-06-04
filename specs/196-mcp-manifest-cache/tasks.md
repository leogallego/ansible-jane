# Tasks: MCP Tool Manifest Cache

**Input**: Design documents from `/specs/196-mcp-manifest-cache/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md
**GitHub Issue**: #223

**Tests**: Not explicitly requested in the spec. Unit tests (serialization, cache CRUD, CachedMcpTool behavior) are recommended as part of the foundational tasks but are not listed as separate task phases. Run `scripts/test-all.sh` before merging per project convention.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

Base package: `app/src/main/kotlin/io/github/leogallego/ansiblejane/`

---

## Phase 1: Setup

**Purpose**: Prepare shared infrastructure for CachedMcpTool to reuse McpTool constants

- [x] T001 Extract WRITE_SUFFIXES and MAX_DESCRIPTION_CHARS from private companion in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/tools/McpTool.kt` to internal module-level constants (or a shared object) so CachedMcpTool can reuse them for isDestructive and description truncation

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Data model, cache storage, and CachedMcpTool — MUST complete before ANY user story

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T002 [P] Create `app/src/main/kotlin/io/github/leogallego/ansiblejane/model/ToolManifest.kt` with `ToolManifest` (schemaVersion, instanceId, servers, cachedAt) and `ServerToolCache` (serverUrl, label, toolset, serverInfo, tools, readOnly) as @Serializable data classes per data-model.md
- [x] T003 Create `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/tools/CachedMcpTool.kt` implementing Tool interface — spec computed from McpToolDefinition with `"[$serverLabel] $description"` format, isDestructive using shared WRITE_SUFFIXES, execute() calls `serverManager.ensureConnected(serverLabel)` then delegates to `McpClient.callTool()`, returns ToolResult with CONNECTION_ERROR on failure. Depends on T006 (ensureConnected)
- [x] T004 Add `saveManifest(instanceId, manifest)`, `loadManifest(instanceId)`, and `deleteManifest(instanceId)` methods to `app/src/main/kotlin/io/github/leogallego/ansiblejane/data/TokenManager.kt` — store as JSON string in DataStore preference key `manifest_<instanceId>`, log warning if serialized size exceeds 100KB
- [x] T005 Add manifest cleanup to `removeInstance()` in `app/src/main/kotlin/io/github/leogallego/ansiblejane/data/TokenManager.kt` — call `deleteManifest(instanceId)` alongside credential deletion to prevent orphaned caches (FR-011)
- [x] T006 Add `ensureConnected(serverLabel): McpClient` to `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — looks up McpServerConfig from `currentInstance.mcpServerUrls` by label, returns existing client if connected, otherwise connects using `connectServer()`. Use per-label Mutex in `ConcurrentHashMap<String, Mutex>` to prevent concurrent connection attempts from lazy execute and background connect (research R8)

**Checkpoint**: Foundation ready — data model exists, cache can be saved/loaded, CachedMcpTool can be instantiated and routed, lazy connection mechanism available

---

## Phase 3: User Story 1 — Instant Tool Availability (Priority: P1) 🎯 MVP

**Goal**: Cached tools are available for ToolRouter routing immediately on app launch, without waiting for MCP server connections. First connection populates the cache for subsequent launches.

**Independent Test**: Connect to an MCP server once, close the app, reopen in airplane mode. Tool metadata (names, descriptions, categories) is visible in ToolRouter routing (logcat ROUTE debug output) and available for query matching.

### Implementation for User Story 1

- [x] T007 [US1] Add cache population to `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — after successful `connectAll()`, build a `ToolManifest` from the connected clients' tool lists and server info, then save via `TokenManager.saveManifest()`. Handle partial success: cache tools from successful servers only, skip failed servers
- [x] T008 [US1] Add `connectAllWithCache()` to `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — do NOT call `disconnectAll()` at the start. For each server: connect normally, then swap that server's tools in `mcpTools` list (remove old entries for that label, add new live entries). This ensures `getAllTools()` never returns empty during the cached→live transition (research R5)
- [x] T009 [US1] Modify `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/presentation/AssistantViewModel.kt` init block — on instance change: (1) cancel any in-flight background connect coroutine from the previous instance, (2) call `TokenManager.loadManifest()`, (3) if cache exists, create `CachedMcpTool` instances from cached `ServerToolCache` entries, (4) pass them to a new `McpServerManager.setCachedTools(tools: List<CachedMcpTool>)` method that replaces `mcpTools` so `getAllTools()` returns cached tools, (5) transition to Active state
- [x] T010 [US1] Launch background coroutine in AssistantViewModel after cache load — store the `Job` reference so it can be cancelled on instance switch (T009 step 1). Call `McpServerManager.connectAllWithCache()` which eagerly connects all servers and swaps cached→live tools per-server. After completion, update the manifest cache with fresh data
- [x] T011 [US1] Handle no-cache fallback in AssistantViewModel init — if `loadManifest()` returns null (first-ever connection or corrupt cache), fall back to current eager `connectAll()` flow, then populate cache on success (T007)
- [x] T012 [US1] Enforce server label uniqueness at the start of `connectAllWithCache()` in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — when iterating `currentInstance.mcpServerUrls`, if duplicate labels are detected, manual config takes precedence over auto-detected; skip the auto-detected duplicate and log warning (FR-016)

**Checkpoint**: US1 is fully functional. App loads cached tools instantly, background connect refreshes them. First-ever connection populates the cache. Partial failures are handled.

---

## Phase 4: User Story 2 — Cache Invalidation & Refresh (Priority: P1)

**Goal**: The cache stays fresh. Version changes trigger automatic refresh, config changes invalidate affected servers, and users can force a manual refresh.

**Independent Test**: Connect to a server (populating cache), modify the server's version or config, reconnect. Verify the cache updates. Tap refresh in Settings > Tools and verify re-fetch.

### Implementation for User Story 2

- [x] T013 [US2] Split `connect()` into `initialize()` + `discoverTools()` in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpClient.kt` — `initialize()` performs MCP handshake and returns `McpServerInfo` (name, version), `discoverTools()` calls `tools/list` and populates session tools. Keep `connect()` as convenience that calls both sequentially for backward compatibility (research R4)
- [x] T014 [US2] Add version-based invalidation to `connectAllWithCache()` in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — for each server: call `initialize()` only, compare `serverInfo.version` against cached `ServerToolCache.serverInfo.version`. If match: skip `discoverTools()`, keep cached tools. If mismatch: call `discoverTools()`, update cache entry for that server (FR-003)
- [x] T015 [US2] Add config change detection in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — before connecting, compare current `McpServerConfig` (URL, toolset, enabled state) against cached `ServerToolCache` (serverUrl, toolset). If any differ, invalidate that server's cache and force full reconnect (FR-004, FR-012)
- [x] T016 [US2] Preserve existing cache on refresh failure in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — covers two scenarios: (a) server unreachable — keep existing cached entry, do not clear it; (b) server reachable for `initialize()` but `discoverTools()` fails — keep existing cache for that server, report the failure, allow retry (FR-006, US2-S4, US2-S6). Report which servers failed via a status mechanism accessible to the UI
- [x] T017 [P] [US2] Add manual refresh action to `app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsViewModel.kt` — `refreshTools()` method that calls connectAllWithCache with force-refresh flag (skip version check, re-fetch all). Guard with AtomicBoolean to ignore concurrent requests (FR-017)
- [x] T018 [US2] Add "Refresh tools" button to `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/ToolsTab.kt` — wired to `SettingsViewModel.refreshTools()`, shows loading indicator during refresh, displays confirmation or per-server failure results

**Checkpoint**: Cache invalidation works via version check, config changes, and manual refresh. Failed refreshes preserve existing cache. Concurrent refreshes are ignored.

---

## Phase 5: User Story 3 — Offline Tool Browsing (Priority: P2)

**Goal**: Users can browse the full tool catalog in Settings > Tools while offline, provided a cache was previously populated.

**Independent Test**: Populate cache while online, enable airplane mode, navigate to Settings > Tools. All tool metadata (names, descriptions, server labels) is browsable.

### Implementation for User Story 3

- [x] T020 [US3] Expose cached tools in `app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsViewModel.kt` — when connection state is not Connected, load manifest from TokenManager and provide cached tool data to the UI via StateFlow
- [x] T021 [US3] Modify `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/McpServerCard.kt` — render tool list from cache when `connectionState` is not `McpConnectionState.Connected`. Currently tools only display when connected (lines 203-222); add a second data source from cached manifest
- [x] T022 [P] [US3] Add tool parameter schema display in McpServerCard or a detail view — show input fields, types, required/optional from cached `McpToolDefinition.inputSchema` in a readable format (US3-S2 acceptance scenario)

**Checkpoint**: Users can browse all cached tool information offline. Tool detail includes parameter schema.

---

## Phase 6: User Story 4 — Lazy Server Connection (Priority: P3)

**Goal**: When a valid cache exists, the app makes zero MCP server connections on startup. Connections are deferred until a tool from that server is actually invoked.

**Independent Test**: Configure 3 MCP servers, launch app, verify zero MCP connections in logcat. Ask a question triggering a tool from server 1 only — verify only server 1 connects.

### Implementation for User Story 4

- [x] T023 [US4] Modify `connectAllWithCache()` in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — when a valid cache exists for all servers, skip all eager connections entirely. Do not call `initialize()` or `discoverTools()` on any server. Rely on `ensureConnected()` triggered by `CachedMcpTool.execute()` for on-demand connections (FR-009)
- [x] T024 [US4] Handle parallel lazy connections in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — when a query matches tools from multiple servers, `ensureConnected()` may be called concurrently for different labels. Per-label Mutex (T006) prevents races within a single label while allowing different labels to connect in parallel
- [x] T025 [US4] Handle lazy connection failure in `CachedMcpTool.execute()` — if `ensureConnected()` throws, return ToolResult with `ErrorType.CONNECTION_ERROR` and a user-friendly message. The LLM receives this error and decides whether to retry or use a different tool — no ChatEngine code change needed (US4-S3, US4-S4 acceptance scenarios)

**Checkpoint**: App makes zero MCP connections on startup when cache exists. Connections happen transparently on first tool invocation. Failures are handled gracefully with fallback.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validation, cleanup, and documentation

- [x] T026 Run quickstart.md validation scenarios on a physical device or emulator — verify each phase's checkpoint (cache population, cache load, version invalidation, offline browsing, lazy connect). Include lazy connect verification: confirm via logcat that only the target server connects when a cached tool is invoked
- [x] T027 Verify manifest cache size stays under 100KB per instance using test data with representative server/tool counts (7 servers × 10 tools). Log warning if exceeded but do not fail
- [x] T028 Update GitHub issue #223 with final implementation status, link to merged PR, and close the issue

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup (T001) — BLOCKS all user stories
- **US1 (Phase 3)**: Depends on Foundational (Phase 2) — this is the MVP
- **US2 (Phase 4)**: Depends on Foundational (Phase 2). Can start after or in parallel with US1 (T013 is independent of US1 tasks, but T014 modifies connectAllWithCache from T008)
- **US3 (Phase 5)**: Depends on Foundational (Phase 2). Independent of US1/US2 for implementation, but more useful once cache is populated (US1)
- **US4 (Phase 6)**: Depends on US1 (Phase 3) — modifies connectAllWithCache behavior from T008/T010
- **Polish (Phase 7)**: Depends on all desired user stories being complete. T026 includes lazy connect verification (moved from former US4 verification task)

### User Story Dependencies

- **US1 (P1)**: Can start after Foundational. No dependencies on other stories. **This is the MVP.**
- **US2 (P1)**: Can start after Foundational. T014 modifies `connectAllWithCache()` created in US1 T008, so T008 should complete first. T017-T018 (refresh UI) are independent.
- **US3 (P2)**: Can start after Foundational. Independent of US1/US2 — reads from cache populated by any path.
- **US4 (P3)**: Depends on US1. Modifies the background connect behavior from eager (US1) to lazy (US4).

### Within Each User Story

- Core data flow before UI (models → services → views)
- McpServerManager changes before AssistantViewModel changes
- Backend changes before UI changes
- Story complete before moving to next priority

### Parallel Opportunities

Within **Phase 2 (Foundational)**:
- T002 (ToolManifest.kt) can run in parallel with T004 (TokenManager) — different files
- T006 (ensureConnected) should complete before T003 (CachedMcpTool calls ensureConnected)
- T004 and T005 (TokenManager changes) must be sequential
- Recommended order: T002 ∥ T004 → T005 → T006 → T003

Within **Phase 4 (US2)**:
- T017 (SettingsViewModel refresh) can run in parallel with T013-T016 (McpClient/McpServerManager changes)

Within **Phase 5 (US3)**:
- T022 (parameter schema display) can run in parallel with T020-T021

Across **user stories** (if team capacity allows):
- US1 and US3 can proceed in parallel after Foundational
- US2 can start once US1 T008 is done (for T014), but T017-T018 are independent

---

## Parallel Example: User Story 1

```
# Phase 2 recommended order:
T002: Create ToolManifest.kt (model/)        ─┐
T004: Add cache CRUD to TokenManager (data/) ─┘ parallel
T005: Add cleanup to removeInstance
T006: Add ensureConnected to McpServerManager
T003: Create CachedMcpTool.kt (depends on T006)

# Phase 3 sequentially:
T007: Add cache population to McpServerManager
T008: Add connectAllWithCache to McpServerManager
T009: Modify AssistantViewModel init (cancel + cache load + setCachedTools)
T010: Launch background connect (store Job for cancellation)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (T001)
2. Complete Phase 2: Foundational (T002-T006, order: T002 ∥ T004 → T005 → T006 → T003)
3. Complete Phase 3: User Story 1 (T007-T012)
4. **STOP and VALIDATE**: Test US1 independently — connect once, restart in airplane mode, verify cached tools available in ToolRouter
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 → Test independently → Deploy (MVP!)
3. Add US2 → Test invalidation → Deploy (cache is now trustworthy)
4. Add US3 → Test offline browsing → Deploy (full offline UX)
5. Add US4 → Test lazy connections → Deploy (performance optimization)
6. Each story adds value without breaking previous stories

### File Modification Summary

| File | Phase | Action |
|------|-------|--------|
| `assistant/tools/McpTool.kt` | Setup | Extract shared constants |
| `model/ToolManifest.kt` | Foundational | NEW |
| `assistant/tools/CachedMcpTool.kt` | Foundational | NEW |
| `data/TokenManager.kt` | Foundational | Add cache CRUD + cleanup |
| `network/mcp/McpServerManager.kt` | Foundational, US1, US2, US4 | Add ensureConnected, connectAllWithCache, cache population, lazy mode |
| `network/mcp/McpClient.kt` | US2 | Split connect() |
| `assistant/presentation/AssistantViewModel.kt` | US1 | Cache-first startup flow |
| `presentation/settings/SettingsViewModel.kt` | US2, US3 | Refresh action, cached tools exposure |
| `ui/settings/ToolsTab.kt` | US2 | Refresh button |
| `ui/settings/McpServerCard.kt` | US3 | Render from cache |

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group per project convention
- Stop at any checkpoint to validate story independently
- Tests not listed as separate tasks — run `scripts/test-all.sh` before PR per project convention
- All file paths are relative to `app/src/main/kotlin/io/github/leogallego/ansiblejane/`

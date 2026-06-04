# Implementation Plan: MCP Tool Manifest Cache

**Branch**: `196-mcp-manifest-cache` | **Date**: 2026-06-03 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/196-mcp-manifest-cache/spec.md`
**GitHub Issue**: #223

## Summary

Add a client-side MCP tool manifest cache that polls servers once, persists the tool catalog per instance, and loads it on subsequent launches for instant tool availability in ToolRouter. The cache enables offline tool browsing in Settings > Tools and lazy server connections (connect only when a tool is actually invoked). No new dependencies required — uses existing Kotlin Serialization and DataStore infrastructure.

## Technical Context

**Language/Version**: Kotlin (current project version)
**Primary Dependencies**: Retrofit, Kotlin Serialization, Koin, Jetpack DataStore, Tink
**Storage**: Jetpack DataStore (Preferences) for manifest cache, same store as instance credentials
**Testing**: JUnit unit tests, Compose UI tests
**Target Platform**: Android (minSdk as configured in project)
**Project Type**: Mobile app (Android)
**Performance Goals**: Cache load < 500ms (SC-001), zero MCP network on cached launch (SC-004)
**Constraints**: < 100KB cache per instance, offline-capable for browsing, atomic writes
**Scale/Scope**: 1-10 MCP servers per instance, 5-50 tools per server, multiple instances

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Kotlin-Only | PASS | All new code is Kotlin |
| II. Compose-First UI | PASS | UI changes use Compose (McpServerCard, ToolsTab) |
| III. MVVM + UDF | PASS | Cache data flows through ViewModels via StateFlow |
| IV. Security-First | PASS | Manifest cache is non-sensitive metadata (tool names, descriptions, schemas). Credentials remain encrypted in DataStore + Tink |
| V. Lean Dependencies | PASS | Zero new dependencies. Uses existing Kotlin Serialization, DataStore |
| VI. API-Driven Design | N/A | MCP tools, not AAP REST API |

**Gate result: PASS** — no violations, no justifications needed.

## Project Structure

### Documentation (this feature)

```text
specs/196-mcp-manifest-cache/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0 research findings
├── data-model.md        # Phase 1 data model
├── quickstart.md        # Phase 1 quickstart guide
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/kotlin/io/github/leogallego/ansiblejane/
├── network/mcp/
│   ├── McpClient.kt              # MODIFY: extract version check from connect()
│   ├── McpServerManager.kt       # MODIFY: add cache-aware connection flow, lazy connect
│   └── McpSession.kt             # UNCHANGED
├── model/
│   ├── AapInstance.kt             # UNCHANGED
│   └── ToolManifest.kt            # NEW: ToolManifest, ServerToolCache data classes
├── data/
│   ├── TokenManager.kt           # MODIFY: add manifest cache CRUD, cleanup on instance delete
│   └── ManifestCacheManager.kt   # NEW: cache load/save/invalidate logic (optional, may inline in TokenManager)
├── assistant/
│   ├── tools/
│   │   ├── ToolSpec.kt            # UNCHANGED
│   │   ├── McpTool.kt            # MODIFY: extract WRITE_SUFFIXES and MAX_DESCRIPTION_CHARS to shared companion for reuse by CachedMcpTool
│   │   └── CachedMcpTool.kt      # NEW: metadata-only tool with lazy connect on execute()
│   ├── engine/
│   │   └── ToolRouter.kt         # UNCHANGED (routing uses only spec + isDestructive)
│   └── presentation/
│       └── AssistantViewModel.kt  # MODIFY: load cache before connect, defer connectAll()
├── presentation/settings/
│   └── SettingsViewModel.kt       # MODIFY: expose cached tools, add refresh action
└── ui/settings/
    ├── ToolsTab.kt                # MODIFY: add refresh button
    └── McpServerCard.kt           # MODIFY: render tools from cache when not connected

app/src/test/kotlin/
└── io/github/leogallego/ansiblejane/
    ├── model/
    │   └── ToolManifestTest.kt    # NEW: serialization round-trip tests
    ├── data/
    │   └── ManifestCacheTest.kt   # NEW: cache load/save/invalidate tests
    └── assistant/tools/
        └── CachedMcpToolTest.kt   # NEW: lazy connect, execute delegation tests
```

**Structure Decision**: Feature-based additions following existing package structure. New files are `ToolManifest.kt` (data model), `CachedMcpTool.kt` (cached tool implementation), and optionally `ManifestCacheManager.kt` (cache operations). Most work is modifications to existing files.

## Phase 0: Research Findings

Research completed inline (see [research.md](research.md) for full details). Key decisions:

### R1: CachedMcpTool design

**Decision**: Create `CachedMcpTool` class implementing `Tool` that holds metadata from cache and lazily acquires an `McpClient` on first `execute()`.

**Rationale**: Research confirmed that `ToolRouter.getToolsForQuery()` only accesses `spec.name`, `spec.description`, and `isDestructive` — never `execute()`. The `McpTool.client` field is only used inside `execute()`. Therefore a `CachedMcpTool` can provide full routing capability with zero client dependency, and trigger lazy connection only when the LLM actually calls the tool.

**Alternatives rejected**:
- Nullable client on McpTool: breaks the existing constructor contract and requires null checks throughout execute()
- Wrapper/proxy pattern: adds indirection without benefit since CachedMcpTool has a clear lifecycle (cached → connected)

### R2: Cache storage mechanism

**Decision**: Store manifest as a separate DataStore Preferences key per instance (`manifest_<instanceId>`), using the same `credentialsDataStore` as TokenManager.

**Rationale**: DataStore's `.edit {}` is transactional (atomic write). The spec requires atomic writes (FR-015), and DataStore already provides this. Keeping manifests in the same DataStore simplifies Koin module wiring and instance cleanup. Separate keys per instance avoid the "one giant JSON blob" problem and allow per-instance cache operations.

**Alternatives rejected**:
- Separate DataStore per instance: over-engineered for < 100KB payloads
- File-based JSON with write-then-rename: works but duplicates what DataStore already provides
- Room/SQLite: adds dependency, violates Lean Dependencies principle

### R3: Lazy connection trigger point

**Decision**: `CachedMcpTool.execute()` calls `McpServerManager.ensureConnected(serverLabel)` which connects if not already connected, then delegates to a real `McpClient.callTool()`.

**Rationale**: The connection must happen transparently at execution time (FR-009). Since `execute()` is already a suspend function, it can await the connection. `McpServerManager` already manages the `clients` map keyed by label — adding `ensureConnected()` is a natural extension.

### R4: AssistantViewModel startup flow change

**Decision**: Change from `connectAll() → register tools` to `loadCache() → register cached tools → background connectAll()`. The assistant enters Active state after cache load, not after server connection.

**Rationale**: The current flow blocks on `connectAll()` before transitioning to Active state (lines 88-95 of AssistantViewModel). With the cache, tools are available immediately. Background `connectAll()` validates cache freshness (version check) and updates if needed. If no cache exists, falls back to the current eager flow.

### R5: ToolRouter registration ordering

**Decision**: No changes to ToolRouter needed. The `registerLocalTools()` → `registerMcpTools()` ordering is already enforced in the `sendMessage()` flow (AssistantViewModel lines 160-161). Cached tools go through `registerMcpTools()` the same as live tools.

**Rationale**: `autoDisableOverlappingMcpTools()` runs on both registration calls. As long as local tools are registered first (which they already are), overlap detection works correctly regardless of whether MCP tools come from cache or live discovery.

## Phase 1: Design

### Data Model

See [data-model.md](data-model.md) for full entity definitions.

Core entities:
- `ToolManifest` — per-instance, contains list of `ServerToolCache`, schema version, timestamp
- `ServerToolCache` — per-server, contains `McpServerInfo` (for version-based invalidation), list of `McpToolDefinition`, server metadata
- `CachedMcpTool` — implements `Tool` interface using cached metadata, triggers lazy connect on `execute()`

### Contracts

No external API contracts added. This feature is entirely internal — it changes how existing MCP tools are cached and loaded, but exposes no new interfaces to users or external systems. The MCP protocol interactions (`initialize`, `tools/list`, `tools/call`) remain unchanged.

### Implementation Phases

#### Phase A: Manifest data model + cache storage (P1 foundation)

**Files**: `ToolManifest.kt`, `TokenManager.kt`
**FRs**: FR-001, FR-007, FR-008, FR-011, FR-015, FR-016
**Tests**: Serialization round-trip, save/load, instance deletion cleanup

1. Create `ToolManifest` and `ServerToolCache` data classes with `@Serializable`
2. Add `saveManifest()` / `loadManifest()` / `deleteManifest()` to TokenManager
3. Add cache cleanup to `removeInstance()`
4. Add server label uniqueness enforcement

#### Phase B: CachedMcpTool + lazy connect (P1 + P3 foundation)

**Files**: `CachedMcpTool.kt`, `McpServerManager.kt`
**FRs**: FR-002, FR-009, FR-013, FR-014
**Tests**: Metadata-only routing, lazy connect on execute, fallback on connect failure

1. Create `CachedMcpTool` implementing `Tool` from cached `McpToolDefinition`
   - `spec.description` MUST use the same `"[$serverLabel] $description"` format as `McpTool` — ToolRouter parses the server label from description brackets for read-only enforcement
   - `isDestructive` MUST use the same `WRITE_SUFFIXES` check as `McpTool`
2. Add `ensureConnected(serverLabel): McpClient` to McpServerManager
   - Looks up `McpServerConfig` from `currentInstance.mcpServerUrls` by label
   - If `clients[label]` already exists and is connected, returns it immediately
   - Otherwise, calls `connectServer(config, httpClient)` and returns the new client
   - Must use a per-label lock (e.g., `Mutex` per label in a `ConcurrentHashMap<String, Mutex>`) to prevent concurrent connection attempts to the same server from both `ensureConnected()` and background `connectAllWithCache()`
3. CachedMcpTool.execute() calls ensureConnected → delegates to `client.callTool()`
4. Handle connection failure in execute() (return ToolResult with CONNECTION_ERROR)

#### Phase C: Cache-aware connection flow (P1 core)

**Files**: `McpServerManager.kt`, `McpClient.kt`, `AssistantViewModel.kt`
**FRs**: FR-002, FR-003, FR-004, FR-012
**Tests**: Cache hit skips tools/list, version mismatch triggers refresh, config change invalidates

1. Split `McpClient.connect()` into two stages:
   - `initialize()`: performs MCP handshake, returns `McpServerInfo` (name + version)
   - `discoverTools()`: calls `tools/list`, populates session tools
   - Current `connect()` calls both sequentially (backward compatible)
2. Add cache-aware `connectAllWithCache()` to McpServerManager:
   - Load manifest from TokenManager
   - For each enabled server: call `initialize()` only, compare `serverInfo.version` against cached version
   - If version matches: skip `discoverTools()`, keep cached tools
   - If version differs: call `discoverTools()`, update cache
   - IMPORTANT: do NOT call `disconnectAll()` at the start — instead, merge new connections alongside cached tools. Replace cached tools with live tools per-server as connections complete
3. Modify AssistantViewModel init block:
   - Load manifest → register cached tools as `CachedMcpTool` instances → transition to Active
   - Launch background coroutine: `connectAllWithCache()` → update cache and swap live tools in
   - If no cache exists: fall back to current eager `connectAll()` flow
4. Ensure `getAllTools()` never returns empty during the cached→live transition:
   - Cached tools remain in `mcpTools` list until replaced by live tools from the same server
   - Use per-server swap (remove cached for label X, add live for label X) instead of clear-all

#### Phase D: UI updates for offline browsing + refresh (P2)

**Files**: `McpServerCard.kt`, `ToolsTab.kt`, `SettingsViewModel.kt`
**FRs**: FR-005, FR-006, FR-010, FR-017
**Tests**: Tools visible when disconnected, refresh button works, concurrent refresh ignored

1. SettingsViewModel: expose cached tools alongside connection-based tools
2. McpServerCard: render tool list from cache when not connected
3. ToolsTab: add "Refresh tools" button
4. Guard concurrent refresh with a Mutex or AtomicBoolean

## Post-Phase 1 Constitution Re-Check

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Kotlin-Only | PASS | All new code is Kotlin |
| II. Compose-First UI | PASS | UI changes are Compose (McpServerCard, ToolsTab) |
| III. MVVM + UDF | PASS | Cached tools flow through SettingsViewModel/AssistantViewModel StateFlow |
| IV. Security-First | PASS | Cache stores non-sensitive metadata. No credentials in cache |
| V. Lean Dependencies | PASS | Zero new dependencies added |
| VI. API-Driven Design | N/A | MCP protocol, not AAP REST API |

**Gate result: PASS** — design maintains all constitution principles.

## Complexity Tracking

No violations to justify. The feature adds:
- 2-3 new files (ToolManifest.kt, CachedMcpTool.kt, optionally ManifestCacheManager.kt)
- Modifications to 6 existing files
- No new dependencies, patterns, or abstractions beyond what exists

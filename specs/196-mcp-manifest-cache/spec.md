# Feature Specification: MCP Tool Manifest Cache

**Feature Branch**: `196-mcp-manifest-cache`
**Created**: 2026-06-03
**Status**: Draft
**Input**: User description: "MCP tool manifest cache: progressive discovery + programmatic tool calling. See GitHub issue #223 for full context."
**GitHub Issue**: #223

## Clarifications

### Session 2026-06-03

- Q: Should the programmatic API cover all tools (local + MCP) or MCP-only? → A: MCP-only (Option A), but after further discussion, programmatic tool calling was descoped entirely. The app is chat-first: UI shortcuts already exist via Retrofit for common actions (templates, inventory, activity), and multi-step orchestration doesn't fit the current interface. The manifest cache value is startup speed and offline UX, not a non-LLM execution path.
- Q: Should cached tool filtering use single-pass or two-pass (names-first) to save tokens? → A: Single-pass (Option A). Analysis showed the cache provides near-zero direct token savings — tokens are consumed when ChatEngine sends schemas to the LLM, which happens identically with or without cache. The cache's value is eliminating 1-3 seconds of MCP discovery latency per launch and enabling offline tool browsing.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Instant tool availability on app launch (Priority: P1)

A user opens Ansible Jane, selects their AAP instance, and navigates to the AI assistant. The app already knows what MCP tools are available from a previous session. The user can immediately ask questions that require tools without waiting for server connections to complete. Tool counts and categories appear instantly in Settings > Tools.

**Why this priority**: This is the core value proposition. Without caching, every app launch requires a full round-trip to every MCP server before tools become usable. On slow networks or with multiple servers, this creates a multi-second delay before the assistant is fully functional.

**Independent Test**: Can be tested by connecting to an MCP server once, closing the app, reopening it in airplane mode, and verifying that tool metadata (names, descriptions, categories) is visible in the Tools tab and available to the ToolRouter for query matching.

**Acceptance Scenarios**:

1. **Given** the user previously connected to an AAP instance with MCP servers, **When** they reopen the app and select that instance, **Then** the cached tool catalog is loaded and available to ToolRouter within 1 second, without any network requests to MCP servers.
2. **Given** cached tools exist for an instance, **When** the user opens Settings > Tools, **Then** they see tool counts per server, tool names, and descriptions immediately, even if the MCP servers are currently unreachable.
3. **Given** no cached tools exist (first-ever connection), **When** the user connects to an AAP instance with MCP enabled, **Then** the app fetches tools from all servers, caches the results, and uses the cache for all subsequent launches.
4. **Given** no cached tools exist (first-ever connection) and 2 of 3 servers succeed while 1 fails, **When** the connection completes, **Then** the cache is populated with tools from the 2 successful servers, the failed server is marked as uncached, and the user is informed which server failed.

---

### User Story 2 - Cache invalidation and refresh (Priority: P1)

A user's AAP administrator deploys updated MCP tools on the server (new tools added, existing tools modified, tools removed). When the app connects to the server, it detects that the server version has changed and automatically refreshes the cached tool catalog. The user can also manually trigger a refresh from the Tools settings tab.

**Why this priority**: A stale cache is worse than no cache if it causes the assistant to reference tools that no longer exist or miss new capabilities. Reliable invalidation is essential for trust.

**Independent Test**: Can be tested by connecting to a server (populating cache), modifying the server's tool list or version, reconnecting, and verifying the cache updates. Can also be tested by tapping a refresh button in Tools settings and verifying tools are re-fetched.

**Acceptance Scenarios**:

1. **Given** a cached manifest exists for server "operations" with server version "1.0", **When** the app connects and the server now reports version "1.1", **Then** the app re-fetches the full tool list from that server and updates the cache.
2. **Given** a cached manifest exists, **When** the user changes MCP server configuration (URL, toolset, enabled/disabled), **Then** the cache for that server is invalidated and re-fetched on next connect.
3. **Given** a cached manifest exists, **When** the user taps "Refresh tools" in Settings > Tools, **Then** all server tool lists are re-fetched regardless of version, the cache is updated, and a confirmation is shown.
4. **Given** a cached manifest exists but a server is unreachable during refresh, **When** the refresh fails for that server, **Then** the existing cache for that server is preserved (not cleared), and the user is informed which servers failed to refresh.
5. **Given** a refresh is already in progress, **When** the user taps "Refresh tools" again, **Then** the second request is ignored (not queued), and the in-progress refresh continues to completion.
6. **Given** a cached manifest exists and a server is reachable for version check but fails on tool list fetch, **When** the version check shows a change, **Then** the existing cache for that server is preserved (not cleared), the failure is reported, and the user can retry.

---

### User Story 3 - Offline tool browsing (Priority: P2)

A user wants to understand what MCP tools are available for their AAP instance before writing prompts or while disconnected from the network. They open Settings > Tools and browse the full tool catalog from the cached manifest.

**Why this priority**: Understanding available tools is essential for effective prompt writing. Currently, tool information is only available while connected. Offline browsing reduces friction and enables learning.

**Independent Test**: Can be tested by populating the cache while online, enabling airplane mode, and navigating to Settings > Tools to verify all tool metadata is browsable.

**Acceptance Scenarios**:

1. **Given** a cached manifest exists and the device is offline, **When** the user opens Settings > Tools, **Then** they see the full tool catalog with names, descriptions, server labels, and toolset groupings.
2. **Given** a cached manifest exists, **When** the user taps on a specific tool in the catalog, **Then** they see the tool's parameter schema (input fields, types, required/optional) rendered in a readable format.

---

### User Story 4 - Lazy server connection (Priority: P3)

When the app launches with a cached manifest, it does not immediately connect to every MCP server. Instead, it defers connections until a tool from that server is actually invoked. This reduces startup time and network usage, especially when the user only interacts with a subset of available servers.

**Why this priority**: Optimization that builds on the manifest cache. Important for users with many configured servers, but only valuable after caching (P1) is solid.

**Independent Test**: Can be tested by configuring 3 MCP servers, launching the app, verifying no MCP connections are made, then asking a question that triggers a tool from server 1 and verifying only server 1 connects.

**Acceptance Scenarios**:

1. **Given** a cached manifest exists for 3 MCP servers, **When** the app launches and loads the cache, **Then** zero MCP server connections are initiated.
2. **Given** tools from the cached manifest are registered in ToolRouter, **When** the user asks a question that matches tools from server "operations" only, **Then** only the "operations" server is connected on-demand, and the other servers remain disconnected.
3. **Given** a lazy connection attempt fails, **When** the tool invocation triggers the connection, **Then** the user sees an error for that specific tool call, and the system falls back to any available local tools in the same category.
4. **Given** a query matches tools from 2 different MCP servers, **When** both servers need lazy connections, **Then** both connections are initiated in parallel. If one succeeds and one fails, the successful server's tools execute normally, the failed server's tools return errors, and local fallback tools are used for the failed server's category.

---

### Edge Cases

- What happens when the cached manifest references tools that no longer exist on the server? The cache is a hint for UI and routing; tool invocation must handle "tool not found" errors gracefully from the server.
- What happens when two instances share the same MCP server URL but different auth? Each instance must have its own independent cache entry, keyed by instance ID.
- What happens when the app is upgraded and the manifest data model changes? The cache must include a schema version; incompatible versions trigger a full re-fetch rather than a crash.
- What happens when the user switches instances rapidly? Cache loading must be scoped to the active instance; switching instances cancels any in-flight cache operations for the previous instance.
- What happens when a server returns an empty tool list? An empty list is a valid cache entry (the server exists but exposes no tools); it must not be treated as a cache miss.
- What happens when the cache file is corrupted or unreadable? The system must treat it as a cache miss and re-fetch from servers, logging the corruption for diagnostics.
- What happens when a user deletes an AAP instance? The cached manifest for that instance must be deleted alongside the instance credentials. Orphaned caches must not accumulate.
- What happens when auto-detection discovers new or changed MCP server endpoints (e.g., after an AAP upgrade)? Auto-detection changes to the server list must trigger cache invalidation for affected servers, the same as manual config changes.
- What happens when the user tries to invoke a cached tool before the server is connected? The system must transparently connect to the specific server (lazy connect), attach a live client to the cached tool, and then execute. If the connection fails, the user sees an error for that tool call.
- What happens when cached MCP tools overlap with local tools? The overlap check (which disables MCP duplicates of local tools) must run after both local tools and cached tools are registered. Local tools must always be registered first to guarantee correct overlap detection.
- What happens when two MCP servers are configured with the same label (e.g., manual and auto-detected)? Server labels must be unique per instance. If a duplicate is detected, the auto-detected server yields to the manual one. The cache keys by server label, so duplicates would cause data loss.
- What happens when the app is killed while writing the manifest cache? The write must be atomic (write to temp file, then rename). A partially written cache file must be treated as corruption on next read — a cache miss, not a crash.
- What happens when an existing user upgrades to a version with manifest caching? No migration is needed. The first launch after upgrade has no cache (cache miss), so the app connects normally and populates the cache. Identical to a first-ever connection.
- What happens when a refresh is already in progress and the user triggers another? The second request is ignored. Only one refresh can be in-flight at a time per instance.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST persist the complete MCP tool catalog (tool names, descriptions, and parameter schemas) per instance after the first successful connection to each MCP server.
- **FR-002**: The system MUST load the cached tool catalog on app launch and make it available to the tool routing system before any MCP server connections are established.
- **FR-003**: The system MUST invalidate the cache for a specific server when the server's reported version changes between connections.
- **FR-004**: The system MUST invalidate the cache for a specific server when the user modifies that server's configuration (URL, toolset, enabled state).
- **FR-005**: The system MUST provide a manual "refresh tools" action in the Tools settings that re-fetches all server tool lists regardless of version.
- **FR-006**: The system MUST preserve the existing cache when a refresh fails for a specific server, rather than clearing it.
- **FR-007**: The system MUST scope cached manifests to individual AAP instances, so multiple instances maintain independent caches.
- **FR-008**: The system MUST include a cache schema version so that data model changes trigger a full re-fetch instead of deserialization errors.
- **FR-009**: The system MUST support lazy server connections, deferring MCP server connections until a tool from that server is actually invoked, when a valid cache exists. The connection must be triggered transparently at tool execution time, not at tool registration time.
- **FR-010**: The system MUST display cached tool information (names, descriptions, server groupings) in the Tools settings tab without requiring an active server connection. The UI must render cached tool data independently of connection state.
- **FR-011**: The system MUST delete cached manifests when the associated AAP instance is removed.
- **FR-012**: The system MUST treat auto-detection changes to the MCP server list as configuration changes that trigger cache invalidation for affected servers.
- **FR-013**: Cached tools MUST be metadata-only (name, description, schema for routing and display). Tool execution MUST go through a live server connection. Cached tools that have not yet established a connection MUST trigger a lazy connect on first invocation.
- **FR-014**: The system MUST guarantee that local tool registration completes before cached MCP tools are registered, so that overlap detection (which disables MCP duplicates of local tools) works correctly.
- **FR-015**: The system MUST write manifest cache atomically (write-then-rename) to prevent partial writes from corrupting the cache if the app is interrupted.
- **FR-016**: The system MUST enforce unique server labels per instance. If a duplicate label is detected (e.g., manual server matching an auto-detected label), the manual configuration takes precedence.
- **FR-017**: The system MUST ignore concurrent refresh requests. Only one refresh operation may be in-flight at a time per instance.

### Key Entities

- **Tool Manifest**: A per-instance collection of server tool caches. Contains the instance identifier, a schema version for forward compatibility, and a timestamp of when the cache was last populated.
- **Server Tool Cache**: A per-server snapshot of discovered tools. Contains the server URL, label, toolset identifier, server version info (used for invalidation), the list of tool definitions, and the server's read-only flag.
- **Tool Definition**: An individual tool's metadata: name, description, and input parameter schema. Already exists in the codebase.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On subsequent app launches with a cached manifest, the tool catalog is available for browsing and routing within 500 milliseconds, without any network requests.
- **SC-002**: Cache invalidation correctly detects server changes in 100% of cases where the server version string differs from the cached version.
- **SC-003**: Users can browse the complete tool catalog (names, descriptions, parameter details) while the device is offline, provided a cache was previously populated.
- **SC-004**: App startup with lazy connections makes zero MCP server network requests when a valid cache exists, reducing startup network traffic to zero for MCP.

## Constraints

- The current tool execution model requires a live client reference (the tool object holds a connection handle). Cached tools must separate metadata (for routing and display) from execution (which requires a connection). This means either a two-phase tool object (metadata-only until connected) or a proxy that triggers lazy connection on first `execute()` call.
- The current connection lifecycle is eager: the assistant screen blocks on `connectAll()` when an instance is selected. Lazy connections require changing this to: load cache → register tools → defer connections. This is a behavioral change to the assistant startup flow.
- The current Tools UI only renders tool lists when the server is in a Connected state. Offline tool browsing requires the UI to accept a second data source (cached metadata) independent of connection state.
- Manifest cache storage should be kept under 100KB per instance to avoid DataStore performance issues. With typical MCP deployments (5-10 servers, 5-15 tools each, ~500 bytes per tool definition), this should fit comfortably. If an instance exceeds this, the system should log a warning but not fail.

## Assumptions

- MCP servers report a `serverInfo.version` string in their `initialize` response that changes when their tool catalog is modified. If a server does not change its version when tools change, the user must use manual refresh.
- The cached tool parameter schemas are sufficient for ToolRouter query matching and UI display. Full schema validation still happens server-side when tools are invoked.
- The manifest cache does not need encryption: tool metadata (names, descriptions, schemas) is not sensitive. Instance credentials remain encrypted separately.
- Lazy connections require that the system transparently handles the MCP `initialize` handshake when a tool is first invoked from a previously-unconnected server.
- The existing per-tool enable/disable state (managed by ToolRouter) is independent of the manifest cache. The cache stores what the server offers; enable/disable is a user-side overlay.
- Local tools (61 Retrofit-backed tools) are always available without caching — they don't depend on MCP connections. The manifest cache only covers MCP tools.
- No maximum cache age (TTL) is enforced. Invalidation is version-based and manual-refresh only. If the server never changes its version string, the cache persists indefinitely. This is acceptable because stale metadata only affects routing and UI display — tool execution always hits the live server, which will reject invalid calls.
- No staleness indicator is shown in the UI. The Tools tab does not distinguish cached vs. live data. This is acceptable for a chat-first app where the user interacts with tools through the LLM, not by inspecting schemas. If users request it, a "last refreshed" timestamp can be added later.
- No automatic refresh on network reconnection. If the user was offline and comes back online, the cached data remains until the user manually refreshes or the app reconnects to the instance (which triggers a version check).

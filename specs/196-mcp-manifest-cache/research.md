# Research: MCP Tool Manifest Cache

**Feature**: 196-mcp-manifest-cache | **Date**: 2026-06-03

## R1: Can tools exist without a live MCP client?

**Question**: Can a Tool implementation participate in ToolRouter routing without an active MCP server connection?

**Finding**: YES. The `Tool` interface requires three members:
- `val spec: ToolSpec` (name, description, parametersSchema)
- `val isDestructive: Boolean` (defaults to false)
- `suspend fun execute(args: JsonObject): ToolResult`

`ToolRouter.getToolsForQuery()` only accesses `spec.name`, `spec.description`, and `isDestructive` during routing. It never calls `execute()`. In `McpTool`, the `client` field is only used inside `execute()` at line 38 (`client.callTool()`). Properties `spec` and `isDestructive` are computed at construction from `McpToolDefinition` — no client dependency.

**Decision**: Create `CachedMcpTool` that implements `Tool` from cached metadata. `execute()` triggers lazy connection via `McpServerManager.ensureConnected()`.

**Source files**:
- `assistant/tools/ToolSpec.kt:11-25` (Tool interface)
- `assistant/tools/McpTool.kt:13-68` (client usage pattern)
- `assistant/engine/ToolRouter.kt:295-357` (routing property access)

---

## R2: Where to store the manifest cache?

**Question**: DataStore Preferences vs. file-based JSON vs. separate database?

**Finding**: DataStore Preferences is the right choice.
- `TokenManager` already uses `preferencesDataStore("credentials")` with atomic `.edit {}` transactions
- Current pattern: serialize complex state as JSON string, store in a single preference key
- DataStore handles concurrent writes safely (internal serialization)
- FR-015 requires atomic writes — DataStore provides this natively

**Size analysis**:
- Typical: 7 servers × 10 tools × 500 bytes = 35KB per instance
- Max reasonable: 10 servers × 50 tools × 500 bytes = 250KB (exceeds 100KB constraint — log warning)
- DataStore practical limit: ~500KB per preference before performance degrades
- Mitigation: separate preference key per instance (`manifest_<instanceId>`)

**Decision**: Use same `credentialsDataStore`, separate key per instance. Avoids new DataStore/dependencies.

**Alternatives rejected**:
- Separate DataStore: over-engineered, adds Koin complexity
- File-based JSON: duplicates DataStore's atomic write behavior
- Room/SQLite: new dependency, violates Lean Dependencies

**Source files**:
- `data/TokenManager.kt:32-34` (DataStore setup)
- `data/TokenManager.kt:147-153` (atomic write pattern)

---

## R3: How should lazy connections be triggered?

**Question**: Where in the execution pipeline does the lazy connect happen?

**Finding**: The natural trigger is `CachedMcpTool.execute()`.

Current execution flow:
1. User message → `AssistantViewModel.sendMessage()`
2. ToolRouter.getToolsForQuery() → selects tools (metadata only)
3. ChatEngine sends tool schemas to LLM
4. LLM returns tool call → `ToolExecutor.execute(toolCall)`
5. Tool.execute(args) → `McpClient.callTool()`

With lazy connect, step 5 becomes:
5a. `CachedMcpTool.execute(args)` → calls `McpServerManager.ensureConnected(serverLabel)`
5b. If not connected: `connectServer()` (initialize + tools/list)
5c. Delegate to `McpClient.callTool()` using the now-connected client

`ensureConnected()` is a natural addition to `McpServerManager` which already manages the `clients: ConcurrentHashMap<String, McpClient>` map.

**Decision**: Lazy connect triggered in `CachedMcpTool.execute()` via `McpServerManager.ensureConnected()`.

**Additional finding**: `ensureConnected(serverLabel)` must look up the `McpServerConfig` from `currentInstance.mcpServerUrls` by label to get the URL and auth settings. `McpServerManager` already stores `currentInstance` (line 27), so this lookup is internal — CachedMcpTool only needs to pass the label.

**Source files**:
- `assistant/presentation/AssistantViewModel.kt:153-238` (sendMessage flow)
- `network/mcp/McpServerManager.kt:24` (clients map)
- `network/mcp/McpServerManager.kt:27` (currentInstance reference)
- `network/mcp/McpServerManager.kt:54-77` (connectServer)

---

## R4: McpClient.connect() split for version-only check

**Question**: How to check server version without re-fetching the full tool list?

**Finding**: The current `McpClient.connect()` (lines 30-86) does `initialize` + `tools/list` in a single method. To support cache-aware connections, we need to separate these:
- `initialize()`: MCP handshake, returns `McpServerInfo` (name, version)
- `discoverTools()`: calls `tools/list`, populates session tools
- `connect()`: calls both (backward compatible for non-cache paths)

This split allows the cache-aware flow to: initialize → compare version → skip discoverTools if cache is valid.

**Decision**: Split `connect()` into `initialize()` + `discoverTools()`. Keep `connect()` as convenience that calls both.

**Source files**:
- `network/mcp/McpClient.kt:30-86` (current connect method)
- `network/mcp/McpClient.kt:92-113` (current listTools method)

---

## R5: disconnectAll() race condition with cached tools

**Question**: `connectAll()` calls `disconnectAll()` (line 31) which clears `mcpTools`. Won't this wipe cached tools during background reconnection?

**Finding**: YES — this is a critical issue. The current flow:
1. `connectAll()` calls `disconnectAll()` → clears `mcpTools` list
2. Re-connects each server → repopulates `mcpTools`
3. Between steps 1 and 2, `getAllTools()` returns empty

With cached tools, this gap means the LLM loses access to all MCP tools during background refresh. This is unacceptable.

**Decision**: The cache-aware `connectAllWithCache()` must NOT call `disconnectAll()`. Instead, it must swap tools per-server: for each server that reconnects, remove its old tools (cached or live) from `mcpTools` and add the new live tools. This ensures `getAllTools()` never returns empty — cached tools remain available until replaced by their live equivalents.

**Source files**:
- `network/mcp/McpServerManager.kt:29-31` (connectAll calls disconnectAll)
- `network/mcp/McpServerManager.kt:79-86` (disconnectAll clears mcpTools)

---

## R6: How to change the AssistantViewModel startup flow?

**Question**: How to transition from eager `connectAll()` to cache-first startup?

**Finding**: Current flow (AssistantViewModel lines 83-101):
```
activeInstance changes → Loading → connectAll() → Active
```

With cache:
```
activeInstance changes → loadManifest() → register cached tools → Active
                       └→ background: connectAll() → version check → update cache if stale
```

The key insight: tool registration happens in `sendMessage()` (lines 153-161), not during init. So the startup flow only needs to:
1. Load cache
2. Make cached tools available to `mcpServerManager.getAllTools()`
3. Transition to Active state

When `sendMessage()` runs, it calls `mcpServerManager.getAllTools()` which returns either cached tools (if background connect hasn't finished) or live tools (if it has). Either way, routing works.

**Decision**: Modify init block to load cache first, then background connectAll(). No changes to sendMessage flow.

**Source files**:
- `assistant/presentation/AssistantViewModel.kt:83-101` (init flow)
- `assistant/presentation/AssistantViewModel.kt:153-161` (sendMessage registration)

---

## R7: Does tool registration ordering work with cached tools?

**Question**: Will overlap detection break if cached tools are registered before/after local tools?

**Finding**: No issue. The registration flow in `sendMessage()` is:
```kotlin
toolRouter.registerLocalTools(localTools)      // line 160
toolRouter.registerMcpTools(mcpTools)           // line 161
```

`registerMcpTools()` calls `autoDisableOverlappingMcpTools()` which checks registered local tool names against `OVERLAP_MAPPING`. This works identically for cached and live MCP tools — the overlap check is name-based, not client-based.

The ordering guarantee is already enforced by sequential execution in `sendMessage()`. No additional synchronization needed.

**Decision**: No changes to ToolRouter. Cached tools participate in overlap detection identically to live tools.

**Source files**:
- `assistant/engine/ToolRouter.kt:243-253` (registration methods)
- `assistant/engine/ToolRouter.kt:366-379` (overlap detection)

---

## R8: Concurrent connection race between ensureConnected() and connectAllWithCache()

**Question**: Can `ensureConnected()` (triggered by CachedMcpTool.execute()) and background `connectAllWithCache()` race to connect the same server simultaneously?

**Finding**: YES. Both paths call `connectServer()` which creates a new `McpClient` and stores it in `clients[label]`. If both run concurrently for the same label:
1. Path A creates `McpClient` A, stores in `clients[label]`
2. Path B creates `McpClient` B, overwrites `clients[label]`
3. Client A is orphaned (never disconnected), leaking resources

`ConcurrentHashMap.put()` is thread-safe for the map operation, but the connect-then-store sequence is not atomic.

**Decision**: Add a per-label `Mutex` in a `ConcurrentHashMap<String, Mutex>`. Both `ensureConnected()` and `connectAllWithCache()` must acquire the mutex for a label before checking `clients[label]` and potentially connecting. The first to acquire connects; the second sees the connected client and returns immediately.

**Source files**:
- `network/mcp/McpServerManager.kt:24` (clients ConcurrentHashMap)
- `network/mcp/McpServerManager.kt:54-77` (connectServer, not synchronized)

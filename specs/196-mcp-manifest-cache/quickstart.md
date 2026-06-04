# Quickstart: MCP Tool Manifest Cache

**Feature**: 196-mcp-manifest-cache | **Date**: 2026-06-03

## What this feature does

Caches MCP tool metadata (names, descriptions, parameter schemas) locally so the app doesn't need to re-discover tools from MCP servers on every launch. Tools are available instantly from cache, and servers are connected lazily (only when a tool is actually invoked).

## Build phases

### Phase A: Data model + storage
Create `ToolManifest` data classes and add cache CRUD to TokenManager.

**Key files to create/modify**:
- `model/ToolManifest.kt` — new data classes
- `data/TokenManager.kt` — add `saveManifest()`, `loadManifest()`, `deleteManifest()`

**Verify**: Write a unit test that saves a manifest, loads it back, and confirms round-trip serialization.

### Phase B: CachedMcpTool + lazy connect
Create the cached tool implementation that provides metadata for routing and triggers lazy connections on execution.

**Key files to create/modify**:
- `assistant/tools/CachedMcpTool.kt` — new Tool implementation
- `network/mcp/McpServerManager.kt` — add `ensureConnected()`

**Verify**: Write a unit test that creates a CachedMcpTool, confirms routing properties work without a client, and confirms execute() triggers connection.

### Phase C: Cache-aware connection flow
Modify the startup and connection flow to check cache before connecting.

**Key files to modify**:
- `network/mcp/McpServerManager.kt` — cache-first in `connectAll()`
- `assistant/presentation/AssistantViewModel.kt` — load cache, defer connections

**Verify**: Launch app with airplane mode after populating cache. Send a message in the assistant — ToolRouter should match and return cached MCP tools (visible in logcat ROUTE debug output). Tools won't appear in Settings > Tools UI until Phase D.

### Phase D: UI updates
Add offline tool display and manual refresh.

**Key files to modify**:
- `presentation/settings/SettingsViewModel.kt` — expose cached tools
- `ui/settings/McpServerCard.kt` — show tools when disconnected
- `ui/settings/ToolsTab.kt` — add refresh button

**Verify**: Browse tools in Settings > Tools while offline. Tap refresh while online and confirm tools update.

## Testing strategy

- **Unit tests**: Serialization, cache operations, CachedMcpTool behavior, version comparison
- **Integration tests**: Full cache lifecycle (populate → reload → invalidate → refresh)
- **Manual device tests**: Airplane mode browsing, lazy connection on first query, refresh button

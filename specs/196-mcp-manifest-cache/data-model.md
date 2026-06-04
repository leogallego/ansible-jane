# Data Model: MCP Tool Manifest Cache

**Feature**: 196-mcp-manifest-cache | **Date**: 2026-06-03

## Entities

### ToolManifest

Per-instance collection of cached server tool data.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| schemaVersion | Int | Yes | Cache format version. Increment on breaking changes. Start at 1. Incompatible versions trigger full re-fetch. |
| instanceId | String | Yes | Unique identifier of the AAP instance this cache belongs to. |
| servers | List\<ServerToolCache\> | Yes | One entry per MCP server. Empty list is valid (instance has no MCP servers). |
| cachedAt | Long | Yes | Unix timestamp (millis) of when the cache was last fully populated. Informational only — not used for TTL. |

**Identity**: Keyed by `instanceId`. One manifest per instance.
**Lifecycle**: Created on first successful MCP connection. Updated on version mismatch or manual refresh. Deleted when instance is removed.

### ServerToolCache

Per-server snapshot of discovered tools.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| serverUrl | String | Yes | MCP server URL used for connection. |
| label | String | Yes | Unique server label within the instance (used as key). |
| toolset | String? | No | Toolset identifier (e.g., "job_management") for category-based routing. |
| serverInfo | McpServerInfo | Yes | Server name and version string from `initialize` response. Used for invalidation. |
| tools | List\<McpToolDefinition\> | Yes | Complete tool list from `tools/list`. Empty list is valid. |
| readOnly | Boolean | Yes | Whether this server is configured as read-only (filters write actions). |

**Identity**: Keyed by `label` within a manifest. Labels must be unique per instance.
**Lifecycle**: Created when server connection succeeds. Updated when server version changes. Removed when server is removed from instance config.

### McpToolDefinition (existing)

Individual tool metadata. Already exists in `network/mcp/McpTypes.kt`.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Tool name (e.g., "list_job_templates"). |
| description | String | No | Human-readable description. Defaults to empty string. |
| inputSchema | JsonObject | No | JSON Schema for tool parameters. Defaults to empty object. |

**Identity**: Keyed by `name` within a server.
**Lifecycle**: Immutable snapshot. Replaced entirely on cache refresh.

### McpServerInfo (existing)

Server identity from MCP `initialize` response. Already exists in `network/mcp/McpTypes.kt`.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| name | String | Yes | Server name (e.g., "aap-mcp-server"). |
| version | String | Yes | Server version string. Changes trigger cache invalidation. |

### CachedMcpTool (new, not persisted)

In-memory representation of a cached tool that implements the `Tool` interface for routing. Not serialized — constructed from `McpToolDefinition` + manifest metadata at cache load time.

| Field | Type | Description |
|-------|------|-------------|
| mcpToolDef | McpToolDefinition | Cached tool definition (name, description, schema). |
| serverLabel | String | Which server provides this tool. |
| toolset | String? | Toolset for category routing. |
| readOnly | Boolean | From ServerToolCache. |
| serverManager | McpServerManager | Reference for lazy connection on execute(). |

**Behavior**:
- `spec`: Computed from `mcpToolDef` at construction. Description MUST use the format `"[$serverLabel] $description"` — identical to McpTool. ToolRouter parses server label from the bracket prefix for read-only enforcement.
- `isDestructive`: Computed from tool name patterns using the same `WRITE_SUFFIXES` list as McpTool. Must share the constant, not duplicate it.
- `execute(args)`: Calls `serverManager.ensureConnected(serverLabel)` to get a live `McpClient`, then delegates to `client.callTool()`. Returns error ToolResult with `ErrorType.CONNECTION_ERROR` if connection fails.

**Invariants**:
- A CachedMcpTool must be indistinguishable from an McpTool when accessed through the `Tool` interface (spec, isDestructive). Only `execute()` behavior differs (lazy connect vs. immediate).
- The `serverManager` reference is NOT serialized — CachedMcpTool is constructed at cache load time, not deserialized from storage.

## Relationships

```
AapInstance (1) ──── (0..1) ToolManifest
                              │
                              ├── (0..*) ServerToolCache
                              │            │
                              │            └── (0..*) McpToolDefinition
                              │            │
                              │            └── (1) McpServerInfo
                              │
                              └── schemaVersion, cachedAt

McpServerConfig (instance config) ←── validates against ──→ ServerToolCache (cached data)
   - URL must match
   - Label must match
   - Config changes invalidate cache
```

## State Transitions

### Manifest Lifecycle

```
[No Cache] ──(first connect succeeds)──→ [Cached]
                                              │
                                    ┌─────────┼─────────┐
                                    ↓         ↓         ↓
                              [Version    [Config    [Manual
                               Mismatch]  Changed]   Refresh]
                                    │         │         │
                                    └────┬────┘         │
                                         ↓              ↓
                                   [Re-fetching] ←──────┘
                                         │
                                    ┌────┴────┐
                                    ↓         ↓
                              [Updated]  [Failed → keep old cache]
                                    │
                                    ↓
                               [Cached]

[Cached] ──(instance deleted)──→ [Deleted]
[Cached] ──(schema version mismatch)──→ [No Cache] ──→ (re-fetch)
[Cached] ──(corrupt/unreadable)──→ [No Cache] ──→ (re-fetch)
```

### CachedMcpTool Execution States

```
[Metadata-Only] ──(execute() called)──→ [Connecting]
                                              │
                                         ┌────┴────┐
                                         ↓         ↓
                                   [Connected]  [Failed]
                                         │         │
                                         ↓         ↓
                                   [Executing]  [Error ToolResult]
                                         │
                                         ↓
                                   [ToolResult]
```

## Storage Schema

**DataStore key pattern**: `manifest_<instanceId>`
**Value**: JSON string of `ToolManifest`
**Location**: Same `credentialsDataStore` as instance credentials

Example key: `manifest_aaplm-ansible-ar-8443`

**Size budget**: < 100KB per instance. Log warning if exceeded.

# Contract: MCP Client

**Exposes**: Internal API for MCP server communication
**Consumers**: ChatEngine (via ToolExecutor), AssistantViewModel (connection state)

## McpClient

```kotlin
class McpClient(
    private val config: McpServerConfig,
    private val httpClient: OkHttpClient,
    private val json: Json
) {
    val connectionState: StateFlow<McpConnectionState>
    val tools: StateFlow<List<McpToolDefinition>>

    suspend fun connect()
    suspend fun disconnect()
    suspend fun listTools(): List<McpToolDefinition>
    suspend fun callTool(name: String, arguments: JsonObject): McpToolResult
}
```

### connect()
- POST `initialize` JSON-RPC to `config.url`
- Store `Mcp-Session-Id` from response header (if present)
- POST `notifications/initialized` notification
- Call `listTools()` to populate tool cache
- Emit `McpConnectionState.Connected`
- On failure: emit `McpConnectionState.Error`, throw `McpConnectionException`

### disconnect()
- If session ID exists: HTTP DELETE to `config.url` with session header
- Clear session state and tool cache
- Emit `McpConnectionState.Disconnected`

### listTools()
- POST `tools/list` JSON-RPC
- Parse response into `List<McpToolDefinition>`
- Cache in `tools` StateFlow
- Handle pagination via `nextCursor` (if present)

### callTool(name, arguments)
- POST `tools/call` JSON-RPC with `{"name": name, "arguments": arguments}`
- Handle both JSON and SSE responses (check Content-Type)
- Return `McpToolResult` (content list + isError flag)
- Map exceptions to structured errors:
  - IOException → CONNECTION_ERROR
  - HTTP 401/403 → AUTH_ERROR
  - HTTP 404 → NOT_FOUND
  - SocketTimeoutException → TIMEOUT
  - HTTP 500+ → SERVER_ERROR

## McpServerManager

```kotlin
class McpServerManager(
    private val httpClientFactory: (AapInstance) -> OkHttpClient,
    private val json: Json
) {
    val connections: StateFlow<Map<String, McpConnectionState>>

    suspend fun connectAll(instance: AapInstance)
    suspend fun disconnectAll()
    fun getAllTools(): List<McpTool>
}
```

### connectAll(instance)
- For each `McpServerConfig` in `instance.mcpServerUrls` where `enabled == true`:
  - Create `McpClient` with OkHttpClient from factory (reuses auth interceptor + TLS config)
  - Connect in parallel via `coroutineScope { async {} }`
  - Isolate per-server failures (one failing server doesn't block others)
- Wrap discovered tools as `McpTool` instances

### getAllTools()
- Returns flat list of `McpTool` from all connected servers
- Tool descriptions prefixed with server label: `[operations] List failed jobs`
- No naming collisions expected (aap-mcp-server uses `controller.*` prefix, ansible-know uses `search_modules`)

## Error Behavior

| Scenario | Behavior |
|----------|----------|
| Server unreachable on connect | McpConnectionState.Error, other servers still connect |
| Server unreachable on tool call | ToolResult(success=false, errorType=CONNECTION_ERROR) |
| Invalid JSON-RPC response | ToolResult(success=false, errorType=SERVER_ERROR) |
| Auth failure (401/403) | ToolResult(success=false, errorType=AUTH_ERROR) |
| Session expired (404 on tool call) | Auto-reconnect (re-initialize), retry once |

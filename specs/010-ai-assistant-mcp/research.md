# Research: AI Assistant with MCP Integration

**Date**: 2026-05-08 | **Branch**: `010-ai-assistant-mcp`

## R1: MCP Streamable HTTP Protocol

### Decision: Hand-rolled client over Kotlin MCP SDK

**Rationale**: The official Kotlin MCP SDK (`io.modelcontextprotocol:kotlin-sdk:0.12.0`) exists and supports `StreamableHttpClientTransport`, but it pulls in Ktor as a transitive dependency. Our Constitution (Principle V — Lean Dependencies) mandates minimal dependency footprint, and we already have OkHttp fully configured with auth interceptors and TLS trust. The protocol surface is 4 JSON-RPC methods — ~130 lines of client code. Hand-rolling avoids ~6 new dependency groups for code we'd write in an afternoon.

**Alternatives considered**:
- **Kotlin MCP SDK + ktor-client-okhttp**: Would reuse OkHttp under Ktor, but SDK is v0.12.0 (early), untested on Android, and adds unnecessary abstraction layers. The `Kotlin-AI-Examples` repo uses it for desktop Compose only.
- **Kai's approach (identical conclusion)**: Hand-rolled MCP client using Ktor (KMP requirement). We follow the same pattern with OkHttp instead.

### Protocol Reference (from spec + aap-mcp-server source)

**Request format**: All methods via HTTP POST to MCP endpoint.
```
Content-Type: application/json
Accept: application/json, text/event-stream
Authorization: Bearer <token>
Mcp-Session-Id: <id>  (if server provided one)
```

**Methods**:

| Method | JSON-RPC | Response |
|--------|----------|----------|
| `initialize` | `{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-03-26","capabilities":{},"clientInfo":{"name":"AAPRemote","version":"1.0"}}}` | JSON with `protocolVersion`, `capabilities`, `serverInfo`. May include `Mcp-Session-Id` header. |
| `notifications/initialized` | `{"jsonrpc":"2.0","method":"notifications/initialized"}` (no `id`) | 202 Accepted, no body |
| `tools/list` | `{"jsonrpc":"2.0","id":2,"method":"tools/list"}` | `{"tools":[{"name":"...","description":"...","inputSchema":{...}}]}` |
| `tools/call` | `{"jsonrpc":"2.0","id":3,"method":"tools/call","params":{"name":"controller.jobs_read","arguments":{"id":42}}}` | `{"content":[{"type":"text","text":"..."}],"isError":false}` |

**Response types**: Server returns `application/json` OR `text/event-stream`. Client must check `Content-Type` header. SSE events use `event: message` with `data:` containing JSON-RPC.

**Critical: aap-mcp-server is STATELESS**:
- No `Mcp-Session-Id` issued (sessionIdGenerator: undefined)
- No GET or DELETE endpoints — POST only
- Fresh transport/server created per request
- Auth validated per request against `/api/gateway/v1/me/`
- Tool names: `{service}.{operation}` (e.g., `controller.jobs_read`)
- Endpoints: `POST /mcp`, `POST /mcp/{toolset}`, `POST /{toolset}/mcp`

**Implication**: Our MCP client should still support `Mcp-Session-Id` for other MCP servers (like a future HTTP-enabled ansible-know), but the primary target doesn't use sessions. This simplifies Phase A considerably — no session expiry/reconnect logic needed initially.

**Error handling**:
- JSON-RPC errors: `-32700` parse, `-32600` invalid request, `-32601` method not found, `-32602` invalid params, `-32603` internal, `-32000` unauthorized
- Tool errors are NOT protocol errors — returned as normal result with `"isError": true`
- Map to our `ErrorType` enum: `-32000` → AUTH_ERROR, `-32601/-32602` → NOT_FOUND, timeout → TIMEOUT, others → SERVER_ERROR

## R2: OkHttp SSE + Kotlin Coroutines

### Decision: callbackFlow bridge with separate SSE OkHttpClient

**Rationale**: Standard pattern, ~10 lines. OkHttp SSE callbacks are thread-safe with `trySend` in callbackFlow. Separate client needed for infinite read timeout on SSE connections.

**Key patterns**:

1. **SSE → Flow bridge**:
```kotlin
fun sseFlow(client: OkHttpClient, request: Request): Flow<ServerSentEvent> = callbackFlow {
    val eventSource = EventSources.createFactory(client)
        .newEventSource(request, object : EventSourceListener() {
            override fun onEvent(es: EventSource, id: String?, type: String?, data: String) {
                trySend(ServerSentEvent(id, type, data))
            }
            override fun onFailure(es: EventSource, t: Throwable?, response: Response?) {
                close(t ?: IOException("SSE failed: ${response?.code}"))
            }
            override fun onClosed(es: EventSource) { close() }
        })
    awaitClose { eventSource.cancel() }
}
```

2. **Dual response handling** (JSON vs SSE from same endpoint):
```kotlin
val response = client.newCall(request).await()
when {
    response.header("Content-Type")?.contains("text/event-stream") == true ->
        parseSSEStream(response.body!!.source())
    else ->
        flowOf(parseJsonRpc(response.body!!.string()))
}
```
Bypass Retrofit for MCP endpoints — Retrofit assumes one content type.

3. **Timeout configuration**:

| Setting | Regular HTTP | SSE / Streaming |
|---------|-------------|-----------------|
| connectTimeout | 30s | 30s |
| readTimeout | 30s | 0 (infinite) |
| writeTimeout | 30s | 30s |
| callTimeout | 60s | 0 (infinite) |

Use `baseClient.newBuilder()` to share connection pool:
```kotlin
val sseClient = baseClient.newBuilder()
    .readTimeout(0, TimeUnit.SECONDS)
    .callTimeout(0, TimeUnit.SECONDS)
    .build()
```

4. **Android gotchas**:
- Collect SSE flows in `viewModelScope` — auto-cancels on ViewModel clear
- OkHttp SSE does NOT auto-reconnect — implement retry with `.retryWhen {}` Flow operator
- `callbackFlow` + `trySend` is thread-safe, no extra synchronization needed
- Use `Channel.BUFFERED` (default) to avoid dropping events under load

## R3: Testing Strategy

### Decision: JUnit 4 + MockWebServer + Turbine, skip UI tests for Phase A

**Rationale**: Minimum viable test setup targeting highest-value tests. JUnit 5 on Android requires extra plugins with instrumented test gaps.

**Dependencies to add**:
```kotlin
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
testImplementation("io.insert-koin:koin-test:4.1.1")
testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
testImplementation("app.cash.turbine:turbine:1.1.0")
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
```

**Test priority (Phase A)**:

| Priority | Target | Strategy | Value |
|----------|--------|----------|-------|
| 1 | MCP Client | MockWebServer with recorded SSE fixtures | Catches serialization bugs — highest ROI |
| 2 | OpenAiCompatibleProvider | MockWebServer with recorded JSON/SSE responses | Tests streaming + tool-call extraction |
| 3 | ChatEngine orchestration | Unit test with fake LlmProvider + fake ToolExecutor | Tests the critical agentic loop |
| 4 | UI (AssistantScreen) | Skip for Phase A | Compose tests are slow during rapid iteration |

**Key pattern**: Define `McpClient` and `LlmProvider` as interfaces/abstract classes with constructor injection via Koin. Create in-memory fakes for unit testing. MockWebServer for integration-level serialization tests.

## R4: Kotlin AI Examples Patterns

### Decision: Borrow agentic loop pattern, adapt for Android

**Rationale**: The `Kotlin-AI-Examples` repo (github.com/Kotlin/Kotlin-AI-Examples) has a working MCP + LLM integration in Kotlin. It's desktop Compose (not Android) but the patterns are transferable.

**Borrowable patterns**:

1. **Tool schema conversion** (`toListChatCompletionTools()`): Iterates MCP tools, maps `inputSchema.properties/required/type` to OpenAI format. We need the same for Anthropic and Gemini formats — adapt, don't copy.

2. **Agentic loop** (`processQuery()`):
   - Send user message + tools to LLM
   - Check response for tool calls
   - Execute each via `mcpClient.callTool()`
   - Append results to conversation
   - Re-send to LLM
   - **Limitation**: Single-turn only. Our ChatEngine needs `while (toolCalls.isNotEmpty() && iterations < 10)`.

3. **Not borrowable for Android**:
   - Uses `runBlocking` (blocks main thread — forbidden on Android)
   - No ViewModel/StateFlow pattern
   - No streaming to UI — blocks until completion
   - No error handling, retry, or cancellation
   - Uses OpenAI Java SDK (we use raw OkHttp)

**Conclusion**: Useful as conceptual validation of the agentic loop, but all implementation must be Android-native (viewModelScope, StateFlow, callbackFlow for streaming).

## R5: Performance & Timeouts (resolved from NEEDS CLARIFICATION)

### Decision: Conservative defaults with user-configurable LLM timeout

**Rationale**: Based on research of MCP server behavior and LLM streaming patterns.

| Parameter | Value | Rationale |
|-----------|-------|-----------|
| MCP connect timeout | 30s | Standard HTTP |
| MCP read timeout | 30s (JSON), infinite (SSE) | SSE streams may be long-running |
| MCP tool call timeout | 60s | AAP API calls can be slow (large inventories) |
| LLM connect timeout | 30s | Standard HTTP |
| LLM response timeout | 120s | Complex queries with tool calling can take time |
| LLM streaming read timeout | infinite | Stream stays open until Done event |
| Max tool call iterations | 10 | Prevents infinite loops |
| Max tool result size | 20K chars (remote), 8K (local) | Keeps context window manageable |
| Chat history limit | 100 messages in-memory | Prevents memory bloat |

## Summary of NEEDS CLARIFICATION Resolutions

| Item | Resolution |
|------|------------|
| Testing framework | JUnit 4 + MockWebServer + Turbine (see R3) |
| Performance goals | Conservative timeouts defined (see R5) |
| MCP session management | Simplified — aap-mcp-server is stateless, but client supports Mcp-Session-Id for other servers (see R1) |
| MCP SDK vs hand-rolled | Hand-rolled confirmed — SDK pulls Ktor, protocol is only 4 methods (see R1) |

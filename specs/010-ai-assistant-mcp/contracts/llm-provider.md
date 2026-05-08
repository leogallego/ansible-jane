# Contract: LLM Provider

**Exposes**: Internal API for LLM communication
**Consumers**: ChatEngine

## LlmProvider (interface)

```kotlin
interface LlmProvider {
    suspend fun generate(messages: List<ChatMessage>, tools: List<ToolSpec>): LlmResult
    fun generateStream(messages: List<ChatMessage>, tools: List<ToolSpec>): Flow<StreamEvent>
    fun isAvailable(): Boolean
    fun modelInfo(): ModelInfo
}
```

### generate(messages, tools)
- Send conversation history + tool definitions to LLM
- Return complete response (text and/or tool calls)
- Blocking until full response received
- Used for non-streaming contexts (tests, background processing)

### generateStream(messages, tools)
- Same as `generate()` but returns `Flow<StreamEvent>`
- Events emitted as they arrive from SSE stream
- Flow terminates with `StreamEvent.Done` (success) or `StreamEvent.Error` (failure)
- Collectors MUST handle all event types
- Flow is cold — new subscription creates new request

### isAvailable()
- Returns `true` if provider is configured and reachable
- For OpenAI-compatible: attempts HEAD or lightweight request to base URL
- For Phase A: simple config check (URL + model non-empty)

### modelInfo()
- Returns display metadata about the configured model
- `isLocal` always `false` in Phase A

## OpenAiCompatibleProvider (Phase A implementation)

```kotlin
class OpenAiCompatibleProvider(
    private val config: LlmProviderConfig.OpenAiCompatible,
    private val httpClient: OkHttpClient,
    private val json: Json
) : LlmProvider
```

### Request format
- POST `{config.url}/chat/completions`
- Headers: `Content-Type: application/json`, optional `Authorization: Bearer {apiKey}`
- Body: `{"model": model, "messages": [...], "tools": [...], "stream": true/false}`

### Tool format conversion
- `ToolSpec.toOpenAiTool()`: wraps in `{"type":"function","function":{"name","description","parameters"}}`
- `parametersSchema` passed through directly as `parameters`

### Response parsing (non-streaming)
- Extract `choices[0].message.content` → `LlmResult.text`
- Extract `choices[0].message.tool_calls` → `LlmResult.toolCalls`
- Each tool call: `{id, function.name, function.arguments}` → `ToolCall(id, name, parseJson(arguments))`

### Response parsing (streaming)
- SSE from `/chat/completions?stream=true`
- `delta.content` → `StreamEvent.TextDelta`
- `delta.tool_calls[i]` with `function.name` → `StreamEvent.ToolCallStart`
- `delta.tool_calls[i]` with `function.arguments` → `StreamEvent.ToolCallArgs`
- `[DONE]` sentinel → accumulate final result → `StreamEvent.Done`

### Error mapping
- HTTP 401/403 → LlmAuthException
- HTTP 429 → LlmRateLimitException (with retry-after if available)
- HTTP 500+ → LlmServerException
- Timeout → LlmTimeoutException
- All mapped to `StreamEvent.Error` in streaming mode

## Schema Passthrough Functions

Each provider implements its own conversion (co-located in provider file):

```kotlin
// OpenAI format (Phase A)
fun ToolSpec.toOpenAiTool(): JsonObject

// Anthropic format (Phase D)
fun ToolSpec.toAnthropicTool(): JsonObject

// Gemini format (Phase D)
fun ToolSpec.toGeminiTool(): JsonObject
```

The `parametersSchema` (raw JsonObject from MCP) is always passed through unchanged as the schema field. Only the wrapper structure differs per provider.

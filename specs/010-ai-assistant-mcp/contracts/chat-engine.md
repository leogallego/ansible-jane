# Contract: Chat Engine

**Exposes**: Internal orchestration API for the agentic tool-calling loop
**Consumers**: AssistantViewModel

## ChatEngine

```kotlin
class ChatEngine(
    private val provider: LlmProvider,
    private val toolExecutor: ToolExecutor,
    private val maxIterations: Int = 10
) {
    fun processMessage(
        userMessage: String,
        history: List<ChatMessage>,
        tools: List<ToolSpec>
    ): Flow<ChatEvent>
}
```

### processMessage(userMessage, history, tools)

Returns a cold `Flow<ChatEvent>` representing the full orchestration cycle.

**Orchestration loop**:
```
1. Prepend system prompt to history
2. Append user message
3. Call provider.generateStream(messages, tools)
4. Collect StreamEvents, re-emit as ChatEvents
5. On Done:
   a. If result has toolCalls AND iterations < maxIterations:
      - For each toolCall: execute via toolExecutor
      - Emit ChatEvent.ToolExecuting / ChatEvent.ToolResult
      - Append tool results as TOOL messages
      - Go to step 3 (next iteration)
   b. If result has text only OR iteration limit reached:
      - Emit ChatEvent.AssistantMessage
      - Flow completes
6. On Error: emit ChatEvent.Error, flow completes
```

### ChatEvent (sealed interface)

```kotlin
sealed interface ChatEvent {
    data class TextDelta(val text: String) : ChatEvent
    data class ToolExecuting(val toolName: String, val args: JsonObject) : ChatEvent
    data class ToolResult(val toolName: String, val result: assistant.tools.ToolResult) : ChatEvent
    data class AssistantMessage(val fullText: String, val toolCallCount: Int) : ChatEvent
    data class Error(val message: String, val cause: Throwable? = null) : ChatEvent
}
```

### System Prompt

Injected at the start of each conversation:
```
You are an AI assistant for Ansible Automation Platform (AAP). You help users 
query and manage their AAP instance using the available tools. Be concise and 
specific. When reporting results, use structured formatting.
```

Additional context injected per instance:
```
Connected AAP instance: {instance.displayLabel} ({instance.baseUrl})
Available tool sources: {server labels}
```

## ToolExecutor

```kotlin
class ToolExecutor(
    private val tools: List<Tool>,
    private val maxResultChars: Int = 20_000
) {
    suspend fun execute(toolCall: ToolCall): assistant.tools.ToolResult
}
```

### execute(toolCall)
- Find tool by `toolCall.name` in flat tool list
- If not found: return `ToolResult(success=false, errorType=NOT_FOUND)`
- Convert `toolCall.arguments` (JsonObject) to `Map<String, Any>`
- Call `tool.execute(args)`
- Smart-truncate `result.data` to `maxResultChars`
- Return result

### Smart truncation
- If `data.length <= maxResultChars`: return as-is
- Else: keep first 60% + `\n\n[... {N} chars truncated ...]\n\n` + last 40%
- Preserves start (most important context) and end (closing brackets for JSON)

## Error Behavior

| Scenario | ChatEvent emitted |
|----------|-------------------|
| LLM unreachable | Error("Unable to reach LLM server") |
| LLM auth failure | Error("LLM authentication failed — check API key") |
| Tool not found | ToolResult with NOT_FOUND → LLM sees error and explains |
| Tool execution error | ToolResult with error type → LLM suggests remediation |
| Iteration limit | AssistantMessage("I wasn't able to complete this request within the tool call limit") |
| LLM response timeout | Error("Response timed out") |

## Thread Safety

- `processMessage` returns a cold Flow — each collector gets its own coroutine context
- No shared mutable state within ChatEngine
- Message history is passed in, not stored — AssistantViewModel owns the state
- ToolExecutor is stateless (tool list set at construction, immutable)

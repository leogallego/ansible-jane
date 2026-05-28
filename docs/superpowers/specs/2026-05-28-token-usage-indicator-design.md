# Token Usage Indicator — Design Spec

## Summary

Add a per-message and session-level token usage indicator to the AI assistant (Jane) chat UI. The indicator shows how many LLM tokens each response consumed and what source handled it (LLM/local/MCP), giving the user visibility into API cost and usage patterns.

## Data Source

Koog's streaming API provides token counts through `StreamFrame.End.metaInfo: ResponseMetaInfo` (Koog 1.0.0, verified from source at tag `1.0.0`).

`ResponseMetaInfo` fields (from `Message.kt` in Koog source):

```kotlin
data class ResponseMetaInfo(
    val timestamp: Instant,
    val totalTokensCount: Int? = null,
    val inputTokensCount: Int? = null,
    val outputTokensCount: Int? = null,
    val modelId: String? = null,
    val metadata: JsonObject? = null
)
```

All three providers the app supports populate these fields:

| Provider | How Koog populates ResponseMetaInfo |
|----------|-------------------------------------|
| OpenAI-compatible | `OpenAILLMClient` extracts `usage.totalTokens`, `usage.promptTokens`, `usage.completionTokens` from API response |
| Gemini | `GoogleLLMClient` extracts `usageMetadata` |
| Ollama | `OllamaClient` extracts `prompt_eval_count` and `eval_count` |

**Fallback:** When `totalTokensCount` is null (rare edge case — provider doesn't report usage), estimate from `totalChars / 4` and flag as estimated. The `~` prefix signals approximation to the user.

## Data Model

### New data class

File: `assistant/engine/ChatMessage.kt`

```kotlin
@Immutable
data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val isEstimated: Boolean = false
)
```

### ChatMessage extension

Add one field to `ChatMessage`:

```kotlin
data class ChatMessage(
    // ... existing fields unchanged
    val tokenUsage: TokenUsage? = null
)
```

### UI state extension

Add session aggregate to `AssistantUiState.Active`:

```kotlin
data class Active(
    // ... existing fields unchanged
    val sessionTokens: Int = 0
)
```

## Engine Changes

### ChatEngine.kt

**New ChatEvent:**

```kotlin
sealed interface ChatEvent {
    // ... existing events unchanged
    data class TokenUsageReport(val usage: TokenUsage) : ChatEvent
}
```

**Token accumulation across agentic loop iterations:**

Add mutable accumulators before the `loop@` block:

```kotlin
var accInputTokens = 0
var accOutputTokens = 0
var accTotalTokens = 0
var anyRealUsage = false
```

In the `StreamFrame.End` handler (line 118), extract metaInfo:

```kotlin
is StreamFrame.End -> {
    val meta = frame.metaInfo
    if (meta.totalTokensCount != null) {
        accInputTokens += meta.inputTokensCount ?: 0
        accOutputTokens += meta.outputTokensCount ?: 0
        accTotalTokens += meta.totalTokensCount ?: 0
        anyRealUsage = true
    }
}
```

Before emitting `ChatEvent.AssistantMessage`, emit `TokenUsageReport`:

```kotlin
val usage = if (anyRealUsage) {
    TokenUsage(
        inputTokens = accInputTokens,
        outputTokens = accOutputTokens,
        totalTokens = accTotalTokens,
        isEstimated = false
    )
} else {
    // Fallback: estimate from total chars processed
    val totalChars = messages.sumOf { it.content.length } + (responseText?.length ?: 0)
    val estimated = totalChars / 4
    TokenUsage(
        inputTokens = estimated * 2 / 3,  // rough split
        outputTokens = estimated / 3,
        totalTokens = estimated,
        isEstimated = true
    )
}
emit(ChatEvent.TokenUsageReport(usage))
emit(ChatEvent.AssistantMessage(finalText, totalToolCalls))
```

This `TokenUsageReport` is emitted at every exit point from the agentic loop (normal completion, max iterations, repeat detection).

### Token accumulation behavior

In the agentic loop, each LLM call triggers a `StreamFrame.End` with its own `metaInfo`. The accumulators sum across all iterations:

```
Iteration 1: LLM → 800 in + 200 out → tool call
Iteration 2: LLM → 1200 in + 150 out → tool call
Iteration 3: LLM → 1500 in + 300 out → final text
Reported: 3500 in + 650 out = 4150 total
```

## ViewModel Changes

### AssistantViewModel.kt

In the `engine.processMessage().collect` block, add a handler for the new event:

```kotlin
var pendingTokenUsage: TokenUsage? = null

// In the when block:
is ChatEvent.TokenUsageReport -> {
    pendingTokenUsage = event.usage
}
is ChatEvent.AssistantMessage -> {
    val finalMsg = ChatMessage(
        // ... existing fields
        tokenUsage = pendingTokenUsage
    )
    repository.addMessage(finalMsg)
    updateState {
        copy(
            messages = repository.getHistory().toImmutableList(),
            isGenerating = false,
            streamingText = null,
            sessionTokens = sessionTokens + (pendingTokenUsage?.totalTokens ?: 0)
        )
    }
}
```

Reset `sessionTokens` when chat is cleared — in the clear chat handler:

```kotlin
updateState {
    copy(
        messages = persistentListOf(),
        sessionTokens = 0
    )
}
```

## UI Changes

### Per-message indicator (ChatBubble.kt)

Extend the existing `SourceBand` composable to accept and display token usage.

**Updated signature:**

```kotlin
@Composable
private fun SourceBand(
    source: ResponseSource,
    toolsUsed: List<String>,
    tokenUsage: TokenUsage? = null,
    modifier: Modifier = Modifier
)
```

**Display format** — appended after tools list in the same Row:

```
local · list_jobs, get_job_status · 2.4K tokens
llm · ~1.2K tokens
local + mcp · ping, controller.list_hosts · 890 tokens
```

Formatting rules:
- `< 1000`: show as-is (e.g., `890 tokens`)
- `>= 1000`: show as `X.YK tokens` (e.g., `2.4K tokens`)
- If `isEstimated`: prefix with `~` (e.g., `~1.2K tokens`)
- Same `labelSmall` style, same muted color as existing source/tools text
- Separated from tools list by the same `·` dot separator

**Propagation:** `AssistantMessage` composable passes `tokenUsage` from `ChatMessage` to `SourceBand`.

### Session aggregate (MainScreen.kt)

Show a compact token counter in the top bar's `actions` area, only when the Assistant tab is selected.

**Placement:** Before the clear-chat icon button, after `ProviderSwitchChip`.

**Display format:**

```
12.4K ▼    [🗑]  [🔔]  [⚙]
```

- Same formatting rules as per-message (K suffix for >= 1000)
- `labelSmall` style, `onSurfaceVariant` color
- Only visible when `sessionTokens > 0`
- The `▼` placeholder is for a future LLM model dropdown — for now, just the token count text

**Data flow:** `MainScreen` observes `AssistantUiState.Active.sessionTokens` from the ViewModel and passes it to the top bar.

## Files Modified

| File | Change |
|------|--------|
| `assistant/engine/ChatMessage.kt` | Add `TokenUsage` data class, add `tokenUsage` field to `ChatMessage` |
| `assistant/engine/ChatEngine.kt` | Extract `ResponseMetaInfo` from `StreamFrame.End`, accumulate across iterations, emit `TokenUsageReport` event |
| `assistant/presentation/AssistantUiState.kt` | Add `sessionTokens: Int` to `Active` |
| `assistant/presentation/AssistantViewModel.kt` | Handle `TokenUsageReport` event, attach to `ChatMessage`, update session total |
| `assistant/ui/ChatBubble.kt` | Extend `SourceBand` to show token count, update `AssistantMessage` to pass token data |
| `ui/main/MainScreen.kt` | Show session token counter in top bar when on Assistant tab |

## Not in scope

- Persisting token usage across sessions (DataStore) — tokens reset on app restart
- Token budget/limit warnings — future enhancement
- Prompt-tokenizer for context window management — separate issue for Token Saver mode improvement
- Per-iteration breakdown UI — aggregated total only

## Future considerations

- **LLM model dropdown in top bar:** The session token counter is placed to co-exist with a future model selector
- **Koog prompt-tokenizer for Token Saver:** Adding `ai.koog:prompt-tokenizer` to improve the existing char-based context trimming in `compactHistory`/`trimMessages` would be a natural follow-up, tracked separately
- **Cost estimation:** With token counts and model name, we could calculate approximate API cost per message

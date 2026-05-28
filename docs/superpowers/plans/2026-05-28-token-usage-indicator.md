# Token Usage Indicator Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show per-message and session-level LLM token consumption in Jane's chat UI, using Koog's `ResponseMetaInfo` as the primary data source.

**Architecture:** Extract token counts from `StreamFrame.End.metaInfo` in ChatEngine, propagate via a new `ChatEvent.TokenUsageReport`, store on `ChatMessage`, aggregate in UI state, and display in `SourceBand` (per-message) and top bar (session total).

**Tech Stack:** Kotlin, Jetpack Compose, Koog 1.0.0 streaming API (`ResponseMetaInfo`), MVVM with StateFlow.

**Issue:** #218

---

## File Map

| File | Action | Responsibility |
|------|--------|---------------|
| `assistant/engine/ChatMessage.kt` | Modify | Add `TokenUsage` data class and `tokenUsage` field |
| `assistant/engine/ChatEngine.kt` | Modify | Extract `ResponseMetaInfo`, emit `TokenUsageReport` event |
| `assistant/presentation/AssistantUiState.kt` | Modify | Add `sessionTokens` to `Active` state |
| `assistant/presentation/AssistantViewModel.kt` | Modify | Handle `TokenUsageReport`, attach to messages, aggregate |
| `assistant/data/IAssistantRepository.kt` | Modify | Add `sessionTokensFlow` |
| `assistant/data/AssistantRepository.kt` | Modify | Implement `sessionTokensFlow`, track on addMessage/clear |
| `assistant/ui/ChatBubble.kt` | Modify | Extend `SourceBand` and `AssistantMessage` with token display |
| `assistant/ui/AssistantScreen.kt` | Modify | Pass `tokenUsage` from `ChatMessage` to `AssistantMessage` |
| `ui/main/MainScreen.kt` | Modify | Show session token counter in top bar |

All paths relative to `app/src/main/kotlin/io/github/leogallego/ansiblejane/`.

---

### Task 1: Add `TokenUsage` data class and `ChatMessage` field

**Files:**
- Modify: `assistant/engine/ChatMessage.kt`

- [ ] **Step 1: Add `TokenUsage` data class**

In `ChatMessage.kt`, add the data class before `ChatMessage`:

```kotlin
@Immutable
data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val isEstimated: Boolean = false
)
```

- [ ] **Step 2: Add `tokenUsage` field to `ChatMessage`**

Add the field after `toolsUsed`:

```kotlin
@Immutable
data class ChatMessage(
    val role: Role,
    val content: String,
    val toolCallsJson: String? = null,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val source: ResponseSource? = null,
    val toolsUsed: List<String> = emptyList(),
    val tokenUsage: TokenUsage? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val id: Long = nextId()
)
```

- [ ] **Step 3: Add `formatTokenCount` utility**

Add a companion function to `TokenUsage` for consistent formatting across UI:

```kotlin
@Immutable
data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val isEstimated: Boolean = false
) {
    fun formatTotal(): String {
        val prefix = if (isEstimated) "~" else ""
        return if (totalTokens >= 1000) {
            val k = totalTokens / 1000
            val remainder = (totalTokens % 1000) / 100
            if (remainder > 0) "$prefix${k}.${remainder}K" else "$prefix${k}K"
        } else {
            "$prefix$totalTokens"
        }
    }
}
```

- [ ] **Step 4: Build to verify no compilation errors**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL (existing call sites use default `tokenUsage = null`)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ChatMessage.kt
git commit -m "feat: add TokenUsage data class and ChatMessage field (#218)"
```

---

### Task 2: Add `TokenUsageReport` event and extract from `StreamFrame.End` in ChatEngine

**Files:**
- Modify: `assistant/engine/ChatEngine.kt:30-41` (ChatEvent sealed interface)
- Modify: `assistant/engine/ChatEngine.kt:74-77` (accumulator vars)
- Modify: `assistant/engine/ChatEngine.kt:118` (StreamFrame.End handler)
- Modify: `assistant/engine/ChatEngine.kt:143-149` (repeat detection exit)
- Modify: `assistant/engine/ChatEngine.kt:210-220` (normal completion exit)
- Modify: `assistant/engine/ChatEngine.kt:222-226` (max iterations exit)

- [ ] **Step 1: Add `TokenUsageReport` to `ChatEvent`**

In `ChatEngine.kt`, add after the `Error` event (line 40):

```kotlin
sealed interface ChatEvent {
    data class TextDelta(val text: String) : ChatEvent
    data class ToolExecuting(val toolName: String, val args: JsonObject) : ChatEvent
    data class ToolResult(val toolName: String, val result: io.github.leogallego.ansiblejane.assistant.tools.ToolResult) : ChatEvent
    data class ConfirmationRequired(
        val toolName: String,
        val args: JsonObject,
        val description: String
    ) : ChatEvent
    data class AssistantMessage(val fullText: String, val toolCallCount: Int) : ChatEvent
    data class Error(val message: String, val cause: Throwable? = null) : ChatEvent
    data class TokenUsageReport(val usage: TokenUsage) : ChatEvent
}
```

- [ ] **Step 2: Add token accumulators before the loop**

After `val softLimit = ...` (line 77), add:

```kotlin
var accInputTokens = 0
var accOutputTokens = 0
var accTotalTokens = 0
var anyRealUsage = false
```

- [ ] **Step 3: Extract `ResponseMetaInfo` from `StreamFrame.End`**

Replace line 118:

```kotlin
// OLD:
is StreamFrame.End -> { /* handled after collect */ }

// NEW:
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

- [ ] **Step 4: Add helper function to build `TokenUsage`**

Add a private function in `ChatEngine`:

```kotlin
private fun buildTokenUsage(
    anyRealUsage: Boolean,
    accInputTokens: Int,
    accOutputTokens: Int,
    accTotalTokens: Int,
    messages: List<ChatMessage>,
    responseText: String?
): TokenUsage {
    return if (anyRealUsage) {
        TokenUsage(
            inputTokens = accInputTokens,
            outputTokens = accOutputTokens,
            totalTokens = accTotalTokens
        )
    } else {
        val totalChars = messages.sumOf { it.content.length } + (responseText?.length ?: 0)
        val estimated = totalChars / 4
        TokenUsage(
            inputTokens = estimated * 2 / 3,
            outputTokens = estimated / 3,
            totalTokens = estimated,
            isEstimated = true
        )
    }
}
```

- [ ] **Step 5: Emit `TokenUsageReport` at repeat-detection exit**

At line 143-149, add the emit before `AssistantMessage`:

```kotlin
if (isRepeatingToolCalls(toolCallHistory)) {
    Log.w(TAG, "ITER $iterations: repeat detected, stopping")
    val usage = buildTokenUsage(anyRealUsage, accInputTokens, accOutputTokens, accTotalTokens, messages, responseText)
    emit(ChatEvent.TokenUsageReport(usage))
    emit(ChatEvent.AssistantMessage(
        (responseText ?: "") + "\n\nStopped: the same tools were being called repeatedly.",
        totalToolCalls
    ))
    return@flow
}
```

- [ ] **Step 6: Emit `TokenUsageReport` at normal completion exit**

At line 210-219, add the emit before `AssistantMessage`:

```kotlin
} else {
    Log.d(TAG, "DONE: $iterations iterations, $totalToolCalls total tool calls, " +
        "response=${responseText?.length ?: 0} chars")
    val finalText = if (completedCalls.isNotEmpty() && iterations >= maxIterations) {
        (responseText ?: "") + "\n\nI wasn't able to complete this request within the tool call limit."
    } else {
        responseText ?: textBuilder.toString()
    }
    val usage = buildTokenUsage(anyRealUsage, accInputTokens, accOutputTokens, accTotalTokens, messages, finalText)
    emit(ChatEvent.TokenUsageReport(usage))
    emit(ChatEvent.AssistantMessage(finalText, totalToolCalls))
    return@flow
}
```

- [ ] **Step 7: Emit `TokenUsageReport` at max-iterations exit**

At line 222-226, add the emit:

```kotlin
val usage = buildTokenUsage(anyRealUsage, accInputTokens, accOutputTokens, accTotalTokens, messages, null)
emit(ChatEvent.TokenUsageReport(usage))
emit(ChatEvent.AssistantMessage(
    "I wasn't able to complete this request within the tool call limit.",
    totalToolCalls
))
```

- [ ] **Step 8: Build to verify**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ChatEngine.kt
git commit -m "feat: extract token usage from Koog ResponseMetaInfo (#218)"
```

---

### Task 3: Add session token tracking to repository and UI state

**Files:**
- Modify: `assistant/data/IAssistantRepository.kt:7-24`
- Modify: `assistant/data/AssistantRepository.kt:34,47,66`
- Modify: `assistant/presentation/AssistantUiState.kt:21-27`

- [ ] **Step 1: Add `sessionTokensFlow` to `IAssistantRepository`**

Add after `val onHistoryCleared`:

```kotlin
interface IAssistantRepository {
    // ... existing members
    val sessionTokensFlow: Flow<Int>
    // ... rest unchanged
}
```

Add `import kotlinx.coroutines.flow.StateFlow` if not already present.

- [ ] **Step 2: Implement `sessionTokensFlow` in `AssistantRepository`**

Add a `MutableStateFlow` and update `addMessage`/`clearHistory`:

```kotlin
class AssistantRepository(...) : IAssistantRepository {
    // ... existing fields
    private val _sessionTokens = MutableStateFlow(0)
    override val sessionTokensFlow: Flow<Int> = _sessionTokens.asStateFlow()

    override fun addMessage(message: ChatMessage) {
        messages.add(message)
        message.tokenUsage?.let { _sessionTokens.value += it.totalTokens }
    }

    override fun clearHistory() {
        messages.clear()
        _sessionTokens.value = 0
        _onHistoryCleared.tryEmit(Unit)
    }
}
```

Add imports: `import kotlinx.coroutines.flow.MutableStateFlow`, `import kotlinx.coroutines.flow.asStateFlow`.

- [ ] **Step 3: Add `sessionTokens` to `AssistantUiState.Active`**

```kotlin
@Immutable
data class Active(
    val messages: ImmutableList<ChatMessage> = persistentListOf(),
    val isGenerating: Boolean = false,
    val streamingText: String? = null,
    val connections: Map<String, McpConnectionState> = emptyMap(),
    val pendingConfirmation: PendingConfirmation? = null,
    val sessionTokens: Int = 0
) : AssistantUiState
```

- [ ] **Step 4: Build to verify**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/data/IAssistantRepository.kt \
      app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/data/AssistantRepository.kt \
      app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/presentation/AssistantUiState.kt
git commit -m "feat: add session token tracking to repository and UI state (#218)"
```

---

### Task 4: Handle `TokenUsageReport` in `AssistantViewModel`

**Files:**
- Modify: `assistant/presentation/AssistantViewModel.kt:236-313` (event collection)
- Modify: `assistant/presentation/AssistantViewModel.kt:338-341` (clearHistory)

- [ ] **Step 1: Add `pendingTokenUsage` variable and event handler**

In the `sendMessage` method, after `val usedToolNames = mutableListOf<String>()` (line 238), add:

```kotlin
var pendingTokenUsage: TokenUsage? = null
```

In the `when (event)` block (line 258), add a new case before `is ChatEvent.AssistantMessage`:

```kotlin
is ChatEvent.TokenUsageReport -> {
    pendingTokenUsage = event.usage
}
```

- [ ] **Step 2: Attach `tokenUsage` to `ChatMessage` in `AssistantMessage` handler**

Modify the `is ChatEvent.AssistantMessage` handler (line 276-296). Add `tokenUsage` to the `ChatMessage` constructor:

```kotlin
is ChatEvent.AssistantMessage -> {
    val responseSource = when {
        usedSources.isEmpty() -> ResponseSource.LLM
        usedSources.size > 1 -> ResponseSource.MIXED
        "local" in usedSources -> ResponseSource.LOCAL
        else -> ResponseSource.MCP
    }
    val finalMsg = ChatMessage(
        role = Role.ASSISTANT,
        content = event.fullText,
        source = responseSource,
        toolsUsed = usedToolNames.distinct(),
        tokenUsage = pendingTokenUsage
    )
    repository.addMessage(finalMsg)
    updateState {
        copy(
            messages = repository.getHistory().toImmutableList(),
            isGenerating = false,
            streamingText = null
        )
    }
}
```

Note: `sessionTokens` no longer needs updating here — it comes from the repository's `sessionTokensFlow` (wired in Task 5).

- [ ] **Step 3: Add import for `TokenUsage`**

Add at the top of `AssistantViewModel.kt`:

```kotlin
import io.github.leogallego.ansiblejane.assistant.engine.TokenUsage
```

- [ ] **Step 4: Build to verify**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/presentation/AssistantViewModel.kt
git commit -m "feat: handle TokenUsageReport in AssistantViewModel (#218)"
```

---

### Task 5: Wire session tokens into UI state via repository flow

**Files:**
- Modify: `assistant/presentation/AssistantViewModel.kt:82-96` (init block where Active state is created)

- [ ] **Step 1: Collect `sessionTokensFlow` and update Active state**

In the ViewModel's `init` block, after the `tokenManager.activeInstance.collect` block that creates `AssistantUiState.Active`, add a second `launch` to observe session tokens:

```kotlin
init {
    // ... existing launch for instance collection

    viewModelScope.launch {
        repository.sessionTokensFlow.collect { tokens ->
            _uiState.update { current ->
                if (current is AssistantUiState.Active) {
                    current.copy(sessionTokens = tokens)
                } else current
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/presentation/AssistantViewModel.kt
git commit -m "feat: wire session token flow into ViewModel UI state (#218)"
```

---

### Task 6: Display per-message token count in `SourceBand`

**Files:**
- Modify: `assistant/ui/ChatBubble.kt:98-104` (AssistantMessage signature)
- Modify: `assistant/ui/ChatBubble.kt:147-148` (SourceBand call)
- Modify: `assistant/ui/ChatBubble.kt:224-273` (SourceBand composable)

- [ ] **Step 1: Add `tokenUsage` parameter to `AssistantMessage`**

Update the signature (line 98-104):

```kotlin
@Composable
fun AssistantMessage(
    content: String,
    source: ResponseSource? = null,
    toolsUsed: List<String> = emptyList(),
    tokenUsage: TokenUsage? = null,
    onCopy: (() -> Unit)? = null,
    onRegenerate: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

Add import at the top of `ChatBubble.kt`:

```kotlin
import io.github.leogallego.ansiblejane.assistant.engine.TokenUsage
```

- [ ] **Step 2: Pass `tokenUsage` to `SourceBand`**

Update the `SourceBand` call (line 147-148):

```kotlin
if (source != null) {
    SourceBand(source = source, toolsUsed = toolsUsed, tokenUsage = tokenUsage)
}
```

- [ ] **Step 3: Update `SourceBand` to accept and display `tokenUsage`**

Replace the entire `SourceBand` composable (lines 224-273):

```kotlin
@Composable
private fun SourceBand(
    source: ResponseSource,
    toolsUsed: List<String>,
    tokenUsage: TokenUsage? = null,
    modifier: Modifier = Modifier
) {
    val sourceLabel = when (source) {
        ResponseSource.LOCAL -> "local"
        ResponseSource.MCP -> "mcp"
        ResponseSource.LLM -> "llm"
        ResponseSource.MIXED -> "local + mcp"
    }
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Column(modifier = modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = sourceLabel,
                style = MaterialTheme.typography.labelSmall,
                color = color,
            )
            if (toolsUsed.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = toolsUsed.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = color,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            if (tokenUsage != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "·",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${tokenUsage.formatTotal()} tokens",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            thickness = 0.5.dp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
```

- [ ] **Step 4: Build to verify**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/ui/ChatBubble.kt
git commit -m "feat: display per-message token count in SourceBand (#218)"
```

---

### Task 7: Pass `tokenUsage` from `AssistantScreen` to `AssistantMessage`

**Files:**
- Modify: `assistant/ui/AssistantScreen.kt:266-275`

- [ ] **Step 1: Add `tokenUsage` argument to `AssistantMessage` call**

At line 266-275, add the parameter:

```kotlin
Role.ASSISTANT -> AssistantMessage(
    content = message.content,
    source = message.source,
    toolsUsed = message.toolsUsed,
    tokenUsage = message.tokenUsage,
    onCopy = {
        clipboardManager.setText(AnnotatedString(message.content))
    },
    onRegenerate = if (isLastAssistant) onRegenerateLastMessage
        else null,
)
```

- [ ] **Step 2: Build to verify**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/ui/AssistantScreen.kt
git commit -m "feat: pass tokenUsage from AssistantScreen to ChatBubble (#218)"
```

---

### Task 8: Show session token counter in top bar

**Files:**
- Modify: `ui/main/MainScreen.kt:65-70` (state observation)
- Modify: `ui/main/MainScreen.kt:130-141` (actions area, before clear chat button)

- [ ] **Step 1: Observe `sessionTokensFlow` from repository**

After line 70 (`val activeProviderKey ...`), add:

```kotlin
val sessionTokens by assistantRepository.sessionTokensFlow
    .collectAsStateWithLifecycle(initialValue = 0)
```

Add import:

```kotlin
import io.github.leogallego.ansiblejane.assistant.engine.TokenUsage
```

- [ ] **Step 2: Add session token counter in top bar actions**

After the `ProviderSwitchChip(...)` block (line 141) and before the `if (selectedTab is TopLevelTab.Assistant)` block (line 142), add:

```kotlin
if (selectedTab is TopLevelTab.Assistant && sessionTokens > 0) {
    val formatted = TokenUsage(0, 0, sessionTokens).formatTotal()
    Text(
        text = "$formatted tokens",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(end = 4.dp)
            .testTag("text_session_tokens"),
    )
}
```

Add import for `testTag` if not already present:

```kotlin
import androidx.compose.ui.platform.testTag
```

- [ ] **Step 3: Build to verify**

Run: `./gradlew app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/main/MainScreen.kt
git commit -m "feat: show session token counter in top bar (#218)"
```

---

### Task 9: Run tests and verify

**Files:**
- No new files

- [ ] **Step 1: Run full test suite**

Run: `TMPDIR=$(pwd)/.tmp ./gradlew test`
Expected: All existing tests pass (no regressions from adding defaulted `tokenUsage = null` field)

- [ ] **Step 2: Run lint**

Run: `TMPDIR=$(pwd)/.tmp ./gradlew lint`
Expected: No new lint errors

- [ ] **Step 3: Verify ChatEngine streaming test still passes**

Run: `TMPDIR=$(pwd)/.tmp ./gradlew test --tests "*ChatEngine*"`
Expected: PASS — the test creates `StreamFrame.End(finishReason = "stop")` which uses `metaInfo = ResponseMetaInfo.Empty`, so `totalTokensCount` will be null and the fallback path runs.

- [ ] **Step 4: Final commit if any fixes needed**

If tests required fixes, commit them:

```bash
git add -A
git commit -m "fix: address test issues from token usage feature (#218)"
```

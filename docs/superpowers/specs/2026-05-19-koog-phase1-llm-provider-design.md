# Koog Phase 1: Replace LLM Provider with Koog Client

**Date:** 2026-05-19
**Issue:** [#56](https://github.com/leogallego/ansible-jane/issues/56)
**Branch:** TBD (feature branch off main)
**Prerequisites:** All met — Kotlin 2.3.21, R8 enabled, OkHttp 5.3.2 (PR #65), Compose BOM 2026.05.00

## Problem

`OpenAiCompatibleProvider` is 370 LOC of hand-rolled HTTP/SSE/JSON code for talking to OpenAI-compatible LLM APIs. It handles request building, SSE streaming, tool call accumulation, and error mapping — all things that Koog's `prompt-executor-openai-client` provides out of the box with better provider compatibility and lenient JSON parsing.

## Goal

Replace `OpenAiCompatibleProvider` with a thin adapter over Koog's `AbstractOpenAILLMClient`. Validate that Koog works in the project (dependency resolution, compilation, runtime) before deeper integration in Phase 2.

## Approach: Adapter Pattern

Create `KoogLlmProvider` implementing our existing `LlmProvider` interface. Wraps Koog's client internally. ChatEngine and everything above it stays untouched.

```
AssistantViewModel
  |-- KoogLlmProvider (new, implements LlmProvider)
       |-- AbstractOpenAILLMClient subclass (Koog)
            |-- Ktor HttpClient (CIO engine, Koog default)
```

### Why adapter, not direct integration

Direct integration (replacing `LlmProvider` with Koog's `LLMClient` throughout ChatEngine) is the right long-term approach but has a blast radius of 8+ files. Phase 1 validates the dependency with minimal risk. Phase 2 removes the adapter.

## Dependencies

Add to `gradle/libs.versions.toml`:

```toml
[versions]
koog = "0.8.0"

[libraries]
koog-openai-client = { group = "ai.koog.prompt", name = "prompt-executor-openai-client", version.ref = "koog" }
```

No Ktor engine dependency needed — CIO comes transitively with Koog.

Existing `okhttp-sse` stays (used by `McpTransport`).

## KoogLlmProvider Design

New file: `app/src/main/kotlin/.../assistant/llm/KoogLlmProvider.kt`

**Constructor:** `LlmProviderConfig.OpenAiCompatible` + `trustSelfSigned: Boolean`.

- Creates Koog client: subclass of `AbstractOpenAILLMClient` with custom `baseUrl` from config
- SSL trust: if `trustSelfSigned` is true, creates a Ktor CIO HttpClient with `CertTrustManager.createTrustAllManager()` in its SSL config. Otherwise uses Koog's default HttpClient.
- API key passed to Koog constructor (Koog adds Bearer header internally)
- No OkHttpClient dependency — Koog uses Ktor CIO directly

**Methods:**

| Method | Behavior |
|--------|----------|
| `generate()` | Convert ChatMessage list to Koog Prompt, ToolSpec list to ToolDescriptor list. Call Koog `execute()`. Map response to `LlmResult`. |
| `generateStream()` | Same conversion. Call Koog `executeStreaming()`. Map `Flow<StreamFrame>` to `Flow<StreamEvent>`. |
| `isAvailable()` | Check config.url and config.model are non-blank (same as before). |
| `modelInfo()` | Return ModelInfo from config (same as before). |

**Type conversions:**

| Our Type | Koog Type |
|----------|-----------|
| `ChatMessage(SYSTEM, content)` | Prompt system message |
| `ChatMessage(USER, content)` | Prompt user message |
| `ChatMessage(ASSISTANT, content, toolCalls)` | Prompt assistant message with tool calls |
| `ChatMessage(TOOL, content, toolCallId)` | Prompt tool result message |
| `ToolSpec(name, desc, schema)` | `ToolDescriptor(name, desc, params)` — parse JSON Schema properties |
| `StreamFrame.TextDelta` | `StreamEvent.TextDelta` |
| `StreamFrame.ToolCallComplete` | Accumulated into `LlmResult.toolCalls` |
| `StreamFrame.End` | `StreamEvent.Done(LlmResult)` |

**Error mapping:** Catch Ktor/IO exceptions, map to our exception types:
- HTTP 401/403 -> `LlmAuthException`
- HTTP 429 -> `LlmRateLimitException`
- HTTP 5xx -> `LlmServerException`
- Timeout -> `LlmTimeoutException`

**Schema compaction:** Apply existing `compactSchema()` to ToolSpec before converting to ToolDescriptor.

## Changes to Existing Files

### `AssistantViewModel.kt` (~2 lines)

```kotlin
// Before
val provider = when (config) {
    is LlmProviderConfig.OpenAiCompatible ->
        OpenAiCompatibleProvider(config, llmClient, json)
}

// After
val trustSelfSigned = tokenManager.activeInstance.value?.trustSelfSigned == true
val provider = when (config) {
    is LlmProviderConfig.OpenAiCompatible ->
        KoogLlmProvider(config, trustSelfSigned)
}
```

### `LlmTypes.kt` — relocate exception classes

Move from `OpenAiCompatibleProvider.kt`:
- `LlmAuthException`
- `LlmRateLimitException`
- `LlmServerException`
- `LlmTimeoutException`

These are referenced by `ChatEngine` and must survive the provider deletion.

### Deletions

- `OpenAiCompatibleProvider.kt` (370 LOC)
- `OpenAiCompatibleProviderTest.kt` (200 LOC)

## What Stays Unchanged

- ChatEngine, ToolExecutor, ToolRouter
- ModelFetcher (keeps its own HTTP call)
- McpTransport (keeps using OkHttp SSE)
- All data models (ChatMessage, ToolSpec, StreamEvent, LlmResult, ToolCall)
- AssistantModule.kt (DI wiring)

## Tests

New `KoogLlmProviderTest.kt` with MockWebServer + Turbine:
- Request format (POST to /chat/completions, Bearer auth, model field)
- Text response parsing
- Tool call parsing (non-streaming and streaming)
- Error mapping (401, 429, 500)
- Streaming events (TextDelta flow, Done with accumulated tool calls)
- `isAvailable()` and `modelInfo()` trivial tests

## Verification

1. `./gradlew compileDebugKotlin` passes
2. `./gradlew testDebugUnitTest` passes (new + existing tests)
3. Device smoke test: configure provider, send message, verify streaming + tool calling

## Future: Phase 2 (Approach C)

Phase 2 removes the adapter and uses Koog types directly in ChatEngine:
- Replace `LlmProvider` interface with Koog's `LLMClient`
- Replace `ChatMessage` with Koog's `Prompt` / `Message` types
- Replace `StreamEvent` with Koog's `StreamFrame`
- Replace `ToolSpec` with Koog's `ToolDescriptor`
- Adopt Koog's agent framework for the tool-calling loop
- ~500+ LOC changes across 8+ files

Track as separate issue after Phase 1 is merged and validated.

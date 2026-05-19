# Koog Evaluation — Full Findings

**Date:** 2026-05-09
**Issue:** [#56](https://github.com/leogallego/aapdroid/issues/56)
**Framework:** [Koog](https://github.com/JetBrains/koog) by JetBrains (Apache 2.0, 4.1K stars)
**Version analyzed:** Commit `24f6695` (May 2026), updated for release 0.8.0 (April 2026)

## Executive Summary

Koog is a viable replacement for ~1,260 lines of custom LLM/MCP/agent code across 4 subsystems. 6 of 8 evaluation questions returned positive results. ~~Two significant risks remain: OkHttp 4→5 version conflict and mandatory Kotlin 2.3.10 upgrade.~~ **Update (May 18):** Kotlin upgraded to 2.3.21, R8 enabled, Compose BOM verified. Only remaining blocker: OkHttp 4→5 compatibility test. Recommendation: incremental adoption in 3 phases.

| Aspect | Current Custom Code | Koog Replacement | Verdict |
|--------|-------------------|------------------|---------|
| LLM provider | 370 LOC, OpenAI-compat only | 9 providers, lenient serialization | Adopt |
| MCP client | 570 LOC, custom JSON-RPC + SSE | Official MCP SDK wrapper, `fromSseUrl()` | Adopt |
| ChatEngine | 210 LOC, tool loop + trim | Agent framework with compression strategies | Adopt |
| ToolExecutor | 113 LOC, caching + truncation | Tool registry + custom tools | Adopt |

---

## Q1: Can We Use Just the LLM Client Modules?

**Verdict: Partial — works but pulls transitive dependencies.**

Importing `prompt-executor-openai-client` gives you `OpenAILLMClient` with streaming chat completions and tool calling. You do NOT get:
- `agents-core` (agent state machine)
- `agents-mcp` (MCP integration)
- `agents-features-*` (history compression, memory, tracing)

But you DO get transitively:
- `agents-tools` (tool descriptor definitions)
- `agents-utils` (internal utilities)
- `rag-base` (vector/RAG base abstractions)
- `http-client-ktor` + Ktor client modules
- `prompt-*` modules (model, llm, structure, markdown, xml)
- ~15 total modules

These transitive modules are lightweight (interfaces + data classes), not the heavyweight agent machinery.

### Gradle dependency

```kotlin
// Minimum for OpenAI client only
implementation("ai.koog.prompt:prompt-executor-openai-client:$koogVersion")
```

---

## Q2: Does the OpenAI Client Handle Non-Standard Providers?

**Verdict: Yes — lenient serialization is a strong point.**

### JSON Configuration (from `AbstractOpenAILLMClient.kt`)

```kotlin
private val defaultJson = Json {
    ignoreUnknownKeys = true  // Handles provider-specific extra fields
    isLenient = true          // Accepts unquoted strings, trailing commas
    encodeDefaults = true     // Includes defaults in requests
    explicitNulls = false     // Omits null fields
    namingStrategy = JsonNamingStrategy.SnakeCase
}
```

### Data model design

Response models use nullable types with defaults:
```kotlin
@Serializable
class OpenAIUsage(
    val promptTokens: Int? = null,
    val completionTokens: Int? = null,
    val totalTokens: Int? = null,
    // ...
)
```

### Custom provider support

```kotlin
class CustomProvider(apiKey: String, baseUrl: String) : AbstractOpenAILLMClient(
    apiKey = apiKey,
    settings = object : OpenAIBaseSettings(
        baseUrl = baseUrl,
        chatCompletionsPath = "/chat/completions"
    ) {}
)
```

### Known provider quirks handled

- **OpenRouter**: Empty string arguments → auto-fixed to `{}`
- **DeepSeek**: Missing `audioTokens` field → fixed via nullable default
- **Multi-provider clients**: OpenRouter, DeepSeek, Ollama, DashScope all extend the same `AbstractOpenAILLMClient` base

### Comparison with our code

Our custom provider is intentionally lenient for Gemini-via-OpenAI-compat, local models, etc. Koog matches this behavior with `ignoreUnknownKeys` + nullable models. Equivalent or better.

---

## Q3: APK Size Impact

**Verdict: ~2-4 MB after R8 (4-8% of current 52 MB APK).**

### Size breakdown (pre-R8)

| Component | Estimated JAR Size |
|-----------|--------------------|
| Ktor client/server modules | ~2.5-3.2 MB |
| Koog's own modules (30+) | ~2-3.3 MB |
| Other transitive deps (kotlinx-io, datetime, logging, MCP SDK) | ~0.6 MB |
| **Total pre-R8** | **~5-7 MB** |
| **After R8 (40-60% shrink)** | **~2-4 MB** |

### Concerns

1. **`koog-agents` is an umbrella** — includes ALL 9 LLM provider clients and ALL features. No way to select only what you need.
2. **Ktor server modules** — `agents-core` transitively depends on `ktor-server-sse` + `ktor-server-cio`, which is dead weight on mobile (~1 MB).
3. **With R8 enabled** (#43), unused provider clients and server modules get stripped. Final impact should be manageable.

### Mitigation

- Enable R8 minification (#43) before adding Koog
- If using individual modules instead of `koog-agents`, avoid `agents-core` until needed

---

## Q4: Can We Keep Our ToolRouter?

**Verdict: Yes — build filtered ToolRegistry per agent run.**

### What Koog does NOT provide
- No built-in per-request tool filtering based on message content
- No "tool interceptor" for selective tool hiding
- No read-only mode or permission-based blocking

### Integration strategies

**Keyword filtering (ToolRouter's primary function):**
```kotlin
val fullRegistry = McpToolRegistryProvider.fromSseUrl(sseUrl)
val filteredRegistry = ToolRegistry {
    fullRegistry.tools
        .filter { tool -> toolRouter.shouldInclude(userMessage, tool.descriptor) }
        .forEach { tool(it) }
}
val agent = AIAgent.builder()
    .toolRegistry(filteredRegistry)
    .build()
```

**Read-only enforcement:**
```kotlin
class ReadOnlyEnvironment(
    private val delegate: AIAgentEnvironment,
    private val writeToolNames: Set<String>
) : AIAgentEnvironment {
    override suspend fun executeTool(toolCall: Message.Tool.Call): ReceivedToolResult {
        if (toolCall.tool in writeToolNames) {
            return ReceivedToolResult(
                content = "Tool '${toolCall.tool}' is blocked in read-only mode",
                resultKind = ToolResultKind.Failure(SecurityException("Write tool blocked")),
                // ...
            )
        }
        return delegate.executeTool(toolCall)
    }
}
```

### Key architectural note

Koog binds `ToolRegistry` at agent construction. Our ToolRouter filters per-message. Solution: create a new agent per user message (cheap) with a filtered registry. This maps naturally to our current ChatEngine flow.

---

## Q5: Does Koog's MCP Client Support SSE Transport?

**Verdict: Yes — full SSE support, wraps official MCP Kotlin SDK.**

### Transport types supported
1. **SSE** — via `SseClientTransport`
2. **Streamable HTTP** — via `StreamableHttpClientTransport`
3. **stdio** — via `StdioClientTransport`

### One-liner connection

```kotlin
val toolRegistry = McpToolRegistryProvider.fromSseUrl(
    "https://aap-gateway.example.com:8448/mcp/sse"
)
```

### Underlying SDK

Koog wraps `io.modelcontextprotocol:kotlin-sdk-client` v0.11.1 (official MCP Kotlin SDK). The MCP module is JVM-only (comment in build.gradle.kts: "Kotlin MCP SDK only supports JVM target for now"). Fine for Android.

### Session management

Handled by the MCP Kotlin SDK internally. `Client.connect(transport)` performs the initialize handshake. No built-in SSE reconnection — if connection drops, create a new client (same as our current approach).

### Platform note

MCP module imports:
```kotlin
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
```

---

## Q6: Ktor Migration Path

**Verdict: Coexistence works, but OkHttp version conflict is the biggest risk.**

### How ktor-client-okhttp bridges existing setup

```kotlin
val sharedOkHttpClient = OkHttpClient.Builder()
    .sslSocketFactory(customSslSocketFactory, customTrustManager)
    .addInterceptor(authInterceptor)
    .build()

// Retrofit (unchanged)
val retrofit = Retrofit.Builder()
    .client(sharedOkHttpClient)
    .build()

// Ktor for Koog (shares same OkHttp transport)
val ktorClient = HttpClient(OkHttp) {
    engine {
        preconfigured = sharedOkHttpClient
    }
}
```

The `preconfigured` property calls `okHttpClient.newBuilder()`, preserving SSL config, interceptors, and connection pool.

### OkHttp version conflict (CRITICAL)

| Component | Version |
|-----------|---------|
| Our app (Retrofit) | OkHttp **4.12.0** (Okio 2.x) |
| Koog | OkHttp **5.3.2** (Okio 3.x) |

Gradle resolves to one version. OkHttp 5.x has:
- Okio 3.x (Kotlin-first API, different from Okio 2.x)
- Same `okhttp3` package but internal API differences
- Retrofit 2.11 *should* be compatible, but needs testing

### Impact assessment

| Usage | After Koog |
|-------|------------|
| Retrofit + OkHttp for AAP API | Unchanged (keep as-is) |
| OkHttp for LLM API calls | **Replaced by Koog** |
| OkHttp for MCP SSE | **Replaced by Koog** |

At the transport level: single OkHttp instance. At the API level: Retrofit (AAP) + Ktor (LLM/MCP). Both are thin wrappers over OkHttp.

### Koog's own OkHttp module (alternative path)

Koog has `http-client-okhttp` with `KoogHttpClient.fromOkHttpClient()` that uses OkHttp directly without Ktor. But it's excluded from `koog-agents` and marked `@Experimental`.

### No need to migrate Retrofit

Retrofit remains fine for AAP REST API calls. Ktor handles LLM/MCP inside Koog. They coexist cleanly.

---

## Q7: History Compression + Chat UI

**Verdict: Fully compatible — compression is LLM-internal only.**

### How compression works

Compression modifies `llmSession.prompt.messages` (what goes to the LLM API). It has zero knowledge of the UI layer. Our LazyColumn display list is completely independent.

```
UI Layer:  messages in LazyColumn → append-only, full history
LLM Layer: agent.prompt.messages → compressed by strategy
```

### Available strategies

| Strategy | How it works | Best for |
|----------|-------------|----------|
| **WholeHistory** | Compress everything into one TLDR | Simple conversations |
| **FromLastNMessages(n)** | Keep last N, discard rest | Recent context only |
| **Chunked(n)** | Split into chunks, summarize each | **Tool-calling apps (recommended)** |
| **RetrieveFactsFromHistory** | Extract named concepts | Targeted context extraction |

### Recommendation for AAPdroid

**`Chunked(10)`** — preserves multi-step tool interaction context (e.g., "listed templates → launched job #42 → checked status"). Each chunk gets its own summary, so the LLM retains awareness of the full workflow.

Alternative: **`RetrieveFactsFromHistory`** with domain-specific concepts:
```kotlin
Concept("aap_instance", "Which AAP instance the user is connected to", FactType.SINGLE)
Concept("completed_actions", "Actions already performed", FactType.MULTIPLE)
```

### Caveat

Each compression triggers an extra LLM API call (`requestLLMWithoutTools()`). On mobile with metered data, trigger compression only when history exceeds a threshold, not on every turn.

---

## Q8: Local Tools Coexistence

**Verdict: Fully supported — same Tool base class, registry merging with priority.**

### Koog's tool abstraction

```kotlin
abstract class Tool<TArgs, TResult>(
    val argsType: TypeToken,
    val resultType: TypeToken,
    val descriptor: ToolDescriptor,
) {
    abstract suspend fun execute(args: TArgs): TResult
}
```

MCP tools (`McpTool`) are just `Tool<JSONObject, CallToolResult?>` subclasses — same interface as any custom tool.

### Wrapping Retrofit tools for Koog

```kotlin
object LaunchJobTool : SimpleTool<LaunchJobTool.Args>(
    argsType = typeToken<Args>(),
    name = "launch_job",
    description = "Launch a job template on AAP by its ID"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The ID of the job template to launch")
        val templateId: Int
    )
    override suspend fun execute(args: Args): String {
        val result = aapApiService.launchJob(args.templateId)
        return "Job launched. Job ID: ${result.id}"
    }
}
```

### Registry merging with priority

```kotlin
val localRegistry = ToolRegistry {
    tool(LaunchJobTool)
    tool(GetStatusTool)
}
val mcpRegistry = McpToolRegistryProvider.fromSseUrl(sseUrl)

// Local tools win on name collisions (distinctBy keeps first)
val combined = localRegistry + mcpRegistry
```

The `+` operator uses `distinctBy { it.name }` — first occurrence wins. Placing local registry first ensures local tools take priority over MCP equivalents with the same name.

### Architecture flow

```
User opens Assistant
  → Connect MCP → mcpRegistry
  → Create local tools → localRegistry
  → Merge: localRegistry + mcpRegistry (local wins)
  → Create Koog agent with combined registry
  → User message → agent.run() → LLM sees all tools
  → Local tool called → Retrofit in-process (no MCP hop)
  → MCP tool called → routed to MCP server
```

---

## Risk Assessment

### High Risk
- **OkHttp 4→5 conflict**: Requires testing Retrofit 2.11 + OkHttp 5.3.2 + Okio 3.x. Custom SSL code (`CertTrustManager`) may need API adjustments.
- **Kotlin 2.2→2.3 upgrade**: Compose compiler version must match. May introduce breaking changes.

### Medium Risk
- **APK size**: ~2-4 MB after R8. Mitigated by enabling R8 first (#43).
- **Ktor server modules**: Dead weight on mobile. No current way to exclude from `koog-agents`.

### Low Risk
- **ToolRouter integration**: Clear path via per-run filtered ToolRegistry.
- **MCP compatibility**: Wraps official SDK, SSE fully supported.
- **History compression**: Orthogonal to UI, well-designed.
- **Local tools**: Clean coexistence via registry merging.

---

## Migration Plan

### Phase 1: LLM Provider Only (Low Risk)

**Replace:** `OpenAiCompatibleProvider` (370 LOC)
**With:** Koog's `OpenAILLMClient` via `prompt-executor-openai-client`
**Keep:** ChatEngine, ToolExecutor, MCP client (unchanged)
**Validates:** Kotlin upgrade, OkHttp 5 compatibility, Ktor coexistence

### Phase 2: Agent Framework (Medium Risk)

**Replace:** ChatEngine (210 LOC) + ToolExecutor (113 LOC)
**With:** Koog's agent framework (`agents-core`)
**Integrate:** ToolRouter builds filtered ToolRegistry per message
**Benefit:** History compression, repetition detection, built-in caching

### Phase 3: MCP Client (Low Risk, after Phase 2)

**Replace:** Custom MCP client (570 LOC)
**With:** Koog's MCP module (`agents-mcp`)
**Use:** `McpToolRegistryProvider.fromSseUrl()` for AAP MCP servers
**Benefit:** Official MCP SDK, streamable HTTP support, session management

### Prerequisites (before Phase 1)

1. ~~Enable R8 minification (#43)~~ — **Done** (PR #60, APK 52→8.2 MB)
2. ~~Upgrade Kotlin 2.2.10 → 2.3.10~~ — **Done** (now 2.3.21, PR #63)
3. Test Retrofit 2.12 + OkHttp 5.3.2 compatibility — **Next step**
4. ~~Verify Compose BOM compatibility with Kotlin 2.3~~ — **Done** (2026.05.00, PR #63)

---

## Status Updates

### May 18, 2026

All prerequisites except OkHttp 5 compatibility are resolved. Koog 0.8.0 (latest stable) confirmed:
- Kotlin 2.3.10 required (we have 2.3.21)
- OkHttp 5.3.2 required (we have 4.12.0 — **only remaining blocker**)
- Ktor 3.2.2 (new dependency, coexists with Retrofit via `ktor-client-okhttp`)
- MCP SDK 0.8.1 (wraps official Kotlin SDK)
- kotlinx-serialization 1.10.0 (we have 1.11.0 — forward compatible)
- New in 0.8.0: LLMClient constructors decoupled from Ktor (#1742), reducing coupling for Phase 1

Next action: spike branch to test OkHttp 4.12→5.3.2 with Retrofit 2.12 + CertTrustManager + AuthInterceptor.

---

## Related Issues

- **#48** — MCP SDK migration → superseded by Phase 3
- **#54** — Local tools layer → independent, coexists with Koog (Q8)
- **#52** — MCP toolset endpoints → complementary, works with Koog's MCP
- **#43** — R8 minification → ~~prerequisite~~ **done**

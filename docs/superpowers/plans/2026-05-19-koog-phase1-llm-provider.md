# Koog Phase 1: LLM Provider Replacement — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace 370 LOC of hand-rolled HTTP/SSE code (`OpenAiCompatibleProvider`) with an adapter over Koog's `OpenAILLMClient`.

**Architecture:** Adapter pattern — `KoogLlmProvider` implements our existing `LlmProvider` interface, delegates to Koog's `AbstractOpenAILLMClient` internally. ChatEngine and all data models stay unchanged.

**Tech Stack:** Koog 0.8.0 (`prompt-executor-openai-client`), Ktor CIO (transitive), kotlinx-serialization

**Spec:** `docs/superpowers/specs/2026-05-19-koog-phase1-llm-provider-design.md`

---

### Task 0: Create feature branch and commit debug versionNameSuffix

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Create branch for the version suffix change**

```bash
git checkout main
git checkout -b chore/debug-version-suffix
```

- [ ] **Step 2: Verify the versionNameSuffix change is present**

The debug `versionNameSuffix = "-dev+$gitSha"` change is already in `app/build.gradle.kts` (uncommitted on main). Verify it looks correct:

```kotlin
buildTypes {
    debug {
        val gitSha = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
        versionNameSuffix = "-dev+$gitSha"
    }
    release {
```

- [ ] **Step 3: Commit and push**

```bash
git add app/build.gradle.kts
git commit -m "Add -dev+<sha> suffix to debug build version name"
```

- [ ] **Step 4: Create PR, merge, then start Phase 1 branch**

```bash
git push -u origin chore/debug-version-suffix
gh pr create --title "Add dev suffix to debug builds" --body "..."
```

After merge:

```bash
git checkout main && git pull
git checkout -b feature/koog-phase1-llm-provider
```

---

### Task 1: Add Koog dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add Koog version and library to version catalog**

In `gradle/libs.versions.toml`, add to `[versions]`:

```toml
koog = "0.8.0"
```

Add to `[libraries]`:

```toml
koog-openai-client = { group = "ai.koog.prompt", name = "prompt-executor-openai-client", version.ref = "koog" }
```

- [ ] **Step 2: Add dependency to build.gradle.kts**

In `app/build.gradle.kts` `dependencies` block, add after the kotlinx-serialization line:

```kotlin
implementation(libs.koog.openai.client)
```

- [ ] **Step 3: Verify dependency resolution**

Run: `./gradlew dependencies --configuration debugRuntimeClasspath | grep koog`

Expected: shows `ai.koog.prompt:prompt-executor-openai-client:0.8.0` and transitive Ktor dependencies.

- [ ] **Step 4: Verify compilation still passes**

Run: `./gradlew compileDebugKotlin`

Expected: BUILD SUCCESSFUL (no code uses Koog yet, just dependency resolution check).

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "Add Koog 0.8.0 prompt-executor-openai-client dependency"
```

---

### Task 2: Move exception classes to LlmTypes.kt

**Files:**
- Modify: `app/src/main/kotlin/com/example/aapremote/assistant/llm/LlmTypes.kt`
- Modify: `app/src/main/kotlin/com/example/aapremote/assistant/llm/OpenAiCompatibleProvider.kt`

The four exception classes currently live in `OpenAiCompatibleProvider.kt` but are referenced by `ChatEngine.kt`. Move them to `LlmTypes.kt` so they survive when we delete the provider.

- [ ] **Step 1: Add exception classes to LlmTypes.kt**

Add at the end of `app/src/main/kotlin/com/example/aapremote/assistant/llm/LlmTypes.kt`:

```kotlin
class LlmAuthException(message: String) : Exception(message)
class LlmRateLimitException(message: String, val retryAfter: Int? = null) : Exception(message)
class LlmServerException(message: String) : Exception(message)
class LlmTimeoutException(message: String) : Exception(message)
```

- [ ] **Step 2: Remove exception classes from OpenAiCompatibleProvider.kt**

Delete lines 39-42 from `OpenAiCompatibleProvider.kt`:

```kotlin
// DELETE these lines:
class LlmAuthException(message: String) : Exception(message)
class LlmRateLimitException(message: String, val retryAfter: Int? = null) : Exception(message)
class LlmServerException(message: String) : Exception(message)
class LlmTimeoutException(message: String) : Exception(message)
```

The imports in `ChatEngine.kt` already reference `com.example.aapremote.assistant.llm.LlmAuthException` etc., which resolves to the same package regardless of which file they're in.

- [ ] **Step 3: Verify compilation**

Run: `./gradlew compileDebugKotlin`

Expected: BUILD SUCCESSFUL (same package, no import changes needed).

- [ ] **Step 4: Run existing tests**

Run: `./gradlew testDebugUnitTest`

Expected: All tests pass (exception classes are in the same package).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/example/aapremote/assistant/llm/LlmTypes.kt
git add app/src/main/kotlin/com/example/aapremote/assistant/llm/OpenAiCompatibleProvider.kt
git commit -m "Move LLM exception classes to LlmTypes.kt"
```

---

### Task 3: Create KoogLlmProvider

**Files:**
- Create: `app/src/main/kotlin/com/example/aapremote/assistant/llm/KoogLlmProvider.kt`

- [ ] **Step 1: Create the adapter class**

Create `app/src/main/kotlin/com/example/aapremote/assistant/llm/KoogLlmProvider.kt`:

```kotlin
package com.example.aapremote.assistant.llm

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.assistant.engine.ChatMessage
import com.example.aapremote.assistant.engine.Role
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.network.CertTrustManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.io.IOException
import java.net.SocketTimeoutException
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import kotlinx.datetime.Clock

class KoogLlmProvider(
    private val config: LlmProviderConfig.OpenAiCompatible,
    trustSelfSigned: Boolean = false
) : LlmProvider {

    private val koogClient: OpenAILLMClient = createClient(config, trustSelfSigned)

    private val model = LLModel(
        provider = LLMProvider.OpenAI,
        id = config.model,
        capabilities = listOf(LLMCapability.Completion, LLMCapability.Tools),
        contextLength = 128_000L,
        maxOutputTokens = 16_384L
    )

    override suspend fun generate(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        maxTokens: Int?
    ): LlmResult {
        val prompt = buildPrompt(messages)
        val toolDescriptors = tools.map { it.toToolDescriptor() }

        try {
            val responses = koogClient.execute(prompt, model, toolDescriptors)
            return responsesToLlmResult(responses)
        } catch (e: Exception) {
            throw mapException(e)
        }
    }

    override fun generateStream(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        maxTokens: Int?
    ): Flow<StreamEvent> = flow {
        val prompt = buildPrompt(messages)
        val toolDescriptors = tools.map { it.toToolDescriptor() }

        try {
            val textBuilder = StringBuilder()
            val toolCalls = mutableListOf<ToolCall>()

            koogClient.executeStreaming(prompt, model, toolDescriptors).collect { frame ->
                when (frame) {
                    is StreamFrame.Append -> {
                        textBuilder.append(frame.text)
                        emit(StreamEvent.TextDelta(frame.text))
                    }
                    is StreamFrame.ToolCall -> {
                        val argsJson = try {
                            Json.parseToJsonElement(frame.content).jsonObject
                        } catch (_: Exception) {
                            JsonObject(emptyMap())
                        }
                        toolCalls.add(ToolCall(
                            id = frame.id ?: "call_${toolCalls.size}",
                            name = frame.name,
                            arguments = argsJson
                        ))
                        emit(StreamEvent.ToolCallStart(
                            frame.id ?: "call_${toolCalls.size - 1}",
                            frame.name
                        ))
                    }
                    is StreamFrame.End -> {
                        val result = LlmResult(
                            text = textBuilder.toString().ifEmpty { null },
                            toolCalls = toolCalls
                        )
                        emit(StreamEvent.Done(result))
                    }
                }
            }
        } catch (e: Exception) {
            emit(StreamEvent.Error(mapException(e)))
        }
    }

    override fun isAvailable(): Boolean =
        config.url.isNotBlank() && config.model.isNotBlank()

    override fun modelInfo(): ModelInfo = ModelInfo(
        name = config.model,
        isLocal = false
    )

    private fun buildPrompt(messages: List<ChatMessage>): Prompt {
        val koogMessages = messages.map { msg ->
            when (msg.role) {
                Role.SYSTEM -> Message.System(
                    msg.content,
                    RequestMetaInfo.create(Clock.System)
                )
                Role.USER -> Message.User(
                    msg.content,
                    RequestMetaInfo.create(Clock.System)
                )
                Role.ASSISTANT -> {
                    if (msg.toolCalls != null && msg.toolCalls.isNotEmpty()) {
                        Message.Tool.Call(
                            id = msg.toolCalls.first().id,
                            tool = msg.toolCalls.first().name,
                            content = msg.toolCalls.first().arguments.toString(),
                            metaInfo = ResponseMetaInfo.create(Clock.System)
                        )
                    } else {
                        Message.Assistant(
                            msg.content,
                            ResponseMetaInfo.create(Clock.System)
                        )
                    }
                }
                Role.TOOL -> Message.Tool.Result(
                    id = msg.toolCallId,
                    tool = "",
                    content = msg.content,
                    metaInfo = RequestMetaInfo.create(Clock.System)
                )
            }
        }
        return Prompt(messages = koogMessages, id = "aapdroid")
    }

    private fun responsesToLlmResult(responses: List<Message.Response>): LlmResult {
        val textParts = mutableListOf<String>()
        val toolCalls = mutableListOf<ToolCall>()

        responses.forEach { response ->
            when (response) {
                is Message.Assistant -> textParts.add(response.content)
                is Message.Tool.Call -> {
                    val argsJson = try {
                        Json.parseToJsonElement(response.content).jsonObject
                    } catch (_: Exception) {
                        JsonObject(emptyMap())
                    }
                    toolCalls.add(ToolCall(
                        id = response.id ?: "call_${toolCalls.size}",
                        name = response.tool,
                        arguments = argsJson
                    ))
                }
                else -> {}
            }
        }

        return LlmResult(
            text = textParts.joinToString("").ifEmpty { null },
            toolCalls = toolCalls
        )
    }

    private fun mapException(e: Exception): Exception = when (e) {
        is ClientRequestException -> {
            val status = e.response.status.value
            val message = e.message ?: "Request failed"
            when (status) {
                401, 403 -> LlmAuthException("Authentication failed — check API key")
                429 -> LlmRateLimitException("Rate limited — try again later")
                else -> LlmServerException("Server error ($status): $message")
            }
        }
        is ServerResponseException -> {
            val status = e.response.status.value
            LlmServerException("Server error ($status)")
        }
        is SocketTimeoutException -> LlmTimeoutException("Response timed out")
        is IOException -> e
        else -> e
    }

    companion object {
        private fun createClient(
            config: LlmProviderConfig.OpenAiCompatible,
            trustSelfSigned: Boolean
        ): OpenAILLMClient {
            val baseUrl = config.url.trimEnd('/')
            val settings = OpenAIClientSettings(
                baseUrl = baseUrl,
                chatCompletionsPath = "chat/completions"
            )
            val httpClient = if (trustSelfSigned) {
                HttpClient(CIO) {
                    engine {
                        https {
                            trustManager = CertTrustManager.createTrustAllManager()
                        }
                    }
                }
            } else {
                HttpClient(CIO)
            }
            return OpenAILLMClient(
                apiKey = config.apiKey ?: "",
                settings = settings,
                baseClient = httpClient
            )
        }
    }
}

private fun ToolSpec.toToolDescriptor(): ToolDescriptor {
    val schema = compactSchema(parametersSchema)
    val properties = schema["properties"]?.jsonObject ?: JsonObject(emptyMap())
    val required = schema["required"]?.jsonArray?.map {
        it.jsonPrimitive.content
    }?.toSet() ?: emptySet()

    val requiredParams = mutableListOf<ToolParameterDescriptor>()
    val optionalParams = mutableListOf<ToolParameterDescriptor>()

    properties.forEach { (name, value) ->
        val prop = value.jsonObject
        val type = when (prop["type"]?.jsonPrimitive?.contentOrNull) {
            "integer" -> ToolParameterType.Integer
            "number" -> ToolParameterType.Float
            "boolean" -> ToolParameterType.Boolean
            "array" -> ToolParameterType.List(ToolParameterType.String)
            else -> ToolParameterType.String
        }

        val enumEntries = prop["enum"]?.jsonArray?.map {
            it.jsonPrimitive.content
        }?.toTypedArray()

        val paramType = if (enumEntries != null) ToolParameterType.Enum(enumEntries) else type
        val descriptor = ToolParameterDescriptor(name, name, paramType)

        if (name in required) requiredParams.add(descriptor)
        else optionalParams.add(descriptor)
    }

    return ToolDescriptor(
        name = this.name,
        description = this.description,
        requiredParameters = requiredParams,
        optionalParameters = optionalParams
    )
}

private fun compactSchema(schema: JsonObject): JsonObject {
    val builder = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
    schema["type"]?.let { builder["type"] = it }
    schema["required"]?.jsonArray?.let { arr ->
        if (arr.isNotEmpty()) builder["required"] = arr
    }
    schema["properties"]?.jsonObject?.let { props ->
        val compactedProps = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
        props.forEach { (key, value) ->
            val prop = value.jsonObject
            val compacted = mutableMapOf<String, kotlinx.serialization.json.JsonElement>()
            prop["type"]?.let { compacted["type"] = it }
            prop["enum"]?.jsonArray?.let { enumArr ->
                if (enumArr.size > 8) {
                    compacted["enum"] = kotlinx.serialization.json.JsonArray(enumArr.take(8))
                } else {
                    compacted["enum"] = enumArr
                }
            }
            compactedProps[key] = JsonObject(compacted)
        }
        builder["properties"] = JsonObject(compactedProps)
    }
    return JsonObject(builder)
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileDebugKotlin`

Expected: BUILD SUCCESSFUL. If import paths differ from what's shown above (Koog package structure may vary), fix imports based on IDE suggestions or `./gradlew compileDebugKotlin 2>&1 | grep "Unresolved reference"`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/com/example/aapremote/assistant/llm/KoogLlmProvider.kt
git commit -m "Add KoogLlmProvider adapter implementing LlmProvider"
```

---

### Task 4: Wire KoogLlmProvider into AssistantViewModel

**Files:**
- Modify: `app/src/main/kotlin/com/example/aapremote/assistant/presentation/AssistantViewModel.kt`

- [ ] **Step 1: Update import**

Replace:
```kotlin
import com.example.aapremote.assistant.llm.OpenAiCompatibleProvider
```
With:
```kotlin
import com.example.aapremote.assistant.llm.KoogLlmProvider
```

- [ ] **Step 2: Update provider instantiation**

In `sendMessage()`, replace lines 126-130:

```kotlin
val llmClient = buildLlmClient()
val provider = when (config) {
    is LlmProviderConfig.OpenAiCompatible ->
        OpenAiCompatibleProvider(config, llmClient, json)
}
```

With:

```kotlin
val trustSelfSigned = tokenManager.activeInstance.value?.trustSelfSigned == true
val provider = when (config) {
    is LlmProviderConfig.OpenAiCompatible ->
        KoogLlmProvider(config, trustSelfSigned)
}
```

- [ ] **Step 3: Remove unused buildLlmClient if no other callers**

Check if `buildLlmClient()` is used elsewhere. It's also used in `fetchAvailableModels()` (line 261) for `ModelFetcher`. Keep the method — just remove the call in `sendMessage()`.

- [ ] **Step 4: Verify compilation**

Run: `./gradlew compileDebugKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Run existing tests**

Run: `./gradlew testDebugUnitTest`

Expected: All tests pass. `ChatEngineTest` uses `FakeLlmProvider` so it won't be affected. `OpenAiCompatibleProviderTest` still passes (provider still exists, just not wired up).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/kotlin/com/example/aapremote/assistant/presentation/AssistantViewModel.kt
git commit -m "Wire KoogLlmProvider into AssistantViewModel"
```

---

### Task 5: Write KoogLlmProvider tests

**Files:**
- Create: `app/src/test/kotlin/com/example/aapremote/assistant/llm/KoogLlmProviderTest.kt`

- [ ] **Step 1: Write test class**

Create `app/src/test/kotlin/com/example/aapremote/assistant/llm/KoogLlmProviderTest.kt`:

```kotlin
package com.example.aapremote.assistant.llm

import app.cash.turbine.test
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.assistant.engine.ChatMessage
import com.example.aapremote.assistant.engine.Role
import com.example.aapremote.assistant.tools.ToolSpec
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KoogLlmProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: KoogLlmProvider

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        val config = LlmProviderConfig.OpenAiCompatible(
            url = server.url("/v1").toString(),
            model = "test-model",
            apiKey = "test-key"
        )
        provider = KoogLlmProvider(config)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `generate sends correct request format`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"choices":[{"message":{"content":"Hello!","role":"assistant"}}]}""")
                .setHeader("Content-Type", "application/json")
        )

        val messages = listOf(ChatMessage(role = Role.USER, content = "Hi"))
        provider.generate(messages, emptyList())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.contains("chat/completions"))

        val body = request.body.readUtf8()
        assertTrue(body.contains("test-model"))
    }

    @Test
    fun `generate parses text response`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"choices":[{"message":{"content":"The answer is 42","role":"assistant"}}]}""")
                .setHeader("Content-Type", "application/json")
        )

        val result = provider.generate(
            listOf(ChatMessage(role = Role.USER, content = "What?")),
            emptyList()
        )

        assertEquals("The answer is 42", result.text)
        assertTrue(result.toolCalls.isEmpty())
    }

    @Test
    fun `generate parses tool calls`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"choices":[{"message":{"role":"assistant","content":null,"tool_calls":[{"id":"call_1","type":"function","function":{"name":"list_jobs","arguments":"{\"status\":\"failed\"}"}}]}}]}""")
                .setHeader("Content-Type", "application/json")
        )

        val result = provider.generate(
            listOf(ChatMessage(role = Role.USER, content = "List failed jobs")),
            emptyList()
        )

        assertNull(result.text)
        assertEquals(1, result.toolCalls.size)
        assertEquals("call_1", result.toolCalls[0].id)
        assertEquals("list_jobs", result.toolCalls[0].name)
    }

    @Test
    fun `generate throws LlmAuthException on 401`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        try {
            provider.generate(
                listOf(ChatMessage(role = Role.USER, content = "test")),
                emptyList()
            )
            throw AssertionError("Expected LlmAuthException")
        } catch (e: LlmAuthException) {
            // expected
        }
    }

    @Test
    fun `generate throws LlmServerException on 500`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        try {
            provider.generate(
                listOf(ChatMessage(role = Role.USER, content = "test")),
                emptyList()
            )
            throw AssertionError("Expected LlmServerException")
        } catch (e: LlmServerException) {
            // expected
        }
    }

    @Test
    fun `isAvailable returns true when config is valid`() {
        assertTrue(provider.isAvailable())
    }

    @Test
    fun `modelInfo returns correct model name`() {
        val info = provider.modelInfo()
        assertEquals("test-model", info.name)
        assertEquals(false, info.isLocal)
    }

    @Test
    fun `generate includes tools in request when provided`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"choices":[{"message":{"content":"ok","role":"assistant"}}]}""")
                .setHeader("Content-Type", "application/json")
        )

        val tools = listOf(
            ToolSpec(
                name = "list_jobs",
                description = "List AAP jobs",
                parametersSchema = buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("status") {
                            put("type", "string")
                        }
                    }
                }
            )
        )

        provider.generate(
            listOf(ChatMessage(role = Role.USER, content = "test")),
            tools
        )

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("list_jobs"))
    }
}
```

- [ ] **Step 2: Run tests**

Run: `./gradlew testDebugUnitTest --tests "com.example.aapremote.assistant.llm.KoogLlmProviderTest"`

Expected: All tests pass. If any fail due to Koog's request format differing from what MockWebServer expects, adjust the test expectations to match Koog's actual wire format.

**Note:** These tests may need adjustment during implementation. Koog uses Ktor CIO (not OkHttp) so MockWebServer may not intercept Ktor's requests directly. If MockWebServer doesn't work with Ktor CIO, switch to Ktor's `MockEngine` for testing:

```kotlin
// Alternative test setup if MockWebServer doesn't work with Ktor CIO
val mockEngine = io.ktor.client.engine.mock.MockEngine { request ->
    respond(
        content = """{"choices":[{"message":{"content":"Hello!","role":"assistant"}}]}""",
        status = HttpStatusCode.OK,
        headers = headersOf("Content-Type", "application/json")
    )
}
```

If this is needed, the `KoogLlmProvider` constructor would need to accept an optional `HttpClient` for testing, or use a factory pattern.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/kotlin/com/example/aapremote/assistant/llm/KoogLlmProviderTest.kt
git commit -m "Add KoogLlmProvider unit tests"
```

---

### Task 6: Delete OpenAiCompatibleProvider

**Files:**
- Delete: `app/src/main/kotlin/com/example/aapremote/assistant/llm/OpenAiCompatibleProvider.kt`
- Delete: `app/src/test/kotlin/com/example/aapremote/assistant/llm/OpenAiCompatibleProviderTest.kt`

- [ ] **Step 1: Verify no remaining references**

```bash
grep -r "OpenAiCompatibleProvider" app/src/ --include="*.kt"
```

Expected: no results (we already updated the import in AssistantViewModel).

- [ ] **Step 2: Check if toOpenAiTool or compactSchema are used elsewhere**

```bash
grep -r "toOpenAiTool\|compactSchema" app/src/ --include="*.kt"
```

Expected: only in `OpenAiCompatibleProvider.kt` (which we're deleting) and `KoogLlmProvider.kt` (which has its own copy). If any other file references them, move the functions instead of deleting.

- [ ] **Step 3: Delete the files**

```bash
git rm app/src/main/kotlin/com/example/aapremote/assistant/llm/OpenAiCompatibleProvider.kt
git rm app/src/test/kotlin/com/example/aapremote/assistant/llm/OpenAiCompatibleProviderTest.kt
```

- [ ] **Step 4: Verify compilation and tests**

Run: `./gradlew compileDebugKotlin && ./gradlew testDebugUnitTest`

Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```bash
git commit -m "Remove OpenAiCompatibleProvider replaced by KoogLlmProvider"
```

---

### Task 7: Final verification

**Files:** None (verification only)

- [ ] **Step 1: Full build**

Run: `./gradlew assembleDebug`

Expected: BUILD SUCCESSFUL. APK generated.

- [ ] **Step 2: Full test suite**

Run: `./gradlew testDebugUnitTest`

Expected: All tests pass.

- [ ] **Step 3: Device smoke test**

Install debug APK on device/emulator. Test:
1. Open AI Assistant
2. Configure LLM provider (OpenRouter, OpenAI, or local model)
3. Send a simple message — verify streaming text response
4. Send a message that triggers tool calls (e.g., "list my job templates") — verify tools execute and results display
5. Verify self-signed cert trust works if applicable

- [ ] **Step 4: Create PR**

```bash
git push -u origin feature/koog-phase1-llm-provider
gh pr create --title "Replace LLM provider with Koog client (Phase 1)" --body "..."
```

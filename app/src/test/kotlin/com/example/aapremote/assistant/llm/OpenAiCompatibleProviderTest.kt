package com.example.aapremote.assistant.llm

import app.cash.turbine.test
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.assistant.engine.ChatMessage
import com.example.aapremote.assistant.engine.Role
import com.example.aapremote.assistant.tools.ToolSpec
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenAiCompatibleProviderTest {

    private lateinit var server: MockWebServer
    private lateinit var provider: OpenAiCompatibleProvider
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        val config = LlmProviderConfig.OpenAiCompatible(
            url = server.url("/v1").toString(),
            model = "test-model",
            apiKey = "test-key"
        )
        provider = OpenAiCompatibleProvider(config, OkHttpClient(), json)
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
        assertTrue(request.path!!.endsWith("/chat/completions"))
        assertEquals("Bearer test-key", request.getHeader("Authorization"))

        val body = request.body.readUtf8()
        assertTrue(body.contains("\"model\":\"test-model\""))
        assertTrue(body.contains("\"stream\":false"))
    }

    @Test
    fun `generate parses text response`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"choices":[{"message":{"content":"The answer is 42","role":"assistant"}}]}""")
                .setHeader("Content-Type", "application/json")
        )

        val result = provider.generate(
            listOf(ChatMessage(role = Role.USER, content = "What is the answer?")),
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

    @Test(expected = LlmAuthException::class)
    fun `generate throws LlmAuthException on 401`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        provider.generate(
            listOf(ChatMessage(role = Role.USER, content = "test")),
            emptyList()
        )
    }

    @Test(expected = LlmRateLimitException::class)
    fun `generate throws LlmRateLimitException on 429`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))

        provider.generate(
            listOf(ChatMessage(role = Role.USER, content = "test")),
            emptyList()
        )
    }

    @Test(expected = LlmServerException::class)
    fun `generate throws LlmServerException on 500`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        provider.generate(
            listOf(ChatMessage(role = Role.USER, content = "test")),
            emptyList()
        )
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
        assertTrue(body.contains("\"tools\""))
        assertTrue(body.contains("\"list_jobs\""))
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
    fun `toOpenAiTool converts ToolSpec correctly`() {
        val spec = ToolSpec(
            name = "test_tool",
            description = "A test tool",
            parametersSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {}
            }
        )

        val result = spec.toOpenAiTool()
        assertEquals("function", result["type"].toString().trim('"'))

        val function = result["function"] as JsonObject
        assertEquals("\"test_tool\"", function["name"].toString())
        assertEquals("\"A test tool\"", function["description"].toString())
        assertNotNull(function["parameters"])
    }
}

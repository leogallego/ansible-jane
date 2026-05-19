package com.example.aapremote.assistant.llm

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

    private fun textResponse(content: String) = MockResponse()
        .setBody(
            """{"id":"chatcmpl-1","object":"chat.completion","created":1700000000,"model":"test-model",""" +
                """"choices":[{"index":0,"message":{"role":"assistant","content":"$content"},"finish_reason":"stop"}],""" +
                """"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}"""
        )
        .setHeader("Content-Type", "application/json")

    private fun toolCallResponse() = MockResponse()
        .setBody(
            """{"id":"chatcmpl-1","object":"chat.completion","created":1700000000,"model":"test-model",""" +
                """"choices":[{"index":0,"message":{"role":"assistant","content":null,""" +
                """"tool_calls":[{"id":"call_1","type":"function","function":{"name":"list_jobs","arguments":"{\"status\":\"failed\"}"}}]},""" +
                """"finish_reason":"tool_calls"}],""" +
                """"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}"""
        )
        .setHeader("Content-Type", "application/json")

    @Test
    fun `generate sends correct request format`() = runTest {
        server.enqueue(textResponse("Hello!"))

        val messages = listOf(ChatMessage(role = Role.USER, content = "Hi"))
        provider.generate(messages, emptyList())

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.endsWith("/chat/completions"))
        assertEquals("Bearer test-key", request.getHeader("Authorization"))

        val body = request.body.readUtf8()
        assertTrue(body.contains("\"model\":\"test-model\""))
    }

    @Test
    fun `generate parses text response`() = runTest {
        server.enqueue(textResponse("The answer is 42"))

        val result = provider.generate(
            listOf(ChatMessage(role = Role.USER, content = "What is the answer?")),
            emptyList()
        )

        assertEquals("The answer is 42", result.text)
        assertTrue(result.toolCalls.isEmpty())
    }

    @Test
    fun `generate parses tool calls`() = runTest {
        server.enqueue(toolCallResponse())

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
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"message":"Unauthorized"}}""")
        )

        try {
            provider.generate(
                listOf(ChatMessage(role = Role.USER, content = "test")),
                emptyList()
            )
            throw AssertionError("Expected exception was not thrown")
        } catch (e: LlmAuthException) {
            // Expected
        }
    }

    @Test
    fun `generate throws LlmRateLimitException on 429`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(429)
                .setBody("""{"error":{"message":"Rate limit exceeded"}}""")
        )

        try {
            provider.generate(
                listOf(ChatMessage(role = Role.USER, content = "test")),
                emptyList()
            )
            throw AssertionError("Expected exception was not thrown")
        } catch (e: LlmRateLimitException) {
            // Expected
        }
    }

    @Test
    fun `generate throws LlmServerException on 500`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"error":{"message":"Internal Server Error"}}""")
        )

        try {
            provider.generate(
                listOf(ChatMessage(role = Role.USER, content = "test")),
                emptyList()
            )
            throw AssertionError("Expected exception was not thrown")
        } catch (e: LlmServerException) {
            // Expected
        }
    }

    @Test
    fun `generate includes tools in request when provided`() = runTest {
        server.enqueue(textResponse("ok"))

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
}

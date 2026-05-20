package com.example.aapremote.assistant.llm

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.assistant.tools.toToolDescriptor
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
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
        provider.close()
        server.shutdown()
    }

    private fun userPrompt(content: String): Prompt = Prompt(
        messages = listOf(Message.User(content = content, metaInfo = RequestMetaInfo.Empty)),
        id = "test"
    )

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

        provider.generate(userPrompt("Hi"), emptyList())

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

        val result = provider.generate(userPrompt("What is the answer?"), emptyList())

        val assistant = result.filterIsInstance<Message.Assistant>()
        assertEquals(1, assistant.size)
        assertEquals("The answer is 42", assistant[0].content)
    }

    @Test
    fun `generate parses tool calls`() = runTest {
        server.enqueue(toolCallResponse())

        val result = provider.generate(userPrompt("List failed jobs"), emptyList())

        val toolCalls = result.filterIsInstance<Message.Tool.Call>()
        assertEquals(1, toolCalls.size)
        assertEquals("call_1", toolCalls[0].id)
        assertEquals("list_jobs", toolCalls[0].tool)
    }

    @Test
    fun `generate throws LlmAuthException on 401`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":{"message":"Unauthorized"}}""")
        )

        try {
            provider.generate(userPrompt("test"), emptyList())
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
            provider.generate(userPrompt("test"), emptyList())
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
            provider.generate(userPrompt("test"), emptyList())
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

        provider.generate(userPrompt("test"), tools.map { it.toToolDescriptor() })

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

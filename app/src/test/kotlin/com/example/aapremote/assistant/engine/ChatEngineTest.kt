package com.example.aapremote.assistant.engine

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import app.cash.turbine.test
import com.example.aapremote.assistant.llm.LlmProvider
import com.example.aapremote.assistant.llm.ModelInfo
import com.example.aapremote.assistant.tools.Tool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatEngineTest {

    @Test
    fun `single turn text response emits TextDelta and AssistantMessage`() = runTest {
        val provider = FakeLlmProvider(listOf(
            FakeResponse(text = "Hello there!")
        ))
        val executor = ToolExecutor(emptyList())
        val engine = ChatEngine(provider, executor)

        engine.processMessage("Hi", emptyList(), emptyList()).test {
            val delta = awaitItem()
            assertTrue(delta is ChatEvent.TextDelta)

            val msg = awaitItem()
            assertTrue(msg is ChatEvent.AssistantMessage)
            assertEquals("Hello there!", (msg as ChatEvent.AssistantMessage).fullText)
            assertEquals(0, msg.toolCallCount)

            awaitComplete()
        }
    }

    @Test
    fun `tool calling loop executes tools and re-sends to LLM`() = runTest {
        val provider = FakeLlmProvider(listOf(
            FakeResponse(toolCalls = listOf(
                FakeToolCall("call_1", "list_jobs", """{"status":"failed"}""")
            )),
            FakeResponse(text = "Found 3 failed jobs.")
        ))
        val fakeTool = FakeTool(
            spec = ToolSpec("list_jobs", "List jobs", JsonObject(emptyMap())),
            result = ToolResult(success = true, data = "[{\"id\":1},{\"id\":2},{\"id\":3}]")
        )
        val executor = ToolExecutor(listOf<Tool>(fakeTool))
        val engine = ChatEngine(provider, executor)

        engine.processMessage("Show failed jobs", emptyList(), emptyList()).test {
            val executing = awaitItem()
            assertTrue(executing is ChatEvent.ToolExecuting)
            assertEquals("list_jobs", (executing as ChatEvent.ToolExecuting).toolName)

            val toolResult = awaitItem()
            assertTrue(toolResult is ChatEvent.ToolResult)

            val delta = awaitItem()
            assertTrue(delta is ChatEvent.TextDelta)

            val msg = awaitItem()
            assertTrue(msg is ChatEvent.AssistantMessage)
            assertEquals("Found 3 failed jobs.", (msg as ChatEvent.AssistantMessage).fullText)
            assertEquals(1, msg.toolCallCount)

            awaitComplete()
        }
    }

    @Test
    fun `max iteration limit stops infinite loops`() = runTest {
        val provider = FakeLlmProvider(
            (1..15).map {
                FakeResponse(toolCalls = listOf(
                    FakeToolCall("call_1", "loop_tool", "{}")
                ))
            }
        )
        val fakeTool = FakeTool(
            spec = ToolSpec("loop_tool", "Loops", JsonObject(emptyMap())),
            result = ToolResult(success = true, data = "ok")
        )
        val executor = ToolExecutor(listOf<Tool>(fakeTool))
        val engine = ChatEngine(provider, executor, maxIterations = 3)

        engine.processMessage("loop", emptyList(), emptyList()).test {
            val events = mutableListOf<ChatEvent>()
            do {
                val event = awaitItem()
                events.add(event)
            } while (event !is ChatEvent.AssistantMessage)

            val lastEvent = events.last()
            assertTrue(lastEvent is ChatEvent.AssistantMessage)
            assertTrue(
                (lastEvent as ChatEvent.AssistantMessage).fullText
                    .contains("tool call limit")
            )

            awaitComplete()
        }
    }

    @Test
    fun `LLM error emits ChatEvent Error`() = runTest {
        val provider = FakeLlmProvider(error = RuntimeException("Connection refused"))
        val executor = ToolExecutor(emptyList())
        val engine = ChatEngine(provider, executor)

        engine.processMessage("test", emptyList(), emptyList()).test {
            val error = awaitItem()
            assertTrue(error is ChatEvent.Error)
            assertTrue((error as ChatEvent.Error).message.contains("Connection refused"))

            awaitComplete()
        }
    }
}

private data class FakeToolCall(val id: String, val name: String, val arguments: String)

private data class FakeResponse(
    val text: String? = null,
    val toolCalls: List<FakeToolCall> = emptyList()
)

private class FakeLlmProvider(
    private val responses: List<FakeResponse> = emptyList(),
    private val error: Throwable? = null
) : LlmProvider {
    private var callIndex = 0

    override suspend fun generate(
        prompt: Prompt,
        tools: List<ToolDescriptor>,
        maxTokens: Int?
    ): List<Message.Response> {
        val resp = responses.getOrElse(callIndex++) { FakeResponse() }
        return buildResponseMessages(resp)
    }

    override fun generateStream(
        prompt: Prompt,
        tools: List<ToolDescriptor>,
        maxTokens: Int?
    ): Flow<StreamFrame> = flow {
        if (error != null) {
            throw error
        }
        val resp = responses.getOrElse(callIndex++) { FakeResponse() }
        resp.text?.let { emit(StreamFrame.TextDelta(it)) }
        for (tc in resp.toolCalls) {
            emit(StreamFrame.ToolCallComplete(
                id = tc.id,
                name = tc.name,
                content = tc.arguments
            ))
        }
        emit(StreamFrame.End(finishReason = "stop"))
    }

    override fun isAvailable(): Boolean = true
    override fun modelInfo(): ModelInfo = ModelInfo("fake-model")

    private fun buildResponseMessages(resp: FakeResponse): List<Message.Response> {
        val messages = mutableListOf<Message.Response>()
        if (resp.text != null) {
            messages.add(Message.Assistant(content = resp.text, metaInfo = ResponseMetaInfo.Empty))
        }
        for (tc in resp.toolCalls) {
            messages.add(Message.Tool.Call(
                id = tc.id,
                tool = tc.name,
                content = tc.arguments,
                metaInfo = ResponseMetaInfo.Empty
            ))
        }
        return messages
    }
}

private class FakeTool(
    override val spec: ToolSpec,
    private val result: ToolResult
) : Tool {
    override suspend fun execute(args: Map<String, Any>): ToolResult = result
}

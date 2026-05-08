package com.example.aapremote.assistant.engine

import app.cash.turbine.test
import com.example.aapremote.assistant.llm.LlmProvider
import com.example.aapremote.assistant.llm.LlmResult
import com.example.aapremote.assistant.llm.ModelInfo
import com.example.aapremote.assistant.llm.StreamEvent
import com.example.aapremote.assistant.llm.ToolCall
import com.example.aapremote.assistant.tools.Tool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatEngineTest {

    @Test
    fun `single turn text response emits TextDelta and AssistantMessage`() = runTest {
        val provider = FakeLlmProvider(listOf(
            LlmResult(text = "Hello there!", toolCalls = emptyList())
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
        val toolCallResult = LlmResult(
            text = null,
            toolCalls = listOf(
                ToolCall("call_1", "list_jobs", buildJsonObject { put("status", "failed") })
            )
        )
        val finalResult = LlmResult(text = "Found 3 failed jobs.", toolCalls = emptyList())

        val provider = FakeLlmProvider(listOf(toolCallResult, finalResult))
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
        val toolCallResult = LlmResult(
            text = null,
            toolCalls = listOf(
                ToolCall("call_1", "loop_tool", JsonObject(emptyMap()))
            )
        )
        val provider = FakeLlmProvider(
            (1..15).map { toolCallResult }.toList()
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

private class FakeLlmProvider(
    private val results: List<LlmResult> = emptyList(),
    private val error: Throwable? = null
) : LlmProvider {
    private var callIndex = 0

    override suspend fun generate(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>
    ): LlmResult = results.getOrElse(callIndex++) { LlmResult() }

    override fun generateStream(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>
    ): Flow<StreamEvent> = flow {
        if (error != null) {
            emit(StreamEvent.Error(error))
            return@flow
        }
        val result = results.getOrElse(callIndex++) { LlmResult() }
        result.text?.let { emit(StreamEvent.TextDelta(it)) }
        emit(StreamEvent.Done(result))
    }

    override fun isAvailable(): Boolean = true
    override fun modelInfo(): ModelInfo = ModelInfo("fake-model")
}

private class FakeTool(
    override val spec: ToolSpec,
    private val result: ToolResult
) : Tool {
    override suspend fun execute(args: Map<String, Any>): ToolResult = result
}

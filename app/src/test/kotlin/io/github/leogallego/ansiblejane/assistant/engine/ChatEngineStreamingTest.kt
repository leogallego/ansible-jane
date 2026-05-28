package io.github.leogallego.ansiblejane.assistant.engine

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.streaming.StreamFrame
import app.cash.turbine.test
import io.github.leogallego.ansiblejane.assistant.llm.LlmProvider
import io.github.leogallego.ansiblejane.assistant.llm.ModelInfo
import io.github.leogallego.ansiblejane.assistant.tools.Tool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatEngineStreamingTest {

    @Test
    fun `SHOULD handle null tool call IDs WHEN Gemini-style ToolCallComplete`() = runTest {
        val provider = StreamingFakeProvider(listOf(
            listOf(
                StreamFrame.ToolCallComplete(id = null, name = "list_hosts", content = """{"page":1}""")
            ),
            listOf(
                StreamFrame.TextDelta("Found 2 hosts."),
                StreamFrame.End(finishReason = "stop")
            )
        ))
        val tool = FakeStreamTool("list_hosts", """[{"id":1,"name":"host1"}]""")
        val engine = ChatEngine(provider, ToolExecutor(listOf<Tool>(tool)))

        engine.processMessage("list hosts", emptyList(), emptyList()).test {
            val executing = awaitItem()
            assertTrue(executing is ChatEvent.ToolExecuting)
            assertEquals("list_hosts", (executing as ChatEvent.ToolExecuting).toolName)

            val result = awaitItem()
            assertTrue(result is ChatEvent.ToolResult)

            val delta = awaitItem()
            assertTrue(delta is ChatEvent.TextDelta)

            val tokenReport = awaitItem()
            assertTrue(tokenReport is ChatEvent.TokenUsageReport)

            val msg = awaitItem()
            assertTrue(msg is ChatEvent.AssistantMessage)
            assertEquals(1, (msg as ChatEvent.AssistantMessage).toolCallCount)

            awaitComplete()
        }
    }

    @Test
    fun `SHOULD generate unique synthetic IDs WHEN multiple Gemini-style null-ID tool calls`() = runTest {
        val provider = StreamingFakeProvider(listOf(
            listOf(
                StreamFrame.ToolCallComplete(id = null, name = "list_hosts", content = """{"page":1}"""),
                StreamFrame.ToolCallComplete(id = null, name = "list_groups", content = "{}"),
                StreamFrame.End(finishReason = "tool_calls")
            ),
            listOf(
                StreamFrame.TextDelta("Found hosts and groups."),
                StreamFrame.End(finishReason = "stop")
            )
        ))
        val tools = listOf<Tool>(
            FakeStreamTool("list_hosts", """[{"id":1}]"""),
            FakeStreamTool("list_groups", """[{"id":2}]""")
        )
        val engine = ChatEngine(provider, ToolExecutor(tools))

        engine.processMessage("list hosts and groups", emptyList(), emptyList()).test {
            val events = mutableListOf<ChatEvent>()
            do {
                val event = awaitItem()
                events.add(event)
            } while (event !is ChatEvent.AssistantMessage)

            val executingEvents = events.filterIsInstance<ChatEvent.ToolExecuting>()
            assertEquals(2, executingEvents.size)
            assertEquals("list_hosts", executingEvents[0].toolName)
            assertEquals("list_groups", executingEvents[1].toolName)

            val msg = events.last() as ChatEvent.AssistantMessage
            assertEquals(2, msg.toolCallCount)

            awaitComplete()
        }
    }

    @Test
    fun `SHOULD accumulate delta args WHEN OpenAI-style ToolCallDelta streaming`() = runTest {
        val provider = StreamingFakeProvider(listOf(
            listOf(
                StreamFrame.ToolCallDelta(id = "call_1", name = "list_jobs", content = """{"sta"""),
                StreamFrame.ToolCallDelta(id = "call_1", name = null, content = """tus":"failed"}"""),
                StreamFrame.ToolCallComplete(id = "call_1", name = "list_jobs", content = """{"status":"failed"}"""),
                StreamFrame.End(finishReason = "tool_calls")
            ),
            listOf(
                StreamFrame.TextDelta("3 failed jobs found."),
                StreamFrame.End(finishReason = "stop")
            )
        ))
        val tool = FakeStreamTool("list_jobs", """[{"id":1},{"id":2},{"id":3}]""")
        val engine = ChatEngine(provider, ToolExecutor(listOf<Tool>(tool)))

        engine.processMessage("show failed jobs", emptyList(), emptyList()).test {
            val events = mutableListOf<ChatEvent>()
            do {
                val event = awaitItem()
                events.add(event)
            } while (event !is ChatEvent.AssistantMessage)

            val executing = events.filterIsInstance<ChatEvent.ToolExecuting>()
            assertEquals(1, executing.size)
            assertEquals("list_jobs", executing[0].toolName)

            val msg = events.last() as ChatEvent.AssistantMessage
            assertEquals(1, msg.toolCallCount)
            assertEquals("3 failed jobs found.", msg.fullText)

            awaitComplete()
        }
    }

    @Test
    fun `SHOULD handle ToolCallDelta with null IDs WHEN Gemini-style delta streaming`() = runTest {
        val provider = StreamingFakeProvider(listOf(
            listOf(
                StreamFrame.ToolCallDelta(id = null, name = "ping", content = "{}"),
                StreamFrame.ToolCallComplete(id = null, name = "ping", content = "{}"),
                StreamFrame.End(finishReason = "tool_calls")
            ),
            listOf(
                StreamFrame.TextDelta("Pong!"),
                StreamFrame.End(finishReason = "stop")
            )
        ))
        val tool = FakeStreamTool("ping", """{"status":"ok"}""")
        val engine = ChatEngine(provider, ToolExecutor(listOf<Tool>(tool)))

        engine.processMessage("ping", emptyList(), emptyList()).test {
            val events = mutableListOf<ChatEvent>()
            do {
                val event = awaitItem()
                events.add(event)
            } while (event !is ChatEvent.AssistantMessage)

            val executing = events.filterIsInstance<ChatEvent.ToolExecuting>()
            assertEquals(1, executing.size)
            assertEquals("ping", executing[0].toolName)

            awaitComplete()
        }
    }

    @Test
    fun `SHOULD handle mixed text and tool calls WHEN provider sends both`() = runTest {
        val provider = StreamingFakeProvider(listOf(
            listOf(
                StreamFrame.TextDelta("Let me check. "),
                StreamFrame.ToolCallComplete(id = "call_1", name = "ping", content = "{}"),
                StreamFrame.End(finishReason = "tool_calls")
            ),
            listOf(
                StreamFrame.TextDelta("Everything is working."),
                StreamFrame.End(finishReason = "stop")
            )
        ))
        val tool = FakeStreamTool("ping", """{"status":"ok"}""")
        val engine = ChatEngine(provider, ToolExecutor(listOf<Tool>(tool)))

        engine.processMessage("check status", emptyList(), emptyList()).test {
            val events = mutableListOf<ChatEvent>()
            do {
                val event = awaitItem()
                events.add(event)
            } while (event !is ChatEvent.AssistantMessage)

            val textDeltas = events.filterIsInstance<ChatEvent.TextDelta>()
            assertTrue(textDeltas.isNotEmpty())

            val executing = events.filterIsInstance<ChatEvent.ToolExecuting>()
            assertEquals(1, executing.size)

            awaitComplete()
        }
    }
}

private class StreamingFakeProvider(
    private val responseFrames: List<List<StreamFrame>>
) : LlmProvider {
    private var callIndex = 0

    override fun generateStream(
        prompt: Prompt,
        tools: List<ToolDescriptor>,
        maxTokens: Int?
    ): Flow<StreamFrame> = flow {
        val frames = responseFrames.getOrElse(callIndex++) { listOf(StreamFrame.End(finishReason = "stop")) }
        for (frame in frames) {
            emit(frame)
        }
    }

    override fun isAvailable(): Boolean = true
    override fun modelInfo(): ModelInfo = ModelInfo("test-streaming")
    override fun close() {}
}

private class FakeStreamTool(
    name: String,
    private val responseData: String
) : Tool {
    override val spec = ToolSpec(name, "Test tool", JsonObject(emptyMap()))
    override suspend fun execute(args: JsonObject): ToolResult =
        ToolResult(success = true, data = responseData)
}

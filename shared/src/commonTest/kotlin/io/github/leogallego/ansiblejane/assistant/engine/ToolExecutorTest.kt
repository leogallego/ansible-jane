package io.github.leogallego.ansiblejane.assistant.engine

import ai.koog.prompt.message.MessagePart
import io.github.leogallego.ansiblejane.TestOnly
import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import io.github.leogallego.ansiblejane.assistant.tools.Tool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolStub
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(TestOnly::class)
class ToolExecutorTest {

    private fun toolCall(name: String, args: String = "{}"): MessagePart.Tool.Call =
        MessagePart.Tool.Call(id = "1", tool = name, args = args)

    @Test
    fun `execute dispatches to correct tool by name`() = runTest {
        val tool1 = ToolStub("tool_a", ToolResult(success = true, data = "result_a"))
        val tool2 = ToolStub("tool_b", ToolResult(success = true, data = "result_b"))
        val executor = ToolExecutor(listOf<Tool>(tool1, tool2))

        val result = executor.execute(toolCall("tool_b"))

        assertTrue(result.success)
        assertEquals("result_b", result.data)
    }

    @Test
    fun `execute returns NOT_FOUND for unknown tool`() = runTest {
        val executor = ToolExecutor(emptyList())

        val result = executor.execute(toolCall("nonexistent"))

        assertFalse(result.success)
        assertEquals(ErrorType.NOT_FOUND, result.errorType)
        assertTrue(result.data!!.contains("not found"))
    }

    @Test
    fun `execute truncates result exceeding maxResultChars`() = runTest {
        val longData = "x".repeat(30_000)
        val tool = ToolStub("big_tool", ToolResult(success = true, data = longData))
        val executor = ToolExecutor(listOf<Tool>(tool), maxResultChars = 20_000)

        val result = executor.execute(toolCall("big_tool"))

        assertTrue(result.success)
        assertTrue(result.data!!.contains("chars truncated"))
        assertTrue(result.data!!.length < 30_000)
    }

    @Test
    fun `execute does not truncate result within limit`() = runTest {
        val data = "short result"
        val tool = ToolStub("small_tool", ToolResult(success = true, data = data))
        val executor = ToolExecutor(listOf<Tool>(tool))

        val result = executor.execute(toolCall("small_tool"))

        assertEquals(data, result.data)
    }

    @Test
    fun `smartTruncate preserves 60-40 split`() {
        val data = "A".repeat(100)
        val truncated = ToolExecutor.smartTruncate(data, 50)

        val firstPortion = truncated.substringBefore("[...")
        val lastPortion = truncated.substringAfterLast("...]")

        assertEquals(30, firstPortion.trimEnd('\n').length)
    }

    @Test
    fun `smartTruncate is no-op for data within limit`() {
        val data = "short"
        assertEquals(data, ToolExecutor.smartTruncate(data, 100))
    }

    @Test
    fun `execute passes arguments to tool`() = runTest {
        var capturedArgs: JsonObject? = null
        val tool = ToolStub("arg_tool", ToolResult(success = true, data = "ok")) { args ->
            capturedArgs = args
        }
        val executor = ToolExecutor(listOf<Tool>(tool))

        val args = buildJsonObject {
            put("name", "test")
            put("count", 42)
            put("enabled", true)
        }
        executor.execute(toolCall("arg_tool", args.toString()))

        assertEquals("test", capturedArgs!!["name"]!!.jsonPrimitive.content)
        assertEquals(42, capturedArgs!!["count"]!!.jsonPrimitive.int)
        assertEquals(true, capturedArgs!!["enabled"]!!.jsonPrimitive.boolean)
    }
}

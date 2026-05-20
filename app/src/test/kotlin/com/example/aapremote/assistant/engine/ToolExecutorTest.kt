package com.example.aapremote.assistant.engine

import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.Tool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolExecutorTest {

    private fun toolCall(name: String, args: String = "{}"): Message.Tool.Call =
        Message.Tool.Call(id = "1", tool = name, content = args, metaInfo = ResponseMetaInfo.Empty)

    @Test
    fun `execute dispatches to correct tool by name`() = runTest {
        val tool1 = StubTool("tool_a", ToolResult(success = true, data = "result_a"))
        val tool2 = StubTool("tool_b", ToolResult(success = true, data = "result_b"))
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
        val tool = StubTool("big_tool", ToolResult(success = true, data = longData))
        val executor = ToolExecutor(listOf<Tool>(tool), maxResultChars = 20_000)

        val result = executor.execute(toolCall("big_tool"))

        assertTrue(result.success)
        assertTrue(result.data!!.contains("chars truncated"))
        assertTrue(result.data!!.length < 30_000)
    }

    @Test
    fun `execute does not truncate result within limit`() = runTest {
        val data = "short result"
        val tool = StubTool("small_tool", ToolResult(success = true, data = data))
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
        var capturedArgs: Map<String, Any>? = null
        val tool = object : Tool {
            override val spec = ToolSpec("arg_tool", "desc", JsonObject(emptyMap()))
            override suspend fun execute(args: Map<String, Any>): ToolResult {
                capturedArgs = args
                return ToolResult(success = true, data = "ok")
            }
        }
        val executor = ToolExecutor(listOf<Tool>(tool))

        val args = buildJsonObject {
            put("name", "test")
            put("count", 42)
            put("enabled", true)
        }
        executor.execute(toolCall("arg_tool", args.toString()))

        assertEquals("test", capturedArgs!!["name"])
        assertEquals(42L, capturedArgs!!["count"])
        assertEquals(true, capturedArgs!!["enabled"])
    }
}

private class StubTool(
    name: String,
    private val result: ToolResult
) : Tool {
    override val spec = ToolSpec(name, "Fake tool", JsonObject(emptyMap()))
    override suspend fun execute(args: Map<String, Any>): ToolResult = result
}

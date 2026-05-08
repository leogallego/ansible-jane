package com.example.aapremote.assistant.engine

import com.example.aapremote.assistant.llm.ToolCall
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

    @Test
    fun `execute dispatches to correct tool by name`() = runTest {
        val tool1 = FakeTool("tool_a", ToolResult(success = true, data = "result_a"))
        val tool2 = FakeTool("tool_b", ToolResult(success = true, data = "result_b"))
        val executor = ToolExecutor(listOf(tool1, tool2))

        val call = ToolCall("1", "tool_b", JsonObject(emptyMap()))
        val result = executor.execute(call)

        assertTrue(result.success)
        assertEquals("result_b", result.data)
    }

    @Test
    fun `execute returns NOT_FOUND for unknown tool`() = runTest {
        val executor = ToolExecutor(emptyList())

        val call = ToolCall("1", "nonexistent", JsonObject(emptyMap()))
        val result = executor.execute(call)

        assertFalse(result.success)
        assertEquals(ErrorType.NOT_FOUND, result.errorType)
        assertTrue(result.data!!.contains("not found"))
    }

    @Test
    fun `execute truncates result exceeding maxResultChars`() = runTest {
        val longData = "x".repeat(30_000)
        val tool = FakeTool("big_tool", ToolResult(success = true, data = longData))
        val executor = ToolExecutor(listOf(tool), maxResultChars = 20_000)

        val call = ToolCall("1", "big_tool", JsonObject(emptyMap()))
        val result = executor.execute(call)

        assertTrue(result.success)
        assertTrue(result.data!!.contains("chars truncated"))
        assertTrue(result.data!!.length < 30_000)
    }

    @Test
    fun `execute does not truncate result within limit`() = runTest {
        val data = "short result"
        val tool = FakeTool("small_tool", ToolResult(success = true, data = data))
        val executor = ToolExecutor(listOf(tool))

        val call = ToolCall("1", "small_tool", JsonObject(emptyMap()))
        val result = executor.execute(call)

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
        val executor = ToolExecutor(listOf(tool))

        val args = buildJsonObject {
            put("name", "test")
            put("count", 42)
            put("enabled", true)
        }
        val call = ToolCall("1", "arg_tool", args)
        executor.execute(call)

        assertEquals("test", capturedArgs!!["name"])
        assertEquals(42L, capturedArgs!!["count"])
        assertEquals(true, capturedArgs!!["enabled"])
    }
}

private class FakeTool(
    name: String,
    private val result: ToolResult
) : Tool {
    override val spec = ToolSpec(name, "Fake tool", JsonObject(emptyMap()))
    override suspend fun execute(args: Map<String, Any>): ToolResult = result
}

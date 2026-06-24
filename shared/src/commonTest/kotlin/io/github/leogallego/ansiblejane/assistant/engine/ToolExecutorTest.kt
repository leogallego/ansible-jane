package io.github.leogallego.ansiblejane.assistant.engine

import ai.koog.prompt.message.MessagePart
import io.github.leogallego.ansiblejane.TestOnly
import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import io.github.leogallego.ansiblejane.assistant.tools.Tool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolStub
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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

    // --- capResultArray tests ---

    @Test
    fun `capResultArray caps array with count key over 10 items`() {
        val items = buildJsonArray {
            repeat(15) { add(buildJsonObject { put("id", it) }) }
        }
        val input = buildJsonObject {
            put("count", 15)
            put("results", items)
        }.toString()

        val output = ToolExecutor.capResultArray(input)
        val parsed = Json.parseToJsonElement(output).jsonObject

        assertEquals(10, parsed["results"]!!.jsonArray.size)
        assertEquals(10, parsed["_showing"]!!.jsonPrimitive.int)
        assertEquals(15, parsed["_total"]!!.jsonPrimitive.int)
        assertTrue(parsed["_note"]!!.jsonPrimitive.content.contains("first 10 of 15"))
    }

    @Test
    fun `capResultArray returns unchanged without count key`() {
        val items = buildJsonArray {
            repeat(15) { add(buildJsonObject { put("id", it) }) }
        }
        val input = buildJsonObject {
            put("results", items)
        }.toString()

        assertEquals(input, ToolExecutor.capResultArray(input))
    }

    @Test
    fun `capResultArray returns unchanged when array has 10 or fewer items`() {
        val items = buildJsonArray {
            repeat(5) { add(buildJsonObject { put("id", it) }) }
        }
        val input = buildJsonObject {
            put("count", 5)
            put("results", items)
        }.toString()

        assertEquals(input, ToolExecutor.capResultArray(input))
    }

    @Test
    fun `capResultArray returns unchanged for non-JSON input`() {
        assertEquals("plain text", ToolExecutor.capResultArray("plain text"))
        assertEquals("{malformed", ToolExecutor.capResultArray("{malformed"))
    }

    @Test
    fun `capResultArray caps first array found when multiple arrays exist`() {
        val firstArray = buildJsonArray {
            repeat(15) { add(buildJsonObject { put("id", it) }) }
        }
        val secondArray = buildJsonArray {
            repeat(20) { add(buildJsonObject { put("name", "item$it") }) }
        }
        val input = buildJsonObject {
            put("count", 15)
            put("results", firstArray)
            put("extras", secondArray)
        }.toString()

        val output = ToolExecutor.capResultArray(input)
        val parsed = Json.parseToJsonElement(output).jsonObject

        assertEquals(10, parsed["results"]!!.jsonArray.size)
        assertEquals(20, parsed["extras"]!!.jsonArray.size)
        assertEquals(10, parsed["_showing"]!!.jsonPrimitive.int)
    }
}

package com.example.aapremote.assistant.tools

import com.example.aapremote.network.mcp.McpClient
import com.example.aapremote.network.mcp.McpToolDefinition
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.SocketTimeoutException

class McpTool(
    private val client: McpClient,
    private val mcpToolDef: McpToolDefinition,
    private val serverLabel: String
) : Tool {

    val source: ToolSource = ToolSource.MCP

    companion object {
        const val MAX_PAGE_SIZE = 10
        private const val MAX_DESCRIPTION_CHARS = 120
    }

    override val spec: ToolSpec = ToolSpec(
        name = mcpToolDef.name,
        description = "[$serverLabel] ${mcpToolDef.description}".take(MAX_DESCRIPTION_CHARS),
        parametersSchema = mcpToolDef.inputSchema
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val jsonArgs = argsToJsonObject(args)
            val cappedArgs = capPageSize(jsonArgs)
            val mcpResult = client.callTool(mcpToolDef.name, cappedArgs)

            val text = mcpResult.content
                .mapNotNull { it.text }
                .joinToString("\n")

            if (mcpResult.isError) {
                ToolResult(
                    success = false,
                    data = text,
                    errorType = ErrorType.SERVER_ERROR
                )
            } else {
                ToolResult(success = true, data = text)
            }
        } catch (e: SocketTimeoutException) {
            ToolResult(success = false, data = "Tool call timed out", errorType = ErrorType.TIMEOUT)
        } catch (e: IOException) {
            ToolResult(
                success = false,
                data = "Connection error: ${e.message}",
                errorType = ErrorType.CONNECTION_ERROR
            )
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "Unexpected error: ${e.message}",
                errorType = ErrorType.SERVER_ERROR
            )
        }
    }

    private fun argsToJsonObject(args: Map<String, Any>): JsonObject = buildJsonObject {
        args.forEach { (key, value) ->
            put(key, toJsonElement(value))
        }
    }

    private fun capPageSize(args: JsonObject, max: Int = MAX_PAGE_SIZE): JsonObject {
        val current = args["page_size"]?.jsonPrimitive?.intOrNull
        if (current != null && current <= max) return args
        return buildJsonObject {
            args.forEach { (k, v) -> put(k, v) }
            put("page_size", JsonPrimitive(current?.coerceAtMost(max) ?: max))
        }
    }

    private fun toJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> buildJsonObject {
            value.forEach { (k, v) -> put(k.toString(), toJsonElement(v)) }
        }
        is List<*> -> buildJsonArray {
            value.forEach { add(toJsonElement(it)) }
        }
        else -> JsonPrimitive(value.toString())
    }
}

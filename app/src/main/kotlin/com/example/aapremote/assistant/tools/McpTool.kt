package com.example.aapremote.assistant.tools

import com.example.aapremote.network.mcp.McpClient
import com.example.aapremote.network.mcp.McpToolDefinition
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.io.IOException
import java.net.SocketTimeoutException

class McpTool(
    private val client: McpClient,
    private val mcpToolDef: McpToolDefinition,
    private val serverLabel: String
) : Tool {

    override val spec: ToolSpec = ToolSpec(
        name = mcpToolDef.name,
        description = "[$serverLabel] ${mcpToolDef.description}",
        parametersSchema = mcpToolDef.inputSchema
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val jsonArgs = argsToJsonObject(args)
            val mcpResult = client.callTool(mcpToolDef.name, jsonArgs)

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

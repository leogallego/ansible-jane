package io.github.leogallego.ansiblejane.assistant.tools

import io.github.leogallego.ansiblejane.network.mcp.McpClient
import io.github.leogallego.ansiblejane.network.mcp.McpToolDefinition
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.SocketTimeoutException

class McpTool(
    private val client: McpClient,
    private val mcpToolDef: McpToolDefinition,
    val serverLabel: String,
    val toolset: String? = null
) : Tool {

    companion object {
        const val MAX_PAGE_SIZE = 10
        private val WRITE_SUFFIXES = Tool.WRITE_SUFFIXES
    }

    override val isDestructive: Boolean =
        WRITE_SUFFIXES.any { mcpToolDef.name.endsWith(it) }

    override val spec: ToolSpec = ToolSpec(
        name = mcpToolDef.name,
        description = "[$serverLabel] ${mcpToolDef.description}".take(Tool.MAX_DESCRIPTION_CHARS),
        parametersSchema = mcpToolDef.inputSchema
    )

    override suspend fun execute(args: JsonObject): ToolResult {
        return try {
            val cappedArgs = capPageSize(args)
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

    private fun capPageSize(args: JsonObject, max: Int = MAX_PAGE_SIZE): JsonObject {
        val current = args["page_size"]?.jsonPrimitive?.intOrNull
        if (current != null && current <= max) return args
        return buildJsonObject {
            args.forEach { (k, v) -> put(k, v) }
            put("page_size", JsonPrimitive(current?.coerceAtMost(max) ?: max))
        }
    }

}

package io.github.leogallego.ansiblejane.assistant.tools

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import kotlin.coroutines.cancellation.CancellationException
import io.github.leogallego.ansiblejane.network.mcp.McpToolDefinition
import kotlinx.serialization.json.JsonObject

class CachedMcpTool(
    private val mcpToolDef: McpToolDefinition,
    override val serverLabel: String,
    override val toolset: String? = null,
    val readOnly: Boolean = false,
    private val serverManager: McpServerManager
) : Tool {

    override val isDestructive: Boolean =
        Tool.WRITE_SUFFIXES.any { mcpToolDef.name.endsWith(it) }

    override val spec: ToolSpec = ToolSpec(
        name = mcpToolDef.name,
        description = "[$serverLabel] ${mcpToolDef.description}".take(Tool.MAX_DESCRIPTION_CHARS),
        parametersSchema = mcpToolDef.inputSchema
    )

    override suspend fun execute(args: JsonObject): ToolResult {
        return try {
            val client = serverManager.ensureConnected(serverLabel)
            val result = client.callTool(mcpToolDef.name, args)

            val text = result.content
                .filterIsInstance<TextContent>()
                .joinToString("\n") { it.text }

            if (result.isError == true) {
                ToolResult(
                    success = false,
                    data = text,
                    errorType = ErrorType.SERVER_ERROR
                )
            } else {
                ToolResult(success = true, data = text)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ToolResult(
                success = false,
                data = "Connection error: ${e.message}",
                errorType = ErrorType.CONNECTION_ERROR
            )
        }
    }
}

package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.assistant.tools.EmptyArgs
import io.github.leogallego.ansiblejane.assistant.tools.Tool
import io.github.leogallego.ansiblejane.assistant.tools.ToolSource

class ListToolsLocalTool(
    private val toolsProvider: () -> List<Pair<Tool, ToolSource>>
) : AapLocalTool<EmptyArgs>(
    typeToken<EmptyArgs>(), EmptyArgs.serializer(),
    "list_tools", "List all available tools and their capabilities"
) {
    override suspend fun execute(args: EmptyArgs): String {
        val tools = toolsProvider()
        val local = tools.filter { it.second == ToolSource.LOCAL }
        val mcp = tools.filter { it.second == ToolSource.MCP }
        val sb = StringBuilder()
        sb.appendLine("Available tools (${tools.size} total):")
        if (local.isNotEmpty()) {
            sb.appendLine("\nLocal tools (${local.size}):")
            local.forEach { (tool, _) ->
                sb.appendLine("- ${tool.spec.name}: ${tool.spec.description}")
            }
        }
        if (mcp.isNotEmpty()) {
            sb.appendLine("\nMCP tools (${mcp.size}):")
            mcp.forEach { (tool, _) ->
                sb.appendLine("- ${tool.spec.name}: ${tool.spec.description}")
            }
        }
        return sb.toString()
    }
}

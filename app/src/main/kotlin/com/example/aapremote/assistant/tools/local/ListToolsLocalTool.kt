package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.Tool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSource
import com.example.aapremote.assistant.tools.ToolSpec
import kotlinx.serialization.json.JsonObject

class ListToolsLocalTool(
    private val toolsProvider: () -> List<Pair<Tool, ToolSource>>
) : LocalTool(
    spec = ToolSpec(
        name = "list_tools",
        description = "List all available tools and their capabilities",
        parametersSchema = buildToolSchema()
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
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
        ToolResult(success = true, data = sb.toString())
    }
}

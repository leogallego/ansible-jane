package com.example.aapremote.assistant.tools

enum class ToolSource { LOCAL, MCP }

abstract class LocalTool(
    override val spec: ToolSpec,
    val destructive: Boolean = false
) : Tool {
    val source: ToolSource = ToolSource.LOCAL
}

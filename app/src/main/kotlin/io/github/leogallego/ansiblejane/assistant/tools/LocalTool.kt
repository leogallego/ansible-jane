package io.github.leogallego.ansiblejane.assistant.tools

enum class ToolSource { LOCAL, MCP }

interface LocalTool : Tool {
    override val destructive: Boolean
        get() = false
}

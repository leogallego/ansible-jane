package io.github.leogallego.ansiblejane.assistant.tools

enum class ToolSource { LOCAL, MCP }

interface LocalTool : Tool {
    override val isDestructive: Boolean
        get() = false
}

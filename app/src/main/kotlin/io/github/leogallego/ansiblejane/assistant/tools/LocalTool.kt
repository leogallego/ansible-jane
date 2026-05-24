package io.github.leogallego.ansiblejane.assistant.tools

enum class ToolSource { LOCAL, MCP }

interface LocalTool : Tool {
    val destructive: Boolean
        get() = false
}

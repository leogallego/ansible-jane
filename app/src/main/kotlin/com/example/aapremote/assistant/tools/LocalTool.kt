package com.example.aapremote.assistant.tools

enum class ToolSource { LOCAL, MCP }

abstract class LocalTool(
    override val spec: ToolSpec,
    val destructive: Boolean = false
) : Tool {
    protected suspend fun executeSafely(block: suspend () -> ToolResult): ToolResult =
        try { block() } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
}

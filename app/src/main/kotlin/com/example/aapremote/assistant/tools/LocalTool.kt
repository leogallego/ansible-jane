package com.example.aapremote.assistant.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

enum class ToolSource { LOCAL, MCP }

abstract class LocalTool(
    override val spec: ToolSpec,
    val destructive: Boolean = false
) : Tool {
    protected suspend fun executeSafely(block: suspend () -> ToolResult): ToolResult =
        try { block() } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }

    protected fun JsonObject.intArg(name: String): Int? =
        this[name]?.jsonPrimitive?.intOrNull

    protected fun JsonObject.stringArg(name: String): String? =
        this[name]?.jsonPrimitive?.contentOrNull?.takeUnless { it in NULL_SENTINELS }

    companion object {
        private val NULL_SENTINELS = setOf(
            "<nil>", "null", "none", "nil", "", "undefined", "N/A"
        )
    }

    protected fun JsonObject.booleanArg(name: String): Boolean? =
        this[name]?.jsonPrimitive?.booleanOrNull
}

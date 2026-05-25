package io.github.leogallego.ansiblejane.assistant.tools

import kotlinx.serialization.json.JsonObject

data class ToolSpec(
    val name: String,
    val description: String,
    val parametersSchema: JsonObject
)

interface Tool {
    val spec: ToolSpec
    val destructive: Boolean
        get() = false
    suspend fun execute(args: JsonObject): ToolResult
}

data class ToolResult(
    val success: Boolean,
    val data: String? = null,
    val errorType: ErrorType? = null
)

enum class ErrorType {
    CONNECTION_ERROR,
    AUTH_ERROR,
    NOT_FOUND,
    TIMEOUT,
    SERVER_ERROR
}

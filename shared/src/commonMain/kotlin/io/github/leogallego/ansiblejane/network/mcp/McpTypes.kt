package io.github.leogallego.ansiblejane.network.mcp

import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

@Serializable
data class McpToolDefinition(
    val name: String,
    val description: String = "",
    val inputSchema: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class McpServerInfo(
    val name: String,
    val version: String
)

sealed interface McpConnectionState {
    data object Disconnected : McpConnectionState
    data object Connecting : McpConnectionState
    data class Connected(
        val serverInfo: McpServerInfo,
        val toolCount: Int
    ) : McpConnectionState
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : McpConnectionState
}

class McpConnectionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

fun toolSchemaToJsonObject(schema: ToolSchema): JsonObject = buildJsonObject {
    put("type", JsonPrimitive(schema.type))
    schema.properties?.let { put("properties", it) }
    schema.required?.let { required ->
        put("required", JsonArray(required.map { JsonPrimitive(it) }))
    }
}

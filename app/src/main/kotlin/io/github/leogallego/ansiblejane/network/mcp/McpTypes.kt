package io.github.leogallego.ansiblejane.network.mcp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val method: String,
    val params: JsonObject? = null
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class McpToolDefinition(
    val name: String,
    val description: String = "",
    val inputSchema: JsonObject = JsonObject(emptyMap())
)

@Serializable
data class McpToolResult(
    val content: List<McpContent> = emptyList(),
    val isError: Boolean = false
)

@Serializable
data class McpContent(
    val type: String,
    val text: String? = null
)

@Serializable
data class McpInitializeResult(
    val protocolVersion: String,
    val capabilities: JsonObject = JsonObject(emptyMap()),
    val serverInfo: McpServerInfo
)

@Serializable
data class McpServerInfo(
    val name: String,
    val version: String
)

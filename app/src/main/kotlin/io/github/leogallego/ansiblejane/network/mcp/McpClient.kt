package io.github.leogallego.ansiblejane.network.mcp

import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger

class McpConnectionException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class McpClient(
    private val serverUrl: String,
    private val transport: McpTransport,
    private val json: Json
) {
    private val session = McpSession()
    private val requestId = AtomicInteger(1)

    val connectionState: StateFlow<McpConnectionState> = session.state
    val tools: StateFlow<List<McpToolDefinition>> = session.tools
    var serverInfo: McpServerInfo? = null
        private set

    suspend fun connect() {
        initialize()
        discoverTools()
    }

    suspend fun initialize(): McpServerInfo {
        session.updateState(McpConnectionState.Connecting)
        try {
            val initParams = buildJsonObject {
                put("protocolVersion", "2025-03-26")
                putJsonObject("capabilities") {}
                putJsonObject("clientInfo") {
                    put("name", "AnsibleJane")
                    put("version", "1.0")
                }
            }
            val initRequest = JsonRpcRequest(
                id = requestId.getAndIncrement(),
                method = "initialize",
                params = initParams
            )

            val transportResult = transport.postJsonRpc(serverUrl, initRequest)
            val response = withTimeout(30_000L) { transportResult.responses.first() }

            if (response.error != null) {
                throw McpConnectionException(
                    "Initialize failed: ${response.error.message} (code ${response.error.code})"
                )
            }

            transportResult.sessionId?.let { session.updateSessionId(it) }

            val initResult = response.result?.let {
                json.decodeFromString(McpInitializeResult.serializer(), it.toString())
            } ?: throw McpConnectionException("No result in initialize response")

            val notifyRequest = JsonRpcRequest(
                method = "notifications/initialized"
            )
            transport.postNotification(serverUrl, notifyRequest, session.sessionId)

            serverInfo = initResult.serverInfo
            return initResult.serverInfo
        } catch (e: McpConnectionException) {
            session.updateState(McpConnectionState.Error(e.message ?: "Connection failed", e))
            throw e
        } catch (e: IOException) {
            val msg = "Unable to reach MCP server: ${e.message}"
            session.updateState(McpConnectionState.Error(msg, e))
            throw McpConnectionException(msg, e)
        } catch (e: Exception) {
            val msg = "MCP connection error: ${e.message}"
            session.updateState(McpConnectionState.Error(msg, e))
            throw McpConnectionException(msg, e)
        }
    }

    suspend fun discoverTools(): List<McpToolDefinition> {
        val discoveredTools = listTools()
        session.updateState(
            McpConnectionState.Connected(
                serverInfo = serverInfo ?: McpServerInfo("unknown", "0"),
                toolCount = discoveredTools.size
            )
        )
        return discoveredTools
    }

    fun disconnect() {
        session.reset()
    }

    suspend fun listTools(): List<McpToolDefinition> {
        val request = JsonRpcRequest(
            id = requestId.getAndIncrement(),
            method = "tools/list"
        )

        val result = transport.postJsonRpc(serverUrl, request, session.sessionId)
        val response = withTimeout(30_000L) { result.responses.first() }

        if (response.error != null) {
            throw McpConnectionException(
                "tools/list failed: ${response.error.message}"
            )
        }

        val toolsJson = response.result?.toString()
            ?: throw McpConnectionException("No result in tools/list response")

        val toolsList = json.decodeFromString(ToolsListResult.serializer(), toolsJson)
        session.updateTools(toolsList.tools)
        return toolsList.tools
    }

    suspend fun callTool(name: String, arguments: JsonObject): McpToolResult {
        return try {
            callToolInternal(name, arguments)
        } catch (_: McpSessionExpiredException) {
            try {
                connect()
                callToolInternal(name, arguments)
            } catch (e: Exception) {
                session.updateState(McpConnectionState.Error("Session expired — reconnect failed"))
                McpToolResult(
                    content = listOf(McpContent("text", "Session expired and reconnect failed: ${e.message}")),
                    isError = true
                )
            }
        }
    }

    private suspend fun callToolInternal(name: String, arguments: JsonObject): McpToolResult {
        val params = buildJsonObject {
            put("name", name)
            put("arguments", arguments)
        }
        val request = JsonRpcRequest(
            id = requestId.getAndIncrement(),
            method = "tools/call",
            params = params
        )

        try {
            val transportResult = transport.postJsonRpc(serverUrl, request, session.sessionId)
            val response = withTimeout(60_000L) { transportResult.responses.first() }

            if (response.error != null) {
                return McpToolResult(
                    content = listOf(McpContent("text", response.error.message)),
                    isError = true
                )
            }

            val resultJson = response.result?.toString()
                ?: return McpToolResult(
                    content = listOf(McpContent("text", "Empty response")),
                    isError = true
                )

            return json.decodeFromString(McpToolResult.serializer(), resultJson)
        } catch (e: McpSessionExpiredException) {
            throw e
        } catch (e: SocketTimeoutException) {
            return McpToolResult(
                content = listOf(McpContent("text", "Tool call timed out")),
                isError = true
            )
        } catch (e: IOException) {
            return McpToolResult(
                content = listOf(McpContent("text", "Connection error: ${e.message}")),
                isError = true
            )
        }
    }

    companion object {
        fun mapJsonRpcError(code: Int): ErrorType = when (code) {
            -32000 -> ErrorType.AUTH_ERROR
            -32601, -32602 -> ErrorType.NOT_FOUND
            -32603 -> ErrorType.SERVER_ERROR
            else -> ErrorType.SERVER_ERROR
        }

        fun mapHttpError(statusCode: Int): ErrorType = when (statusCode) {
            401, 403 -> ErrorType.AUTH_ERROR
            404 -> ErrorType.NOT_FOUND
            in 500..599 -> ErrorType.SERVER_ERROR
            else -> ErrorType.SERVER_ERROR
        }
    }
}

@kotlinx.serialization.Serializable
internal data class ToolsListResult(
    val tools: List<McpToolDefinition> = emptyList()
)

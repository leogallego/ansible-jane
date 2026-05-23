package io.github.leogallego.ansiblejane.network.mcp

import io.github.leogallego.ansiblejane.assistant.tools.McpTool
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.model.McpServerConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class McpServerManager(
    private val httpClientFactory: (AapInstance) -> OkHttpClient,
    private val json: Json
) {
    private val _connections = MutableStateFlow<Map<String, McpConnectionState>>(emptyMap())
    val connections: StateFlow<Map<String, McpConnectionState>> = _connections.asStateFlow()

    private val clients = mutableMapOf<String, McpClient>()
    private val mcpTools = mutableListOf<McpTool>()

    suspend fun connectAll(instance: AapInstance) {
        disconnectAll()

        val configs = instance.mcpServerUrls?.filter { it.enabled } ?: return
        if (configs.isEmpty()) return

        val httpClient = httpClientFactory(instance)

        coroutineScope {
            configs.map { config ->
                async {
                    connectServer(config, httpClient)
                }
            }.forEach { deferred ->
                try {
                    deferred.await()
                } catch (_: Exception) {
                    // Per-server failure isolated — others continue
                }
            }
        }
    }

    private suspend fun connectServer(config: McpServerConfig, httpClient: OkHttpClient) {
        val transport = McpTransport(httpClient, json)
        val client = McpClient(config.url, transport, json)

        _connections.update { it + (config.label to McpConnectionState.Connecting) }

        try {
            client.connect()
            clients[config.label] = client

            val tools = client.tools.value.map { toolDef ->
                McpTool(client, toolDef, config.label)
            }
            synchronized(mcpTools) { mcpTools.addAll(tools) }

            _connections.update { it + (config.label to client.connectionState.value) }
        } catch (e: Exception) {
            _connections.update {
                it + (config.label to McpConnectionState.Error(
                    "Failed to connect: ${e.message}", e
                ))
            }
        }
    }

    fun disconnectAll() {
        clients.values.forEach { client ->
            try { client.disconnect() } catch (_: Exception) {}
        }
        clients.clear()
        synchronized(mcpTools) { mcpTools.clear() }
        _connections.update { emptyMap() }
    }

    fun getAllTools(): List<McpTool> = synchronized(mcpTools) { mcpTools.toList() }

    fun refreshConnections() {
        clients.forEach { (label, client) ->
            _connections.update { it + (label to client.connectionState.value) }
        }
    }
}

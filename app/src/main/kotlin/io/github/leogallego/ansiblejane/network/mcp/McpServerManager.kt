package io.github.leogallego.ansiblejane.network.mcp

import io.github.leogallego.ansiblejane.assistant.tools.CachedMcpTool
import io.github.leogallego.ansiblejane.assistant.tools.McpTool
import io.github.leogallego.ansiblejane.assistant.tools.Tool
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.model.McpServerConfig
import io.github.leogallego.ansiblejane.model.ToolManifest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class McpServerManager(
    private val httpClientFactory: (AapInstance, McpServerConfig) -> OkHttpClient,
    private val json: Json
) {
    private val _connections = MutableStateFlow<Map<String, McpConnectionState>>(emptyMap())
    val connections: StateFlow<Map<String, McpConnectionState>> = _connections.asStateFlow()

    private val clients = ConcurrentHashMap<String, McpClient>()
    private val mcpTools = mutableListOf<Tool>()
    private val connectionMutexes = ConcurrentHashMap<String, Mutex>()
    @Volatile
    private var currentInstance: AapInstance? = null

    suspend fun connectAll(instance: AapInstance) {
        currentInstance = instance
        disconnectAll()

        val configs = instance.mcpServerUrls?.filter { it.enabled } ?: return
        if (configs.isEmpty()) return

        coroutineScope {
            configs.map { config ->
                async {
                    val httpClient = httpClientFactory(instance, config)
                    connectServer(config, httpClient)
                }
            }.forEach { deferred ->
                try {
                    deferred.await()
                } catch (e: CancellationException) {
                    throw e
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
                McpTool(client, toolDef, config.label, config.toolset)
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

    fun getAllTools(): List<Tool> = synchronized(mcpTools) { mcpTools.toList() }

    fun getToolsForServer(label: String): List<Tool> =
        synchronized(mcpTools) {
            mcpTools.filter { tool -> tool.serverLabelOrNull() == label }
        }

    fun setCachedTools(tools: List<Tool>) {
        synchronized(mcpTools) {
            mcpTools.clear()
            mcpTools.addAll(tools)
        }
    }

    suspend fun ensureConnected(serverLabel: String): McpClient {
        clients[serverLabel]?.let { return it }

        val mutex = connectionMutexes.getOrPut(serverLabel) { Mutex() }
        return mutex.withLock {
            clients[serverLabel]?.let { return it }

            val instance = currentInstance
                ?: throw IllegalStateException("No active instance")
            val config = instance.mcpServerUrls?.find { it.label == serverLabel }
                ?: throw IllegalStateException("No MCP config for server: $serverLabel")

            val httpClient = httpClientFactory(instance, config)
            val transport = McpTransport(httpClient, json)
            val client = McpClient(config.url, transport, json)

            _connections.update { it + (serverLabel to McpConnectionState.Connecting) }

            try {
                client.connect()
                clients[serverLabel] = client

                val liveTools = client.tools.value.map { toolDef ->
                    McpTool(client, toolDef, config.label, config.toolset)
                }
                synchronized(mcpTools) {
                    mcpTools.removeAll { tool ->
                        tool.spec.name in liveTools.map { it.spec.name }
                    }
                    mcpTools.addAll(liveTools)
                }

                _connections.update { it + (serverLabel to client.connectionState.value) }
                client
            } catch (e: Exception) {
                _connections.update {
                    it + (serverLabel to McpConnectionState.Error(
                        "Failed to connect: ${e.message}", e
                    ))
                }
                throw e
            }
        }
    }

    suspend fun connectAllWithCache(
        instance: AapInstance,
        manifest: ToolManifest? = null,
        forceRefresh: Boolean = false
    ) {
        currentInstance = instance

        val configs = instance.mcpServerUrls?.filter { it.enabled } ?: return
        if (configs.isEmpty()) return

        val seen = mutableSetOf<String>()
        val dedupedConfigs = configs.filter { config ->
            if (config.label in seen) {
                android.util.Log.w(TAG, "Duplicate server label '${config.label}' — skipping auto-detected")
                false
            } else {
                seen.add(config.label)
                true
            }
        }

        val cachedByLabel = manifest?.servers?.associateBy { it.label } ?: emptyMap()

        if (!forceRefresh && manifest != null) {
            val allCached = dedupedConfigs.all { config ->
                val cached = cachedByLabel[config.label] ?: return@all false
                cached.serverUrl == config.url && cached.toolset == config.toolset
            }
            if (allCached) {
                android.util.Log.d(TAG, "All servers cached — deferring connections to lazy ensureConnected()")
                return
            }
        }

        coroutineScope {
            dedupedConfigs.map { config ->
                async {
                    val mutex = connectionMutexes.getOrPut(config.label) { Mutex() }
                    mutex.withLock {
                        if (clients.containsKey(config.label)) return@async

                        val cached = cachedByLabel[config.label]
                        val configChanged = cached != null && (
                            cached.serverUrl != config.url ||
                            cached.toolset != config.toolset
                        )

                        val httpClient = httpClientFactory(instance, config)
                        val transport = McpTransport(httpClient, json)
                        val client = McpClient(config.url, transport, json)

                        _connections.update { it + (config.label to McpConnectionState.Connecting) }

                        try {
                            val info = client.initialize()
                            clients[config.label] = client

                            val versionMatch = !forceRefresh && !configChanged &&
                                cached?.serverInfo?.version == info.version

                            if (versionMatch) {
                                android.util.Log.d(TAG, "Version match for ${config.label} — keeping cached tools")
                                _connections.update {
                                    it + (config.label to McpConnectionState.Connected(
                                        serverInfo = info,
                                        toolCount = cached?.tools?.size ?: 0
                                    ))
                                }
                            } else {
                                client.discoverTools()
                                val liveTools = client.tools.value.map { toolDef ->
                                    McpTool(client, toolDef, config.label, config.toolset)
                                }
                                synchronized(mcpTools) {
                                    mcpTools.removeAll { it.serverLabelOrNull() == config.label }
                                    mcpTools.addAll(liveTools)
                                }
                                _connections.update { it + (config.label to client.connectionState.value) }
                            }
                        } catch (e: Exception) {
                            _connections.update {
                                it + (config.label to McpConnectionState.Error(
                                    "Failed to connect: ${e.message}", e
                                ))
                            }
                        }
                    }
                }
            }.forEach { deferred ->
                try {
                    deferred.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) { }
            }
        }
    }

    suspend fun reconnectServer(label: String) {
        val instance = currentInstance ?: return
        val config = instance.mcpServerUrls?.find { it.label == label } ?: return

        clients[label]?.let { client ->
            try { client.disconnect() } catch (_: Exception) {}
        }
        clients.remove(label)
        synchronized(mcpTools) { mcpTools.removeAll { it.serverLabelOrNull() == label } }
        _connections.update { it + (label to McpConnectionState.Connecting) }

        val httpClient = httpClientFactory(instance, config)
        connectServer(config, httpClient)
    }

    fun refreshConnections() {
        clients.forEach { (label, client) ->
            _connections.update { it + (label to client.connectionState.value) }
        }
    }

    private fun Tool.serverLabelOrNull(): String? = when (this) {
        is McpTool -> serverLabel
        is CachedMcpTool -> serverLabel
        else -> null
    }

    companion object {
        private const val TAG = "McpServerManager"
    }
}

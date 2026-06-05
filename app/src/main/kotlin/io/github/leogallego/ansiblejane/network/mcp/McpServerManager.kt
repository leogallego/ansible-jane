package io.github.leogallego.ansiblejane.network.mcp

import io.github.leogallego.ansiblejane.assistant.tools.McpTool
import io.github.leogallego.ansiblejane.assistant.tools.Tool
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.model.McpServerConfig
import io.github.leogallego.ansiblejane.model.ServerToolCache
import io.github.leogallego.ansiblejane.model.ToolManifest
import io.github.leogallego.ansiblejane.assistant.engine.DebugLog as Log
import io.ktor.client.HttpClient
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.StreamableHttpClientTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
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

private data class SdkClientState(
    val client: Client,
    val ktorClient: HttpClient,
    val serverInfo: McpServerInfo,
    val toolDefs: List<McpToolDefinition>
)

class McpServerManager(
    private val ktorClientFactory: (AapInstance, McpServerConfig) -> HttpClient
) {
    private val _connections = MutableStateFlow<Map<String, McpConnectionState>>(emptyMap())
    val connections: StateFlow<Map<String, McpConnectionState>> = _connections.asStateFlow()

    private val clients = ConcurrentHashMap<String, SdkClientState>()
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
                async { connectServer(config, ktorClientFactory(instance, config)) }
            }.forEach { deferred ->
                try {
                    deferred.await()
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }
    }

    private suspend fun connectServer(config: McpServerConfig, ktorClient: HttpClient) {
        _connections.update { it + (config.label to McpConnectionState.Connecting) }
        try {
            val state = connectAndDiscover(config.url, ktorClient)
            registerServer(config.label, state, config.toolset)
        } catch (e: Exception) {
            _connections.update {
                it + (config.label to McpConnectionState.Error(
                    "Failed to connect: ${e.message}", e
                ))
            }
        }
    }

    suspend fun disconnectAll() {
        clients.values.forEach { state ->
            try { state.client.close() } catch (_: Exception) {}
            try { state.ktorClient.close() } catch (_: Exception) {}
        }
        clients.clear()
        connectionMutexes.clear()
        synchronized(mcpTools) { mcpTools.clear() }
        _connections.update { emptyMap() }
    }

    fun getAllTools(): List<Tool> = synchronized(mcpTools) { mcpTools.toList() }

    fun getToolsForServer(label: String): List<Tool> =
        synchronized(mcpTools) {
            mcpTools.filter { it.serverLabel == label }
        }

    fun setCachedTools(tools: List<Tool>) {
        synchronized(mcpTools) {
            mcpTools.clear()
            mcpTools.addAll(tools)
        }
    }

    suspend fun ensureConnected(serverLabel: String): Client {
        clients[serverLabel]?.let { return it.client }

        val mutex = connectionMutexes.getOrPut(serverLabel) { Mutex() }
        return mutex.withLock {
            clients[serverLabel]?.let { return it.client }

            val instance = currentInstance
                ?: throw IllegalStateException("No active instance")
            val config = instance.mcpServerUrls?.find { it.label == serverLabel }
                ?: throw IllegalStateException("No MCP config for server: $serverLabel")

            val ktorClient = ktorClientFactory(instance, config)
            _connections.update { it + (serverLabel to McpConnectionState.Connecting) }

            try {
                val state = connectAndDiscover(config.url, ktorClient)
                registerServer(config.label, state, config.toolset)
                state.client
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
                Log.w(TAG, "Duplicate server label '${config.label}' — skipping auto-detected")
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
                Log.d(TAG, "All servers cached — deferring connections to lazy ensureConnected()")
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

                        val ktorClient = ktorClientFactory(instance, config)
                        val sdkClient = Client(clientInfo = CLIENT_INFO)
                        val transport = StreamableHttpClientTransport(
                            client = ktorClient,
                            url = config.url
                        )

                        _connections.update { it + (config.label to McpConnectionState.Connecting) }

                        try {
                            sdkClient.connect(transport)
                            val serverInfo = extractServerInfo(sdkClient)

                            val versionMatch = !forceRefresh && !configChanged &&
                                cached != null && cached.serverInfo.version == serverInfo.version

                            val toolDefs = if (versionMatch) {
                                Log.d(TAG, "Version match for ${config.label} — keeping cached tools")
                                cached!!.tools
                            } else {
                                discoverToolDefs(sdkClient)
                            }

                            registerServer(
                                config.label,
                                SdkClientState(sdkClient, ktorClient, serverInfo, toolDefs),
                                config.toolset
                            )
                        } catch (e: Exception) {
                            closeSafely(sdkClient, ktorClient)
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

        clients.remove(label)?.let { state ->
            try { state.client.close() } catch (_: Exception) {}
            try { state.ktorClient.close() } catch (_: Exception) {}
        }
        synchronized(mcpTools) { mcpTools.removeAll { it.serverLabel == label } }
        _connections.update { it + (label to McpConnectionState.Connecting) }

        val ktorClient = ktorClientFactory(instance, config)
        connectServer(config, ktorClient)
    }

    fun refreshConnections() {
        clients.forEach { (label, state) ->
            _connections.update {
                it + (label to McpConnectionState.Connected(
                    serverInfo = state.serverInfo,
                    toolCount = state.toolDefs.size
                ))
            }
        }
    }

    fun buildManifest(instance: AapInstance): ToolManifest? {
        val allTools = getAllTools()
        if (allTools.isEmpty()) return null

        val configs = instance.mcpServerUrls?.filter { it.enabled } ?: return null
        val configByLabel = configs.associateBy { it.label }

        val serverLabels = allTools
            .filterIsInstance<McpTool>()
            .map { it.serverLabel }
            .distinct()

        val serverCaches = serverLabels.mapNotNull { label ->
            val config = configByLabel[label] ?: return@mapNotNull null
            val state = clients[label] ?: return@mapNotNull null

            ServerToolCache(
                serverUrl = config.url,
                label = label,
                toolset = config.toolset,
                serverInfo = state.serverInfo,
                tools = state.toolDefs,
                readOnly = config.readOnly
            )
        }

        if (serverCaches.isEmpty()) return null

        return ToolManifest(
            instanceId = instance.id,
            servers = serverCaches,
            cachedAt = System.currentTimeMillis()
        )
    }

    // -- helpers --

    private suspend fun connectAndDiscover(
        url: String,
        ktorClient: HttpClient
    ): SdkClientState {
        val sdkClient = Client(clientInfo = CLIENT_INFO)
        val transport = StreamableHttpClientTransport(client = ktorClient, url = url)
        try {
            sdkClient.connect(transport)
            val serverInfo = extractServerInfo(sdkClient)
            val toolDefs = discoverToolDefs(sdkClient)
            return SdkClientState(sdkClient, ktorClient, serverInfo, toolDefs)
        } catch (e: Exception) {
            closeSafely(sdkClient, ktorClient)
            throw e
        }
    }

    private fun extractServerInfo(client: Client): McpServerInfo {
        val sv = client.serverVersion
        return McpServerInfo(name = sv?.name ?: "unknown", version = sv?.version ?: "0")
    }

    private suspend fun discoverToolDefs(client: Client): List<McpToolDefinition> {
        return client.listTools().tools.map { tool ->
            McpToolDefinition(
                name = tool.name,
                description = tool.description ?: "",
                inputSchema = toolSchemaToJsonObject(tool.inputSchema)
            )
        }
    }

    private fun registerServer(label: String, state: SdkClientState, toolset: String?) {
        clients[label] = state
        val tools = state.toolDefs.map { McpTool(state.client, it, label, toolset) }
        synchronized(mcpTools) {
            mcpTools.removeAll { it.serverLabel == label }
            mcpTools.addAll(tools)
        }
        _connections.update {
            it + (label to McpConnectionState.Connected(
                serverInfo = state.serverInfo,
                toolCount = state.toolDefs.size
            ))
        }
    }

    private suspend fun closeSafely(sdkClient: Client, ktorClient: HttpClient) {
        try { sdkClient.close() } catch (_: Exception) {}
        try { ktorClient.close() } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "McpServerManager"
        private val CLIENT_INFO = Implementation(name = "AnsibleJane", version = "1.0")
    }
}

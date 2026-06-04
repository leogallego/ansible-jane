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
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
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
        val sdkClient = Client(
            clientInfo = Implementation(name = "AnsibleJane", version = "1.0")
        )
        val transport = StreamableHttpClientTransport(
            client = ktorClient,
            url = config.url
        )

        _connections.update { it + (config.label to McpConnectionState.Connecting) }

        try {
            sdkClient.connect(transport)

            val sv = sdkClient.serverVersion
            val serverInfo = McpServerInfo(
                name = sv?.name ?: "unknown",
                version = sv?.version ?: "0"
            )

            val sdkTools = sdkClient.listTools().tools
            val toolDefs = sdkTools.map { tool ->
                McpToolDefinition(
                    name = tool.name,
                    description = tool.description ?: "",
                    inputSchema = toolSchemaToJsonObject(tool.inputSchema)
                )
            }

            val state = SdkClientState(sdkClient, ktorClient, serverInfo, toolDefs)
            clients[config.label] = state

            val tools = toolDefs.map { toolDef ->
                McpTool(sdkClient, toolDef, config.label, config.toolset)
            }
            synchronized(mcpTools) { mcpTools.addAll(tools) }

            _connections.update {
                it + (config.label to McpConnectionState.Connected(
                    serverInfo = serverInfo,
                    toolCount = toolDefs.size
                ))
            }
        } catch (e: Exception) {
            try { ktorClient.close() } catch (_: Exception) {}
            _connections.update {
                it + (config.label to McpConnectionState.Error(
                    "Failed to connect: ${e.message}", e
                ))
            }
        }
    }

    fun disconnectAll() {
        clients.values.forEach { state ->
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
            val sdkClient = Client(
                clientInfo = Implementation(name = "AnsibleJane", version = "1.0")
            )
            val transport = StreamableHttpClientTransport(
                client = ktorClient,
                url = config.url
            )

            _connections.update { it + (serverLabel to McpConnectionState.Connecting) }

            try {
                sdkClient.connect(transport)

                val sv = sdkClient.serverVersion
                val serverInfo = McpServerInfo(
                    name = sv?.name ?: "unknown",
                    version = sv?.version ?: "0"
                )

                val sdkTools = sdkClient.listTools().tools
                val toolDefs = sdkTools.map { tool ->
                    McpToolDefinition(
                        name = tool.name,
                        description = tool.description ?: "",
                        inputSchema = toolSchemaToJsonObject(tool.inputSchema)
                    )
                }

                val state = SdkClientState(sdkClient, ktorClient, serverInfo, toolDefs)
                clients[serverLabel] = state

                val liveTools = toolDefs.map { toolDef ->
                    McpTool(sdkClient, toolDef, config.label, config.toolset)
                }
                synchronized(mcpTools) {
                    mcpTools.removeAll { it.serverLabel == serverLabel }
                    mcpTools.addAll(liveTools)
                }

                _connections.update {
                    it + (serverLabel to McpConnectionState.Connected(
                        serverInfo = serverInfo,
                        toolCount = toolDefs.size
                    ))
                }
                sdkClient
            } catch (e: Exception) {
                try { ktorClient.close() } catch (_: Exception) {}
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
                        val sdkClient = Client(
                            clientInfo = Implementation(name = "AnsibleJane", version = "1.0")
                        )
                        val transport = StreamableHttpClientTransport(
                            client = ktorClient,
                            url = config.url
                        )

                        _connections.update { it + (config.label to McpConnectionState.Connecting) }

                        try {
                            sdkClient.connect(transport)

                            val sv = sdkClient.serverVersion
                            val serverInfo = McpServerInfo(
                                name = sv?.name ?: "unknown",
                                version = sv?.version ?: "0"
                            )

                            val versionMatch = !forceRefresh && !configChanged &&
                                cached != null && cached.serverInfo.version == serverInfo.version

                            if (versionMatch) {
                                Log.d(TAG, "Version match for ${config.label} — keeping cached tools")
                                clients[config.label] = SdkClientState(
                                    sdkClient, ktorClient, serverInfo, cached.tools
                                )
                                _connections.update {
                                    it + (config.label to McpConnectionState.Connected(
                                        serverInfo = serverInfo,
                                        toolCount = cached.tools.size
                                    ))
                                }
                            } else {
                                val sdkTools = sdkClient.listTools().tools
                                val toolDefs = sdkTools.map { tool ->
                                    McpToolDefinition(
                                        name = tool.name,
                                        description = tool.description ?: "",
                                        inputSchema = toolSchemaToJsonObject(tool.inputSchema)
                                    )
                                }

                                clients[config.label] = SdkClientState(
                                    sdkClient, ktorClient, serverInfo, toolDefs
                                )

                                val liveTools = toolDefs.map { toolDef ->
                                    McpTool(sdkClient, toolDef, config.label, config.toolset)
                                }
                                synchronized(mcpTools) {
                                    mcpTools.removeAll { it.serverLabel == config.label }
                                    mcpTools.addAll(liveTools)
                                }
                                _connections.update {
                                    it + (config.label to McpConnectionState.Connected(
                                        serverInfo = serverInfo,
                                        toolCount = toolDefs.size
                                    ))
                                }
                            }
                        } catch (e: Exception) {
                            try { ktorClient.close() } catch (_: Exception) {}
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

        clients[label]?.let { state ->
            try { state.ktorClient.close() } catch (_: Exception) {}
        }
        clients.remove(label)
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

    companion object {
        private const val TAG = "McpServerManager"
    }
}

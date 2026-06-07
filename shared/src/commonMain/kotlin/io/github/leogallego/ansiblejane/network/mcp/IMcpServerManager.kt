package io.github.leogallego.ansiblejane.network.mcp

import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.model.ToolManifest
import kotlinx.coroutines.flow.StateFlow

interface IMcpServerManager {
    val connections: StateFlow<Map<String, McpConnectionState>>
    fun getAllTools(): List<Any>
    suspend fun connectAllWithCache(instance: AapInstance, forceRefresh: Boolean = false)
    suspend fun disconnectAll()
    suspend fun reconnectServer(label: String)
    suspend fun buildManifest(instance: AapInstance): ToolManifest?
}

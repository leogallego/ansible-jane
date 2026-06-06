package io.github.leogallego.ansiblejane.data.backup

import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.model.McpServerConfig
import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val version: Int = 2,
    val createdAt: Long,
    val instances: List<BackupInstance>,
    val llmConfig: LlmProviderConfig? = null,
    val llmConfigs: Map<String, LlmProviderConfig>? = null,
    val activeProvider: String? = null
)

@Serializable
data class BackupInstance(
    val id: String,
    val baseUrl: String,
    val token: String,
    val alias: String? = null,
    val apiVersion: String,
    val trustSelfSigned: Boolean = false,
    val certFingerprint: String? = null,
    val mcpServerUrls: List<McpServerConfig>? = null,
    val mcpEnabled: Boolean = false
)

class BackupDecryptionException(message: String) : Exception(message)

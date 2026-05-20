package com.example.aapremote.data.backup

import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.model.McpServerConfig
import kotlinx.serialization.Serializable

@Serializable
data class BackupEnvelope(
    val version: Int = 1,
    val createdAt: Long,
    val instances: List<BackupInstance>,
    val llmConfig: LlmProviderConfig? = null
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

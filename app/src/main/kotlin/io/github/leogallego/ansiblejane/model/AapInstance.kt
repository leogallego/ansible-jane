package io.github.leogallego.ansiblejane.model

import kotlinx.serialization.Serializable
import java.net.URI

@Serializable
data class McpServerConfig(
    val url: String,
    val label: String,
    val enabled: Boolean = true,
    val isAutoDetected: Boolean = false,
    val useInstanceAuth: Boolean = true,
    val readOnly: Boolean = false
)

data class AapInstance(
    val id: String,
    val baseUrl: String,
    val token: String,
    val alias: String? = null,
    val apiVersion: String = "v2",
    val trustSelfSigned: Boolean = false,
    val certFingerprint: String? = null,
    val mcpServerUrls: List<McpServerConfig>? = null,
    val mcpEnabled: Boolean = false
) {
    val displayLabel: String
        get() = alias ?: URI(baseUrl).host.orEmpty()

    val hostname: String
        get() = URI(baseUrl).host.orEmpty()
}

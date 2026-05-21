package com.example.aapremote.data

import com.example.aapremote.model.AapInstance
import com.example.aapremote.model.McpServerConfig
import com.example.aapremote.network.ApiVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ITokenManager {
    val instances: StateFlow<List<AapInstance>>
    val activeInstance: StateFlow<AapInstance?>
    val isLoggedIn: Flow<Boolean>

    suspend fun saveInstance(
        baseUrl: String,
        token: String,
        alias: String? = null,
        apiVersion: ApiVersion,
        trustSelfSigned: Boolean = false,
        certFingerprint: String? = null,
        existingId: String? = null
    ): String

    suspend fun removeInstance(instanceId: String): Boolean
    suspend fun setActiveInstance(instanceId: String)
    fun getInstanceById(instanceId: String): AapInstance?
    suspend fun loadCredentials(): Boolean

    suspend fun saveCredentials(
        baseUrl: String,
        token: String,
        apiVersion: ApiVersion,
        trustSelfSigned: Boolean = false,
        certFingerprint: String? = null,
        alias: String? = null,
        existingId: String? = null
    ): String

    suspend fun updateMcpConfig(
        instanceId: String,
        enabled: Boolean,
        servers: List<McpServerConfig>?
    )

    suspend fun clearCredentials()
}

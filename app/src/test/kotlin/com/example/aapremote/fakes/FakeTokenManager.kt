package com.example.aapremote.fakes

import com.example.aapremote.data.ITokenManager
import com.example.aapremote.model.AapInstance
import com.example.aapremote.model.McpServerConfig
import com.example.aapremote.network.ApiVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeTokenManager : ITokenManager {
    private val _instances = MutableStateFlow<List<AapInstance>>(emptyList())
    override val instances: StateFlow<List<AapInstance>> = _instances.asStateFlow()

    private val _activeInstance = MutableStateFlow<AapInstance?>(null)
    override val activeInstance: StateFlow<AapInstance?> = _activeInstance.asStateFlow()

    override val isLoggedIn: Flow<Boolean> = _instances.map { it.isNotEmpty() }

    var saveInstanceResult: String = "test-id"

    override suspend fun saveInstance(
        baseUrl: String,
        token: String,
        alias: String?,
        apiVersion: ApiVersion,
        trustSelfSigned: Boolean,
        certFingerprint: String?,
        existingId: String?
    ): String {
        val id = existingId ?: saveInstanceResult
        val instance = AapInstance(
            id = id,
            baseUrl = baseUrl,
            token = token,
            alias = alias,
            apiVersion = apiVersion.name,
            trustSelfSigned = trustSelfSigned,
            certFingerprint = certFingerprint
        )
        _instances.value = _instances.value + instance
        if (_activeInstance.value == null) _activeInstance.value = instance
        return id
    }

    override suspend fun removeInstance(instanceId: String): Boolean {
        val before = _instances.value.size
        _instances.value = _instances.value.filter { it.id != instanceId }
        if (_activeInstance.value?.id == instanceId) {
            _activeInstance.value = _instances.value.firstOrNull()
        }
        return _instances.value.size < before
    }

    override suspend fun setActiveInstance(instanceId: String) {
        _activeInstance.value = _instances.value.find { it.id == instanceId }
    }

    override fun getInstanceById(instanceId: String): AapInstance? {
        return _instances.value.find { it.id == instanceId }
    }

    override suspend fun loadCredentials(): Boolean {
        return _instances.value.isNotEmpty()
    }

    override suspend fun saveCredentials(
        baseUrl: String,
        token: String,
        apiVersion: ApiVersion,
        trustSelfSigned: Boolean,
        certFingerprint: String?,
        alias: String?,
        existingId: String?
    ): String = saveInstance(baseUrl, token, alias, apiVersion, trustSelfSigned, certFingerprint, existingId)

    override suspend fun updateMcpConfig(
        instanceId: String,
        enabled: Boolean,
        servers: List<McpServerConfig>?
    ) {
        _instances.value = _instances.value.map {
            if (it.id == instanceId) it.copy(mcpEnabled = enabled, mcpServerUrls = servers) else it
        }
        _activeInstance.value?.let { active ->
            if (active.id == instanceId) {
                _activeInstance.value = _instances.value.find { it.id == instanceId }
            }
        }
    }

    override suspend fun clearCredentials() {
        _instances.value = emptyList()
        _activeInstance.value = null
    }

    fun setInstances(list: List<AapInstance>) {
        _instances.value = list
        _activeInstance.value = list.firstOrNull()
    }

    fun setActiveInstanceDirect(instance: AapInstance?) {
        _activeInstance.value = instance
    }
}

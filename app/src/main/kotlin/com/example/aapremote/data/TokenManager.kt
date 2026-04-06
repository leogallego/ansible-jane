package com.example.aapremote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.aapremote.model.AapInstance
import com.example.aapremote.network.ApiVersion
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.util.UUID

private val Context.credentialsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "credentials"
)

@Serializable
data class SerializedInstance(
    val id: String,
    val encryptedUrl: String,
    val encryptedToken: String,
    val alias: String? = null,
    val apiVersion: String = "CONTROLLER_V2",
    val trustSelfSigned: Boolean = false,
    val certFingerprint: String? = null
)

@Serializable
data class InstancesState(
    val instances: List<SerializedInstance> = emptyList(),
    val activeInstanceId: String? = null
)

class TokenManager(private val context: Context) {

    private val aead: Aead

    // Legacy cached fields for backward compatibility during transition
    var cachedToken: String? = null
        private set
    var cachedBaseUrl: String? = null
        private set
    var cachedApiVersion: ApiVersion = ApiVersion.CONTROLLER_V2
        private set
    var cachedTrustSelfSigned: Boolean = false
        private set

    // Multi-instance reactive state
    private val _instances = MutableStateFlow<List<AapInstance>>(emptyList())
    val instances: StateFlow<List<AapInstance>> = _instances.asStateFlow()

    private val _activeInstance = MutableStateFlow<AapInstance?>(null)
    val activeInstance: StateFlow<AapInstance?> = _activeInstance.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        AeadConfig.register()
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, "aap_keyset", "aap_keyset_prefs")
            .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
            .withMasterKeyUri("android-keystore://aap_master_key")
            .build()
            .keysetHandle
        aead = keysetHandle.getPrimitive(Aead::class.java)
    }

    private companion object {
        // Legacy keys (kept for cleanup detection)
        val KEY_BASE_URL = stringPreferencesKey("base_url")
        val KEY_TOKEN = stringPreferencesKey("token")
        val KEY_API_VERSION = stringPreferencesKey("api_version")
        val KEY_TRUST_SELF_SIGNED = booleanPreferencesKey("trust_self_signed")
        val KEY_CERT_FINGERPRINT = stringPreferencesKey("cert_fingerprint")

        // Multi-instance key
        val KEY_INSTANCES_JSON = stringPreferencesKey("instances_json")
    }

    private fun encrypt(value: String): String {
        val ciphertext = aead.encrypt(
            value.toByteArray(StandardCharsets.UTF_8),
            null
        )
        return android.util.Base64.encodeToString(ciphertext, android.util.Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val ciphertext = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        val plaintext = aead.decrypt(ciphertext, null)
        return String(plaintext, StandardCharsets.UTF_8)
    }

    private fun toAapInstance(serialized: SerializedInstance): AapInstance {
        return AapInstance(
            id = serialized.id,
            baseUrl = decrypt(serialized.encryptedUrl),
            token = decrypt(serialized.encryptedToken),
            alias = serialized.alias,
            apiVersion = serialized.apiVersion,
            trustSelfSigned = serialized.trustSelfSigned,
            certFingerprint = serialized.certFingerprint
        )
    }

    private fun toSerialized(instance: AapInstance): SerializedInstance {
        return SerializedInstance(
            id = instance.id,
            encryptedUrl = encrypt(instance.baseUrl),
            encryptedToken = encrypt(instance.token),
            alias = instance.alias,
            apiVersion = instance.apiVersion,
            trustSelfSigned = instance.trustSelfSigned,
            certFingerprint = instance.certFingerprint
        )
    }

    private suspend fun readState(): InstancesState {
        val prefs = context.credentialsDataStore.data.first()
        val jsonString = prefs[KEY_INSTANCES_JSON] ?: return InstancesState()
        return try {
            json.decodeFromString<InstancesState>(jsonString)
        } catch (_: Exception) {
            InstancesState()
        }
    }

    private suspend fun writeState(state: InstancesState) {
        val jsonString = json.encodeToString(state)
        context.credentialsDataStore.edit { prefs ->
            prefs[KEY_INSTANCES_JSON] = jsonString
        }
        updateReactiveState(state)
    }

    private fun updateReactiveState(state: InstancesState) {
        val decryptedInstances = state.instances.mapNotNull { serialized ->
            try {
                toAapInstance(serialized)
            } catch (_: Exception) {
                null
            }
        }
        _instances.value = decryptedInstances

        val active = decryptedInstances.find { it.id == state.activeInstanceId }

        // Update cached fields BEFORE emitting activeInstance to avoid race conditions
        // where ViewModels observe the change and make API calls with stale tokens
        if (active != null) {
            cachedBaseUrl = active.baseUrl
            cachedToken = active.token
            cachedApiVersion = try {
                ApiVersion.valueOf(active.apiVersion)
            } catch (_: Exception) {
                ApiVersion.CONTROLLER_V2
            }
            cachedTrustSelfSigned = active.trustSelfSigned
        } else {
            cachedBaseUrl = null
            cachedToken = null
            cachedApiVersion = ApiVersion.CONTROLLER_V2
            cachedTrustSelfSigned = false
        }

        _activeInstance.value = active
    }

    /**
     * Save a new instance or update an existing one.
     * If this is the first instance, it becomes active automatically.
     * Returns the instance ID.
     */
    suspend fun saveInstance(
        baseUrl: String,
        token: String,
        alias: String? = null,
        apiVersion: ApiVersion,
        trustSelfSigned: Boolean = false,
        certFingerprint: String? = null,
        existingId: String? = null
    ): String {
        val normalizedUrl = baseUrl.trimEnd('/').lowercase()

        val state = readState()

        // Check for duplicate URL (skip if updating existing instance)
        val duplicate = state.instances.find { serialized ->
            val id = existingId ?: ""
            if (serialized.id == id) return@find false
            try {
                val existingUrl = decrypt(serialized.encryptedUrl).trimEnd('/').lowercase()
                existingUrl == normalizedUrl
            } catch (_: Exception) {
                false
            }
        }
        if (duplicate != null) {
            throw IllegalArgumentException("An instance with this URL already exists")
        }

        val instanceId = existingId ?: UUID.randomUUID().toString()

        val instance = AapInstance(
            id = instanceId,
            baseUrl = baseUrl,
            token = token,
            alias = alias?.ifBlank { null },
            apiVersion = apiVersion.name,
            trustSelfSigned = trustSelfSigned,
            certFingerprint = certFingerprint
        )

        val serialized = toSerialized(instance)

        val updatedInstances = if (existingId != null) {
            state.instances.map { if (it.id == existingId) serialized else it }
        } else {
            state.instances + serialized
        }

        val activeId = when {
            // First instance ever added — make it active
            state.instances.isEmpty() && existingId == null -> instanceId
            // Re-auth or update of existing instance — ensure it's active
            existingId != null -> existingId
            // Adding a new instance — keep current active
            else -> state.activeInstanceId
        }

        writeState(InstancesState(instances = updatedInstances, activeInstanceId = activeId))
        return instanceId
    }

    /**
     * Remove an instance by ID.
     * If the removed instance was active, promotes the next available instance.
     * Returns true if an instance was removed.
     */
    suspend fun removeInstance(instanceId: String): Boolean {
        val state = readState()
        val updatedInstances = state.instances.filter { it.id != instanceId }
        if (updatedInstances.size == state.instances.size) return false

        val newActiveId = if (state.activeInstanceId == instanceId) {
            updatedInstances.firstOrNull()?.id
        } else {
            state.activeInstanceId
        }

        writeState(InstancesState(instances = updatedInstances, activeInstanceId = newActiveId))
        return true
    }

    /**
     * Set the active instance by ID.
     */
    suspend fun setActiveInstance(instanceId: String) {
        val state = readState()
        if (state.instances.none { it.id == instanceId }) return
        writeState(state.copy(activeInstanceId = instanceId))
    }

    /**
     * Get a specific instance by ID.
     */
    fun getInstanceById(instanceId: String): AapInstance? {
        return _instances.value.find { it.id == instanceId }
    }

    /**
     * Load all instances from DataStore and update reactive state.
     * Also handles legacy credential cleanup (R-005).
     * Returns true if any instances exist after loading.
     */
    suspend fun loadCredentials(): Boolean {
        val prefs = context.credentialsDataStore.data.first()

        // Legacy cleanup (R-005): if instances_json is absent but old keys exist, clear them
        if (prefs[KEY_INSTANCES_JSON] == null) {
            val hasLegacyKeys = prefs[KEY_BASE_URL] != null || prefs[KEY_TOKEN] != null
            if (hasLegacyKeys) {
                context.credentialsDataStore.edit { p ->
                    p.remove(KEY_BASE_URL)
                    p.remove(KEY_TOKEN)
                    p.remove(KEY_API_VERSION)
                    p.remove(KEY_TRUST_SELF_SIGNED)
                    p.remove(KEY_CERT_FINGERPRINT)
                }
            }
            updateReactiveState(InstancesState())
            return false
        }

        val state = readState()
        updateReactiveState(state)
        return state.instances.isNotEmpty()
    }

    /**
     * Legacy compatibility: save credentials as a single instance.
     * Used by AuthRepository during the connect flow.
     */
    suspend fun saveCredentials(
        baseUrl: String,
        token: String,
        apiVersion: ApiVersion,
        trustSelfSigned: Boolean = false,
        certFingerprint: String? = null,
        alias: String? = null,
        existingId: String? = null
    ): String {
        return saveInstance(
            baseUrl = baseUrl,
            token = token,
            alias = alias,
            apiVersion = apiVersion,
            trustSelfSigned = trustSelfSigned,
            certFingerprint = certFingerprint,
            existingId = existingId
        )
    }

    /**
     * Clear all instances (full logout).
     */
    suspend fun clearCredentials() {
        context.credentialsDataStore.edit { it.clear() }
        _instances.value = emptyList()
        _activeInstance.value = null
        cachedToken = null
        cachedBaseUrl = null
        cachedApiVersion = ApiVersion.CONTROLLER_V2
        cachedTrustSelfSigned = false
    }

    val isLoggedIn: Flow<Boolean> = context.credentialsDataStore.data.map { prefs ->
        val jsonString = prefs[KEY_INSTANCES_JSON]
        if (jsonString != null) {
            try {
                val state = json.decodeFromString<InstancesState>(jsonString)
                state.instances.isNotEmpty()
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
    }
}

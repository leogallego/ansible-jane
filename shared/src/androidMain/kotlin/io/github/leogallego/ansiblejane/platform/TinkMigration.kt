package io.github.leogallego.ansiblejane.platform

import android.content.Context
import android.util.Log
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AeadKeyTemplates
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class TinkMigration(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun migrateIfNeeded(
        dataStoreFactory: DataStoreFactory,
        newKeyStorage: SecureKeyStorage
    ): MigrationResult {
        val tinkPrefs = context.getSharedPreferences(TINK_PREFS_NAME, Context.MODE_PRIVATE)
        val hasTinkKeyset = tinkPrefs.contains(TINK_KEYSET_KEY)

        if (!hasTinkKeyset) {
            return MigrationResult.NotNeeded
        }

        val tinkAead = try {
            AeadConfig.register()
            val keysetHandle = AndroidKeysetManager.Builder()
                .withSharedPref(context, TINK_KEYSET_KEY, TINK_PREFS_NAME)
                .withKeyTemplate(AeadKeyTemplates.AES256_GCM)
                .withMasterKeyUri("android-keystore://aap_master_key")
                .build()
                .keysetHandle
            keysetHandle.getPrimitive(
                com.google.crypto.tink.RegistryConfiguration.get(),
                Aead::class.java
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Tink for migration", e)
            return MigrationResult.Failed("Cannot read Tink keyset: ${e.message}")
        }

        val credentialsStore = dataStoreFactory.createPreferencesDataStore("credentials")
        val prefs = credentialsStore.data.first()
        val instancesJson = prefs[stringPreferencesKey("instances_json")] ?: run {
            cleanupTinkKeyset(tinkPrefs)
            return MigrationResult.NotNeeded
        }

        val state = try {
            json.decodeFromString<MigrationInstancesState>(instancesJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse instances state", e)
            return MigrationResult.Failed("Cannot parse stored credentials: ${e.message}")
        }

        var migratedCount = 0
        var failedCount = 0
        val migratedInstances = state.instances.map { instance ->
            try {
                val decryptedUrl = tinkDecrypt(tinkAead, instance.encryptedUrl)
                val decryptedToken = tinkDecrypt(tinkAead, instance.encryptedToken)

                val reEncryptedUrl = newEncrypt(newKeyStorage, decryptedUrl)
                val reEncryptedToken = newEncrypt(newKeyStorage, decryptedToken)

                migratedCount++
                instance.copy(encryptedUrl = reEncryptedUrl, encryptedToken = reEncryptedToken)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to migrate instance ${instance.id}", e)
                failedCount++
                instance
            }
        }

        val llmKeysJson = prefs[stringPreferencesKey("llm_api_keys")]
        var migratedLlmKeys: String? = llmKeysJson
        if (llmKeysJson != null) {
            try {
                val keysMap = json.decodeFromString<Map<String, String>>(llmKeysJson)
                val migratedMap = keysMap.mapValues { (_, encrypted) ->
                    val decrypted = tinkDecrypt(tinkAead, encrypted)
                    newEncrypt(newKeyStorage, decrypted)
                }
                migratedLlmKeys = json.encodeToString(
                    MapSerializer(String.serializer(), String.serializer()),
                    migratedMap
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to migrate LLM API keys", e)
                failedCount++
            }
        }

        if (failedCount > 0 && migratedCount == 0) {
            return MigrationResult.Failed("All $failedCount instances failed migration")
        }

        val newState = state.copy(instances = migratedInstances)
        val newStateJson = json.encodeToString(MigrationInstancesState.serializer(), newState)

        credentialsStore.edit { mutablePrefs ->
            mutablePrefs[stringPreferencesKey("instances_json")] = newStateJson
            if (migratedLlmKeys != null) {
                mutablePrefs[stringPreferencesKey("llm_api_keys")] = migratedLlmKeys
            }
        }

        cleanupTinkKeyset(tinkPrefs)

        Log.i(TAG, "Migration complete: $migratedCount migrated, $failedCount failed")
        return if (failedCount > 0) {
            MigrationResult.Partial(migratedCount, failedCount)
        } else {
            MigrationResult.Success(migratedCount)
        }
    }

    private fun tinkDecrypt(aead: Aead, base64Ciphertext: String): String {
        val ciphertext = Base64.decode(base64Ciphertext)
        val plaintext = aead.decrypt(ciphertext, null)
        return plaintext.decodeToString()
    }

    private fun newEncrypt(keyStorage: SecureKeyStorage, plaintext: String): String {
        val ciphertext = keyStorage.encrypt(plaintext.encodeToByteArray())
        return Base64.encode(ciphertext)
    }

    private fun cleanupTinkKeyset(tinkPrefs: android.content.SharedPreferences) {
        tinkPrefs.edit().clear().apply()
    }

    sealed class MigrationResult {
        data object NotNeeded : MigrationResult()
        data class Success(val count: Int) : MigrationResult()
        data class Partial(val migrated: Int, val failed: Int) : MigrationResult()
        data class Failed(val reason: String) : MigrationResult()
    }

    @Serializable
    private data class MigrationInstancesState(
        val instances: List<MigrationSerializedInstance> = emptyList(),
        val activeInstanceId: String? = null
    )

    @Serializable
    private data class MigrationSerializedInstance(
        val id: String,
        val encryptedUrl: String,
        val encryptedToken: String,
        val alias: String? = null,
        val apiVersion: String = "CONTROLLER_V2",
        val trustSelfSigned: Boolean = false,
        val certFingerprint: String? = null,
        val mcpEnabled: Boolean = false
    )

    companion object {
        private const val TAG = "TinkMigration"
        private const val TINK_PREFS_NAME = "aap_keyset_prefs"
        private const val TINK_KEYSET_KEY = "aap_keyset"
    }
}

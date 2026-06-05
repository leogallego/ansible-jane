package io.github.leogallego.ansiblejane.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.leogallego.ansiblejane.model.ToolManifest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ToolManifestRepository(private val context: Context) : IToolManifestRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun saveManifest(instanceId: String, manifest: ToolManifest) {
        val key = stringPreferencesKey("manifest_$instanceId")
        val jsonString = json.encodeToString(manifest)
        if (jsonString.length > SIZE_WARNING_THRESHOLD) {
            Log.w(TAG, "Manifest cache for $instanceId exceeds 100KB (${jsonString.length} chars)")
        }
        context.credentialsDataStore.edit { prefs ->
            prefs[key] = jsonString
        }
    }

    override suspend fun loadManifest(instanceId: String): ToolManifest? {
        val key = stringPreferencesKey("manifest_$instanceId")
        val prefs = context.credentialsDataStore.data.first()
        val jsonString = prefs[key] ?: return null
        return try {
            val manifest = json.decodeFromString<ToolManifest>(jsonString)
            when {
                manifest.schemaVersion != ToolManifest.CURRENT_SCHEMA_VERSION -> {
                    Log.w(TAG, "Manifest schema version mismatch: ${manifest.schemaVersion} != ${ToolManifest.CURRENT_SCHEMA_VERSION}")
                    deleteManifest(instanceId)
                    null
                }
                System.currentTimeMillis() - manifest.cachedAt > MAX_CACHE_AGE_MS -> {
                    Log.d(TAG, "Manifest cache expired for $instanceId")
                    deleteManifest(instanceId)
                    null
                }
                else -> manifest
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deserialize manifest for $instanceId: ${e.message}")
            deleteManifest(instanceId)
            null
        }
    }

    override suspend fun deleteManifest(instanceId: String) {
        val key = stringPreferencesKey("manifest_$instanceId")
        context.credentialsDataStore.edit { prefs ->
            prefs.remove(key)
        }
    }

    companion object {
        private const val TAG = "ToolManifestRepository"
        const val MAX_CACHE_AGE_MS = 7L * 24 * 60 * 60 * 1000
        private const val SIZE_WARNING_THRESHOLD = 100_000
    }
}

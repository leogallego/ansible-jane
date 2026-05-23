package io.github.leogallego.ansiblejane.assistant.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage
import io.github.leogallego.ansiblejane.assistant.data.KnownProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.assistantDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "assistant_config"
)

class AssistantRepository(private val context: Context) : IAssistantRepository {

    private val messages = mutableListOf<ChatMessage>()
    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        private const val TAG = "AssistantRepository"
        val KEY_LLM_CONFIGS = stringPreferencesKey("llm_configs")
        val KEY_ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        const val MAX_MESSAGES = 100
    }

    override fun addMessage(message: ChatMessage) {
        messages.add(message)
        if (messages.size > MAX_MESSAGES) {
            messages.removeAt(0)
        }
    }

    override fun getHistory(): List<ChatMessage> = messages.toList()

    override fun clearHistory() {
        messages.clear()
    }

    override suspend fun saveLlmConfig(config: LlmProviderConfig) {
        val providerKey = when (config) {
            is LlmProviderConfig.OpenAiCompatible -> KnownProvider.fromUrl(config.url).name
        }
        val allConfigs = loadAllLlmConfigs().toMutableMap()
        allConfigs[providerKey] = config
        val mapJson = json.encodeToString(
            MapSerializer(String.serializer(), LlmProviderConfig.serializer()),
            allConfigs
        )
        context.assistantDataStore.edit { prefs ->
            prefs[KEY_LLM_CONFIGS] = mapJson
            prefs[KEY_ACTIVE_PROVIDER] = providerKey
        }
    }

    override suspend fun loadLlmConfig(): LlmProviderConfig? {
        val prefs = context.assistantDataStore.data.first()
        val activeProvider = prefs[KEY_ACTIVE_PROVIDER] ?: return null
        return loadAllLlmConfigs()[activeProvider]
    }

    override suspend fun saveAllLlmConfigs(configs: Map<String, LlmProviderConfig>) {
        val mapJson = json.encodeToString(
            MapSerializer(String.serializer(), LlmProviderConfig.serializer()),
            configs
        )
        context.assistantDataStore.edit { prefs ->
            prefs[KEY_LLM_CONFIGS] = mapJson
        }
    }

    override suspend fun loadAllLlmConfigs(): Map<String, LlmProviderConfig> {
        val prefs = context.assistantDataStore.data.first()
        val mapJson = prefs[KEY_LLM_CONFIGS] ?: return emptyMap()
        return try {
            json.decodeFromString(
                MapSerializer(String.serializer(), LlmProviderConfig.serializer()),
                mapJson
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deserialize LLM configs", e)
            emptyMap()
        }
    }

    override val activeConfigFlow: Flow<LlmProviderConfig?> =
        context.assistantDataStore.data.map { prefs ->
            val key = prefs[KEY_ACTIVE_PROVIDER] ?: return@map null
            val mapJson = prefs[KEY_LLM_CONFIGS] ?: return@map null
            try {
                val configs = json.decodeFromString(
                    MapSerializer(String.serializer(), LlmProviderConfig.serializer()),
                    mapJson
                )
                configs[key]
            } catch (e: Exception) {
                Log.w(TAG, "Failed to deserialize active config from flow", e)
                null
            }
        }.distinctUntilChanged()
}

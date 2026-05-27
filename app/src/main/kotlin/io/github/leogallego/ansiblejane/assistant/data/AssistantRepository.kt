package io.github.leogallego.ansiblejane.assistant.data

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage
import io.github.leogallego.ansiblejane.assistant.engine.Role
import io.github.leogallego.ansiblejane.assistant.data.KnownProvider
import io.github.leogallego.ansiblejane.data.ITokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.assistantDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "assistant_config"
)

class AssistantRepository(
    private val context: Context,
    private val tokenManager: ITokenManager
) : IAssistantRepository {

    private val messages = mutableListOf<ChatMessage>()
    private val json = Json { ignoreUnknownKeys = true }
    private val _onHistoryCleared = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val onHistoryCleared: SharedFlow<Unit> = _onHistoryCleared.asSharedFlow()

    private companion object {
        private const val TAG = "AssistantRepository"
        val KEY_LLM_CONFIGS = stringPreferencesKey("llm_configs")
        val KEY_ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        val KEY_DISABLED_TOOLS = stringPreferencesKey("disabled_tools")
        const val MAX_MESSAGES = 100
    }

    override fun addMessage(message: ChatMessage) {
        messages.add(message)
        if (messages.size > MAX_MESSAGES) {
            messages.removeAt(0)
        }
    }

    override fun getHistory(): List<ChatMessage> = messages.toList()

    override fun removeLastAssistantMessage() {
        val index = messages.indexOfLast { it.role == Role.ASSISTANT }
        if (index >= 0) messages.removeAt(index)
    }

    override fun removeLastUserMessage() {
        val index = messages.indexOfLast { it.role == Role.USER }
        if (index >= 0) messages.removeAt(index)
    }

    override fun clearHistory() {
        messages.clear()
        _onHistoryCleared.tryEmit(Unit)
    }

    override suspend fun saveLlmConfig(config: LlmProviderConfig) {
        val providerKey = when (config) {
            is LlmProviderConfig.OpenAiCompatible -> KnownProvider.fromUrl(config.url).name
        }

        val apiKey = when (config) {
            is LlmProviderConfig.OpenAiCompatible -> config.apiKey
        }
        if (!apiKey.isNullOrBlank()) {
            tokenManager.saveLlmApiKey(providerKey, apiKey)
        }
        val strippedConfig = stripApiKey(config)

        val allConfigs = loadAllLlmConfigsRaw().toMutableMap()
        allConfigs[providerKey] = strippedConfig
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
        val config = loadAllLlmConfigsRaw()[activeProvider] ?: return null
        return mergeApiKey(activeProvider, config)
    }

    override suspend fun saveAllLlmConfigs(configs: Map<String, LlmProviderConfig>) {
        for ((key, config) in configs) {
            val apiKey = when (config) {
                is LlmProviderConfig.OpenAiCompatible -> config.apiKey
            }
            if (!apiKey.isNullOrBlank()) {
                tokenManager.saveLlmApiKey(key, apiKey)
            }
        }
        val stripped = configs.mapValues { (_, config) -> stripApiKey(config) }
        val mapJson = json.encodeToString(
            MapSerializer(String.serializer(), LlmProviderConfig.serializer()),
            stripped
        )
        context.assistantDataStore.edit { prefs ->
            prefs[KEY_LLM_CONFIGS] = mapJson
        }
    }

    override suspend fun loadAllLlmConfigs(): Map<String, LlmProviderConfig> {
        val raw = loadAllLlmConfigsRaw()
        val keys = tokenManager.loadAllLlmApiKeys()
        return raw.mapValues { (providerKey, config) ->
            val apiKey = keys[providerKey]
            if (apiKey != null) mergeApiKeyValue(config, apiKey) else config
        }
    }

    private suspend fun loadAllLlmConfigsRaw(): Map<String, LlmProviderConfig> {
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

    private suspend fun mergeApiKey(providerKey: String, config: LlmProviderConfig): LlmProviderConfig {
        val apiKey = tokenManager.loadLlmApiKey(providerKey)
        return if (apiKey != null) mergeApiKeyValue(config, apiKey) else config
    }

    private fun mergeApiKeyValue(config: LlmProviderConfig, apiKey: String): LlmProviderConfig =
        when (config) {
            is LlmProviderConfig.OpenAiCompatible -> config.copy(apiKey = apiKey)
        }

    private fun stripApiKey(config: LlmProviderConfig): LlmProviderConfig =
        when (config) {
            is LlmProviderConfig.OpenAiCompatible -> config.copy(apiKey = null)
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
                val config = configs[key] ?: return@map null
                val apiKey = tokenManager.loadLlmApiKey(key)
                if (apiKey != null) mergeApiKeyValue(config, apiKey) else config
            } catch (e: Exception) {
                Log.w(TAG, "Failed to deserialize active config from flow", e)
                null
            }
        }.distinctUntilChanged()

    override val savedConfigsFlow: Flow<Map<String, LlmProviderConfig>> =
        context.assistantDataStore.data.map { prefs ->
            val mapJson = prefs[KEY_LLM_CONFIGS] ?: return@map emptyMap()
            try {
                val configs = json.decodeFromString(
                    MapSerializer(String.serializer(), LlmProviderConfig.serializer()),
                    mapJson
                )
                val keys = tokenManager.loadAllLlmApiKeys()
                configs.mapValues { (providerKey, config) ->
                    val apiKey = keys[providerKey]
                    if (apiKey != null) mergeApiKeyValue(config, apiKey) else config
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to deserialize saved configs from flow", e)
                emptyMap()
            }
        }.distinctUntilChanged()

    override val activeProviderKeyFlow: Flow<String?> =
        context.assistantDataStore.data.map { prefs ->
            prefs[KEY_ACTIVE_PROVIDER]
        }.distinctUntilChanged()

    override suspend fun switchActiveProvider(providerKey: String) {
        context.assistantDataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROVIDER] = providerKey
        }
    }

    override suspend fun saveDisabledTools(tools: Set<String>) {
        val encoded = json.encodeToString(
            kotlinx.serialization.builtins.SetSerializer(String.serializer()),
            tools
        )
        context.assistantDataStore.edit { prefs ->
            prefs[KEY_DISABLED_TOOLS] = encoded
        }
    }

    override suspend fun getDisabledTools(): Set<String> {
        val prefs = context.assistantDataStore.data.first()
        val encoded = prefs[KEY_DISABLED_TOOLS] ?: return emptySet()
        return try {
            json.decodeFromString(
                kotlinx.serialization.builtins.SetSerializer(String.serializer()),
                encoded
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to deserialize disabled tools", e)
            emptySet()
        }
    }
}

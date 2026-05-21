package com.example.aapremote.assistant.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.aapremote.assistant.engine.ChatMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.assistantDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "assistant_config"
)

class AssistantRepository(private val context: Context) : IAssistantRepository {

    private val messages = mutableListOf<ChatMessage>()
    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        val KEY_LLM_CONFIG = stringPreferencesKey("llm_config")
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
        val jsonString = json.encodeToString(LlmProviderConfig.serializer(), config)
        context.assistantDataStore.edit { prefs ->
            prefs[KEY_LLM_CONFIG] = jsonString
        }
    }

    override suspend fun loadLlmConfig(): LlmProviderConfig? {
        val prefs = context.assistantDataStore.data.first()
        val jsonString = prefs[KEY_LLM_CONFIG] ?: return null
        return try {
            json.decodeFromString(LlmProviderConfig.serializer(), jsonString)
        } catch (_: Exception) {
            null
        }
    }
}

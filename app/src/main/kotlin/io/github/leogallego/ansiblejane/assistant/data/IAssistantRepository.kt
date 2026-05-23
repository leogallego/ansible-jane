package io.github.leogallego.ansiblejane.assistant.data

import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage
import kotlinx.coroutines.flow.Flow

interface IAssistantRepository {
    fun addMessage(message: ChatMessage)
    fun getHistory(): List<ChatMessage>
    fun clearHistory()
    suspend fun saveLlmConfig(config: LlmProviderConfig)
    suspend fun loadLlmConfig(): LlmProviderConfig?
    suspend fun saveAllLlmConfigs(configs: Map<String, LlmProviderConfig>)
    suspend fun loadAllLlmConfigs(): Map<String, LlmProviderConfig>
    val activeConfigFlow: Flow<LlmProviderConfig?>
}

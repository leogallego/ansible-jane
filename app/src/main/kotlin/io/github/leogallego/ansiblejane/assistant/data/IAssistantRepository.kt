package io.github.leogallego.ansiblejane.assistant.data

import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow

interface IAssistantRepository {
    fun addMessage(message: ChatMessage)
    fun getHistory(): List<ChatMessage>
    fun removeLastAssistantMessage()
    fun clearHistory()
    val onHistoryCleared: SharedFlow<Unit>
    suspend fun saveLlmConfig(config: LlmProviderConfig)
    suspend fun loadLlmConfig(): LlmProviderConfig?
    suspend fun saveAllLlmConfigs(configs: Map<String, LlmProviderConfig>)
    suspend fun loadAllLlmConfigs(): Map<String, LlmProviderConfig>
    val activeConfigFlow: Flow<LlmProviderConfig?>
    val savedConfigsFlow: Flow<Map<String, LlmProviderConfig>>
    val activeProviderKeyFlow: Flow<String?>
    suspend fun switchActiveProvider(providerKey: String)
}

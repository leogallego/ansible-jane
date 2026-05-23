package io.github.leogallego.ansiblejane.assistant.data

import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage

interface IAssistantRepository {
    fun addMessage(message: ChatMessage)
    fun getHistory(): List<ChatMessage>
    fun clearHistory()
    suspend fun saveLlmConfig(config: LlmProviderConfig)
    suspend fun loadLlmConfig(): LlmProviderConfig?
}

package com.example.aapremote.assistant.data

import com.example.aapremote.assistant.engine.ChatMessage

interface IAssistantRepository {
    fun addMessage(message: ChatMessage)
    fun getHistory(): List<ChatMessage>
    fun clearHistory()
    suspend fun saveLlmConfig(config: LlmProviderConfig)
    suspend fun loadLlmConfig(): LlmProviderConfig?
}

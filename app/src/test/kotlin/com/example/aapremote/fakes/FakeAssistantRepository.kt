package com.example.aapremote.fakes

import com.example.aapremote.assistant.data.IAssistantRepository
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.assistant.engine.ChatMessage

class FakeAssistantRepository : IAssistantRepository {
    private val messages = mutableListOf<ChatMessage>()
    var savedConfig: LlmProviderConfig? = null

    override fun addMessage(message: ChatMessage) {
        messages.add(message)
    }

    override fun getHistory(): List<ChatMessage> = messages.toList()

    override fun clearHistory() {
        messages.clear()
    }

    override suspend fun saveLlmConfig(config: LlmProviderConfig) {
        savedConfig = config
    }

    override suspend fun loadLlmConfig(): LlmProviderConfig? = savedConfig
}

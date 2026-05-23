package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage

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

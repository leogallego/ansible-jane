package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.assistant.data.KnownProvider
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage

class FakeAssistantRepository : IAssistantRepository {
    private val messages = mutableListOf<ChatMessage>()
    var savedConfig: LlmProviderConfig? = null
    var allConfigs = mutableMapOf<String, LlmProviderConfig>()
    var activeProvider: String? = null

    override fun addMessage(message: ChatMessage) {
        messages.add(message)
    }

    override fun getHistory(): List<ChatMessage> = messages.toList()

    override fun clearHistory() {
        messages.clear()
    }

    override suspend fun saveLlmConfig(config: LlmProviderConfig) {
        val providerKey = when (config) {
            is LlmProviderConfig.OpenAiCompatible -> KnownProvider.fromUrl(config.url).name
        }
        allConfigs[providerKey] = config
        activeProvider = providerKey
        savedConfig = config
    }

    override suspend fun loadLlmConfig(): LlmProviderConfig? {
        val key = activeProvider ?: return savedConfig
        return allConfigs[key] ?: savedConfig
    }

    override suspend fun saveAllLlmConfigs(configs: Map<String, LlmProviderConfig>) {
        allConfigs = configs.toMutableMap()
    }

    override suspend fun loadAllLlmConfigs(): Map<String, LlmProviderConfig> = allConfigs.toMap()
}

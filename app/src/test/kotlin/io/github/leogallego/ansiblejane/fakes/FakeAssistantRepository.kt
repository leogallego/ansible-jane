package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.assistant.data.KnownProvider
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage
import io.github.leogallego.ansiblejane.assistant.engine.Role
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class FakeAssistantRepository : IAssistantRepository {
    private val messages = mutableListOf<ChatMessage>()
    var savedConfig: LlmProviderConfig? = null
    var allConfigs = mutableMapOf<String, LlmProviderConfig>()
    var activeProvider: String? = null

    private val _onHistoryCleared = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val onHistoryCleared: SharedFlow<Unit> = _onHistoryCleared.asSharedFlow()

    private val _activeConfigFlow = MutableStateFlow<LlmProviderConfig?>(null)
    override val activeConfigFlow: Flow<LlmProviderConfig?> = _activeConfigFlow

    private val _savedConfigsFlow = MutableStateFlow<Map<String, LlmProviderConfig>>(emptyMap())
    override val savedConfigsFlow: Flow<Map<String, LlmProviderConfig>> = _savedConfigsFlow

    private val _activeProviderKeyFlow = MutableStateFlow<String?>(null)
    override val activeProviderKeyFlow: Flow<String?> = _activeProviderKeyFlow

    override fun addMessage(message: ChatMessage) {
        messages.add(message)
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
        allConfigs[providerKey] = config
        activeProvider = providerKey
        savedConfig = config
        _activeConfigFlow.value = config
        _savedConfigsFlow.value = allConfigs.toMap()
        _activeProviderKeyFlow.value = providerKey
    }

    override suspend fun loadLlmConfig(): LlmProviderConfig? {
        val key = activeProvider ?: return savedConfig
        return allConfigs[key] ?: savedConfig
    }

    override suspend fun saveAllLlmConfigs(configs: Map<String, LlmProviderConfig>) {
        allConfigs = configs.toMutableMap()
        _savedConfigsFlow.value = allConfigs.toMap()
    }

    override suspend fun loadAllLlmConfigs(): Map<String, LlmProviderConfig> = allConfigs.toMap()

    override suspend fun switchActiveProvider(providerKey: String) {
        activeProvider = providerKey
        _activeProviderKeyFlow.value = providerKey
        _activeConfigFlow.value = allConfigs[providerKey]
    }
}

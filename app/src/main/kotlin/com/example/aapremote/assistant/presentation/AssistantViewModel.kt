package com.example.aapremote.assistant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.assistant.data.AssistantRepository
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.assistant.engine.ChatEngine
import com.example.aapremote.assistant.engine.ChatEvent
import com.example.aapremote.assistant.engine.ChatMessage
import com.example.aapremote.assistant.engine.Role
import com.example.aapremote.assistant.engine.ToolExecutor
import com.example.aapremote.assistant.llm.OpenAiCompatibleProvider
import com.example.aapremote.data.TokenManager
import com.example.aapremote.model.McpServerConfig
import com.example.aapremote.network.mcp.McpConnectionState
import com.example.aapremote.network.mcp.McpServerManager
import com.example.aapremote.network.networkJson
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class AssistantViewModel(
    private val mcpServerManager: McpServerManager,
    private val repository: AssistantRepository,
    private val tokenManager: TokenManager,
    private val httpClient: OkHttpClient,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    val activeInstance get() = tokenManager.activeInstance.value

    private var generateJob: Job? = null
    private var llmConfig: LlmProviderConfig? = null

    init {
        viewModelScope.launch {
            llmConfig = repository.loadLlmConfig()
        }

        viewModelScope.launch {
            tokenManager.activeInstance
                .distinctUntilChangedBy { Triple(it?.id, it?.mcpEnabled, it?.mcpServerUrls) }
                .collect { instance ->
                    if (instance != null) {
                        _uiState.update { AssistantUiState.Loading }
                        mcpServerManager.connectAll(instance)
                        _uiState.update {
                            AssistantUiState.Active(
                                messages = repository.getHistory(),
                                connections = mcpServerManager.connections.value
                            )
                        }
                    } else {
                        mcpServerManager.disconnectAll()
                        _uiState.update { AssistantUiState.Idle }
                    }
                }
        }

        viewModelScope.launch {
            mcpServerManager.connections.collect { connections ->
                _uiState.update { current ->
                    if (current is AssistantUiState.Active) current.copy(connections = connections)
                    else current
                }
            }
        }
    }

    fun updateInputText(text: String) {
        _uiState.update { current ->
            if (current is AssistantUiState.Active) current.copy(inputText = text)
            else current
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val config = llmConfig
        if (config == null) {
            _uiState.update { current ->
                if (current is AssistantUiState.Active) {
                    current.copy(
                        messages = current.messages + ChatMessage(
                            role = Role.ASSISTANT,
                            content = "Please configure an LLM provider in settings first."
                        )
                    )
                } else current
            }
            return
        }

        val userMessage = ChatMessage(role = Role.USER, content = text)
        repository.addMessage(userMessage)

        updateState { copy(
            messages = repository.getHistory(),
            inputText = "",
            isGenerating = true
        ) }

        val provider = when (config) {
            is LlmProviderConfig.OpenAiCompatible ->
                OpenAiCompatibleProvider(config, httpClient, json)
        }

        val tools = mcpServerManager.getAllTools()
        val toolSpecs = tools.map { it.spec }
        val toolExecutor = ToolExecutor(tools)
        val engine = ChatEngine(provider, toolExecutor)

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            val textBuilder = StringBuilder()

            engine.processMessage(text, repository.getHistory(), toolSpecs)
                .collect { event ->
                    when (event) {
                        is ChatEvent.TextDelta -> {
                            textBuilder.append(event.text)
                            updateState {
                                val streamingMsg = ChatMessage(
                                    role = Role.ASSISTANT,
                                    content = textBuilder.toString()
                                )
                                val msgs = messages.dropLast(
                                    if (messages.lastOrNull()?.role == Role.ASSISTANT &&
                                        messages.lastOrNull()?.toolCalls == null) 1 else 0
                                ) + streamingMsg
                                copy(messages = msgs)
                            }
                        }
                        is ChatEvent.ToolExecuting -> {
                            val indicator = ChatMessage(
                                role = Role.ASSISTANT,
                                content = "Querying tool: ${event.toolName}..."
                            )
                            updateState { copy(messages = messages + indicator) }
                        }
                        is ChatEvent.ToolResult -> {
                            // Tool results are internal — LLM will summarize
                            updateState {
                                val msgs = messages.dropLast(
                                    if (messages.lastOrNull()?.content?.startsWith("Querying tool:") == true) 1 else 0
                                )
                                copy(messages = msgs)
                            }
                            textBuilder.clear()
                        }
                        is ChatEvent.AssistantMessage -> {
                            val finalMsg = ChatMessage(
                                role = Role.ASSISTANT,
                                content = event.fullText
                            )
                            repository.addMessage(finalMsg)
                            updateState {
                                copy(
                                    messages = repository.getHistory(),
                                    isGenerating = false
                                )
                            }
                        }
                        is ChatEvent.Error -> {
                            val errorMsg = ChatMessage(
                                role = Role.ASSISTANT,
                                content = "Error: ${event.message}"
                            )
                            repository.addMessage(errorMsg)
                            updateState {
                                copy(
                                    messages = repository.getHistory(),
                                    isGenerating = false
                                )
                            }
                        }
                    }
                }
        }
    }

    fun clearHistory() {
        repository.clearHistory()
        updateState { copy(messages = emptyList()) }
    }

    fun updateLlmConfig(config: LlmProviderConfig) {
        llmConfig = config
        viewModelScope.launch {
            repository.saveLlmConfig(config)
        }
    }

    fun toggleMcpEnabled(enabled: Boolean) {
        val instance = tokenManager.activeInstance.value ?: return
        viewModelScope.launch {
            val servers = if (enabled && instance.mcpServerUrls.isNullOrEmpty()) {
                val base = "${instance.baseUrl.trimEnd('/')}:8448"
                listOf(
                    McpServerConfig(url = "$base/mcp", label = "AAP", isAutoDetected = true)
                )
            } else {
                instance.mcpServerUrls
            }
            tokenManager.updateMcpConfig(instance.id, enabled, if (enabled) servers else null)
        }
    }

    fun addMcpServer(url: String, label: String) {
        val instance = tokenManager.activeInstance.value ?: return
        viewModelScope.launch {
            val current = instance.mcpServerUrls?.toMutableList() ?: mutableListOf()
            current.add(McpServerConfig(url = url.trimEnd('/'), label = label))
            tokenManager.updateMcpConfig(instance.id, true, current)
        }
    }

    fun removeMcpServer(url: String) {
        val instance = tokenManager.activeInstance.value ?: return
        viewModelScope.launch {
            val updated = instance.mcpServerUrls?.filter { it.url != url }
            val enabled = !updated.isNullOrEmpty()
            tokenManager.updateMcpConfig(instance.id, enabled, updated)
        }
    }

    fun updateMcpServer(oldUrl: String, newUrl: String, label: String) {
        val instance = tokenManager.activeInstance.value ?: return
        viewModelScope.launch {
            val updated = instance.mcpServerUrls?.map {
                if (it.url == oldUrl) it.copy(url = newUrl.trimEnd('/'), label = label, isAutoDetected = false)
                else it
            }
            tokenManager.updateMcpConfig(instance.id, instance.mcpEnabled, updated)
        }
    }

    private inline fun updateState(crossinline transform: AssistantUiState.Active.() -> AssistantUiState.Active) {
        _uiState.update { current ->
            if (current is AssistantUiState.Active) current.transform()
            else current
        }
    }
}

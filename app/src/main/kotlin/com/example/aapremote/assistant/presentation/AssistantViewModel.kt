package com.example.aapremote.assistant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.assistant.data.AssistantRepository
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.assistant.data.ModelFetcher
import com.example.aapremote.assistant.data.TokenSavingMode
import com.example.aapremote.assistant.engine.ChatEngine
import com.example.aapremote.assistant.engine.ChatEvent
import com.example.aapremote.assistant.engine.ChatMessage
import com.example.aapremote.assistant.engine.Role
import com.example.aapremote.assistant.engine.ToolExecutor
import com.example.aapremote.assistant.engine.ToolRouter
import com.example.aapremote.assistant.llm.OpenAiCompatibleProvider
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.data.TokenManager
import com.example.aapremote.model.McpServerConfig
import com.example.aapremote.network.CertTrustManager
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
    private val json: Json,
    private val localTools: List<LocalTool> = emptyList()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    val activeInstance get() = tokenManager.activeInstance.value

    private var generateJob: Job? = null
    private val _llmConfig = MutableStateFlow<LlmProviderConfig?>(null)
    val llmConfig: StateFlow<LlmProviderConfig?> = _llmConfig.asStateFlow()

    private val _fetchedModels = MutableStateFlow<List<String>>(emptyList())
    val fetchedModels: StateFlow<List<String>> = _fetchedModels.asStateFlow()

    private val _modelFetchState = MutableStateFlow<ModelFetchState>(ModelFetchState.Idle)
    val modelFetchState: StateFlow<ModelFetchState> = _modelFetchState.asStateFlow()

    init {
        viewModelScope.launch {
            _llmConfig.value = repository.loadLlmConfig()
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

        val config = _llmConfig.value
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

        val llmClient = buildLlmClient()
        val provider = when (config) {
            is LlmProviderConfig.OpenAiCompatible ->
                OpenAiCompatibleProvider(config, llmClient, json)
        }

        mcpServerManager.refreshConnections()
        val serverConfigs = tokenManager.activeInstance.value?.mcpServerUrls ?: emptyList()
        val toolRouter = ToolRouter()
        toolRouter.registerLocalTools(localTools)
        toolRouter.registerMcpTools(mcpServerManager.getAllTools())
        val tools = toolRouter.getToolsForQuery(text, serverConfigs)
        val mode = config.tokenSavingMode
        val hasAnyTools = localTools.isNotEmpty() || mcpServerManager.getAllTools().isNotEmpty()
        val noToolMatch = tools.isEmpty() && hasAnyTools

        if (noToolMatch && mode == TokenSavingMode.MINIMAL) {
            val guidanceMsg = ChatMessage(
                role = Role.ASSISTANT,
                content = "I can help you query your AAP instance. Try asking about:\n\n" +
                    "- **Inventory** — hosts, groups, inventories\n" +
                    "- **Jobs** — job templates, workflows, schedules\n" +
                    "- **Users** — users, teams, organizations, roles\n" +
                    "- **Credentials** — credentials, secrets\n" +
                    "- **Monitoring** — system health, instance status\n" +
                    "- **Configuration** — projects, settings, notifications"
            )
            repository.addMessage(guidanceMsg)
            updateState { copy(messages = repository.getHistory(), isGenerating = false) }
            return
        }

        val mcpToolLimit = when (mode) {
            TokenSavingMode.STANDARD -> Int.MAX_VALUE
            TokenSavingMode.TOKEN_SAVER -> 5
            TokenSavingMode.MINIMAL -> 3
        }
        val matchedLocal = tools.filterIsInstance<LocalTool>()
        val matchedMcp = tools.filter { it !is LocalTool }.take(mcpToolLimit)
        val budgetedTools = if (noToolMatch) emptyList() else matchedLocal + matchedMcp
        val toolSpecs = budgetedTools.map { it.spec }
        val toolExecutor = ToolExecutor(budgetedTools)
        val engine = ChatEngine(provider, toolExecutor)
        val maxTokens = if (noToolMatch && mode == TokenSavingMode.TOKEN_SAVER) 256 else null

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            val textBuilder = StringBuilder()
            var hasPlaceholder = false

            fun replaceOrAddAssistant(content: String) {
                updateState {
                    val msg = ChatMessage(role = Role.ASSISTANT, content = content)
                    val msgs = if (hasPlaceholder) messages.dropLast(1) + msg
                        else messages + msg
                    copy(messages = msgs)
                }
                hasPlaceholder = true
            }

            replaceOrAddAssistant("Thinking...")

            engine.processMessage(text, repository.getHistory(), toolSpecs, maxTokens)
                .collect { event ->
                    when (event) {
                        is ChatEvent.TextDelta -> {
                            textBuilder.append(event.text)
                            replaceOrAddAssistant(textBuilder.toString())
                        }
                        is ChatEvent.ToolExecuting -> {
                            replaceOrAddAssistant("Querying: ${event.toolName}...")
                        }
                        is ChatEvent.ToolResult -> {
                            replaceOrAddAssistant("Processing results...")
                            textBuilder.clear()
                        }
                        is ChatEvent.AssistantMessage -> {
                            hasPlaceholder = false
                            updateState {
                                val msgs = messages.dropLast(1)
                                copy(messages = msgs)
                            }
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
                            hasPlaceholder = false
                            updateState {
                                val msgs = messages.dropLast(1)
                                copy(messages = msgs)
                            }
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
        _llmConfig.value = config
        viewModelScope.launch {
            repository.saveLlmConfig(config)
        }
    }

    fun fetchAvailableModels(baseUrl: String, apiKey: String?) {
        viewModelScope.launch {
            _modelFetchState.value = ModelFetchState.Loading
            val fetcher = ModelFetcher(buildLlmClient(), json)
            when (val result = fetcher.fetchModels(baseUrl, apiKey)) {
                is ModelFetcher.Result.Success -> {
                    _fetchedModels.value = result.models
                    _modelFetchState.value = ModelFetchState.Success(result.models.size)
                }
                is ModelFetcher.Result.Error -> {
                    _modelFetchState.value = ModelFetchState.Error(result.message)
                }
            }
        }
    }

    fun clearFetchedModels() {
        _fetchedModels.value = emptyList()
        _modelFetchState.value = ModelFetchState.Idle
    }

    fun toggleMcpEnabled(enabled: Boolean) {
        val instance = tokenManager.activeInstance.value ?: return
        viewModelScope.launch {
            val servers = if (enabled && instance.mcpServerUrls.isNullOrEmpty()) {
                val base = "${instance.baseUrl.trimEnd('/')}:8448"
                listOf(
                    McpServerConfig(url = "$base/mcp", label = "aap", isAutoDetected = true, readOnly = true)
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

    fun toggleServerReadOnly(url: String, readOnly: Boolean) {
        val instance = tokenManager.activeInstance.value ?: return
        viewModelScope.launch {
            val updated = instance.mcpServerUrls?.map {
                if (it.url == url) it.copy(readOnly = readOnly) else it
            }
            tokenManager.updateMcpConfig(instance.id, instance.mcpEnabled, updated)
        }
    }

    private fun buildLlmClient(): OkHttpClient {
        val instance = tokenManager.activeInstance.value
        val builder = httpClient.newBuilder()
        if (instance?.trustSelfSigned == true) {
            val tm = CertTrustManager.createTrustAllManager()
            builder.sslSocketFactory(CertTrustManager.createSslSocketFactory(tm), tm)
            builder.hostnameVerifier { _, _ -> true }
        }
        return builder.build()
    }

    override fun onCleared() {
        super.onCleared()
        generateJob?.cancel()
        mcpServerManager.disconnectAll()
    }

    private inline fun updateState(crossinline transform: AssistantUiState.Active.() -> AssistantUiState.Active) {
        _uiState.update { current ->
            if (current is AssistantUiState.Active) current.transform()
            else current
        }
    }
}

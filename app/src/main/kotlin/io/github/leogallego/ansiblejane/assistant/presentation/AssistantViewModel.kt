package io.github.leogallego.ansiblejane.assistant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.data.ModelFetcher
import io.github.leogallego.ansiblejane.assistant.data.TokenSavingMode
import io.github.leogallego.ansiblejane.assistant.engine.ChatEngine
import io.github.leogallego.ansiblejane.assistant.engine.ChatEvent
import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage
import io.github.leogallego.ansiblejane.assistant.engine.ResponseSource
import io.github.leogallego.ansiblejane.assistant.engine.Role
import io.github.leogallego.ansiblejane.assistant.engine.ToolExecutor
import io.github.leogallego.ansiblejane.assistant.engine.ToolRouter
import io.github.leogallego.ansiblejane.assistant.llm.GeminiLlmProvider
import io.github.leogallego.ansiblejane.assistant.llm.KoogLlmProvider
import io.github.leogallego.ansiblejane.assistant.llm.LlmProvider
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.local.ListToolsLocalTool
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.model.McpServerConfig
import io.github.leogallego.ansiblejane.network.CertTrustManager
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import io.github.leogallego.ansiblejane.assistant.engine.DebugLog as Log

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
    private val repository: IAssistantRepository,
    private val tokenManager: ITokenManager,
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val localTools: List<LocalTool> = emptyList()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AssistantUiState>(AssistantUiState.Idle)
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    val activeInstance get() = tokenManager.activeInstance.value

    private var generateJob: Job? = null
    private var cachedProvider: LlmProvider? = null
    private var cachedProviderKey: String? = null

    private val _llmConfig = MutableStateFlow<LlmProviderConfig?>(null)
    val llmConfig: StateFlow<LlmProviderConfig?> = _llmConfig.asStateFlow()

    private val _fetchedModels = MutableStateFlow<List<String>>(emptyList())
    val fetchedModels: StateFlow<List<String>> = _fetchedModels.asStateFlow()

    private val _modelFetchState = MutableStateFlow<ModelFetchState>(ModelFetchState.Idle)
    val modelFetchState: StateFlow<ModelFetchState> = _modelFetchState.asStateFlow()

    init {
        Log.d(TAG, "INIT: ${localTools.size} local tools: ${localTools.map { it.spec.name }}")

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

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val config = _llmConfig.value
        if (config == null) {
            _uiState.update { current ->
                if (current is AssistantUiState.Active) {
                    current.copy(
                        messages = current.messages + ChatMessage(
                            role = Role.ASSISTANT,
                            content = "Please configure an LLM provider in settings first.",
                            source = ResponseSource.LLM
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
            isGenerating = true
        ) }

        val trustSelfSigned = tokenManager.activeInstance.value?.trustSelfSigned == true
        val provider = getOrCreateProvider(config as LlmProviderConfig.OpenAiCompatible, trustSelfSigned)

        mcpServerManager.refreshConnections()
        val mcpTools = mcpServerManager.getAllTools()
        val serverConfigs = tokenManager.activeInstance.value?.mcpServerUrls ?: emptyList()
        val toolRouter = ToolRouter()
        toolRouter.registerLocalTools(localTools)
        toolRouter.registerMcpTools(mcpTools)
        Log.d(TAG, "ROUTE: query=\"$text\", ${localTools.size} local, ${mcpTools.size} mcp tools available")
        val queryResult = toolRouter.getToolsForQuery(text, serverConfigs)
        val mode = config.tokenSavingMode
        Log.d(TAG, "ROUTE: categoryMatched=${queryResult.categoryMatched}, " +
            "${queryResult.tools.size} tools selected, mode=$mode")

        val noToolsForCategory = queryResult.categoryMatched && queryResult.tools.isEmpty()
        val generalQueryInToolsOnly = !queryResult.categoryMatched && mode == TokenSavingMode.TOOLS_ONLY

        if (noToolsForCategory || generalQueryInToolsOnly) {
            Log.d(TAG, "ROUTE: no tools path — noToolsForCategory=$noToolsForCategory, " +
                "generalQueryInToolsOnly=$generalQueryInToolsOnly")
            val hasMcp = mcpServerManager.getAllTools().isNotEmpty()
            val content = if (generalQueryInToolsOnly) {
                "I can help you query your AAP instance. Try asking about:\n\n" +
                    "- **Inventory** — hosts, groups, inventories\n" +
                    "- **Jobs** — job templates, workflows, schedules\n" +
                    "- **Users** — users, teams, organizations, roles\n" +
                    "- **Credentials** — credentials, secrets\n" +
                    "- **Monitoring** — system health, instance status\n" +
                    "- **Configuration** — projects, settings, notifications"
            } else if (!hasMcp) {
                "I don't have the right tools for that query. This may require an MCP server connection.\n\n" +
                    "I can help with:\n" +
                    "- **Inventory** — hosts, groups, inventories\n" +
                    "- **Jobs** — job templates, workflows, schedules\n" +
                    "- **Monitoring** — system health, instance status\n" +
                    "- **Credentials** — credentials, secrets\n" +
                    "- **Configuration** — projects, execution environments"
            } else {
                "I don't have the right tools for that query. Try asking about:\n\n" +
                    "- **Inventory** — hosts, groups, inventories\n" +
                    "- **Jobs** — job templates, workflows, schedules\n" +
                    "- **Users** — users, teams, organizations, roles\n" +
                    "- **Credentials** — credentials, secrets\n" +
                    "- **Monitoring** — system health, instance status\n" +
                    "- **Configuration** — projects, settings, notifications"
            }
            val guidanceMsg = ChatMessage(role = Role.ASSISTANT, content = content, source = ResponseSource.LLM)
            repository.addMessage(guidanceMsg)
            updateState { copy(messages = repository.getHistory(), isGenerating = false) }
            return
        }

        val mcpLimit = when (mode) {
            TokenSavingMode.STANDARD -> 10
            TokenSavingMode.TOKEN_SAVER -> 5
            TokenSavingMode.TOOLS_ONLY -> 3
        }
        val matchedLocal = queryResult.tools.filterIsInstance<LocalTool>()
        val matchedMcp = queryResult.tools.filter { it !is LocalTool }.take(mcpLimit)
        val budgetedTools = if (matchedLocal.isEmpty() && matchedMcp.isEmpty()) {
            val listTool = ListToolsLocalTool { toolRouter.getAllRegisteredTools() }
            listOf(listTool)
        } else {
            matchedLocal + matchedMcp
        }
        Log.d(TAG, "BUDGET: ${budgetedTools.size} tools [${budgetedTools.map { it.spec.name }}]" +
            if (matchedLocal.isEmpty() && matchedMcp.isEmpty()) " (fallback: list_tools)" else "")
        val toolSpecs = budgetedTools.map { it.spec }
        val toolExecutor = ToolExecutor(budgetedTools)
        val engine = ChatEngine(provider, toolExecutor)
        val maxTokens: Int? = null
        val contextChars = when (mode) {
            TokenSavingMode.STANDARD -> 16_000
            TokenSavingMode.TOKEN_SAVER -> 8_000
            TokenSavingMode.TOOLS_ONLY -> 4_000
        }

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            val textBuilder = StringBuilder()
            val usedSources = mutableSetOf<String>()
            val usedToolNames = mutableListOf<String>()
            val localNames = matchedLocal.map { it.spec.name }.toSet()

            updateState { copy(streamingText = "Thinking...") }

            engine.processMessage(text, repository.getHistory(), toolSpecs, maxTokens, contextChars)
                .collect { event ->
                    when (event) {
                        is ChatEvent.TextDelta -> {
                            textBuilder.append(event.text)
                            updateState { copy(streamingText = "Generating response...") }
                        }
                        is ChatEvent.ToolExecuting -> {
                            val toolSource = if (event.toolName in localNames) "local" else "mcp"
                            usedSources.add(toolSource)
                            usedToolNames.add(event.toolName)
                            updateState { copy(streamingText = "Querying [$toolSource]: ${event.toolName}...") }
                        }
                        is ChatEvent.ToolResult -> {
                            updateState { copy(streamingText = "Processing results...") }
                            textBuilder.clear()
                        }
                        is ChatEvent.AssistantMessage -> {
                            val responseSource = when {
                                usedSources.isEmpty() -> ResponseSource.LLM
                                usedSources.size > 1 -> ResponseSource.MIXED
                                "local" in usedSources -> ResponseSource.LOCAL
                                else -> ResponseSource.MCP
                            }
                            val finalMsg = ChatMessage(
                                role = Role.ASSISTANT,
                                content = event.fullText,
                                source = responseSource,
                                toolsUsed = usedToolNames.distinct()
                            )
                            repository.addMessage(finalMsg)
                            updateState {
                                copy(
                                    messages = repository.getHistory(),
                                    isGenerating = false,
                                    streamingText = null
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
                                    isGenerating = false,
                                    streamingText = null
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
        generateJob?.cancel()
        cachedProvider?.close()
        cachedProvider = null
        cachedProviderKey = null
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

    private fun getOrCreateProvider(
        config: LlmProviderConfig.OpenAiCompatible,
        trustSelfSigned: Boolean
    ): LlmProvider {
        val key = "${config.url}|${config.model}|${config.apiKey}|$trustSelfSigned"
        cachedProvider?.let { if (cachedProviderKey == key) return it }
        cachedProvider?.close()
        val isGemini = config.url.contains("generativelanguage.googleapis.com")
        val provider: LlmProvider = if (isGemini) {
            GeminiLlmProvider(apiKey = config.apiKey ?: "", modelId = config.model)
        } else {
            KoogLlmProvider(config, trustSelfSigned)
        }
        return provider.also {
            cachedProvider = it
            cachedProviderKey = key
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
        cachedProvider?.close()
        cachedProvider = null
        mcpServerManager.disconnectAll()
    }

    private inline fun updateState(crossinline transform: AssistantUiState.Active.() -> AssistantUiState.Active) {
        _uiState.update { current ->
            if (current is AssistantUiState.Active) current.transform()
            else current
        }
    }

    companion object {
        private const val TAG = "AssistantVM"
    }
}

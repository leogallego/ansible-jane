package io.github.leogallego.ansiblejane.assistant.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
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
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import io.github.leogallego.ansiblejane.assistant.engine.DebugLog as Log

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AssistantViewModel(
    private val mcpServerManager: McpServerManager,
    private val repository: IAssistantRepository,
    private val tokenManager: ITokenManager,
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

    init {
        Log.d(TAG, "INIT: ${localTools.size} local tools: ${localTools.map { it.spec.name }}")

        viewModelScope.launch {
            repository.activeConfigFlow.collect { config ->
                val oldKey = cachedProviderKey
                _llmConfig.value = config
                if (config != null && oldKey != null) {
                    val newKey = when (config) {
                        is LlmProviderConfig.OpenAiCompatible ->
                            "${config.url}|${config.model}|${config.apiKey}"
                    }
                    if (newKey != oldKey.substringBeforeLast("|")) {
                        cachedProvider?.close()
                        cachedProvider = null
                        cachedProviderKey = null
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.onHistoryCleared.collect {
                updateState { copy(messages = persistentListOf()) }
            }
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
                                messages = repository.getHistory().toImmutableList(),
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
                        messages = (current.messages + ChatMessage(
                            role = Role.ASSISTANT,
                            content = "Please configure an LLM provider in settings first.",
                            source = ResponseSource.LLM
                        )).toImmutableList()
                    )
                } else current
            }
            return
        }

        val userMessage = ChatMessage(role = Role.USER, content = text)
        repository.addMessage(userMessage)

        updateState { copy(
            messages = repository.getHistory().toImmutableList(),
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
            updateState { copy(messages = repository.getHistory().toImmutableList(), isGenerating = false) }
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

            engine.processMessage(
                text, repository.getHistory(), toolSpecs, maxTokens, contextChars,
                onConfirmationRequired = { toolName, description, _ ->
                    val deferred = CompletableDeferred<Boolean>()
                    val pending = PendingConfirmation(
                        toolName = toolName,
                        description = description,
                        continuation = deferred
                    )
                    updateState { copy(pendingConfirmation = pending) }
                    val result = deferred.await()
                    updateState { copy(pendingConfirmation = null) }
                    result
                }
            ).collect { event ->
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
                        is ChatEvent.ConfirmationRequired -> {
                            updateState { copy(streamingText = "Waiting for confirmation...") }
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
                                    messages = repository.getHistory().toImmutableList(),
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
                                    messages = repository.getHistory().toImmutableList(),
                                    isGenerating = false,
                                    streamingText = null
                                )
                            }
                        }
                    }
                }
        }
    }

    fun confirmAction(approved: Boolean) {
        val current = _uiState.value
        if (current is AssistantUiState.Active) {
            current.pendingConfirmation?.continuation?.complete(approved)
        }
    }

    fun stopGeneration() {
        generateJob?.cancel()
        updateState { copy(isGenerating = false, streamingText = null, pendingConfirmation = null) }
    }

    fun regenerateLastMessage() {
        val history = repository.getHistory()
        val lastUserMsg = history.lastOrNull { it.role == Role.USER } ?: return
        repository.removeLastAssistantMessage()
        updateState { copy(messages = repository.getHistory().toImmutableList()) }
        sendMessage(lastUserMsg.content)
    }

    fun clearHistory() {
        repository.clearHistory()
        updateState { copy(messages = persistentListOf()) }
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

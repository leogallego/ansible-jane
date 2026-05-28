package io.github.leogallego.ansiblejane.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.data.ModelFetcher
import io.github.leogallego.ansiblejane.assistant.presentation.ModelFetchState
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.data.IUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.McpServerConfig
import io.github.leogallego.ansiblejane.network.ApiVersion
import io.github.leogallego.ansiblejane.network.CertTrustManager
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import io.github.leogallego.ansiblejane.network.InstanceDiscovery
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import io.github.leogallego.ansiblejane.ui.components.DateFormatter
import io.github.leogallego.ansiblejane.ui.components.TimeFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.time.ZoneId

class SettingsViewModel(
    private val tokenManager: ITokenManager,
    private val apiProvider: IAapApiProvider,
    private val userPreferences: IUserPreferencesRepository,
    private val assistantRepository: IAssistantRepository,
    private val mcpServerManager: McpServerManager,
    private val instanceDiscovery: InstanceDiscovery,
    private val httpClient: OkHttpClient,
    private val json: Json
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val configs = assistantRepository.loadAllLlmConfigs()
            val activeConfig = assistantRepository.loadLlmConfig()
            val initialActiveKey = assistantRepository.activeProviderKeyFlow.first()

            combine(
                tokenManager.instances,
                tokenManager.activeInstance,
                userPreferences.timezoneId,
                userPreferences.timeFormat,
                mcpServerManager.connections
            ) { instances, active, timezone, timeFormat, connections ->
                DateFormatter.zoneOverride = timezone?.let {
                    try { ZoneId.of(it) } catch (_: Exception) { null }
                }
                DateFormatter.timeFormat = timeFormat

                val current = _uiState.value
                val preservedTab = (current as? SettingsUiState.Ready)?.currentTab ?: SettingsTab.General
                val preservedFetchedModels = (current as? SettingsUiState.Ready)?.fetchedModels ?: emptyList()
                val preservedModelFetchState = (current as? SettingsUiState.Ready)?.modelFetchState ?: ModelFetchState.Idle
                val preservedSavedConfigs = (current as? SettingsUiState.Ready)?.savedConfigs ?: configs
                val preservedActiveConfig = (current as? SettingsUiState.Ready)?.activeConfig ?: activeConfig
                val preservedActiveKey = (current as? SettingsUiState.Ready)?.activeProviderKey ?: initialActiveKey
                val preservedDetails = (current as? SettingsUiState.Ready)?.selectedInstanceForDetails
                val preservedThemeMode = (current as? SettingsUiState.Ready)?.themeMode
                    ?: io.github.leogallego.ansiblejane.ui.components.ThemeMode.SYSTEM

                SettingsUiState.Ready(
                    currentTab = preservedTab,
                    instances = instances,
                    selectedInstance = active,
                    selectedInstanceForDetails = preservedDetails,
                    timezoneId = timezone,
                    timeFormat = timeFormat,
                    themeMode = preservedThemeMode,
                    savedConfigs = preservedSavedConfigs,
                    activeConfig = preservedActiveConfig,
                    activeProviderKey = preservedActiveKey,
                    fetchedModels = preservedFetchedModels,
                    modelFetchState = preservedModelFetchState,
                    mcpEnabled = active?.mcpEnabled ?: false,
                    mcpServers = active?.mcpServerUrls ?: emptyList(),
                    connections = connections
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }

        viewModelScope.launch {
            userPreferences.themeMode.collect { mode ->
                updateReady { copy(themeMode = mode) }
            }
        }

        viewModelScope.launch {
            assistantRepository.activeProviderKeyFlow.collect { key ->
                updateReady { copy(activeProviderKey = key) }
            }
        }

        viewModelScope.launch {
            assistantRepository.savedConfigsFlow.collect { configs ->
                updateReady { copy(savedConfigs = configs) }
            }
        }

        viewModelScope.launch {
            assistantRepository.activeConfigFlow.collect { config ->
                updateReady { copy(activeConfig = config) }
            }
        }
    }

    // --- Tab ---

    fun selectTab(tab: SettingsTab) {
        updateReady { copy(currentTab = tab) }
    }

    // --- Instances ---

    fun switchInstance(instanceId: String) {
        viewModelScope.launch {
            tokenManager.setActiveInstance(instanceId)
        }
    }

    fun removeInstance(instanceId: String) {
        viewModelScope.launch {
            apiProvider.evictInstance(instanceId)
            tokenManager.removeInstance(instanceId)
        }
    }

    fun showInstanceDetails(instanceId: String) {
        updateReady {
            val instance = instances.find { it.id == instanceId }
            copy(selectedInstanceForDetails = instance)
        }
    }

    fun dismissDetails() {
        updateReady { copy(selectedInstanceForDetails = null, instanceEditError = null) }
    }

    fun saveInstanceEdits(
        instanceId: String,
        token: String?,
        alias: String?,
        trustSelfSigned: Boolean
    ) {
        viewModelScope.launch {
            updateReady { copy(instanceEditSaving = true, instanceEditError = null) }
            try {
                val instance = tokenManager.getInstanceById(instanceId)
                if (instance == null) {
                    updateReady {
                        copy(
                            instanceEditSaving = false,
                            instanceEditError = "Instance not found"
                        )
                    }
                    return@launch
                }
                val apiVersion = try {
                    ApiVersion.valueOf(instance.apiVersion)
                } catch (_: Exception) {
                    ApiVersion.CONTROLLER_V2
                }
                tokenManager.saveInstance(
                    baseUrl = instance.baseUrl,
                    token = token ?: instance.token,
                    alias = alias,
                    apiVersion = apiVersion,
                    trustSelfSigned = trustSelfSigned,
                    existingId = instanceId
                )
                if (token != null || trustSelfSigned != instance.trustSelfSigned) {
                    apiProvider.evictInstance(instanceId)
                }
                val updated = tokenManager.instances.value.find { it.id == instanceId }
                updateReady {
                    copy(
                        selectedInstanceForDetails = updated,
                        instanceEditSaving = false
                    )
                }
            } catch (e: Exception) {
                val updated = tokenManager.instances.value.find { it.id == instanceId }
                updateReady {
                    copy(
                        selectedInstanceForDetails = updated,
                        instanceEditSaving = false,
                        instanceEditError = e.message ?: "Failed to save changes"
                    )
                }
            }
        }
    }

    fun refreshInstanceInfo(instanceId: String) {
        val instance = tokenManager.instances.value.find { it.id == instanceId } ?: return
        viewModelScope.launch {
            updateReady { copy(discoveryRefreshing = true, discoveryError = null) }
            try {
                val client = buildDiscoveryClient(instance)
                val apiVersion = try {
                    ApiVersion.valueOf(instance.apiVersion)
                } catch (_: Exception) {
                    ApiVersion.CONTROLLER_V2
                }
                val info = instanceDiscovery.discover(
                    instance.baseUrl, instance.token, apiVersion, client
                )
                tokenManager.updateInstanceInfo(instanceId, info)
                val updated = tokenManager.instances.value.find { it.id == instanceId }
                updateReady { copy(selectedInstanceForDetails = updated) }
            } catch (e: Exception) {
                updateReady { copy(discoveryError = e.message ?: "Discovery failed") }
            } finally {
                updateReady { copy(discoveryRefreshing = false) }
            }
        }
    }

    private fun buildDiscoveryClient(instance: io.github.leogallego.ansiblejane.model.AapInstance): OkHttpClient {
        val builder = httpClient.newBuilder()
        if (instance.trustSelfSigned) {
            val tm = CertTrustManager.createTrustAllManager()
            builder.sslSocketFactory(CertTrustManager.createSslSocketFactory(tm), tm)
            builder.hostnameVerifier { _, _ -> true }
        }
        return builder.build()
    }

    // --- General (Display) ---

    fun setTimezone(zoneId: String?) {
        viewModelScope.launch {
            userPreferences.setTimezoneId(zoneId)
        }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            userPreferences.setTimeFormat(format)
        }
    }

    fun setThemeMode(mode: io.github.leogallego.ansiblejane.ui.components.ThemeMode) {
        viewModelScope.launch {
            userPreferences.setThemeMode(mode)
        }
    }

    // --- Agent (LLM Config) ---

    fun saveProviderConfig(providerKey: String, config: LlmProviderConfig) {
        viewModelScope.launch {
            val current = assistantRepository.loadAllLlmConfigs().toMutableMap()
            current[providerKey] = config
            assistantRepository.saveAllLlmConfigs(current)
        }
    }

    fun switchActiveProvider(providerKey: String) {
        viewModelScope.launch {
            assistantRepository.switchActiveProvider(providerKey)
        }
    }

    fun fetchAvailableModels(baseUrl: String, apiKey: String?) {
        viewModelScope.launch {
            updateReady { copy(modelFetchState = ModelFetchState.Loading) }
            val fetcher = ModelFetcher(buildLlmClient(), json)
            when (val result = fetcher.fetchModels(baseUrl, apiKey)) {
                is ModelFetcher.Result.Success -> {
                    updateReady {
                        copy(
                            fetchedModels = result.models,
                            modelFetchState = ModelFetchState.Success(result.models.size)
                        )
                    }
                }
                is ModelFetcher.Result.Error -> {
                    updateReady { copy(modelFetchState = ModelFetchState.Error(result.message)) }
                }
            }
        }
    }

    fun clearFetchedModels() {
        updateReady { copy(fetchedModels = emptyList(), modelFetchState = ModelFetchState.Idle) }
    }

    // --- Tools (MCP) ---

    fun toggleMcpEnabled(enabled: Boolean) {
        val instance = tokenManager.activeInstance.value ?: return
        viewModelScope.launch {
            if (!enabled) {
                tokenManager.updateMcpConfig(instance.id, false, instance.mcpServerUrls)
                return@launch
            }
            val base = "${instance.baseUrl.trimEnd('/')}:8448"
            val manualServers = instance.mcpServerUrls
                ?.filter { !it.isAutoDetected }
                ?: emptyList()
            val autoDetected = listOf(
                McpServerConfig(
                    url = "$base/job_management/mcp", label = "Jobs",
                    isAutoDetected = true, readOnly = true, toolset = "job_management"
                ),
                McpServerConfig(
                    url = "$base/inventory_management/mcp", label = "Inventory",
                    isAutoDetected = true, readOnly = true, toolset = "inventory_management"
                ),
                McpServerConfig(
                    url = "$base/system_monitoring/mcp", label = "Monitoring",
                    isAutoDetected = true, readOnly = true, toolset = "system_monitoring"
                ),
                McpServerConfig(
                    url = "$base/user_management/mcp", label = "Users",
                    isAutoDetected = true, readOnly = true, toolset = "user_management"
                ),
                McpServerConfig(
                    url = "$base/security_compliance/mcp", label = "Security",
                    isAutoDetected = true, readOnly = true, toolset = "security_compliance"
                ),
                McpServerConfig(
                    url = "$base/platform_configuration/mcp", label = "Configuration",
                    isAutoDetected = true, readOnly = true, toolset = "platform_configuration"
                ),
            )
            val manualUrls = manualServers.map { it.url }.toSet()
            tokenManager.updateMcpConfig(
                instance.id, true,
                manualServers + autoDetected.filter { it.url !in manualUrls }
            )
        }
    }

    fun addMcpServer(url: String, label: String, toolset: String? = null) {
        val instance = tokenManager.activeInstance.value ?: return
        viewModelScope.launch {
            val current = instance.mcpServerUrls?.toMutableList() ?: mutableListOf()
            current.add(McpServerConfig(url = url.trimEnd('/'), label = label, toolset = toolset))
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

    fun toggleServerReadOnly(url: String, readOnly: Boolean) {
        val instance = tokenManager.activeInstance.value ?: return
        viewModelScope.launch {
            val updated = instance.mcpServerUrls?.map {
                if (it.url == url) it.copy(readOnly = readOnly) else it
            }
            tokenManager.updateMcpConfig(instance.id, instance.mcpEnabled, updated)
        }
    }

    // --- Chat History ---

    fun clearHistory() {
        assistantRepository.clearHistory()
    }

    // --- Private helpers ---

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

    private inline fun updateReady(crossinline transform: SettingsUiState.Ready.() -> SettingsUiState.Ready) {
        _uiState.update { current ->
            if (current is SettingsUiState.Ready) current.transform()
            else current
        }
    }
}

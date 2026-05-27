package io.github.leogallego.ansiblejane.presentation.settings

import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.presentation.ModelFetchState
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.model.McpServerConfig
import io.github.leogallego.ansiblejane.network.mcp.McpConnectionState
import io.github.leogallego.ansiblejane.ui.components.ThemeMode
import io.github.leogallego.ansiblejane.ui.components.TimeFormat

enum class SettingsTab(val label: String) {
    General("General"),
    Instances("Instances"),
    Agent("Agent"),
    Tools("Tools")
}

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Ready(
        val currentTab: SettingsTab = SettingsTab.General,
        // Instances
        val instances: List<AapInstance> = emptyList(),
        val selectedInstance: AapInstance? = null,
        val selectedInstanceForDetails: AapInstance? = null,
        val discoveryRefreshing: Boolean = false,
        val discoveryError: String? = null,
        val instanceEditSaving: Boolean = false,
        val instanceEditError: String? = null,
        // General
        val timezoneId: String? = null,
        val timeFormat: TimeFormat = TimeFormat.SYSTEM,
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        // Agent (LLM config)
        val savedConfigs: Map<String, LlmProviderConfig> = emptyMap(),
        val activeConfig: LlmProviderConfig? = null,
        val activeProviderKey: String? = null,
        val fetchedModels: List<String> = emptyList(),
        val modelFetchState: ModelFetchState = ModelFetchState.Idle,
        // Tools (MCP)
        val mcpEnabled: Boolean = false,
        val mcpServers: List<McpServerConfig> = emptyList(),
        val connections: Map<String, McpConnectionState> = emptyMap()
    ) : SettingsUiState
}

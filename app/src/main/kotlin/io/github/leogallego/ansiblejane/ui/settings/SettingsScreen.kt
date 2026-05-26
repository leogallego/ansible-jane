package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leogallego.ansiblejane.presentation.settings.SettingsTab
import io.github.leogallego.ansiblejane.presentation.settings.SettingsUiState
import io.github.leogallego.ansiblejane.presentation.settings.SettingsViewModel
import io.github.leogallego.ansiblejane.ui.components.DetailScaffold
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    onAddInstance: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DetailScaffold(title = "Settings", onNavigateBack = onNavigateBack) {
        when (val state = uiState) {
            is SettingsUiState.Loading -> {
            }
            is SettingsUiState.Ready -> {
                SettingsContent(
                    state = state,
                    onSelectTab = { viewModel.selectTab(it) },
                    onSwitchInstance = { viewModel.switchInstance(it) },
                    onRemoveInstance = { viewModel.removeInstance(it) },
                    onShowDetails = { viewModel.showInstanceDetails(it) },
                    onDismissDetails = { viewModel.dismissDetails() },
                    onRefreshInstanceInfo = { viewModel.refreshInstanceInfo(it) },
                    onAddInstance = onAddInstance,
                    onTimezoneSelected = { viewModel.setTimezone(it) },
                    onTimeFormatSelected = { viewModel.setTimeFormat(it) },
                    onThemeModeSelected = { viewModel.setThemeMode(it) },
                    onClearHistory = { viewModel.clearHistory() },
                    onLogout = onLogout,
                    onSaveProviderConfig = { key, config -> viewModel.saveProviderConfig(key, config) },
                    onSwitchActiveProvider = { viewModel.switchActiveProvider(it) },
                    onFetchModels = { url, key -> viewModel.fetchAvailableModels(url, key) },
                    onClearFetchedModels = { viewModel.clearFetchedModels() },
                    onToggleMcp = { viewModel.toggleMcpEnabled(it) },
                    onAddMcpServer = { url, label, toolset -> viewModel.addMcpServer(url, label, toolset) },
                    onRemoveMcpServer = { viewModel.removeMcpServer(it) },
                    onToggleReadOnly = { url, readOnly -> viewModel.toggleServerReadOnly(url, readOnly) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState.Ready,
    onSelectTab: (SettingsTab) -> Unit,
    onSwitchInstance: (String) -> Unit,
    onRemoveInstance: (String) -> Unit,
    onShowDetails: (String) -> Unit,
    onDismissDetails: () -> Unit,
    onRefreshInstanceInfo: (String) -> Unit,
    onAddInstance: () -> Unit,
    onTimezoneSelected: (String?) -> Unit,
    onTimeFormatSelected: (io.github.leogallego.ansiblejane.ui.components.TimeFormat) -> Unit,
    onThemeModeSelected: (io.github.leogallego.ansiblejane.ui.components.ThemeMode) -> Unit,
    onClearHistory: () -> Unit,
    onLogout: () -> Unit,
    onSaveProviderConfig: (String, io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig) -> Unit,
    onSwitchActiveProvider: (String) -> Unit,
    onFetchModels: (String, String?) -> Unit,
    onClearFetchedModels: () -> Unit,
    onToggleMcp: (Boolean) -> Unit,
    onAddMcpServer: (String, String, String?) -> Unit,
    onRemoveMcpServer: (String) -> Unit,
    onToggleReadOnly: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        SettingsTabSelector(
            currentTab = state.currentTab,
            onSelectTab = onSelectTab
        )

        when (state.currentTab) {
            SettingsTab.General -> GeneralTab(
                timezoneId = state.timezoneId,
                timeFormat = state.timeFormat,
                themeMode = state.themeMode,
                onTimezoneSelected = onTimezoneSelected,
                onTimeFormatSelected = onTimeFormatSelected,
                onThemeModeSelected = onThemeModeSelected,
                modifier = Modifier.weight(1f)
            )
            SettingsTab.Instances -> InstancesTab(
                instances = state.instances,
                selectedInstance = state.selectedInstance,
                selectedInstanceForDetails = state.selectedInstanceForDetails,
                discoveryRefreshing = state.discoveryRefreshing,
                discoveryError = state.discoveryError,
                onSwitchInstance = onSwitchInstance,
                onRemoveInstance = onRemoveInstance,
                onShowDetails = onShowDetails,
                onDismissDetails = onDismissDetails,
                onRefreshInstanceInfo = onRefreshInstanceInfo,
                onAddInstance = onAddInstance,
                onLogout = onLogout,
                modifier = Modifier.weight(1f)
            )
            SettingsTab.Agent -> AgentTab(
                activeProviderKey = state.activeProviderKey,
                activeConfig = state.activeConfig,
                savedConfigs = state.savedConfigs,
                fetchedModels = state.fetchedModels,
                modelFetchState = state.modelFetchState,
                onFetchModels = onFetchModels,
                onClearFetchedModels = onClearFetchedModels,
                onSaveProviderConfig = onSaveProviderConfig,
                onSwitchActiveProvider = onSwitchActiveProvider,
                onClearHistory = onClearHistory,
                modifier = Modifier.weight(1f)
            )
            SettingsTab.Tools -> ToolsTab(
                mcpEnabled = state.mcpEnabled,
                mcpServers = state.mcpServers,
                connections = state.connections,
                onToggleMcp = onToggleMcp,
                onAddMcpServer = onAddMcpServer,
                onRemoveMcpServer = onRemoveMcpServer,
                onToggleReadOnly = onToggleReadOnly,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingsTabSelector(
    currentTab: SettingsTab,
    onSelectTab: (SettingsTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .widthIn(max = 900.dp)
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            SettingsTab.entries.forEach { tab ->
                val isSelected = currentTab == tab
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelectTab(tab) }
                        .testTag("tab_${tab.name.lowercase()}"),
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    } else {
                        Color.Transparent
                    }
                ) {
                    Text(
                        text = tab.label,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.R
import io.github.leogallego.ansiblejane.assistant.tools.ToolSource
import io.github.leogallego.ansiblejane.model.McpServerConfig
import io.github.leogallego.ansiblejane.network.mcp.McpConnectionState
import io.github.leogallego.ansiblejane.presentation.settings.LocalToolUiState
import io.github.leogallego.ansiblejane.presentation.settings.McpToolUiState

@Composable
fun ToolsTab(
    mcpEnabled: Boolean,
    mcpServers: List<McpServerConfig>,
    connections: Map<String, McpConnectionState>,
    mcpServerTools: Map<String, List<McpToolUiState>>,
    localTools: List<LocalToolUiState>,
    expandedMcpServers: Set<String>,
    expandedCategories: Set<String>,
    disabledTools: Set<String>,
    onToggleMcp: (Boolean) -> Unit,
    onAddMcpServer: (url: String, label: String, toolset: String?) -> Unit,
    onRemoveMcpServer: (url: String) -> Unit,
    onToggleReadOnly: (url: String, readOnly: Boolean) -> Unit,
    onToggleToolEnabled: (name: String, source: ToolSource, enabled: Boolean) -> Unit,
    onToggleExpandMcpServer: (label: String) -> Unit,
    onToggleExpandCategory: (category: String) -> Unit,
    onRefreshMcpServer: (label: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddServerSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // MCP Section
        Text(
            text = stringResource(R.string.tools_mcp_servers),
            style = MaterialTheme.typography.titleMedium
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Enable AAP MCP", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Auto-detect at {instance}/{toolset}/mcp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = mcpEnabled,
                onCheckedChange = onToggleMcp,
                modifier = Modifier.testTag("switch_mcp_enabled")
            )
        }

        if (mcpEnabled) {
            mcpServers.forEach { server ->
                McpServerCard(
                    server = server,
                    connectionState = connections[server.label],
                    tools = mcpServerTools[server.label] ?: emptyList(),
                    expanded = server.label in expandedMcpServers,
                    disabledTools = disabledTools,
                    onToggleExpand = { onToggleExpandMcpServer(server.label) },
                    onToggleEnabled = { onToggleMcp(it) },
                    onToggleReadOnly = { onToggleReadOnly(server.url, it) },
                    onToggleTool = { name, enabled ->
                        onToggleToolEnabled(name, ToolSource.MCP, enabled)
                    },
                    onRefresh = { onRefreshMcpServer(server.label) },
                    onRemove = { onRemoveMcpServer(server.url) }
                )
            }

            OutlinedButton(
                onClick = { showAddServerSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    stringResource(R.string.tools_mcp_add_server),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // Local tools section
        LocalToolsSection(
            tools = localTools,
            expandedCategories = expandedCategories,
            onToggleCategory = onToggleExpandCategory,
            onToggleTool = { name, enabled ->
                onToggleToolEnabled(name, ToolSource.LOCAL, enabled)
            }
        )
    }

    if (showAddServerSheet) {
        AddMcpServerSheet(
            onDismiss = { showAddServerSheet = false },
            onAdd = onAddMcpServer
        )
    }
}

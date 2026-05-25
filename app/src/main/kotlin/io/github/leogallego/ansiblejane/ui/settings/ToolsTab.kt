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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.model.McpServerConfig
import io.github.leogallego.ansiblejane.network.mcp.McpConnectionState

@Composable
fun ToolsTab(
    mcpEnabled: Boolean,
    mcpServers: List<McpServerConfig>,
    connections: Map<String, McpConnectionState>,
    onToggleMcp: (Boolean) -> Unit,
    onAddMcpServer: (url: String, label: String, toolset: String?) -> Unit,
    onRemoveMcpServer: (url: String) -> Unit,
    onToggleReadOnly: (url: String, readOnly: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddServer by remember { mutableStateOf(false) }
    var newServerUrl by remember { mutableStateOf("") }
    var newServerLabel by remember { mutableStateOf("") }
    var newServerToolset by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "MCP Servers",
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
                val connState = connections[server.label]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(server.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            server.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (server.toolset != null) {
                            Text(
                                "Toolset: ${server.toolset}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ConnectionStatusIcon(connState)
                        if (!server.isAutoDetected) {
                            IconButton(
                                onClick = { onRemoveMcpServer(server.url) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove ${server.label}",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Read Only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = server.readOnly,
                        onCheckedChange = { onToggleReadOnly(server.url, it) },
                        modifier = Modifier.height(32.dp)
                    )
                }
            }

            if (showAddServer) {
                OutlinedTextField(
                    value = newServerUrl,
                    onValueChange = { newServerUrl = it },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://mcp-server:3000/mcp") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newServerLabel,
                    onValueChange = { newServerLabel = it },
                    label = { Text("Label") },
                    placeholder = { Text("knowledge") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = newServerToolset,
                    onValueChange = { newServerToolset = it },
                    label = { Text("Toolset (optional)") },
                    placeholder = { Text("job_management") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            newServerUrl = ""
                            newServerLabel = ""
                            newServerToolset = ""
                            showAddServer = false
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            onAddMcpServer(
                                newServerUrl, newServerLabel,
                                newServerToolset.ifBlank { null }
                            )
                            newServerUrl = ""
                            newServerLabel = ""
                            newServerToolset = ""
                            showAddServer = false
                        },
                        modifier = Modifier.weight(1f),
                        enabled = newServerUrl.isNotBlank() && newServerLabel.isNotBlank()
                    ) { Text("Add") }
                }
            } else {
                TextButton(onClick = { showAddServer = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Add MCP Server", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }

        HorizontalDivider()

        // Local tools stub
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Local Tools",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Per-tool enable/disable coming soon. " +
                        "Manage which local AAP tools are available to the agent.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusIcon(state: McpConnectionState?) {
    when (state) {
        is McpConnectionState.Connected -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Connected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text("${state.toolCount} tools", style = MaterialTheme.typography.bodySmall)
            }
        }
        is McpConnectionState.Error -> {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
        is McpConnectionState.Connecting -> {
            Text(
                "Connecting...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        else -> {
            Text(
                "Not connected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

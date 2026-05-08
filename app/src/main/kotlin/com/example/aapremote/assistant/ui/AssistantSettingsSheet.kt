package com.example.aapremote.assistant.ui

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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.model.McpServerConfig
import com.example.aapremote.network.mcp.McpConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSettingsSheet(
    mcpEnabled: Boolean,
    mcpServers: List<McpServerConfig>,
    connections: Map<String, McpConnectionState>,
    currentLlmConfig: LlmProviderConfig?,
    onToggleMcp: (Boolean) -> Unit,
    onAddMcpServer: (url: String, label: String) -> Unit,
    onRemoveMcpServer: (url: String) -> Unit,
    onSaveLlmConfig: (LlmProviderConfig) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val savedConfig = currentLlmConfig as? LlmProviderConfig.OpenAiCompatible
    var llmUrl by remember { mutableStateOf(savedConfig?.url ?: "") }
    var llmModel by remember { mutableStateOf(savedConfig?.model ?: "") }
    var llmApiKey by remember { mutableStateOf(savedConfig?.apiKey ?: "") }

    var apiKeyVisible by remember { mutableStateOf(false) }
    var showAddServer by remember { mutableStateOf(false) }
    var newServerUrl by remember { mutableStateOf("") }
    var newServerLabel by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Assistant Settings",
                style = MaterialTheme.typography.headlineSmall
            )

            HorizontalDivider()

            // --- MCP Servers Section ---
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
                    onCheckedChange = onToggleMcp
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddServer = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel") }
                        Button(
                            onClick = {
                                onAddMcpServer(newServerUrl, newServerLabel)
                                newServerUrl = ""
                                newServerLabel = ""
                                showAddServer = false
                            },
                            modifier = Modifier.weight(1f),
                            enabled = newServerUrl.isNotBlank() && newServerLabel.isNotBlank()
                        ) { Text("Add") }
                    }
                } else {
                    TextButton(
                        onClick = { showAddServer = true }
                    ) {
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

            // --- LLM Provider Section ---
            Text(
                text = "LLM Provider",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = llmUrl,
                onValueChange = { llmUrl = it },
                label = { Text("API URL") },
                placeholder = { Text("http://localhost:11434/v1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = llmModel,
                onValueChange = { llmModel = it },
                label = { Text("Model") },
                placeholder = { Text("llama3.1:8b") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = llmApiKey,
                onValueChange = { llmApiKey = it },
                label = { Text("API Key (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (apiKeyVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                        Icon(
                            if (apiKeyVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                            contentDescription = if (apiKeyVisible) "Hide API key" else "Show API key"
                        )
                    }
                }
            )

            Button(
                onClick = {
                    val config = LlmProviderConfig.OpenAiCompatible(
                        url = llmUrl.trimEnd('/'),
                        model = llmModel,
                        apiKey = llmApiKey.ifBlank { null }
                    )
                    onSaveLlmConfig(config)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = llmUrl.isNotBlank() && llmModel.isNotBlank()
            ) {
                Text("Save LLM Config")
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear Chat History")
            }

            Spacer(modifier = Modifier.height(24.dp))
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

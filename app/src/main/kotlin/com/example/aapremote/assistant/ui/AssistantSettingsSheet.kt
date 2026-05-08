package com.example.aapremote.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.network.mcp.McpConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSettingsSheet(
    connections: Map<String, McpConnectionState>,
    onSaveLlmConfig: (LlmProviderConfig) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var llmUrl by remember { mutableStateOf("") }
    var llmModel by remember { mutableStateOf("") }
    var llmApiKey by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Assistant Settings",
                style = MaterialTheme.typography.headlineSmall
            )

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
                visualTransformation = PasswordVisualTransformation()
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

            Spacer(modifier = Modifier.height(8.dp))

            if (connections.isNotEmpty()) {
                Text(
                    text = "MCP Connections",
                    style = MaterialTheme.typography.titleMedium
                )

                connections.forEach { (label, connState) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        when (connState) {
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
                                    Text(
                                        "${connState.toolCount} tools",
                                        style = MaterialTheme.typography.bodySmall
                                    )
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
                            is McpConnectionState.Disconnected -> {
                                Text(
                                    "Disconnected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

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

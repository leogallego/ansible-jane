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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aapremote.assistant.data.KnownProvider
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.assistant.data.TokenSavingMode
import com.example.aapremote.assistant.presentation.ModelFetchState
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
    onToggleReadOnly: (url: String, readOnly: Boolean) -> Unit,
    fetchedModels: List<String>,
    modelFetchState: ModelFetchState,
    onFetchModels: (url: String, apiKey: String?) -> Unit,
    onClearFetchedModels: () -> Unit,
    onSaveLlmConfig: (LlmProviderConfig) -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val savedConfig = currentLlmConfig as? LlmProviderConfig.OpenAiCompatible
    val initialProvider = remember {
        if (savedConfig != null) KnownProvider.fromUrl(savedConfig.url) else KnownProvider.OPENROUTER
    }

    var selectedProvider by remember { mutableStateOf(initialProvider) }
    var llmUrl by remember { mutableStateOf(savedConfig?.url ?: selectedProvider.baseUrl) }
    var llmModel by remember { mutableStateOf(savedConfig?.model ?: "") }
    var llmApiKey by remember { mutableStateOf(savedConfig?.apiKey ?: "") }

    var providerState by remember {
        mutableStateOf<Map<KnownProvider, Pair<String, String>>>(
            if (savedConfig != null) mapOf(initialProvider to ((savedConfig.model ?: "") to (savedConfig.apiKey ?: "")))
            else emptyMap()
        )
    }

    var tokenMode by remember { mutableStateOf(savedConfig?.tokenSavingMode ?: TokenSavingMode.STANDARD) }

    var apiKeyVisible by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var tokenModeExpanded by remember { mutableStateOf(false) }
    var showAddServer by remember { mutableStateOf(false) }
    var newServerUrl by remember { mutableStateOf("") }
    var newServerLabel by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .semantics { testTagsAsResourceId = true }
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

            ExposedDropdownMenuBox(
                expanded = providerExpanded,
                onExpandedChange = { providerExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedProvider.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Provider") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_provider")
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = providerExpanded,
                    onDismissRequest = { providerExpanded = false }
                ) {
                    KnownProvider.entries.forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.displayName) },
                            onClick = {
                                if (selectedProvider != provider) {
                                    providerState = providerState + (selectedProvider to (llmModel to llmApiKey))
                                    selectedProvider = provider
                                    llmUrl = provider.baseUrl
                                    val restored = providerState[provider]
                                    llmModel = restored?.first ?: ""
                                    llmApiKey = restored?.second ?: ""
                                    onClearFetchedModels()
                                }
                                providerExpanded = false
                            }
                        )
                    }
                }
            }

            if (selectedProvider.urlEditable) {
                OutlinedTextField(
                    value = llmUrl,
                    onValueChange = { llmUrl = it },
                    label = { Text("API URL") },
                    placeholder = { Text(selectedProvider.baseUrl.ifEmpty { "https://your-api.com/v1" }) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = if (selectedProvider == KnownProvider.OLLAMA) {
                        { Text("Use 10.0.2.2 instead of localhost for emulator") }
                    } else null
                )
            }

            if (selectedProvider != KnownProvider.CUSTOM) {
                val allModels = remember(selectedProvider, fetchedModels) {
                    (selectedProvider.defaultModels + fetchedModels).distinct()
                }
                val filteredModels = remember(allModels, llmModel) {
                    if (llmModel.isBlank()) allModels
                    else allModels.filter { it.contains(llmModel, ignoreCase = true) }
                }

                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it }
                ) {
                    OutlinedTextField(
                        value = llmModel,
                        onValueChange = {
                            llmModel = it
                            modelExpanded = true
                        },
                        label = { Text("Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_model")
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                        singleLine = true
                    )
                    if (filteredModels.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = modelExpanded,
                            onDismissRequest = { modelExpanded = false }
                        ) {
                            filteredModels.take(20).forEach { modelId ->
                                DropdownMenuItem(
                                    text = {
                                        Text(modelId, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    },
                                    onClick = {
                                        llmModel = modelId
                                        modelExpanded = false
                                    }
                                )
                            }
                            if (filteredModels.size > 20) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "${filteredModels.size - 20} more — type to filter",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    },
                                    onClick = {},
                                    enabled = false
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (val state = modelFetchState) {
                        is ModelFetchState.Loading -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp))
                                Text("Fetching...", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        is ModelFetchState.Success -> {
                            Text(
                                "${state.count} models available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        is ModelFetchState.Error -> {
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    TextButton(
                        onClick = {
                            val url = if (selectedProvider.urlEditable) llmUrl
                                else selectedProvider.baseUrl
                            onFetchModels(url, llmApiKey.ifBlank { null })
                        },
                        enabled = modelFetchState !is ModelFetchState.Loading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Refresh", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            } else {
                OutlinedTextField(
                    value = llmModel,
                    onValueChange = { llmModel = it },
                    label = { Text("Model") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (selectedProvider.requiresApiKey) {
                OutlinedTextField(
                    value = llmApiKey,
                    onValueChange = { llmApiKey = it },
                    label = { Text("API Key${if (selectedProvider == KnownProvider.CUSTOM) " (optional)" else ""}") },
                    modifier = Modifier.fillMaxWidth().testTag("field_api_key"),
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
            }

            HorizontalDivider()

            // --- Token Saving Mode ---
            Text(
                text = "Token Usage",
                style = MaterialTheme.typography.titleMedium
            )

            ExposedDropdownMenuBox(
                expanded = tokenModeExpanded,
                onExpandedChange = { tokenModeExpanded = it }
            ) {
                OutlinedTextField(
                    value = tokenMode.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mode") },
                    supportingText = { Text(tokenMode.description) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(tokenModeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = tokenModeExpanded,
                    onDismissRequest = { tokenModeExpanded = false }
                ) {
                    TokenSavingMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(mode.displayName)
                                    Text(
                                        mode.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                tokenMode = mode
                                tokenModeExpanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val effectiveUrl = if (selectedProvider.urlEditable) llmUrl
                        else selectedProvider.baseUrl
                    val config = LlmProviderConfig.OpenAiCompatible(
                        url = effectiveUrl.trimEnd('/'),
                        model = llmModel,
                        apiKey = llmApiKey.ifBlank { null },
                        tokenSavingMode = tokenMode
                    )
                    onSaveLlmConfig(config)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().testTag("button_save_llm"),
                enabled = (llmUrl.isNotBlank() || !selectedProvider.urlEditable) && llmModel.isNotBlank()
            ) {
                Text("Save LLM Config")
            }

            HorizontalDivider()

            OutlinedButton(
                onClick = onClearHistory,
                modifier = Modifier.fillMaxWidth().testTag("button_clear_history")
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

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.assistant.data.KnownProvider
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.data.TokenSavingMode
import io.github.leogallego.ansiblejane.assistant.presentation.ModelFetchState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentTab(
    activeConfig: LlmProviderConfig?,
    savedConfigs: Map<String, LlmProviderConfig>,
    fetchedModels: List<String>,
    modelFetchState: ModelFetchState,
    onFetchModels: (url: String, apiKey: String?) -> Unit,
    onClearFetchedModels: () -> Unit,
    onSaveLlmConfig: (LlmProviderConfig) -> Unit,
    onSaveAllConfigs: (Map<String, LlmProviderConfig>) -> Unit,
    modifier: Modifier = Modifier
) {
    val savedConfig = activeConfig as? LlmProviderConfig.OpenAiCompatible
    val initialProvider = remember {
        if (savedConfig != null) KnownProvider.fromUrl(savedConfig.url) else KnownProvider.OPENROUTER
    }

    var selectedProvider by remember { mutableStateOf(initialProvider) }
    var llmUrl by remember { mutableStateOf(savedConfig?.url ?: selectedProvider.baseUrl) }
    var llmModel by remember { mutableStateOf(savedConfig?.model ?: "") }
    var llmApiKey by remember { mutableStateOf(savedConfig?.apiKey ?: "") }

    var providerState by remember {
        val initial = mutableMapOf<KnownProvider, Pair<String, String>>()
        for ((key, config) in savedConfigs) {
            val provider = try { KnownProvider.valueOf(key) } catch (_: Exception) { continue }
            if (config is LlmProviderConfig.OpenAiCompatible) {
                initial[provider] = config.model to (config.apiKey ?: "")
            }
        }
        if (savedConfig != null && initialProvider !in initial) {
            initial[initialProvider] = savedConfig.model to (savedConfig.apiKey ?: "")
        }
        mutableStateOf<Map<KnownProvider, Pair<String, String>>>(initial)
    }

    var tokenMode by remember { mutableStateOf(savedConfig?.tokenSavingMode ?: TokenSavingMode.STANDARD) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var tokenModeExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "LLM Provider",
            style = MaterialTheme.typography.titleMedium
        )

        // Provider selector
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
                    val providerConfig = savedConfigs[provider.name] as? LlmProviderConfig.OpenAiCompatible
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(provider.displayName)
                                if (providerConfig != null) {
                                    Text(
                                        providerConfig.model,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            if (selectedProvider != provider) {
                                providerState = providerState + (selectedProvider to (llmModel to llmApiKey))

                                val effectiveUrl = if (selectedProvider.urlEditable) llmUrl
                                else selectedProvider.baseUrl
                                val currentConfig = LlmProviderConfig.OpenAiCompatible(
                                    url = effectiveUrl.trimEnd('/'),
                                    model = llmModel,
                                    apiKey = llmApiKey.ifBlank { null },
                                    tokenSavingMode = tokenMode
                                )
                                onSaveAllConfigs(savedConfigs + (selectedProvider.name to currentConfig))

                                selectedProvider = provider
                                val restoredConfig = savedConfigs[provider.name] as? LlmProviderConfig.OpenAiCompatible
                                llmUrl = restoredConfig?.url ?: provider.baseUrl
                                val restored = providerState[provider]
                                llmModel = restored?.first ?: restoredConfig?.model ?: ""
                                llmApiKey = restored?.second ?: restoredConfig?.apiKey ?: ""
                                tokenMode = restoredConfig?.tokenSavingMode ?: TokenSavingMode.STANDARD
                                onClearFetchedModels()
                            }
                            providerExpanded = false
                        }
                    )
                }
            }
        }

        // URL field (if editable)
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

        // Model selector
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

        // API Key
        if (selectedProvider.requiresApiKey) {
            OutlinedTextField(
                value = llmApiKey,
                onValueChange = { llmApiKey = it },
                label = { Text("API Key${if (selectedProvider == KnownProvider.CUSTOM) " (optional)" else ""}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_api_key"),
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

        // Token Saving Mode
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
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("button_save_llm"),
            enabled = (llmUrl.isNotBlank() || !selectedProvider.urlEditable) && llmModel.isNotBlank()
        ) {
            Text("Save LLM Config")
        }

        HorizontalDivider()

        // Persona stub
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
                    text = "Persona",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Persona configuration coming soon. Customize Jane's behavior, " +
                        "system prompts, and response style.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

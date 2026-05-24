package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.assistant.data.KnownProvider
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.data.TokenSavingMode
import io.github.leogallego.ansiblejane.assistant.presentation.ModelFetchState

@Composable
fun AgentTab(
    activeProviderKey: String?,
    activeConfig: LlmProviderConfig?,
    savedConfigs: Map<String, LlmProviderConfig>,
    fetchedModels: List<String>,
    modelFetchState: ModelFetchState,
    onFetchModels: (url: String, apiKey: String?) -> Unit,
    onClearFetchedModels: () -> Unit,
    onSaveProviderConfig: (providerKey: String, LlmProviderConfig) -> Unit,
    onSwitchActiveProvider: (String) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedProvider by remember { mutableStateOf<KnownProvider?>(null) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "LLM Provider",
            style = MaterialTheme.typography.titleMedium
        )

        val sortedProviders = remember(activeProviderKey, savedConfigs) {
            KnownProvider.entries.sortedWith(compareByDescending<KnownProvider> {
                it.name == activeProviderKey
            }.thenByDescending {
                val cfg = savedConfigs[it.name] as? LlmProviderConfig.OpenAiCompatible
                cfg != null && cfg.model.isNotBlank()
            })
        }

        sortedProviders.forEach { provider ->
            val providerConfig = savedConfigs[provider.name] as? LlmProviderConfig.OpenAiCompatible
            val isActive = activeProviderKey == provider.name
            val isConfigured = providerConfig != null && providerConfig.model.isNotBlank()
            val isExpanded = expandedProvider == provider

            ProviderCard(
                provider = provider,
                config = providerConfig,
                isActive = isActive,
                isConfigured = isConfigured,
                isExpanded = isExpanded,
                fetchedModels = if (isExpanded) fetchedModels else emptyList(),
                modelFetchState = if (isExpanded) modelFetchState else ModelFetchState.Idle,
                onToggleExpand = {
                    if (isExpanded) {
                        expandedProvider = null
                    } else {
                        expandedProvider = provider
                        onClearFetchedModels()
                    }
                },
                onFetchModels = onFetchModels,
                onSave = { config ->
                    onSaveProviderConfig(provider.name, config)
                },
                onSetActive = {
                    expandedProvider = null
                    onSwitchActiveProvider(provider.name)
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        OutlinedButton(
            onClick = { showClearHistoryConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("button_clear_history")
        ) {
            Text("Clear Chat History")
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear Chat History") },
            text = {
                Text("This will remove all messages from the assistant chat. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryConfirm = false
                        onClearHistory()
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderCard(
    provider: KnownProvider,
    config: LlmProviderConfig.OpenAiCompatible?,
    isActive: Boolean,
    isConfigured: Boolean,
    isExpanded: Boolean,
    fetchedModels: List<String>,
    modelFetchState: ModelFetchState,
    onToggleExpand: () -> Unit,
    onFetchModels: (String, String?) -> Unit,
    onSave: (LlmProviderConfig) -> Unit,
    onSetActive: () -> Unit
) {
    val border = if (isActive) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    } else null

    val dotColor = when {
        isActive -> Color(0xFF4CAF50)
        isConfigured -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_provider_${provider.name}"),
        border = border
    ) {
        Column {
            // Collapsed header — always visible
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = provider.displayName,
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (isConfigured && config?.tokenSavingMode != null &&
                            config.tokenSavingMode != TokenSavingMode.STANDARD
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.padding(top = 1.dp)
                            ) {
                                Text(
                                    text = config.tokenSavingMode.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (isConfigured) config?.model ?: "" else "Not configured",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ProviderConfigFields(
                    provider = provider,
                    config = config,
                    isActive = isActive,
                    isConfigured = isConfigured,
                    fetchedModels = fetchedModels,
                    modelFetchState = modelFetchState,
                    onFetchModels = onFetchModels,
                    onSave = onSave,
                    onSetActive = onSetActive
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderConfigFields(
    provider: KnownProvider,
    config: LlmProviderConfig.OpenAiCompatible?,
    isActive: Boolean,
    isConfigured: Boolean,
    fetchedModels: List<String>,
    modelFetchState: ModelFetchState,
    onFetchModels: (String, String?) -> Unit,
    onSave: (LlmProviderConfig) -> Unit,
    onSetActive: () -> Unit
) {
    var url by remember(provider) { mutableStateOf(config?.url ?: provider.baseUrl) }
    var model by remember(provider) { mutableStateOf(config?.model ?: "") }
    var apiKey by remember(provider) { mutableStateOf(config?.apiKey ?: "") }
    var tokenMode by remember(provider) { mutableStateOf(config?.tokenSavingMode ?: TokenSavingMode.STANDARD) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalDivider()

        // URL field
        if (provider.urlEditable) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("API URL") },
                placeholder = { Text(provider.baseUrl.ifEmpty { "https://your-api.com/v1" }) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = if (provider == KnownProvider.OLLAMA) {
                    { Text("Use 10.0.2.2 instead of localhost for emulator") }
                } else null
            )
        }

        // Model selector
        if (provider != KnownProvider.CUSTOM) {
            val allModels = remember(provider, fetchedModels) {
                (provider.defaultModels + fetchedModels).distinct()
            }
            val filteredModels = remember(allModels, model) {
                if (model.isBlank()) allModels
                else allModels.filter { it.contains(model, ignoreCase = true) }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = modelExpanded,
                    onExpandedChange = { modelExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = {
                            model = it
                            modelExpanded = true
                        },
                        label = { Text("Model") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_model_${provider.name}")
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
                                        model = modelId
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
                IconButton(
                    onClick = {
                        val effectiveUrl = if (provider.urlEditable) url else provider.baseUrl
                        onFetchModels(effectiveUrl, apiKey.ifBlank { null })
                    },
                    enabled = modelFetchState !is ModelFetchState.Loading
                ) {
                    if (modelFetchState is ModelFetchState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Fetch models")
                    }
                }
            }
            when (val state = modelFetchState) {
                is ModelFetchState.Success -> Text(
                    "${state.count} models available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is ModelFetchState.Error -> Text(
                    state.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                else -> {}
            }
        } else {
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Model") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        // API Key
        if (provider.requiresApiKey) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key${if (provider == KnownProvider.CUSTOM) " (optional)" else ""}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_api_key_${provider.name}"),
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

        // Token saving mode — segmented buttons
        Text(
            text = "Token Usage",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            TokenSavingMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = tokenMode == mode,
                    onClick = { tokenMode = mode },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = TokenSavingMode.entries.size
                    )
                ) {
                    Text(mode.displayName, style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Save + Active toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    val effectiveUrl = if (provider.urlEditable) url else provider.baseUrl
                    val newConfig = LlmProviderConfig.OpenAiCompatible(
                        url = effectiveUrl.trimEnd('/'),
                        model = model,
                        apiKey = apiKey.ifBlank { null },
                        tokenSavingMode = tokenMode
                    )
                    onSave(newConfig)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("button_save_${provider.name}"),
                enabled = (url.isNotBlank() || !provider.urlEditable) && model.isNotBlank()
            ) {
                Text("Save")
            }
            if (isConfigured) {
                Switch(
                    checked = isActive,
                    onCheckedChange = { if (!isActive) onSetActive() },
                    modifier = Modifier.testTag("switch_active_${provider.name}")
                )
            }
        }
    }
}

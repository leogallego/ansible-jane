package io.github.leogallego.ansiblejane.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.leogallego.ansiblejane.assistant.data.KnownProvider
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.data.TokenSavingMode
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

@Composable
fun ProviderSwitchChip(
    activeProviderKey: String?,
    activeConfig: LlmProviderConfig?,
    savedConfigs: Map<String, LlmProviderConfig>,
    onSwitchProvider: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val configuredProviders = savedConfigs.filter { (_, config) ->
        config is LlmProviderConfig.OpenAiCompatible && config.model.isNotBlank()
    }
    val isInteractive = configuredProviders.size > 1

    val activeProvider = activeProviderKey?.let {
        try { KnownProvider.valueOf(it) } catch (_: IllegalArgumentException) { KnownProvider.CUSTOM }
    }

    val chipLabel = when {
        activeConfig == null -> "No LLM"
        activeProvider != null -> activeProvider.displayName
        else -> "LLM"
    }

    val chipColor = if (activeConfig != null)
        MaterialTheme.colorScheme.surfaceVariant
    else
        MaterialTheme.colorScheme.errorContainer

    val textColor = if (activeConfig != null)
        MaterialTheme.colorScheme.onSurfaceVariant
    else
        MaterialTheme.colorScheme.onErrorContainer

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .widthIn(max = 140.dp)
                .clip(RoundedCornerShape(50))
                .background(chipColor)
                .clickable {
                    if (isInteractive) expanded = true
                    else onNavigateToSettings()
                }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("chip_provider"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = chipLabel,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (isInteractive) {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = textColor
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(16.dp)
        ) {
            configuredProviders.forEach { (key, config) ->
                val isActive = key == activeProviderKey
                val provider = try {
                    KnownProvider.valueOf(key)
                } catch (_: IllegalArgumentException) {
                    KnownProvider.CUSTOM
                }
                val model = (config as? LlmProviderConfig.OpenAiCompatible)?.model ?: ""

                DropdownMenuItem(
                    modifier = Modifier
                        .testTag("menu_provider_$key")
                        .then(
                            if (isActive) Modifier
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                            else Modifier.padding(horizontal = 4.dp)
                        ),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.widthIn(max = 240.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isActive)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = model,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isActive)
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        if (!isActive) {
                            onSwitchProvider(key)
                        }
                    }
                )
            }
        }
    }
}

private val previewConfig = LlmProviderConfig.OpenAiCompatible(
    url = "http://localhost:11434/v1",
    model = "llama3.1:8b",
    tokenSavingMode = TokenSavingMode.STANDARD
)

@PreviewLightDark
@Composable
private fun ProviderSwitchChipActivePreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        ProviderSwitchChip(
            activeProviderKey = KnownProvider.OLLAMA.name,
            activeConfig = previewConfig,
            savedConfigs = mapOf(KnownProvider.OLLAMA.name to previewConfig),
            onSwitchProvider = {},
            onNavigateToSettings = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun ProviderSwitchChipNoLlmPreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        ProviderSwitchChip(
            activeProviderKey = null,
            activeConfig = null,
            savedConfigs = emptyMap(),
            onSwitchProvider = {},
            onNavigateToSettings = {}
        )
    }
}

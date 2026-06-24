package io.github.leogallego.ansiblejane.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.leogallego.ansiblejane.model.JobTemplate
import io.github.leogallego.ansiblejane.model.JobTemplateSummaryFields
import io.github.leogallego.ansiblejane.model.Label
import io.github.leogallego.ansiblejane.model.LabelSummary
import io.github.leogallego.ansiblejane.model.LaunchableTemplate
import io.github.leogallego.ansiblejane.model.UserCapabilities
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TemplateCard(
    template: LaunchableTemplate,
    onClick: () -> Unit,
    onLaunch: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    testTagPrefix: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptic = LocalHapticFeedback.current
    ElevatedCard(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pressScale(interactionSource)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (template.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = template.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (template.labels.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        template.labels.forEach { label ->
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text = label.name,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                border = AssistChipDefaults.assistChipBorder(enabled = true)
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (onToggleFavorite != null) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("button_favorite_${template.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (isFavorite) stringResource(Res.string.cd_remove_from_favorites) else stringResource(Res.string.cd_add_to_favorites),
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                if (template.canStart) {
                    val launchTag = if (testTagPrefix.isNotEmpty()) {
                        Modifier.testTag("${testTagPrefix}_launch_${template.id}")
                    } else {
                        Modifier
                    }
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLaunch()
                        },
                        modifier = Modifier.size(48.dp).then(launchTag)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = stringResource(Res.string.cd_launch_template, template.name),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

private val previewTemplate = JobTemplate(
    id = 1,
    name = "Deploy Web Application",
    description = "Deploys the web application to production servers with rolling updates",
    summaryFields = JobTemplateSummaryFields(
        labels = LabelSummary(
            count = 2,
            results = listOf(Label(1, "production"), Label(2, "deploy"))
        ),
        userCapabilities = UserCapabilities(start = true)
    )
)

@PreviewLightDark
@Composable
private fun TemplateCardLaunchablePreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        TemplateCard(
            template = previewTemplate,
            onClick = {},
            onLaunch = {}
        )
    }
}

@PreviewLightDark
@Composable
private fun TemplateCardReadOnlyPreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        TemplateCard(
            template = previewTemplate.copy(
                name = "System Health Check",
                description = "Runs health checks across all managed hosts",
                summaryFields = JobTemplateSummaryFields()
            ),
            onClick = {},
            onLaunch = {}
        )
    }
}

package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.R
import io.github.leogallego.ansiblejane.presentation.settings.LocalToolUiState

private val CATEGORY_ORDER = listOf(
    "JOBS", "INVENTORY", "MONITORING", "USERS",
    "SECURITY", "CONFIGURATION", "EDA", "PLATFORM"
)

@Composable
private fun categoryDisplayName(category: String): String = when (category) {
    "JOBS" -> stringResource(R.string.tools_category_jobs)
    "INVENTORY" -> stringResource(R.string.tools_category_inventory)
    "MONITORING" -> stringResource(R.string.tools_category_monitoring)
    "USERS" -> stringResource(R.string.tools_category_users)
    "SECURITY" -> stringResource(R.string.tools_category_security)
    "CONFIGURATION" -> stringResource(R.string.tools_category_configuration)
    "EDA" -> stringResource(R.string.tools_category_eda)
    "PLATFORM" -> stringResource(R.string.tools_category_platform)
    else -> category
}

@Composable
fun LocalToolsSection(
    tools: List<LocalToolUiState>,
    expandedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    onToggleTool: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = remember(tools) { tools.groupBy { it.category } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.tools_local_title),
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text = stringResource(R.string.tools_local_description, tools.size, grouped.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        CATEGORY_ORDER.forEach { category ->
            val categoryTools = grouped[category] ?: return@forEach
            val isExpanded = category in expandedCategories
            val displayName = categoryDisplayName(category)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleCategory(category) }
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .testTag("section_category_$category"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "${categoryTools.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse $displayName" else "Expand $displayName",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        categoryTools.forEach { tool ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tool.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = tool.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Switch(
                                    checked = tool.isEnabled,
                                    onCheckedChange = { onToggleTool(tool.name, it) },
                                    modifier = Modifier.testTag("switch_local_tool_${tool.name}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

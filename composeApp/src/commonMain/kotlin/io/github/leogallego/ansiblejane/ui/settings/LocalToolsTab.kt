package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.presentation.settings.LocalToolUiState

@Composable
fun LocalToolsTab(
    tools: List<LocalToolUiState>,
    expandedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    onToggleTool: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LocalToolsSection(
        tools = tools,
        expandedCategories = expandedCategories,
        onToggleCategory = onToggleCategory,
        onToggleTool = onToggleTool,
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    )
}

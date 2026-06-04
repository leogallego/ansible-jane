package io.github.leogallego.ansiblejane.presentation.settings

data class LocalToolUiState(
    val name: String,
    val description: String,
    val category: String,
    val isEnabled: Boolean
)

data class McpToolUiState(
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val inputSchema: String? = null
)

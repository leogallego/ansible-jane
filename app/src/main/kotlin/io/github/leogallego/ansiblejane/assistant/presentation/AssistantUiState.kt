package io.github.leogallego.ansiblejane.assistant.presentation

import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.network.mcp.McpConnectionState

sealed interface AssistantUiState {
    data object Idle : AssistantUiState
    data object Loading : AssistantUiState
    data class Active(
        val messages: List<ChatMessage> = emptyList(),
        val isGenerating: Boolean = false,
        val streamingText: String? = null,
        val connections: Map<String, McpConnectionState> = emptyMap()
    ) : AssistantUiState
    data class Error(val error: AppError) : AssistantUiState
}

sealed interface ModelFetchState {
    data object Idle : ModelFetchState
    data object Loading : ModelFetchState
    data class Success(val count: Int) : ModelFetchState
    data class Error(val message: String) : ModelFetchState
}

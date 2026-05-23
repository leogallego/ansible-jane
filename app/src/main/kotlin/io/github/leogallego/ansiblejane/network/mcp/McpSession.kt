package io.github.leogallego.ansiblejane.network.mcp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface McpConnectionState {
    data object Disconnected : McpConnectionState
    data object Connecting : McpConnectionState
    data class Connected(
        val serverInfo: McpServerInfo,
        val toolCount: Int
    ) : McpConnectionState
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : McpConnectionState
}

class McpSession {
    private val _state = MutableStateFlow<McpConnectionState>(McpConnectionState.Disconnected)
    val state: StateFlow<McpConnectionState> = _state.asStateFlow()

    private val _tools = MutableStateFlow<List<McpToolDefinition>>(emptyList())
    val tools: StateFlow<List<McpToolDefinition>> = _tools.asStateFlow()

    var sessionId: String? = null
        private set

    fun updateState(newState: McpConnectionState) {
        _state.update { newState }
    }

    fun updateTools(newTools: List<McpToolDefinition>) {
        _tools.update { newTools }
    }

    fun updateSessionId(id: String?) {
        sessionId = id
    }

    fun reset() {
        _state.update { McpConnectionState.Disconnected }
        _tools.update { emptyList() }
        sessionId = null
    }
}

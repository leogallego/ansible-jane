package com.example.aapremote.network.mcp

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        _state.value = newState
    }

    fun updateTools(newTools: List<McpToolDefinition>) {
        _tools.value = newTools
    }

    fun updateSessionId(id: String?) {
        sessionId = id
    }

    fun reset() {
        _state.value = McpConnectionState.Disconnected
        _tools.value = emptyList()
        sessionId = null
    }
}

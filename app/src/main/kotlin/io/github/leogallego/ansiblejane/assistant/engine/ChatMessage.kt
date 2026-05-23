package io.github.leogallego.ansiblejane.assistant.engine

import androidx.compose.runtime.Immutable
import java.util.concurrent.atomic.AtomicLong

enum class Role { USER, ASSISTANT, TOOL, SYSTEM }

enum class ResponseSource { LOCAL, MCP, LLM, MIXED }

@Immutable
data class ChatMessage(
    val role: Role,
    val content: String,
    val toolCallsJson: String? = null,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val source: ResponseSource? = null,
    val toolsUsed: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val id: Long = nextId()
) {
    companion object {
        private val counter = AtomicLong(0)
        fun nextId(): Long = counter.getAndIncrement()
    }
}

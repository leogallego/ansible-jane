package io.github.leogallego.ansiblejane.assistant.engine

import androidx.compose.runtime.Immutable
import java.util.concurrent.atomic.AtomicLong

enum class Role { USER, ASSISTANT, TOOL, SYSTEM }

enum class ResponseSource { LOCAL, MCP, LLM, MIXED }

@Immutable
data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val isEstimated: Boolean = false
) {
    fun formatTotal(): String {
        val prefix = if (isEstimated) "~" else ""
        return if (totalTokens >= 1000) {
            val k = totalTokens / 1000
            val remainder = (totalTokens % 1000) / 100
            if (remainder > 0) "$prefix${k}.${remainder}K" else "$prefix${k}K"
        } else {
            "$prefix$totalTokens"
        }
    }
}

@Immutable
data class ChatMessage(
    val role: Role,
    val content: String,
    val toolCallsJson: String? = null,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val source: ResponseSource? = null,
    val toolsUsed: List<String> = emptyList(),
    val tokenUsage: TokenUsage? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val id: Long = nextId()
) {
    companion object {
        private val counter = AtomicLong(0)
        fun nextId(): Long = counter.getAndIncrement()
    }
}

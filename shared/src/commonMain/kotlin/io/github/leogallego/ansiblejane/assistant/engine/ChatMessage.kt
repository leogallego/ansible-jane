package io.github.leogallego.ansiblejane.assistant.engine

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi

enum class Role { USER, ASSISTANT, TOOL, SYSTEM }

enum class ResponseSource { LOCAL, MCP, LLM, MIXED }

data class TokenUsage(
    val inputTokens: Int,
    val outputTokens: Int,
    val totalTokens: Int,
    val isEstimated: Boolean = false
) {
    fun formatTotal(): String = formatTokenCount(totalTokens, isEstimated)

    companion object {
        fun formatTokenCount(count: Int, isEstimated: Boolean = false): String {
            val prefix = if (isEstimated) "~" else ""
            return if (count >= 1000) {
                val k = count / 1000
                val remainder = (count % 1000) / 100
                if (remainder > 0) "$prefix${k}.${remainder}K" else "$prefix${k}K"
            } else {
                "$prefix$count"
            }
        }
    }
}

@OptIn(ExperimentalAtomicApi::class)
private val messageCounter = AtomicLong(0)

data class ChatMessage(
    val role: Role,
    val content: String,
    val toolCallsJson: String? = null,
    val toolCallId: String? = null,
    val toolName: String? = null,
    val source: ResponseSource? = null,
    val toolsUsed: List<String> = emptyList(),
    val tokenUsage: TokenUsage? = null,
    val timestamp: Long = 0L,
    val id: Long = nextId()
) {
    companion object {
        @OptIn(ExperimentalAtomicApi::class)
        fun nextId(): Long = messageCounter.fetchAndAdd(1)
    }
}

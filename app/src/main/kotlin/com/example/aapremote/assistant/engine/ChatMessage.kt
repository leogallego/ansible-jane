package com.example.aapremote.assistant.engine

import java.util.concurrent.atomic.AtomicLong

enum class Role { USER, ASSISTANT, TOOL, SYSTEM }

data class ChatMessage(
    val role: Role,
    val content: String,
    val toolCallsJson: String? = null,
    val toolCallId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val id: Long = nextId.getAndIncrement()
) {
    companion object {
        private val nextId = AtomicLong(0)
    }
}

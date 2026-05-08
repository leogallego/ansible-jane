package com.example.aapremote.assistant.engine

import com.example.aapremote.assistant.llm.ToolCall

enum class Role { USER, ASSISTANT, TOOL, SYSTEM }

data class ChatMessage(
    val role: Role,
    val content: String,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

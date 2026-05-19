package com.example.aapremote.assistant.llm

import kotlinx.serialization.json.JsonObject

sealed interface StreamEvent {
    data class TextDelta(val text: String) : StreamEvent
    data class ToolCallStart(val id: String, val name: String) : StreamEvent
    data class ToolCallArgs(val id: String, val argsDelta: String) : StreamEvent
    data class Done(val result: LlmResult) : StreamEvent
    data class Error(val cause: Throwable) : StreamEvent
}

data class LlmResult(
    val text: String? = null,
    val toolCalls: List<ToolCall> = emptyList()
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: JsonObject
)

data class ModelInfo(
    val name: String,
    val isLocal: Boolean = false
)

class LlmAuthException(message: String) : Exception(message)
class LlmRateLimitException(message: String, val retryAfter: Int? = null) : Exception(message)
class LlmServerException(message: String) : Exception(message)
class LlmTimeoutException(message: String) : Exception(message)

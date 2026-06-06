package io.github.leogallego.ansiblejane.assistant.llm

data class ModelInfo(
    val name: String,
    val isLocal: Boolean = false
)

class LlmAuthException(message: String) : Exception(message)
class LlmRateLimitException(message: String, val retryAfter: Int? = null) : Exception(message)
class LlmServerException(message: String) : Exception(message)
class LlmTimeoutException(message: String) : Exception(message)

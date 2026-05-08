package com.example.aapremote.assistant.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class TokenSavingMode {
    @SerialName("standard")
    STANDARD,
    @SerialName("token_saver")
    TOKEN_SAVER,
    @SerialName("minimal")
    MINIMAL;

    val displayName: String
        get() = when (this) {
            STANDARD -> "Standard"
            TOKEN_SAVER -> "Token Saver"
            MINIMAL -> "Minimal"
        }

    val description: String
        get() = when (this) {
            STANDARD -> "Full conversation with LLM, tools when relevant"
            TOKEN_SAVER -> "Short replies for general chat, tools when relevant"
            MINIMAL -> "Tools only — no LLM call for general chat"
        }
}

@Serializable
sealed interface LlmProviderConfig {
    val tokenSavingMode: TokenSavingMode

    @Serializable
    @SerialName("openai_compatible")
    data class OpenAiCompatible(
        val url: String,
        val model: String,
        val apiKey: String? = null,
        override val tokenSavingMode: TokenSavingMode = TokenSavingMode.STANDARD
    ) : LlmProviderConfig
}

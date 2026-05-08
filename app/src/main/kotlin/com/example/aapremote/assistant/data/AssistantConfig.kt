package com.example.aapremote.assistant.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface LlmProviderConfig {
    @Serializable
    @SerialName("openai_compatible")
    data class OpenAiCompatible(
        val url: String,
        val model: String,
        val apiKey: String? = null
    ) : LlmProviderConfig
}

package io.github.leogallego.ansiblejane.assistant.llm

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.http.client.KoogHttpClientException
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.LLMClientException
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider as KoogLLMProvider
import ai.koog.prompt.streaming.StreamFrame
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.net.SocketTimeoutException

class GeminiLlmProvider(
    apiKey: String,
    modelId: String
) : LlmProvider {

    private val client = GoogleLLMClient(
        apiKey = apiKey,
        httpClientFactory = KtorKoogHttpClient.Factory()
    )

    private val model = LLModel(
        provider = KoogLLMProvider.Google,
        id = modelId,
        capabilities = listOf(
            LLMCapability.Completion,
            LLMCapability.Tools,
            LLMCapability.Temperature
        )
    )

    override fun generateStream(
        prompt: Prompt,
        tools: List<ToolDescriptor>,
        maxTokens: Int?
    ): Flow<StreamFrame> = flow {
        client.executeStreaming(prompt, model, tools).collect { frame ->
            emit(frame)
        }
    }.catch { e ->
        throw mapException(e)
    }

    override fun isAvailable(): Boolean = true

    override fun modelInfo(): ModelInfo = ModelInfo(
        name = model.id,
        isLocal = false
    )

    override fun close() {
        client.close()
    }

    internal fun mapException(e: Throwable): Throwable = when (e) {
        is LlmAuthException, is LlmRateLimitException,
        is LlmServerException, is LlmTimeoutException -> e
        is LLMClientException -> {
            val cause = e.cause
            if (cause != null) mapException(cause) else LlmServerException("LLM error: ${e.message}")
        }
        is KoogHttpClientException -> {
            val code = e.statusCode
            when {
                code == 401 || code == 403 -> LlmAuthException("Authentication failed — check your Google API key")
                code == 429 -> LlmRateLimitException("Rate limited — try again later")
                code != null && code >= 500 -> LlmServerException("Gemini server error ($code): ${e.message}")
                code != null -> LlmServerException("Gemini error ($code): ${e.message}")
                else -> LlmServerException("Gemini error: ${e.message}")
            }
        }
        is ClientRequestException -> {
            val code = e.response.status.value
            when (code) {
                401, 403 -> LlmAuthException("Authentication failed — check your Google API key")
                429 -> LlmRateLimitException("Rate limited — try again later")
                else -> LlmServerException("Gemini error ($code): ${e.message}")
            }
        }
        is ServerResponseException -> LlmServerException("Gemini server error: ${e.message}")
        is SocketTimeoutException -> LlmTimeoutException("Request timed out")
        else -> e
    }
}

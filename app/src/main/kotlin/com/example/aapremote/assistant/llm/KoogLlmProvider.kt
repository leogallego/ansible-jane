package com.example.aapremote.assistant.llm

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider as KoogLLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import ai.koog.http.client.KoogHttpClientException
import ai.koog.prompt.executor.clients.LLMClientException
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.network.CertTrustManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.io.Closeable
import java.net.SocketTimeoutException

class KoogLlmProvider(
    private val config: LlmProviderConfig.OpenAiCompatible,
    trustSelfSigned: Boolean = false
) : LlmProvider, Closeable {

    private val client: OpenAILLMClient = run {
        val parsed = java.net.URI(config.url.trimEnd('/'))
        val baseUrl = "${parsed.scheme}://${parsed.host}" +
            if (parsed.port > 0) ":${parsed.port}" else ""
        val pathPrefix = parsed.path.trimStart('/').trimEnd('/')
        val chatPath = if (pathPrefix.isNotEmpty()) "$pathPrefix/chat/completions"
            else "v1/chat/completions"
        val settings = OpenAIClientSettings(
            baseUrl = baseUrl,
            chatCompletionsPath = chatPath
        )
        val httpClient = if (trustSelfSigned) {
            HttpClient(CIO) {
                engine {
                    https {
                        trustManager = CertTrustManager.createTrustAllManager()
                    }
                }
            }
        } else {
            HttpClient(CIO)
        }
        OpenAILLMClient(
            apiKey = config.apiKey ?: "",
            settings = settings,
            baseClient = httpClient
        )
    }

    private val model = LLModel(
        provider = KoogLLMProvider.OpenAI,
        id = config.model,
        capabilities = listOf(
            LLMCapability.Completion,
            LLMCapability.Tools,
            LLMCapability.Temperature,
            LLMCapability.OpenAIEndpoint.Completions
        )
    )

    override suspend fun generate(
        prompt: Prompt,
        tools: List<ToolDescriptor>,
        maxTokens: Int?
    ): List<Message.Response> = try {
        client.execute(prompt, model, tools)
    } catch (e: Exception) {
        throw mapException(e)
    }

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

    override fun isAvailable(): Boolean =
        config.url.isNotBlank() && config.model.isNotBlank()

    override fun modelInfo(): ModelInfo = ModelInfo(
        name = config.model,
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
                code == 401 || code == 403 -> LlmAuthException("Authentication failed: ${e.message}")
                code == 429 -> LlmRateLimitException("Rate limited: ${e.message}")
                code != null && code >= 500 -> LlmServerException("Server error ($code): ${e.message}")
                code != null -> LlmServerException("Client error ($code): ${e.message}")
                else -> LlmServerException("HTTP error: ${e.message}")
            }
        }
        is ClientRequestException -> {
            val code = e.response.status.value
            when (code) {
                401, 403 -> LlmAuthException("Authentication failed: ${e.message}")
                429 -> LlmRateLimitException("Rate limited: ${e.message}")
                else -> LlmServerException("Client error ($code): ${e.message}")
            }
        }
        is ServerResponseException -> LlmServerException("Server error: ${e.message}")
        is SocketTimeoutException -> LlmTimeoutException("Request timed out")
        else -> e
    }
}

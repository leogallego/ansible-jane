package io.github.leogallego.ansiblejane.assistant.llm

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.http.client.KoogHttpClientException
import ai.koog.http.client.KoogHttpClient
import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.LLMClientException
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIFunction
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIMessage
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIToolCall
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIToolChoice
import ai.koog.prompt.executor.clients.openai.base.models.OpenAITool
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider as KoogLLMProvider
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.streaming.StreamFrame
import io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig
import io.github.leogallego.ansiblejane.assistant.engine.DebugLog as Log
import io.github.leogallego.ansiblejane.network.createPlatformHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.io.Closeable
import java.net.SocketTimeoutException

private class FixedOpenAILLMClient(
    apiKey: String,
    settings: OpenAIClientSettings,
    httpClientFactory: KoogHttpClient.Factory
) : OpenAILLMClient(apiKey = apiKey, settings = settings, httpClientFactory = httpClientFactory) {

    override fun serializeProviderChatRequest(
        messages: List<OpenAIMessage>,
        model: LLModel,
        tools: List<OpenAITool>?,
        toolChoice: OpenAIToolChoice?,
        params: LLMParams,
        stream: Boolean
    ): String {
        // Koog bug: convertPromptToMessages does Json.encodeToString(it.args)
        // which double-encodes the tool call arguments string.
        // Fix: unwrap the extra quoting before serialization.
        val fixed = messages.map { msg ->
            if (msg is OpenAIMessage.Assistant && msg.toolCalls != null) {
                OpenAIMessage.Assistant(
                    content = msg.content,
                    reasoningContent = msg.reasoningContent,
                    toolCalls = msg.toolCalls!!.map { tc ->
                        OpenAIToolCall(
                            id = tc.id,
                            function = OpenAIFunction(
                                name = tc.function.name,
                                arguments = unwrapDoubleEncoding(tc.function.arguments)
                            )
                        )
                    }
                )
            } else msg
        }
        return super.serializeProviderChatRequest(fixed, model, tools, toolChoice, params, stream)
    }

    private fun unwrapDoubleEncoding(args: String): String {
        if (!args.startsWith("\"")) return args
        return try {
            Json.decodeFromString<String>(args)
        } catch (_: Exception) {
            args
        }
    }
}

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
        val factory = if (trustSelfSigned) {
            KtorKoogHttpClient.Factory(baseClient = createPlatformHttpClient(trustSelfSigned = true))
        } else {
            KtorKoogHttpClient.Factory()
        }
        FixedOpenAILLMClient(
            apiKey = config.apiKey ?: "",
            settings = settings,
            httpClientFactory = factory
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

    override fun generateStream(
        prompt: Prompt,
        tools: List<ToolDescriptor>,
        maxTokens: Int?
    ): Flow<StreamFrame> = flow {
        Log.d(TAG, "Request: model=${config.model}, tools=${tools.size}, messages=${prompt.messages.size}")
        val startTime = System.currentTimeMillis()
        var frameCount = 0
        client.executeStreaming(prompt, model, tools).collect { frame ->
            frameCount++
            emit(frame)
        }
        Log.d(TAG, "Complete: ${frameCount} frames in ${System.currentTimeMillis() - startTime}ms")
    }.catch { e ->
        Log.d(TAG, "Error: ${e::class.simpleName}: ${e.message}")
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

    companion object {
        private const val TAG = "KoogLlmProvider"
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

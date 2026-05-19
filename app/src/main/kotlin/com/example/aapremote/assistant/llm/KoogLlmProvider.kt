package com.example.aapremote.assistant.llm

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.LLMProvider as KoogLLMProvider
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.assistant.engine.ChatMessage
import com.example.aapremote.assistant.engine.Role
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.network.CertTrustManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.SocketTimeoutException

class KoogLlmProvider(
    private val config: LlmProviderConfig.OpenAiCompatible,
    trustSelfSigned: Boolean = false
) : LlmProvider {

    private val json = Json { ignoreUnknownKeys = true }

    private val client: OpenAILLMClient = run {
        val baseUrl = config.url.trimEnd('/')
        val settings = OpenAIClientSettings(
            baseUrl = baseUrl,
            chatCompletionsPath = "chat/completions"
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
            LLMCapability.Temperature
        )
    )

    override suspend fun generate(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        maxTokens: Int?
    ): LlmResult {
        val prompt = buildPrompt(messages)
        val toolDescriptors = tools.map { it.toToolDescriptor() }
        return try {
            val responses = client.execute(prompt, model, toolDescriptors)
            mapResponsesToResult(responses)
        } catch (e: Exception) {
            throw mapException(e)
        }
    }

    override fun generateStream(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        maxTokens: Int?
    ): Flow<StreamEvent> = flow {
        val prompt = buildPrompt(messages)
        val toolDescriptors = tools.map { it.toToolDescriptor() }
        val toolCalls = mutableMapOf<String, MutableToolCall>()
        val textAccumulator = StringBuilder()

        client.executeStreaming(prompt, model, toolDescriptors).collect { frame ->
            when (frame) {
                is StreamFrame.TextDelta -> {
                    textAccumulator.append(frame.text)
                    emit(StreamEvent.TextDelta(frame.text))
                }
                is StreamFrame.ToolCallDelta -> {
                    val id = frame.id ?: return@collect
                    val tc = toolCalls.getOrPut(id) { MutableToolCall(id) }
                    if (frame.name != null) {
                        tc.name = frame.name
                        emit(StreamEvent.ToolCallStart(id, frame.name!!))
                    }
                    if (frame.content != null) {
                        tc.args.append(frame.content)
                        emit(StreamEvent.ToolCallArgs(id, frame.content!!))
                    }
                }
                is StreamFrame.ToolCallComplete -> {
                    val id = frame.id ?: return@collect
                    val tc = toolCalls.getOrPut(id) { MutableToolCall(id) }
                    tc.name = frame.name
                    tc.args.clear()
                    tc.args.append(frame.content)
                    if (!tc.startEmitted) {
                        emit(StreamEvent.ToolCallStart(id, frame.name))
                        tc.startEmitted = true
                    }
                }
                is StreamFrame.End -> {
                    val completedCalls = toolCalls.values
                        .filter { it.name != null }
                        .map { tc ->
                            val argsJson = try {
                                json.parseToJsonElement(tc.args.toString()).jsonObject
                            } catch (_: Exception) {
                                JsonObject(emptyMap())
                            }
                            ToolCall(id = tc.id, name = tc.name!!, arguments = argsJson)
                        }
                    val result = LlmResult(
                        text = textAccumulator.toString().ifEmpty { null },
                        toolCalls = completedCalls
                    )
                    emit(StreamEvent.Done(result))
                }
                else -> { /* Ignore reasoning frames */ }
            }
        }
    }.catch { e ->
        emit(StreamEvent.Error(mapException(e)))
    }

    override fun isAvailable(): Boolean =
        config.url.isNotBlank() && config.model.isNotBlank()

    override fun modelInfo(): ModelInfo = ModelInfo(
        name = config.model,
        isLocal = false
    )

    private fun buildPrompt(messages: List<ChatMessage>): Prompt {
        val koogMessages = messages.flatMap { msg ->
            when (msg.role) {
                Role.SYSTEM -> listOf(Message.System(
                    content = msg.content,
                    metaInfo = RequestMetaInfo.Empty
                ))
                Role.USER -> listOf(Message.User(
                    content = msg.content,
                    metaInfo = RequestMetaInfo.Empty
                ))
                Role.ASSISTANT -> {
                    if (!msg.toolCalls.isNullOrEmpty()) {
                        msg.toolCalls.map { tc ->
                            Message.Tool.Call(
                                id = tc.id,
                                tool = tc.name,
                                content = tc.arguments.toString(),
                                metaInfo = ResponseMetaInfo.Empty
                            )
                        }
                    } else {
                        listOf(Message.Assistant(
                            content = msg.content,
                            metaInfo = ResponseMetaInfo.Empty
                        ))
                    }
                }
                Role.TOOL -> listOf(Message.Tool.Result(
                    id = msg.toolCallId,
                    tool = msg.toolCallId ?: "",
                    content = msg.content,
                    metaInfo = RequestMetaInfo.Empty
                ))
            }
        }
        return Prompt(messages = koogMessages, id = "chat")
    }

    private fun mapResponsesToResult(responses: List<Message.Response>): LlmResult {
        var text: String? = null
        val toolCalls = mutableListOf<ToolCall>()

        responses.forEach { response ->
            when (response) {
                is Message.Assistant -> text = response.content.ifEmpty { null }
                is Message.Tool.Call -> {
                    val argsJson = try {
                        json.parseToJsonElement(response.content).jsonObject
                    } catch (_: Exception) {
                        JsonObject(emptyMap())
                    }
                    toolCalls.add(
                        ToolCall(
                            id = response.id ?: "call_${toolCalls.size}",
                            name = response.tool,
                            arguments = argsJson
                        )
                    )
                }
                else -> { /* Ignore reasoning */ }
            }
        }
        return LlmResult(text = text, toolCalls = toolCalls)
    }

    private fun mapException(e: Throwable): Throwable = when (e) {
        is LlmAuthException, is LlmRateLimitException,
        is LlmServerException, is LlmTimeoutException -> e
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

    private class MutableToolCall(val id: String) {
        var name: String? = null
        val args = StringBuilder()
        var startEmitted = false
    }
}

private fun ToolSpec.toToolDescriptor(): ToolDescriptor {
    val schema = compactSchema(parametersSchema)
    val properties = schema["properties"]?.jsonObject ?: emptyMap()
    val requiredNames = schema["required"]?.jsonArray
        ?.map { it.jsonPrimitive.content }?.toSet() ?: emptySet()

    val required = mutableListOf<ToolParameterDescriptor>()
    val optional = mutableListOf<ToolParameterDescriptor>()

    properties.forEach { (paramName, paramValue) ->
        val prop = paramValue.jsonObject
        val descriptor = ToolParameterDescriptor(
            name = paramName,
            description = paramName,
            type = parseParamType(prop)
        )
        if (paramName in requiredNames) required.add(descriptor) else optional.add(descriptor)
    }

    return ToolDescriptor(
        name = name,
        description = description,
        requiredParameters = required,
        optionalParameters = optional
    )
}

private fun parseParamType(prop: JsonObject): ToolParameterType {
    val enumValues = prop["enum"]?.jsonArray
    if (enumValues != null) {
        return ToolParameterType.Enum(enumValues.map { it.jsonPrimitive.content }.toTypedArray())
    }
    return when (prop["type"]?.jsonPrimitive?.content) {
        "integer" -> ToolParameterType.Integer
        "number" -> ToolParameterType.Float
        "boolean" -> ToolParameterType.Boolean
        "array" -> ToolParameterType.List(ToolParameterType.String)
        else -> ToolParameterType.String
    }
}

private fun compactSchema(schema: JsonObject): JsonObject {
    val builder = mutableMapOf<String, JsonElement>()
    schema["type"]?.let { builder["type"] = it }
    schema["required"]?.jsonArray?.let { arr ->
        if (arr.isNotEmpty()) builder["required"] = arr
    }
    schema["properties"]?.jsonObject?.let { props ->
        val compactedProps = mutableMapOf<String, JsonElement>()
        props.forEach { (key, value) ->
            val prop = value.jsonObject
            val compacted = mutableMapOf<String, JsonElement>()
            prop["type"]?.let { compacted["type"] = it }
            prop["enum"]?.jsonArray?.let { enumArr ->
                if (enumArr.size > 8) {
                    compacted["enum"] = JsonArray(enumArr.take(8))
                } else {
                    compacted["enum"] = enumArr
                }
            }
            compactedProps[key] = JsonObject(compacted)
        }
        builder["properties"] = JsonObject(compactedProps)
    }
    return JsonObject(builder)
}

package com.example.aapremote.assistant.llm

import com.example.aapremote.assistant.data.LlmProviderConfig
import com.example.aapremote.assistant.engine.ChatMessage
import com.example.aapremote.assistant.engine.Role
import com.example.aapremote.assistant.tools.ToolSpec
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LlmAuthException(message: String) : Exception(message)
class LlmRateLimitException(message: String, val retryAfter: Int? = null) : Exception(message)
class LlmServerException(message: String) : Exception(message)
class LlmTimeoutException(message: String) : Exception(message)

class OpenAiCompatibleProvider(
    private val config: LlmProviderConfig.OpenAiCompatible,
    private val httpClient: OkHttpClient,
    private val json: Json
) : LlmProvider {

    private val sseClient = httpClient.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()

    override suspend fun generate(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>
    ): LlmResult {
        val body = buildRequestBody(messages, tools, stream = false)
        val request = buildHttpRequest(body)
        val response = executeRequest(request)

        return response.use { resp ->
            checkHttpErrors(resp)
            val responseBody = resp.body?.string()
                ?: throw IOException("Empty response body")
            parseNonStreamingResponse(responseBody)
        }
    }

    override fun generateStream(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>
    ): Flow<StreamEvent> = callbackFlow {
        val body = buildRequestBody(messages, tools, stream = true)
        val request = buildHttpRequest(body)

        val toolCallAccumulators = mutableMapOf<Int, ToolCallAccumulator>()
        val textAccumulator = StringBuilder()

        val eventSource = EventSources.createFactory(sseClient)
            .newEventSource(request, object : EventSourceListener() {
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    if (data.trim() == "[DONE]") {
                        val toolCalls = toolCallAccumulators.values
                            .filter { it.name != null }
                            .map { acc ->
                                val argsJson = try {
                                    json.parseToJsonElement(acc.argsBuilder.toString()).jsonObject
                                } catch (_: Exception) {
                                    JsonObject(emptyMap())
                                }
                                ToolCall(
                                    id = acc.id,
                                    name = acc.name!!,
                                    arguments = argsJson
                                )
                            }
                        val result = LlmResult(
                            text = textAccumulator.toString().ifEmpty { null },
                            toolCalls = toolCalls
                        )
                        trySend(StreamEvent.Done(result))
                        close()
                        return
                    }

                    try {
                        val chunk = json.parseToJsonElement(data).jsonObject
                        val choices = chunk["choices"]?.jsonArray ?: return
                        if (choices.isEmpty()) return

                        val delta = choices[0].jsonObject["delta"]?.jsonObject ?: return

                        val content = delta["content"]?.jsonPrimitive?.contentOrNull
                        if (content != null) {
                            textAccumulator.append(content)
                            trySend(StreamEvent.TextDelta(content))
                        }

                        val toolCallsArr = delta["tool_calls"]?.jsonArray
                        toolCallsArr?.forEach { tcElement ->
                            val tc = tcElement.jsonObject
                            val index = tc["index"]?.jsonPrimitive?.intOrNull ?: 0
                            val function = tc["function"]?.jsonObject

                            val acc = toolCallAccumulators.getOrPut(index) {
                                ToolCallAccumulator(
                                    id = tc["id"]?.jsonPrimitive?.contentOrNull ?: "call_$index"
                                )
                            }

                            val name = function?.get("name")?.jsonPrimitive?.contentOrNull
                            if (name != null) {
                                acc.name = name
                                trySend(StreamEvent.ToolCallStart(acc.id, name))
                            }

                            val args = function?.get("arguments")?.jsonPrimitive?.contentOrNull
                            if (args != null) {
                                acc.argsBuilder.append(args)
                                trySend(StreamEvent.ToolCallArgs(acc.id, args))
                            }
                        }
                    } catch (_: Exception) {
                        // Skip malformed chunks
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    val error = when {
                        response?.code == 401 || response?.code == 403 ->
                            LlmAuthException("LLM authentication failed — check API key")
                        response?.code == 429 ->
                            LlmRateLimitException("Rate limited", response.header("Retry-After")?.toIntOrNull())
                        response != null && response.code >= 500 ->
                            LlmServerException("LLM server error: ${response.code}")
                        t is SocketTimeoutException ->
                            LlmTimeoutException("Response timed out")
                        t != null -> t
                        else -> IOException("SSE stream failed: ${response?.code}")
                    }
                    trySend(StreamEvent.Error(error))
                    close()
                }

                override fun onClosed(eventSource: EventSource) {
                    close()
                }
            })

        awaitClose { eventSource.cancel() }
    }

    override fun isAvailable(): Boolean =
        config.url.isNotBlank() && config.model.isNotBlank()

    override fun modelInfo(): ModelInfo = ModelInfo(
        name = config.model,
        isLocal = false
    )

    private fun buildRequestBody(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
        stream: Boolean
    ): String {
        val body = buildJsonObject {
            put("model", config.model)
            put("stream", stream)
            put("messages", buildMessagesArray(messages))
            if (tools.isNotEmpty()) {
                put("tools", buildToolsArray(tools))
            }
        }
        return json.encodeToString(JsonObject.serializer(), body)
    }

    private fun buildMessagesArray(messages: List<ChatMessage>): JsonArray = buildJsonArray {
        messages.forEach { msg ->
            add(buildJsonObject {
                put("role", msg.role.toOpenAiRole())
                put("content", msg.content)
                if (msg.toolCallId != null) {
                    put("tool_call_id", msg.toolCallId)
                }
                if (msg.toolCalls != null && msg.toolCalls.isNotEmpty()) {
                    put("tool_calls", buildJsonArray {
                        msg.toolCalls.forEach { tc ->
                            add(buildJsonObject {
                                put("id", tc.id)
                                put("type", "function")
                                put("function", buildJsonObject {
                                    put("name", tc.name)
                                    put("arguments", json.encodeToString(JsonObject.serializer(), tc.arguments))
                                })
                            })
                        }
                    })
                }
            })
        }
    }

    private fun buildToolsArray(tools: List<ToolSpec>): JsonArray = buildJsonArray {
        tools.forEach { tool ->
            add(tool.toOpenAiTool())
        }
    }

    private fun buildHttpRequest(body: String): Request {
        val url = "${config.url.trimEnd('/')}/chat/completions"
        return Request.Builder()
            .url(url)
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .apply {
                config.apiKey?.let { header("Authorization", "Bearer $it") }
            }
            .build()
    }

    private suspend fun executeRequest(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = httpClient.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : okhttp3.Callback {
                override fun onResponse(call: okhttp3.Call, response: Response) {
                    cont.resume(response)
                }
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    cont.resumeWithException(e)
                }
            })
        }

    private fun checkHttpErrors(response: Response) {
        if (response.code in 200..299) return

        val errorDetail = try {
            val body = response.peekBody(4096).string()
            val errorObj = json.parseToJsonElement(body).jsonObject["error"]
            when (errorObj) {
                is JsonPrimitive -> errorObj.content
                is JsonObject -> errorObj["message"]?.jsonPrimitive?.contentOrNull
                else -> null
            }
        } catch (_: Exception) { null }

        when (response.code) {
            401, 403 -> throw LlmAuthException(
                errorDetail ?: "Authentication failed — check API key"
            )
            429 -> throw LlmRateLimitException(
                errorDetail ?: "Rate limited — try again later",
                response.header("Retry-After")?.toIntOrNull()
            )
            in 500..599 -> throw LlmServerException(
                errorDetail ?: "Server error (${response.code})"
            )
            else -> throw IOException(
                errorDetail ?: "Unexpected response: ${response.code}"
            )
        }
    }

    private fun parseNonStreamingResponse(body: String): LlmResult {
        val responseObj = json.parseToJsonElement(body).jsonObject
        val choices = responseObj["choices"]?.jsonArray
            ?: return LlmResult()
        if (choices.isEmpty()) return LlmResult()

        val message = choices[0].jsonObject["message"]?.jsonObject
            ?: return LlmResult()

        val content = message["content"]?.jsonPrimitive?.contentOrNull
        val toolCallsArr = message["tool_calls"]?.jsonArray

        val toolCalls = toolCallsArr?.mapNotNull { tc ->
            val tcObj = tc.jsonObject
            val function = tcObj["function"]?.jsonObject ?: return@mapNotNull null
            val argsStr = function["arguments"]?.jsonPrimitive?.contentOrNull ?: ""
            val argsJson = try {
                json.parseToJsonElement(argsStr).jsonObject
            } catch (_: Exception) {
                JsonObject(emptyMap())
            }
            ToolCall(
                id = tcObj["id"]?.jsonPrimitive?.contentOrNull ?: "call_${tc.hashCode()}",
                name = function["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null,
                arguments = argsJson
            )
        } ?: emptyList()

        return LlmResult(text = content, toolCalls = toolCalls)
    }

    private class ToolCallAccumulator(val id: String) {
        var name: String? = null
        val argsBuilder: StringBuilder = StringBuilder()
    }
}

fun ToolSpec.toOpenAiTool(): JsonObject = buildJsonObject {
    put("type", "function")
    put("function", buildJsonObject {
        put("name", name)
        put("description", description)
        put("parameters", parametersSchema)
    })
}

private fun Role.toOpenAiRole(): String = when (this) {
    Role.USER -> "user"
    Role.ASSISTANT -> "assistant"
    Role.TOOL -> "tool"
    Role.SYSTEM -> "system"
}

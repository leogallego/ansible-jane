package com.example.aapremote.network.mcp

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import okio.BufferedSource
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class McpTransport(
    baseClient: OkHttpClient,
    private val json: Json
) {
    private val httpClient = baseClient
    private val sseClient = baseClient.newBuilder()
        .readTimeout(0, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()

    suspend fun postJsonRpc(
        url: String,
        request: JsonRpcRequest,
        sessionId: String? = null
    ): McpTransportResult {
        val body = json.encodeToString(JsonRpcRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", "application/json, text/event-stream")
            .apply { sessionId?.let { header("Mcp-Session-Id", it) } }
            .build()

        val response = executeRequest(httpRequest)

        if (response.code == 404 && sessionId != null) {
            response.close()
            throw McpSessionExpiredException("Session expired (HTTP 404)")
        }

        val newSessionId = response.header("Mcp-Session-Id")

        val contentType = response.header("Content-Type") ?: ""
        val responseFlow = when {
            contentType.contains("text/event-stream") -> parseSseFromBody(response)
            else -> flowOf(parseJsonResponse(response))
        }
        return McpTransportResult(responseFlow, newSessionId)
    }

    suspend fun postNotification(
        url: String,
        request: JsonRpcRequest,
        sessionId: String? = null
    ) {
        val body = json.encodeToString(JsonRpcRequest.serializer(), request)
            .toRequestBody("application/json".toMediaType())

        val httpRequest = Request.Builder()
            .url(url)
            .post(body)
            .header("Accept", "application/json, text/event-stream")
            .header("Content-Type", "application/json")
            .apply { sessionId?.let { header("Mcp-Session-Id", it) } }
            .build()

        try {
            val response = executeRequest(httpRequest)
            response.close()
        } catch (_: IOException) {
            // Some servers reject notifications — non-fatal
        }
    }

    fun extractSessionId(response: Response): String? =
        response.header("Mcp-Session-Id")

    fun createSseFlow(request: Request): Flow<SseEvent> = callbackFlow {
        val eventSource = EventSources.createFactory(sseClient)
            .newEventSource(request, object : EventSourceListener() {
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    trySend(SseEvent(id, type, data))
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    close(t ?: IOException("SSE failed: ${response?.code}"))
                }

                override fun onClosed(eventSource: EventSource) {
                    close()
                }
            })

        awaitClose { eventSource.cancel() }
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

    private fun parseJsonResponse(response: Response): JsonRpcResponse {
        val responseBody = response.body?.string()
            ?: throw IOException("Empty response body")
        return json.decodeFromString(JsonRpcResponse.serializer(), responseBody)
    }

    private fun parseSseFromBody(response: Response): Flow<JsonRpcResponse> = flow {
        response.use { resp ->
            val source = resp.body?.source()
                ?: throw IOException("Empty response body")
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data.isNotEmpty()) {
                        val rpcResponse = json.decodeFromString(
                            JsonRpcResponse.serializer(), data
                        )
                        emit(rpcResponse)
                    }
                }
            }
        }
    }
}

class McpSessionExpiredException(message: String) : IOException(message)

data class McpTransportResult(
    val responses: Flow<JsonRpcResponse>,
    val sessionId: String? = null
)

data class SseEvent(
    val id: String?,
    val type: String?,
    val data: String
)

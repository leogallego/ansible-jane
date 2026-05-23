package io.github.leogallego.ansiblejane.assistant.data

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume

class ModelFetcher(
    private val httpClient: OkHttpClient,
    private val json: Json
) {
    sealed interface Result {
        data class Success(val models: List<String>) : Result
        data class Error(val message: String) : Result
    }

    suspend fun fetchModels(baseUrl: String, apiKey: String?): Result {
        val url = "${baseUrl.trimEnd('/')}/models"

        val requestBuilder = Request.Builder().url(url).get()
        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $apiKey")
        }

        val response = try {
            executeRequest(requestBuilder.build())
        } catch (e: IOException) {
            return Result.Error("Could not reach server: ${e.message}")
        }

        return response.use { resp ->
            when (resp.code) {
                in 200..299 -> parseModelsResponse(resp)
                401, 403 -> Result.Error("Authentication failed — check API key")
                else -> Result.Error("Server returned ${resp.code}")
            }
        }
    }

    private fun parseModelsResponse(response: Response): Result {
        val body = response.body.string()
        return try {
            val root = json.parseToJsonElement(body).jsonObject
            val data = root["data"]?.jsonArray ?: return Result.Success(emptyList())
            val models = data.mapNotNull { element ->
                element.jsonObject["id"]?.jsonPrimitive?.content
            }.sorted()
            Result.Success(models)
        } catch (e: Exception) {
            Result.Error("Failed to parse models: ${e.message}")
        }
    }

    private suspend fun executeRequest(request: Request): Response =
        suspendCancellableCoroutine { cont ->
            val call = httpClient.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (cont.isActive) cont.resumeWith(kotlin.Result.failure(e))
                }

                override fun onResponse(call: Call, response: Response) {
                    if (cont.isActive) cont.resume(response)
                }
            })
        }
}

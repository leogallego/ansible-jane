package io.github.leogallego.ansiblejane.assistant.data

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ModelFetcher(
    private val httpClient: HttpClient,
    private val json: Json
) {
    sealed interface Result {
        data class Success(val models: List<String>) : Result
        data class Error(val message: String) : Result
    }

    suspend fun fetchModels(baseUrl: String, apiKey: String?): Result {
        val url = "${baseUrl.trimEnd('/')}/models"

        val response = try {
            httpClient.get(url) {
                if (!apiKey.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                }
            }
        } catch (e: Exception) {
            return Result.Error("Could not reach server: ${e.message}")
        }

        return when (response.status.value) {
            in 200..299 -> parseModelsResponse(response.bodyAsText())
            401, 403 -> Result.Error("Authentication failed — check API key")
            else -> Result.Error("Server returned ${response.status.value}")
        }
    }

    private fun parseModelsResponse(body: String): Result {
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
}

package com.example.aapremote.assistant.engine

import ai.koog.prompt.message.Message
import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.Tool
import com.example.aapremote.assistant.tools.ToolResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ToolExecutor(
    private val tools: List<Tool>,
    private val maxResultChars: Int = 8_000
) {
    private val resultCache = mutableMapOf<String, Pair<Long, ToolResult>>()

    suspend fun execute(toolCall: Message.Tool.Call): ToolResult {
        val tool = tools.find { it.spec.name == toolCall.tool }
            ?: return ToolResult(
                success = false,
                data = "Tool '${toolCall.tool}' not found",
                errorType = ErrorType.NOT_FOUND
            )

        val cacheKey = "${toolCall.tool}:${toolCall.content.hashCode()}"
        val cached = resultCache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.first < CACHE_TTL_MS) {
            return cached.second
        }

        val argsJson = try {
            val parsed = json.parseToJsonElement(toolCall.content)
            if (parsed is JsonObject) parsed else JsonObject(emptyMap())
        } catch (_: Exception) {
            JsonObject(emptyMap())
        }

        val result = try {
            withTimeout(30_000L) {
                tool.execute(argsJson)
            }
        } catch (_: TimeoutCancellationException) {
            return ToolResult(
                success = false,
                data = "Tool '${toolCall.tool}' timed out after 30s",
                errorType = ErrorType.TIMEOUT
            )
        }

        val capped = if (result.data != null) {
            result.copy(data = capResultArray(result.data))
        } else result

        val finalResult = if (capped.data != null && capped.data.length > maxResultChars) {
            capped.copy(data = smartTruncate(capped.data, maxResultChars))
        } else {
            capped
        }
        if (finalResult.success) {
            resultCache[cacheKey] = System.currentTimeMillis() to finalResult
        }
        return finalResult
    }

    companion object {
        private const val CACHE_TTL_MS = 120_000L
        private const val MAX_ARRAY_ITEMS = 10
        private val json = Json { ignoreUnknownKeys = true }

        fun capResultArray(data: String): String {
            val parsed = try { json.parseToJsonElement(data) } catch (_: Exception) { return data }
            if (parsed !is JsonObject) return data
            val results = parsed["results"]
            if (results !is JsonArray || results.size <= MAX_ARRAY_ITEMS) return data
            val total = parsed["count"]?.jsonPrimitive?.intOrNull ?: results.size
            val capped = buildJsonObject {
                parsed.forEach { (k, v) ->
                    when (k) {
                        "results" -> put("results", buildJsonArray {
                            results.take(MAX_ARRAY_ITEMS).forEach { add(it) }
                        })
                        else -> put(k, v)
                    }
                }
                put("_showing", MAX_ARRAY_ITEMS)
                put("_total", total)
                put("_note", "Showing first $MAX_ARRAY_ITEMS of $total. Ask user if they want more.")
            }
            return json.encodeToString(JsonObject.serializer(), capped)
        }

        fun smartTruncate(data: String, maxChars: Int): String {
            if (data.length <= maxChars) return data
            val firstPortion = (maxChars * 0.6).toInt()
            val lastPortion = maxChars - firstPortion
            val truncated = data.length - maxChars
            return data.take(firstPortion) +
                "\n\n[... $truncated chars truncated ...]\n\n" +
                data.takeLast(lastPortion)
        }
    }
}

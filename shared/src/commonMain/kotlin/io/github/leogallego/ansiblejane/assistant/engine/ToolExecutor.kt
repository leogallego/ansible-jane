package io.github.leogallego.ansiblejane.assistant.engine

import ai.koog.prompt.message.MessagePart
import io.github.leogallego.ansiblejane.assistant.engine.DebugLog as Log
import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import io.github.leogallego.ansiblejane.assistant.tools.Tool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
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

    fun findTool(name: String): Tool? = tools.find { it.spec.name == name }

    suspend fun execute(toolCall: MessagePart.Tool.Call): ToolResult {
        val tool = tools.find { it.spec.name == toolCall.tool }
            ?: run {
                Log.w(TAG, "EXEC: tool '${toolCall.tool}' not found in ${tools.size} registered tools")
                return ToolResult(
                    success = false,
                    data = "Tool '${toolCall.tool}' not found",
                    errorType = ErrorType.NOT_FOUND
                )
            }

        val isDestructive = tool.isDestructive
        val cacheKey = "${toolCall.tool}:${toolCall.args.hashCode()}"
        if (!isDestructive) {
            val cached = resultCache[cacheKey]
            if (cached != null && kotlin.time.Clock.System.now().toEpochMilliseconds() - cached.first < CACHE_TTL_MS) {
                Log.d(TAG, "EXEC: cache hit for ${toolCall.tool}")
                return cached.second
            }
        }

        val argsJson = try {
            val parsed = json.parseToJsonElement(toolCall.args)
            if (parsed is JsonObject) parsed else JsonObject(emptyMap())
        } catch (e: Exception) {
            Log.w(TAG, "EXEC: malformed args for ${toolCall.tool}: ${e.message}")
            return ToolResult(
                success = false,
                data = "Invalid tool arguments: ${e.message}"
            )
        }

        Log.d(TAG, "EXEC: ${toolCall.tool}(${toolCall.args.take(200)})")
        val startMs = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val result = try {
            withTimeout(30_000L) {
                tool.execute(argsJson)
            }
        } catch (_: TimeoutCancellationException) {
            Log.w(TAG, "EXEC: ${toolCall.tool} timed out after 30s")
            return ToolResult(
                success = false,
                data = "Tool '${toolCall.tool}' timed out after 30s",
                errorType = ErrorType.TIMEOUT
            )
        }
        val elapsedMs = kotlin.time.Clock.System.now().toEpochMilliseconds() - startMs
        val rawLen = result.data?.length ?: 0

        val capped = if (result.data != null) {
            result.copy(data = capResultArray(result.data))
        } else result
        val cappedLen = capped.data?.length ?: 0

        val finalResult = if (capped.data != null && capped.data.length > maxResultChars) {
            capped.copy(data = smartTruncate(capped.data, maxResultChars))
        } else {
            capped
        }
        val finalLen = finalResult.data?.length ?: 0
        Log.d(TAG, "EXEC: ${toolCall.tool} ${if (finalResult.success) "OK" else "FAIL"} " +
            "in ${elapsedMs}ms, result=${rawLen}→${if (cappedLen != rawLen) "${cappedLen}→" else ""}${finalLen} chars")
        Log.d(TAG, "EXEC DATA: ${finalResult.data?.take(500)}")

        if (finalResult.success && !isDestructive) {
            resultCache[cacheKey] = kotlin.time.Clock.System.now().toEpochMilliseconds() to finalResult
        }
        return finalResult
    }

    companion object {
        private const val TAG = "ToolExecutor"
        private const val CACHE_TTL_MS = 120_000L
        private const val MAX_ARRAY_ITEMS = 10
        private val json = Json { ignoreUnknownKeys = true }

        fun capResultArray(data: String): String {
            val parsed = try { json.parseToJsonElement(data) } catch (_: Exception) { return data }
            if (parsed !is JsonObject) return data
            val (arrayKey, results) = parsed.entries
                .firstOrNull { it.value is JsonArray } ?: return data
            val arrayValue = results as JsonArray
            if (arrayValue.size <= MAX_ARRAY_ITEMS) return data
            val total = parsed["count"]?.jsonPrimitive?.intOrNull ?: arrayValue.size
            val capped = buildJsonObject {
                parsed.forEach { (k, v) ->
                    if (k == arrayKey) {
                        put(k, buildJsonArray {
                            arrayValue.take(MAX_ARRAY_ITEMS).forEach { add(it) }
                        })
                    } else {
                        put(k, v)
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

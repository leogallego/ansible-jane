package com.example.aapremote.assistant.engine

import com.example.aapremote.assistant.llm.ToolCall
import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.Tool
import com.example.aapremote.assistant.tools.ToolResult
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class ToolExecutor(
    private val tools: List<Tool>,
    private val maxResultChars: Int = 8_000
) {
    suspend fun execute(toolCall: ToolCall): ToolResult {
        val tool = tools.find { it.spec.name == toolCall.name }
            ?: return ToolResult(
                success = false,
                data = "Tool '${toolCall.name}' not found",
                errorType = ErrorType.NOT_FOUND
            )

        val result = try {
            withTimeout(30_000L) {
                val args = jsonObjectToMap(toolCall.arguments)
                tool.execute(args)
            }
        } catch (_: TimeoutCancellationException) {
            return ToolResult(
                success = false,
                data = "Tool '${toolCall.name}' timed out after 30s",
                errorType = ErrorType.TIMEOUT
            )
        }

        return if (result.data != null && result.data.length > maxResultChars) {
            result.copy(data = smartTruncate(result.data, maxResultChars))
        } else {
            result
        }
    }

    private fun jsonObjectToMap(json: kotlinx.serialization.json.JsonObject): Map<String, Any> {
        return json.entries.associate { (key, value) ->
            key to jsonElementToAny(value)
        }
    }

    private fun jsonElementToAny(element: kotlinx.serialization.json.JsonElement): Any {
        return when (element) {
            is kotlinx.serialization.json.JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" -> true
                    element.content == "false" -> false
                    element.content.contains('.') -> element.content.toDoubleOrNull() ?: element.content
                    else -> element.content.toLongOrNull() ?: element.content
                }
            }
            is kotlinx.serialization.json.JsonArray -> element.map { jsonElementToAny(it) }
            is kotlinx.serialization.json.JsonObject -> jsonObjectToMap(element)
        }
    }

    companion object {
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

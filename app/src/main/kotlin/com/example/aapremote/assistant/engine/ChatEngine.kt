package com.example.aapremote.assistant.engine

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.example.aapremote.assistant.llm.LlmAuthException
import com.example.aapremote.assistant.llm.LlmProvider
import com.example.aapremote.assistant.llm.LlmRateLimitException
import com.example.aapremote.assistant.llm.LlmServerException
import com.example.aapremote.assistant.llm.LlmTimeoutException
import com.example.aapremote.assistant.engine.DebugLog as Log
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.assistant.tools.toToolDescriptor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.coroutineContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException

sealed interface ChatEvent {
    data class TextDelta(val text: String) : ChatEvent
    data class ToolExecuting(val toolName: String, val args: JsonObject) : ChatEvent
    data class ToolResult(val toolName: String, val result: com.example.aapremote.assistant.tools.ToolResult) : ChatEvent
    data class AssistantMessage(val fullText: String, val toolCallCount: Int) : ChatEvent
    data class Error(val message: String, val cause: Throwable? = null) : ChatEvent
}

class ChatEngine(
    private val provider: LlmProvider,
    private val toolExecutor: ToolExecutor,
    private val maxIterations: Int = 10
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun processMessage(
        userMessage: String,
        history: List<ChatMessage>,
        tools: List<ToolSpec>,
        maxTokens: Int? = null
    ): Flow<ChatEvent> = flow {
        try {
            val messages = mutableListOf<ChatMessage>()
            messages.add(ChatMessage(role = Role.SYSTEM, content = SYSTEM_PROMPT))
            history.filter { it.role == Role.USER || it.role == Role.ASSISTANT }
                .forEach { messages.add(it) }
            messages.add(ChatMessage(role = Role.USER, content = userMessage))

            val toolDescriptors = tools.map { it.toToolDescriptor() }
            val toolSchemaChars = toolDescriptors.sumOf {
                it.name.length + it.description.length +
                    it.requiredParameters.sumOf { p -> p.name.length + p.description.length + 20 } +
                    it.optionalParameters.sumOf { p -> p.name.length + p.description.length + 20 }
            }
            val msgChars = messages.sumOf { it.content.length }
            Log.d(TAG, "PAYLOAD: ${tools.size} tools (~${toolSchemaChars} schema chars), " +
                "${messages.size} messages (~${msgChars} msg chars), total ~${toolSchemaChars + msgChars} chars")
            Log.d(TAG, "PAYLOAD tools: ${tools.map { it.name }}")
            var iterations = 0
            var totalToolCalls = 0
            val toolCallHistory = mutableListOf<List<String>>()

            loop@ while (iterations < maxIterations) {
                iterations++
                coroutineContext.ensureActive()
                val textBuilder = StringBuilder()
                val pendingToolCalls = mutableMapOf<String, MutableToolCall>()

                trimMessages(messages)
                val prompt = buildPrompt(messages)

                provider.generateStream(prompt, toolDescriptors, maxTokens).collect { frame ->
                    when (frame) {
                        is StreamFrame.TextDelta -> {
                            textBuilder.append(frame.text)
                            emit(ChatEvent.TextDelta(frame.text))
                        }
                        is StreamFrame.ToolCallDelta -> {
                            val id = frame.id ?: return@collect
                            val tc = pendingToolCalls.getOrPut(id) { MutableToolCall(id) }
                            if (frame.name != null) tc.name = frame.name
                            if (frame.content != null) tc.args.append(frame.content)
                        }
                        is StreamFrame.ToolCallComplete -> {
                            val id = frame.id ?: return@collect
                            val tc = pendingToolCalls.getOrPut(id) { MutableToolCall(id) }
                            tc.name = frame.name
                            tc.args.clear()
                            tc.args.append(frame.content)
                        }
                        is StreamFrame.End -> { /* handled after collect */ }
                        else -> { /* ReasoningDelta etc — ignore */ }
                    }
                }

                val completedCalls = pendingToolCalls.values
                    .filter { it.name != null }
                    .map { tc ->
                        Message.Tool.Call(
                            id = tc.id,
                            tool = tc.name!!,
                            content = tc.args.toString().ifEmpty { "{}" },
                            metaInfo = ResponseMetaInfo.Empty
                        )
                    }

                val responseText = textBuilder.toString().ifEmpty { null }

                if (completedCalls.isNotEmpty() && iterations < maxIterations) {
                    Log.d(TAG, "ITER $iterations: ${completedCalls.size} tool calls: " +
                        "${completedCalls.map { it.tool }}")
                    val currentSignature = completedCalls.map {
                        "${it.tool}:${it.content.hashCode()}"
                    }
                    toolCallHistory.add(currentSignature)

                    if (isRepeatingToolCalls(toolCallHistory)) {
                        Log.w(TAG, "ITER $iterations: repeat detected, stopping")
                        emit(ChatEvent.AssistantMessage(
                            (responseText ?: "") + "\n\nStopped: the same tools were being called repeatedly.",
                            totalToolCalls
                        ))
                        return@flow
                    }

                    val toolCallsJson = json.encodeToString(
                        JsonArray.serializer(),
                        JsonArray(completedCalls.map { tc ->
                            JsonObject(mapOf(
                                "id" to kotlinx.serialization.json.JsonPrimitive(tc.id),
                                "name" to kotlinx.serialization.json.JsonPrimitive(tc.tool),
                                "arguments" to kotlinx.serialization.json.JsonPrimitive(tc.content)
                            ))
                        })
                    )
                    messages.add(ChatMessage(
                        role = Role.ASSISTANT,
                        content = responseText ?: "",
                        toolCallsJson = toolCallsJson
                    ))

                    val toolSummaries = mutableListOf<String>()
                    for (toolCall in completedCalls) {
                        val argsJson = try {
                            json.parseToJsonElement(toolCall.content).jsonObject
                        } catch (_: Exception) {
                            JsonObject(emptyMap())
                        }
                        emit(ChatEvent.ToolExecuting(toolCall.tool, argsJson))

                        val toolResult = toolExecutor.execute(toolCall)
                        totalToolCalls++

                        emit(ChatEvent.ToolResult(toolCall.tool, toolResult))

                        messages.add(ChatMessage(
                            role = Role.TOOL,
                            content = toolResult.data
                                ?: "Tool execution failed: ${toolResult.errorType ?: "unknown error"}",
                            toolCallId = toolCall.id,
                            toolName = toolCall.tool
                        ))
                        toolSummaries.add("${toolCall.tool}: ${toolResult.data?.take(200) ?: "error"}")
                    }

                    if (iterations > 1) {
                        compactToolMessages(messages, toolSummaries)
                    }
                } else {
                    Log.d(TAG, "DONE: $iterations iterations, $totalToolCalls total tool calls, " +
                        "response=${responseText?.length ?: 0} chars")
                    val finalText = if (completedCalls.isNotEmpty() && iterations >= maxIterations) {
                        (responseText ?: "") + "\n\nI wasn't able to complete this request within the tool call limit."
                    } else {
                        responseText ?: textBuilder.toString()
                    }
                    emit(ChatEvent.AssistantMessage(finalText, totalToolCalls))
                    return@flow
                }
            }

            emit(ChatEvent.AssistantMessage(
                "I wasn't able to complete this request within the tool call limit.",
                totalToolCalls
            ))
        } catch (e: CancellationException) {
            throw e
        } catch (e: LlmAuthException) {
            Log.e(TAG, "AUTH error: ${e.message}", e)
            emit(ChatEvent.Error(e.message ?: "Authentication failed — check API key", e))
        } catch (e: LlmRateLimitException) {
            Log.w(TAG, "RATE_LIMIT: ${e.message}")
            emit(ChatEvent.Error(e.message ?: "Rate limited — try again later", e))
        } catch (e: LlmTimeoutException) {
            Log.w(TAG, "TIMEOUT: ${e.message}")
            emit(ChatEvent.Error("Response timed out", e))
        } catch (e: IOException) {
            Log.e(TAG, "IO error: ${e.message}", e)
            emit(ChatEvent.Error("Unable to reach LLM server: ${e.message}", e))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            emit(ChatEvent.Error("Error: ${e.message}", e))
        }
    }

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
                    if (msg.toolCallsJson != null) {
                        parseToolCalls(msg.toolCallsJson)
                    } else {
                        listOf(Message.Assistant(
                            content = msg.content,
                            metaInfo = ResponseMetaInfo.Empty
                        ))
                    }
                }
                Role.TOOL -> listOf(Message.Tool.Result(
                    id = msg.toolCallId,
                    tool = msg.toolName ?: msg.toolCallId ?: "",
                    content = msg.content,
                    metaInfo = RequestMetaInfo.Empty
                ))
            }
        }
        return Prompt(messages = koogMessages, id = "chat")
    }

    private fun parseToolCalls(toolCallsJson: String): List<Message.Tool.Call> {
        return try {
            val element = json.parseToJsonElement(toolCallsJson)
            if (element is JsonArray) {
                element.map { el ->
                    val obj = el.jsonObject
                    Message.Tool.Call(
                        id = obj["id"]?.jsonPrimitive?.content,
                        tool = obj["name"]?.jsonPrimitive?.content ?: "",
                        content = obj["arguments"]?.jsonPrimitive?.content ?: "{}",
                        metaInfo = ResponseMetaInfo.Empty
                    )
                }
            } else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun compactToolMessages(
        messages: MutableList<ChatMessage>,
        currentSummaries: List<String>
    ) {
        val keepFrom = messages.indexOfLast { it.role == Role.USER } + 1
        val toCompact = mutableListOf<Int>()
        for (i in keepFrom until messages.size - currentSummaries.size) {
            if (messages[i].role == Role.TOOL || (messages[i].role == Role.ASSISTANT && messages[i].toolCallsJson != null)) {
                toCompact.add(i)
            }
        }
        if (toCompact.isEmpty()) return
        val summary = toCompact
            .filter { messages[it].role == Role.TOOL }
            .joinToString("; ") { i ->
                val msg = messages[i]
                val preview = msg.content.take(100).replace("\n", " ")
                "tool_result: $preview"
            }
        for (i in toCompact.reversed()) {
            messages.removeAt(i)
        }
        if (summary.isNotBlank()) {
            messages.add(keepFrom, ChatMessage(role = Role.ASSISTANT, content = "[Previous tool calls: $summary]"))
        }
    }

    private fun isRepeatingToolCalls(history: List<List<String>>): Boolean {
        if (history.size < 3) return false
        val last = history.last()
        return history[history.size - 2] == last && history[history.size - 3] == last
    }

    private fun trimMessages(
        messages: MutableList<ChatMessage>,
        maxChars: Int = DEFAULT_CONTEXT_CHARS
    ) {
        val totalChars = messages.sumOf { it.content.length }
        if (totalChars <= maxChars) return
        Log.d(TAG, "TRIM: $totalChars chars > $maxChars limit, trimming ${messages.size} messages")

        val systemMessages = messages.takeWhile { it.role == Role.SYSTEM }
        val systemChars = systemMessages.sumOf { it.content.length }
        val available = maxChars - systemChars

        var kept = 0
        var keepFrom = messages.size
        for (i in messages.indices.reversed()) {
            if (messages[i].role == Role.SYSTEM) break
            kept += messages[i].content.length
            if (kept > available) break
            keepFrom = i
        }

        if (keepFrom > systemMessages.size) {
            messages.subList(systemMessages.size, keepFrom).clear()
        }
    }

    private class MutableToolCall(val id: String) {
        var name: String? = null
        val args = StringBuilder()
    }

    companion object {
        private const val TAG = "ChatEngine"
        private const val DEFAULT_CONTEXT_CHARS = 16_000
        const val SYSTEM_PROMPT = """You are a concise AI assistant for Ansible Automation Platform (AAP). Rules:
- NEVER fabricate, invent, or guess data. Only present information returned by tool calls. If you have no tool to answer a question, say so clearly.
- When results contain more than 10 items, show a summary with total count and the 5 most recent. Ask before listing all.
- Use short structured formatting (bullets, bold labels). No lengthy prose.
- Never repeat raw tool output verbatim — summarize it.
- You have local tools (list_jobs, launch_job, etc.) that connect directly to the AAP instance and MCP tools (controller.*, eda.*) for extended capabilities. Prefer local tools when available.
- For write operations (launch, cancel, toggle), explain what you will do and wait for confirmation."""
    }
}

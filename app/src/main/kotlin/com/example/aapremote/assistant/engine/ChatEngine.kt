package com.example.aapremote.assistant.engine

import android.util.Log
import com.example.aapremote.assistant.llm.LlmAuthException
import com.example.aapremote.assistant.llm.LlmProvider
import com.example.aapremote.assistant.llm.LlmTimeoutException
import com.example.aapremote.assistant.llm.StreamEvent
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
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
    fun processMessage(
        userMessage: String,
        history: List<ChatMessage>,
        tools: List<ToolSpec>
    ): Flow<ChatEvent> = flow {
        try {
            val messages = mutableListOf<ChatMessage>()
            messages.add(ChatMessage(role = Role.SYSTEM, content = SYSTEM_PROMPT))
            messages.addAll(history)
            messages.add(ChatMessage(role = Role.USER, content = userMessage))

            var iterations = 0
            var totalToolCalls = 0
            var streamError: Throwable? = null

            loop@ while (iterations < maxIterations) {
                iterations++
                val textBuilder = StringBuilder()
                var lastResult: com.example.aapremote.assistant.llm.LlmResult? = null

                provider.generateStream(messages, tools).collect { event ->
                    when (event) {
                        is StreamEvent.TextDelta -> {
                            textBuilder.append(event.text)
                            emit(ChatEvent.TextDelta(event.text))
                        }
                        is StreamEvent.ToolCallStart -> {}
                        is StreamEvent.ToolCallArgs -> {}
                        is StreamEvent.Done -> {
                            lastResult = event.result
                        }
                        is StreamEvent.Error -> {
                            streamError = event.cause
                        }
                    }
                }

                if (streamError != null) {
                    emit(ChatEvent.Error(
                        "LLM error: ${streamError!!.message}",
                        streamError
                    ))
                    return@flow
                }

                val result = lastResult ?: break

                Log.d("ChatEngine", "Iteration $iterations: text=${result.text?.take(100)} toolCalls=${result.toolCalls.size}")

                if (result.toolCalls.isNotEmpty() && iterations < maxIterations) {
                    val assistantContent = result.text ?: ""
                    messages.add(ChatMessage(
                        role = Role.ASSISTANT,
                        content = assistantContent,
                        toolCalls = result.toolCalls
                    ))

                    for (toolCall in result.toolCalls) {
                        Log.d("ChatEngine", "Calling tool: ${toolCall.name} args=${toolCall.arguments}")
                        emit(ChatEvent.ToolExecuting(toolCall.name, toolCall.arguments))

                        val toolResult = toolExecutor.execute(toolCall)
                        totalToolCalls++
                        Log.d("ChatEngine", "Tool result: success=${toolResult.success} size=${toolResult.data?.length ?: 0}")

                        emit(ChatEvent.ToolResult(toolCall.name, toolResult))

                        messages.add(ChatMessage(
                            role = Role.TOOL,
                            content = toolResult.data ?: "",
                            toolCallId = toolCall.id
                        ))
                    }
                } else {
                    val finalText = if (result.toolCalls.isNotEmpty() && iterations >= maxIterations) {
                        (result.text ?: "") + "\n\nI wasn't able to complete this request within the tool call limit."
                    } else {
                        result.text ?: textBuilder.toString()
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
            emit(ChatEvent.Error("LLM authentication failed — check API key", e))
        } catch (e: LlmTimeoutException) {
            emit(ChatEvent.Error("Response timed out", e))
        } catch (e: IOException) {
            emit(ChatEvent.Error("Unable to reach LLM server: ${e.message}", e))
        } catch (e: Exception) {
            emit(ChatEvent.Error("Error: ${e.message}", e))
        }
    }

    companion object {
        const val SYSTEM_PROMPT = """You are an AI assistant for Ansible Automation Platform (AAP). You help users query and manage their AAP instance using the available tools. Be concise and specific. When reporting results, use structured formatting."""
    }
}

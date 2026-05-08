package com.example.aapremote.assistant.llm

import com.example.aapremote.assistant.engine.ChatMessage
import com.example.aapremote.assistant.tools.ToolSpec
import kotlinx.coroutines.flow.Flow

interface LlmProvider {
    suspend fun generate(messages: List<ChatMessage>, tools: List<ToolSpec>): LlmResult
    fun generateStream(messages: List<ChatMessage>, tools: List<ToolSpec>): Flow<StreamEvent>
    fun isAvailable(): Boolean
    fun modelInfo(): ModelInfo
}

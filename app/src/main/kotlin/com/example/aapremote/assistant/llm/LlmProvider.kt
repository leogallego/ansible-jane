package com.example.aapremote.assistant.llm

import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.message.Message
import ai.koog.prompt.streaming.StreamFrame
import kotlinx.coroutines.flow.Flow

interface LlmProvider {
    suspend fun generate(prompt: Prompt, tools: List<ToolDescriptor>, maxTokens: Int? = null): List<Message.Response>
    fun generateStream(prompt: Prompt, tools: List<ToolDescriptor>, maxTokens: Int? = null): Flow<StreamFrame>
    fun isAvailable(): Boolean
    fun modelInfo(): ModelInfo
}

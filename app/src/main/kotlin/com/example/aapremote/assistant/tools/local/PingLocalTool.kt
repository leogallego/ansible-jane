package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.InfrastructureRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

class PingLocalTool(
    private val repository: InfrastructureRepository
) : LocalTool(
    spec = ToolSpec(
        name = "ping",
        description = "Quick health check of the AAP cluster — returns version, HA status, and node info",
        parametersSchema = buildToolSchema()
    )
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val ping = repository.ping().getOrThrow()
            ToolResult(success = true, data = networkJson.encodeToString(ping))
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}

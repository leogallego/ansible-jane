package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.EdaActivationRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

class GetEdaActivationLocalTool(
    private val repository: EdaActivationRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_eda_activation",
        description = "Get details of a specific EDA rulebook activation by ID — status, restart policy, decision environment",
        parametersSchema = buildToolSchema(
            Triple("activation_id", "integer", "ID of the EDA activation"),
            required = listOf("activation_id")
        )
    )
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult = executeSafely {
        val activationId = (args["activation_id"] as? Number)?.toInt()
            ?: return@executeSafely ToolResult(success = false, data = "activation_id is required", errorType = ErrorType.NOT_FOUND)
        val activation = repository.getActivation(activationId).getOrThrow()
        ToolResult(success = true, data = networkJson.encodeToString(activation))
    }
}

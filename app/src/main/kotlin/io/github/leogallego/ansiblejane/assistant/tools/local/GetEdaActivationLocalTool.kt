package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.EdaActivationRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

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
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val activationId = args.intArg("activation_id")
            ?: return@executeSafely ToolResult(success = false, data = "activation_id is required", errorType = ErrorType.NOT_FOUND)
        val activation = repository.getActivation(activationId).getOrThrow()
        ToolResult(success = true, data = networkJson.encodeToString(activation))
    }
}

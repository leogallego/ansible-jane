package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.InfrastructureRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class GetInstanceLocalTool(
    private val repository: InfrastructureRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_instance",
        description = "Get details of a specific AAP instance by ID, including capacity and health",
        parametersSchema = buildToolSchema(
            Triple("instance_id", "integer", "ID of the instance"),
            required = listOf("instance_id")
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val instanceId = args.intArg("instance_id")
            ?: return@executeSafely ToolResult(success = false, data = "instance_id is required", errorType = ErrorType.NOT_FOUND)
        val instance = repository.getInstance(instanceId).getOrThrow()
        ToolResult(success = true, data = networkJson.encodeToString(instance))
    }
}

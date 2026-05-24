package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.ControllerReadOnlyRepository
import kotlinx.serialization.json.JsonObject

class GetConfigLocalTool(
    private val repository: ControllerReadOnlyRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_config",
        description = "Get AAP configuration including license info, version, and platform details",
        parametersSchema = buildToolSchema()
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val config = repository.getConfig().getOrThrow()
        ToolResult(success = true, data = config.toString())
    }
}

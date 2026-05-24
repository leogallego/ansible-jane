package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.ControllerReadOnlyRepository
import kotlinx.serialization.json.JsonObject

class GetSettingsLocalTool(
    private val repository: ControllerReadOnlyRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_settings",
        description = "Get AAP system settings categories and their endpoints",
        parametersSchema = buildToolSchema()
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val settings = repository.getSettings().getOrThrow()
        ToolResult(success = true, data = settings.toString())
    }
}

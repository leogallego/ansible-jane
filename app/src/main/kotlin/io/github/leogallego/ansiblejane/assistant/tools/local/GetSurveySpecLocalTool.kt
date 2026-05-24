package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.ControllerReadOnlyRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class GetSurveySpecLocalTool(
    private val repository: ControllerReadOnlyRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_survey_spec",
        description = "Get the survey specification for a job template (required variables and prompts)",
        parametersSchema = buildToolSchema(
            Triple("id", "integer", "Job template ID"),
            required = listOf("id")
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val id = args.intArg("id") ?: return@executeSafely ToolResult(
            success = false, data = "Error: 'id' parameter is required"
        )
        val survey = repository.getSurveySpec(id).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(survey)
        )
    }
}

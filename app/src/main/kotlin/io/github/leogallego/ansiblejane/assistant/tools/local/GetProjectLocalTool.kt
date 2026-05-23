package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.ErrorType
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.ProjectRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class GetProjectLocalTool(
    private val repository: ProjectRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_project",
        description = "Get details of a specific project by ID — SCM URL, branch, last sync status",
        parametersSchema = buildToolSchema(
            Triple("project_id", "integer", "ID of the project"),
            required = listOf("project_id")
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val projectId = args.intArg("project_id")
            ?: return@executeSafely ToolResult(success = false, data = "project_id is required", errorType = ErrorType.NOT_FOUND)
        val project = repository.getProject(projectId).getOrThrow()
        ToolResult(success = true, data = networkJson.encodeToString(project))
    }
}

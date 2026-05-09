package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.ProjectRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

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
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val projectId = (args["project_id"] as? Number)?.toInt()
                ?: return ToolResult(success = false, data = "project_id is required", errorType = ErrorType.NOT_FOUND)
            val project = repository.getProject(projectId).getOrThrow()
            ToolResult(success = true, data = networkJson.encodeToString(project))
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}

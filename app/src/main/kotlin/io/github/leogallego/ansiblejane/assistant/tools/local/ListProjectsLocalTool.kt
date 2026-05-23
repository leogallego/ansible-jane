package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.ProjectRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListProjectsLocalTool(
    private val repository: ProjectRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_projects",
        description = "List projects with SCM type, URL, branch, and sync status",
        parametersSchema = buildToolSchema(
            Triple("search", "string", "Search term to filter projects by name"),
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 25, max 25)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val pageSize = args.intArg("page_size")?.coerceIn(1, 25) ?: 25
        val result = repository.getProjects(
            page = args.pageArg(),
            pageSize = pageSize,
            search = args.stringArg("search")
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "projects" to networkJson.encodeToString(result.projects)
            ))
        )
    }
}

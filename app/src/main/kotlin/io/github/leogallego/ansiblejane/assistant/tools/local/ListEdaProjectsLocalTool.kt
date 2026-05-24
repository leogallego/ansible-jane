package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.EdaReadOnlyRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListEdaProjectsLocalTool(
    private val repository: EdaReadOnlyRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_eda_projects",
        description = "List EDA projects (Git repositories containing rulebooks)",
        parametersSchema = buildToolSchema(
            Triple("name", "string", "Filter by project name"),
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 20, max 20)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val pageSize = args.intArg("page_size")?.coerceIn(1, 20) ?: 20
        val result = repository.getProjects(
            page = args.pageArg(),
            pageSize = pageSize,
            name = args.stringArg("name")
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "projects" to networkJson.encodeToString(result.items)
            ))
        )
    }
}

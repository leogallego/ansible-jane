package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.ControllerReadOnlyRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListTeamsLocalTool(
    private val repository: ControllerReadOnlyRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_teams",
        description = "List teams in AAP with optional search",
        parametersSchema = buildToolSchema(
            Triple("search", "string", "Search term to filter by team name"),
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 25, max 25)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val pageSize = args.intArg("page_size")?.coerceIn(1, 25) ?: 25
        val result = repository.getTeams(
            page = args.pageArg(),
            pageSize = pageSize,
            search = args.stringArg("search")
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "teams" to networkJson.encodeToString(result.items)
            ))
        )
    }
}

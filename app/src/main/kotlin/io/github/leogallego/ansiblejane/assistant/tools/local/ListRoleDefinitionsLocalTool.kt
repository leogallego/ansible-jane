package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.ControllerReadOnlyRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListRoleDefinitionsLocalTool(
    private val repository: ControllerReadOnlyRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_role_definitions",
        description = "List RBAC role definitions with their permissions",
        parametersSchema = buildToolSchema(
            Triple("search", "string", "Search term to filter role definitions"),
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 25, max 25)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val pageSize = args.intArg("page_size")?.coerceIn(1, 25) ?: 25
        val result = repository.getRoleDefinitions(
            page = args.pageArg(),
            pageSize = pageSize,
            search = args.stringArg("search")
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "role_definitions" to networkJson.encodeToString(result.items)
            ))
        )
    }
}

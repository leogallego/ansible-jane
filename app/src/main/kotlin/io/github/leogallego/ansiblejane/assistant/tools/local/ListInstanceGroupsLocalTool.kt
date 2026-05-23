package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.InfrastructureRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListInstanceGroupsLocalTool(
    private val repository: InfrastructureRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_instance_groups",
        description = "List instance groups with capacity and container group info",
        parametersSchema = buildToolSchema(
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 25, max 25)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val pageSize = args.intArg("page_size")?.coerceIn(1, 25) ?: 25
        val result = repository.getInstanceGroups(
            page = args.pageArg(),
            pageSize = pageSize
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "instance_groups" to networkJson.encodeToString(result.instanceGroups)
            ))
        )
    }
}

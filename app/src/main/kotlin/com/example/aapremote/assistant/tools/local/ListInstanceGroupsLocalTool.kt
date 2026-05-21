package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.InfrastructureRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

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
    override suspend fun execute(args: Map<String, Any>): ToolResult = executeSafely {
        val pageSize = (args["page_size"] as? Number)?.toInt()?.coerceIn(1, 25) ?: 25
        val result = repository.getInstanceGroups(
            page = (args["page"] as? Number)?.toInt() ?: 1,
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

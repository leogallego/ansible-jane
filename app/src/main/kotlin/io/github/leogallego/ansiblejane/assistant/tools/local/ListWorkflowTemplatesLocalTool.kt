package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.WorkflowRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListWorkflowTemplatesLocalTool(
    private val repository: WorkflowRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_workflow_templates",
        description = "List workflow job templates with optional search and label filter",
        parametersSchema = buildToolSchema(
            Triple("search", "string", "Search term to filter by name"),
            Triple("labels", "string", "Filter by label name (case-insensitive contains)"),
            Triple("page", "integer", "Page number (default 1)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val result = repository.getWorkflowTemplates(
            page = args.pageArg(),
            search = args.stringArg("search"),
            labelFilter = args.stringArg("labels")
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "templates" to networkJson.encodeToString(result.templates)
            ))
        )
    }
}

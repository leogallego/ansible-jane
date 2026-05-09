package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.WorkflowRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

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
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val result = repository.getWorkflowTemplates(
                page = (args["page"] as? Number)?.toInt() ?: 1,
                search = args["search"] as? String,
                labelFilter = args["labels"] as? String
            ).getOrThrow()
            ToolResult(
                success = true,
                data = networkJson.encodeToString(mapOf(
                    "count" to result.totalCount.toString(),
                    "templates" to networkJson.encodeToString(result.templates)
                ))
            )
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}

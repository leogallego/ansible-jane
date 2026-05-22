package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.TemplateRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListJobTemplatesLocalTool(
    private val repository: TemplateRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_job_templates",
        description = "List job templates with optional search and label filter",
        parametersSchema = buildToolSchema(
            Triple("search", "string", "Search term to filter templates by name"),
            Triple("labels", "string", "Filter by label name (case-insensitive contains)"),
            Triple("page", "integer", "Page number (default 1)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val result = repository.getTemplates(
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

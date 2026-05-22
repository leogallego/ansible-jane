package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.JobRepository
import com.example.aapremote.model.JobStatus
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class ListJobsLocalTool(
    private val repository: JobRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_jobs",
        description = "List recent jobs with optional status filter",
        parametersSchema = buildToolSchema(
            Triple("status", "string", "Filter by status: pending, waiting, running, successful, failed, error, canceled"),
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 10, max 20)"),
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val statusFilter = args.stringArg("status")?.let { statusStr ->
            JobStatus.entries.filter { it.apiValue == statusStr }.toSet()
        } ?: emptySet()
        val pageSize = args.intArg("page_size")?.coerceIn(1, 20) ?: 10
        val result = repository.getRecentJobs(
            page = args.pageArg(),
            pageSize = pageSize,
            statusFilters = statusFilter
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "jobs" to networkJson.encodeToString(result.jobs)
            ))
        )
    }
}

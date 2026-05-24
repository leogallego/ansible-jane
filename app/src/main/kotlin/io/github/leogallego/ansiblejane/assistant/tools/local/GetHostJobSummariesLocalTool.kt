package io.github.leogallego.ansiblejane.assistant.tools.local

import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.data.HostRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

class GetHostJobSummariesLocalTool(
    private val repository: HostRepository
) : LocalTool(
    spec = ToolSpec(
        name = "get_host_job_summaries",
        description = "Get job execution summaries for a specific host (pass/fail history)",
        parametersSchema = buildToolSchema(
            Triple("id", "integer", "Host ID"),
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 10, max 20)"),
            required = listOf("id")
        )
    )
) {
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val id = args.intArg("id") ?: return@executeSafely ToolResult(
            success = false, data = "Error: 'id' parameter is required"
        )
        val pageSize = args.intArg("page_size")?.coerceIn(1, 20) ?: 10
        val result = repository.getHostJobSummaries(
            hostId = id,
            page = args.pageArg(),
            pageSize = pageSize
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "job_host_summaries" to networkJson.encodeToString(result.summaries)
            ))
        )
    }
}

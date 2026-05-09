package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.ProjectRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

class ListExecutionEnvironmentsLocalTool(
    private val repository: ProjectRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_execution_environments",
        description = "List execution environments with image and organization info",
        parametersSchema = buildToolSchema(
            Triple("page", "integer", "Page number (default 1)"),
            Triple("page_size", "integer", "Results per page (default 25, max 25)"),
        )
    )
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val pageSize = (args["page_size"] as? Number)?.toInt()?.coerceIn(1, 25) ?: 25
            val result = repository.getExecutionEnvironments(
                page = (args["page"] as? Number)?.toInt() ?: 1,
                pageSize = pageSize
            ).getOrThrow()
            ToolResult(
                success = true,
                data = networkJson.encodeToString(mapOf(
                    "count" to result.totalCount.toString(),
                    "execution_environments" to networkJson.encodeToString(result.executionEnvironments)
                ))
            )
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}

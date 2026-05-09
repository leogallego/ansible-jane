package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.ScheduleRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

class ListSchedulesLocalTool(
    private val repository: ScheduleRepository
) : LocalTool(
    spec = ToolSpec(
        name = "list_schedules",
        description = "List job schedules with their enabled/disabled status",
        parametersSchema = buildToolSchema(
            Triple("page", "integer", "Page number (default 1)"),
        )
    )
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val result = repository.getSchedules(
                page = (args["page"] as? Number)?.toInt() ?: 1
            ).getOrThrow()
            ToolResult(
                success = true,
                data = networkJson.encodeToString(mapOf(
                    "count" to result.totalCount.toString(),
                    "schedules" to networkJson.encodeToString(result.schedules)
                ))
            )
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}

package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.ScheduleRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString

class ToggleScheduleLocalTool(
    private val repository: ScheduleRepository
) : LocalTool(
    spec = ToolSpec(
        name = "toggle_schedule",
        description = "Enable or disable a schedule by ID",
        parametersSchema = buildToolSchema(
            Triple("schedule_id", "integer", "ID of the schedule"),
            Triple("enabled", "boolean", "true to enable, false to disable"),
            required = listOf("schedule_id", "enabled")
        )
    ),
    destructive = true
) {
    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val scheduleId = (args["schedule_id"] as? Number)?.toInt()
                ?: return ToolResult(success = false, data = "schedule_id is required", errorType = ErrorType.NOT_FOUND)
            val enabled = args["enabled"] as? Boolean
                ?: return ToolResult(success = false, data = "enabled is required", errorType = ErrorType.NOT_FOUND)
            val schedule = repository.toggleSchedule(scheduleId, enabled).getOrThrow()
            ToolResult(
                success = true,
                data = networkJson.encodeToString(schedule)
            )
        } catch (e: Exception) {
            ToolResult(success = false, data = "Error: ${e.message}", errorType = ErrorType.SERVER_ERROR)
        }
    }
}

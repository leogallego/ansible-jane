package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.ErrorType
import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.ScheduleRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

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
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val scheduleId = args.intArg("schedule_id")
            ?: return@executeSafely ToolResult(success = false, data = "schedule_id is required", errorType = ErrorType.NOT_FOUND)
        val enabled = args.booleanArg("enabled")
            ?: return@executeSafely ToolResult(success = false, data = "enabled is required", errorType = ErrorType.NOT_FOUND)
        val schedule = repository.toggleSchedule(scheduleId, enabled).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(schedule)
        )
    }
}

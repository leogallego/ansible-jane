package com.example.aapremote.assistant.tools.local

import com.example.aapremote.assistant.tools.LocalTool
import com.example.aapremote.assistant.tools.ToolResult
import com.example.aapremote.assistant.tools.ToolSpec
import com.example.aapremote.data.ScheduleRepository
import com.example.aapremote.network.networkJson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject

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
    override suspend fun execute(args: JsonObject): ToolResult = executeSafely {
        val result = repository.getSchedules(
            page = args.pageArg()
        ).getOrThrow()
        ToolResult(
            success = true,
            data = networkJson.encodeToString(mapOf(
                "count" to result.totalCount.toString(),
                "schedules" to networkJson.encodeToString(result.schedules)
            ))
        )
    }
}

package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.ScheduleRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ListSchedulesLocalTool(
    private val repository: ScheduleRepository
) : AapLocalTool<ListSchedulesLocalTool.Args>(
    typeToken<Args>(),
    Args.serializer(),
    name = "list_schedules",
    description = "List job schedules with their enabled/disabled status"
) {
    @Serializable
    data class Args(
        @property:LLMDescription("Page number (default 1)")
        val page: Int = 1,
    )

    override suspend fun execute(args: Args): String {
        val result = repository.getSchedules(
            page = args.page.coerceAtLeast(1)
        ).getOrThrow()
        return buildJsonObject {
            put("count", result.totalCount)
            put("schedules", networkJson.encodeToJsonElement(result.schedules))
        }.toString()
    }
}

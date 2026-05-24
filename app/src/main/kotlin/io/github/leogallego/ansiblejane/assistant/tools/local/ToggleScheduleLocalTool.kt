package io.github.leogallego.ansiblejane.assistant.tools.local

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.serialization.typeToken
import io.github.leogallego.ansiblejane.assistant.tools.AapLocalTool
import io.github.leogallego.ansiblejane.data.ScheduleRepository
import io.github.leogallego.ansiblejane.network.networkJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

class ToggleScheduleLocalTool(
    private val repository: ScheduleRepository
) : AapLocalTool<ToggleScheduleLocalTool.Args>(
    typeToken<Args>(), Args.serializer(),
    name = "toggle_schedule",
    description = "Enable or disable a schedule by ID",
    destructive = true
) {
    @Serializable
    data class Args(
        @property:LLMDescription("ID of the schedule")
        @SerialName("schedule_id")
        val scheduleId: Int,
        @property:LLMDescription("true to enable, false to disable")
        val enabled: Boolean
    )

    override suspend fun execute(args: Args): String {
        val schedule = repository.toggleSchedule(args.scheduleId, args.enabled).getOrThrow()
        return networkJson.encodeToString(schedule)
    }
}

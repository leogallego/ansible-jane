package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.data.IScheduleRepository
import io.github.leogallego.ansiblejane.data.SchedulesResult
import io.github.leogallego.ansiblejane.model.Schedule

class FakeScheduleRepository : IScheduleRepository {
    var schedules = listOf<Schedule>()
    var shouldFail = false
    var failureException: Exception = RuntimeException("Test error")
    var toggleResult: Schedule? = null
    var toggleShouldFail = false
    var toggleFailureException: Exception = RuntimeException("Toggle failed")
    var hasMore = false

    override suspend fun getSchedules(page: Int, pageSize: Int): Result<SchedulesResult> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(SchedulesResult(schedules, hasMore = hasMore, totalCount = schedules.size))
    }

    override suspend fun toggleSchedule(id: Int, enabled: Boolean): Result<Schedule> {
        if (toggleShouldFail) return Result.failure(toggleFailureException)
        if (shouldFail) return Result.failure(failureException)
        val schedule = toggleResult ?: schedules.find { it.id == id }?.copy(enabled = enabled)
        return if (schedule != null) Result.success(schedule) else Result.failure(RuntimeException("Not found"))
    }
}

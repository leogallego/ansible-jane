package com.example.aapremote.data

import com.example.aapremote.model.Schedule
import com.example.aapremote.network.AapApiProvider

class ScheduleRepository(private val apiProvider: AapApiProvider) : IScheduleRepository {

    override suspend fun getSchedules(page: Int, pageSize: Int): Result<SchedulesResult> {
        return try {
            val response = apiProvider.getApiService().getSchedules(
                page = page,
                pageSize = pageSize
            )
            Result.success(
                SchedulesResult(
                    schedules = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleSchedule(id: Int, enabled: Boolean): Result<Schedule> {
        return try {
            val updated = apiProvider.getApiService().toggleSchedule(id, mapOf("enabled" to enabled))
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class SchedulesResult(
    val schedules: List<Schedule>,
    val hasMore: Boolean,
    val totalCount: Int
)

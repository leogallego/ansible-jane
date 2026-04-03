package com.example.aapremote.data

import com.example.aapremote.model.Schedule
import com.example.aapremote.network.AapApiService

class ScheduleRepository(private val apiService: AapApiService) {

    suspend fun getSchedules(page: Int = 1, pageSize: Int = 20): Result<SchedulesResult> {
        return try {
            val response = apiService.getSchedules(
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

    suspend fun toggleSchedule(id: Int, enabled: Boolean): Result<Schedule> {
        return try {
            val updated = apiService.toggleSchedule(id, mapOf("enabled" to enabled))
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

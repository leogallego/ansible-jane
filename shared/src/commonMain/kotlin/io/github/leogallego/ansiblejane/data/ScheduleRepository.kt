package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Schedule
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlin.coroutines.cancellation.CancellationException

class ScheduleRepository(private val apiProvider: IAapApiProvider) : IScheduleRepository {

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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun toggleSchedule(id: Int, enabled: Boolean): Result<Schedule> {
        return try {
            val updated = apiProvider.getApiService().toggleSchedule(id, mapOf("enabled" to enabled))
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

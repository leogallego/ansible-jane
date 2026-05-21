package com.example.aapremote.data

import com.example.aapremote.model.Schedule

interface IScheduleRepository {
    suspend fun getSchedules(page: Int = 1, pageSize: Int = 20): Result<SchedulesResult>
    suspend fun toggleSchedule(id: Int, enabled: Boolean): Result<Schedule>
}

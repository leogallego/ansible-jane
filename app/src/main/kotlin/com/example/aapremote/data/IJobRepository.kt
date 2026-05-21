package com.example.aapremote.data

import com.example.aapremote.model.Job
import com.example.aapremote.model.JobStatus
import kotlinx.coroutines.flow.Flow

interface IJobRepository {
    suspend fun getJobStatus(jobId: Int): Result<Job>
    fun pollJobStatus(jobId: Int): Flow<Job>
    suspend fun getJobStdout(jobId: Int): Result<String>
    suspend fun getRecentJobs(
        page: Int = 1,
        pageSize: Int = 20,
        statusFilters: Set<JobStatus> = emptySet()
    ): Result<RecentJobsResult>
}

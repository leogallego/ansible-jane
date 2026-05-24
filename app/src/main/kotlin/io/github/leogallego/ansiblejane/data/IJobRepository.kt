package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Job
import io.github.leogallego.ansiblejane.model.JobStatus
import kotlinx.coroutines.flow.Flow

interface IJobRepository {
    suspend fun getJobStatus(jobId: Int): Result<Job>
    fun pollJobStatus(jobId: Int): Flow<Job>
    suspend fun getJobStdout(jobId: Int): Result<String>
    suspend fun getRecentJobs(
        page: Int = 1,
        pageSize: Int = 20,
        statusFilters: Set<JobStatus> = emptySet(),
        search: String? = null
    ): Result<RecentJobsResult>
}

package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Job
import io.github.leogallego.ansiblejane.model.JobStatus
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class JobRepository(private val apiProvider: IAapApiProvider) : IJobRepository {

    override suspend fun getJobStatus(jobId: Int): Result<Job> {
        return try {
            Result.success(apiProvider.getApiService().getJob(jobId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun pollJobStatus(jobId: Int): Flow<Job> = flow {
        while (true) {
            try {
                val job = apiProvider.getApiService().getJob(jobId)
                emit(job)
                if (job.status.isTerminal) break
                delay(5000)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    override suspend fun getJobStdout(jobId: Int): Result<String> {
        return try {
            val response = apiProvider.getApiService().getJobStdout(jobId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRecentJobs(
        page: Int,
        pageSize: Int,
        statusFilters: Set<JobStatus>,
        search: String?,
        createdAfter: String?
    ): Result<RecentJobsResult> {
        return try {
            val orStatus = if (statusFilters.size > 1) {
                statusFilters.map { it.apiValue }
            } else {
                null
            }
            val singleStatus = if (statusFilters.size == 1) {
                statusFilters.first().apiValue
            } else {
                null
            }
            val response = apiProvider.getApiService().getJobs(
                orderBy = "-created",
                pageSize = pageSize,
                page = page,
                status = singleStatus,
                orStatus = orStatus,
                search = search,
                createdAfter = createdAfter
            )
            Result.success(
                RecentJobsResult(
                    jobs = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class RecentJobsResult(
    val jobs: List<Job>,
    val hasMore: Boolean,
    val totalCount: Int
)

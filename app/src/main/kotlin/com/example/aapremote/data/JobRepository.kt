package com.example.aapremote.data

import com.example.aapremote.model.Job
import com.example.aapremote.network.AapApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class JobRepository(private val apiService: AapApiService) {

    suspend fun getJobStatus(jobId: Int): Result<Job> {
        return try {
            Result.success(apiService.getJob(jobId))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get job status: ${e.message}"))
        }
    }

    fun pollJobStatus(jobId: Int): Flow<Job> = flow {
        while (true) {
            try {
                val job = apiService.getJob(jobId)
                emit(job)
                if (job.status.isTerminal) break
                delay(5000)
            } catch (e: Exception) {
                throw Exception("Failed to poll job status: ${e.message}")
            }
        }
    }

    suspend fun getJobStdout(jobId: Int): Result<String> {
        return try {
            val response = apiService.getJobStdout(jobId)
            Result.success(response.string())
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get job output: ${e.message}"))
        }
    }

    suspend fun getRecentJobs(page: Int = 1, pageSize: Int = 20): Result<RecentJobsResult> {
        return try {
            val response = apiService.getJobs(
                orderBy = "-created",
                pageSize = pageSize,
                page = page
            )
            Result.success(
                RecentJobsResult(
                    jobs = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(Exception("Failed to load recent jobs: ${e.message}"))
        }
    }
}

data class RecentJobsResult(
    val jobs: List<Job>,
    val hasMore: Boolean,
    val totalCount: Int
)

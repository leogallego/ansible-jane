package com.example.aapremote.fakes

import com.example.aapremote.data.IJobRepository
import com.example.aapremote.data.RecentJobsResult
import com.example.aapremote.model.Job
import com.example.aapremote.model.JobStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeJobRepository : IJobRepository {
    var jobs = listOf<Job>()
    var jobStdout = ""
    var shouldFail = false
    var stdoutShouldFail = false
    var failureException: Exception = RuntimeException("Test error")
    var polledJob: Job? = null
    var hasMore = false
    var lastRequestedPage = 0

    override suspend fun getJobStatus(jobId: Int): Result<Job> {
        if (shouldFail) return Result.failure(failureException)
        val job = jobs.find { it.id == jobId } ?: polledJob
        return if (job != null) Result.success(job)
        else Result.failure(RuntimeException("Job not found"))
    }

    override fun pollJobStatus(jobId: Int): Flow<Job> = flow {
        if (shouldFail) throw failureException
        polledJob?.let { emit(it) }
    }

    override suspend fun getJobStdout(jobId: Int): Result<String> {
        if (stdoutShouldFail) return Result.failure(failureException)
        return Result.success(jobStdout)
    }

    override suspend fun getRecentJobs(page: Int, pageSize: Int, statusFilters: Set<JobStatus>): Result<RecentJobsResult> {
        lastRequestedPage = page
        if (shouldFail) return Result.failure(failureException)
        return Result.success(RecentJobsResult(jobs, hasMore = hasMore, totalCount = jobs.size))
    }
}

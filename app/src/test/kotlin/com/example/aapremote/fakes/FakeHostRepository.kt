package com.example.aapremote.fakes

import com.example.aapremote.data.HostListResult
import com.example.aapremote.data.IHostRepository
import com.example.aapremote.data.JobHostSummaryResult
import com.example.aapremote.model.Host
import com.example.aapremote.model.JobHostSummary
import kotlinx.serialization.json.JsonElement

class FakeHostRepository : IHostRepository {
    var hosts = listOf<Host>()
    var inventoryHosts = listOf<Host>()
    var hostFacts = mapOf<String, JsonElement>()
    var jobSummaries = listOf<JobHostSummary>()
    var shouldFail = false
    var failureException: Exception = RuntimeException("Test error")
    var hasMore = false
    var inventoryHostsHasMore = false

    override suspend fun getAllHosts(page: Int, pageSize: Int, search: String?): Result<HostListResult> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(HostListResult(hosts, hasMore = hasMore, totalCount = hosts.size))
    }

    override suspend fun getInventoryHosts(inventoryId: Int, page: Int, pageSize: Int, search: String?): Result<HostListResult> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(HostListResult(inventoryHosts, hasMore = inventoryHostsHasMore, totalCount = inventoryHosts.size))
    }

    override suspend fun getHostFacts(hostId: Int): Result<Map<String, JsonElement>> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(hostFacts)
    }

    override suspend fun getHostJobSummaries(hostId: Int, page: Int, pageSize: Int): Result<JobHostSummaryResult> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(JobHostSummaryResult(jobSummaries, hasMore = false, totalCount = jobSummaries.size))
    }
}

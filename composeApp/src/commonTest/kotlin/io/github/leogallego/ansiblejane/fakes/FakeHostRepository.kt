package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.data.HostListResult
import io.github.leogallego.ansiblejane.data.IHostRepository
import io.github.leogallego.ansiblejane.data.JobHostSummaryResult
import io.github.leogallego.ansiblejane.model.Host
import io.github.leogallego.ansiblejane.model.JobHostSummary
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

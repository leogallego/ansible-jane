package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Host
import io.github.leogallego.ansiblejane.model.JobHostSummary
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.json.JsonElement

class HostRepository(private val apiProvider: IAapApiProvider) : IHostRepository {

    override suspend fun getAllHosts(
        page: Int,
        pageSize: Int,
        search: String?
    ): Result<HostListResult> {
        return try {
            val response = apiProvider.getApiService().getHosts(
                page = page,
                pageSize = pageSize,
                search = search
            )
            Result.success(
                HostListResult(
                    hosts = response.results,
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

    override suspend fun getInventoryHosts(
        inventoryId: Int,
        page: Int,
        pageSize: Int,
        search: String?
    ): Result<HostListResult> {
        return try {
            val response = apiProvider.getApiService().getInventoryHosts(
                id = inventoryId,
                page = page,
                pageSize = pageSize,
                search = search
            )
            Result.success(
                HostListResult(
                    hosts = response.results,
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

    override suspend fun getHostFacts(hostId: Int): Result<Map<String, JsonElement>> {
        return try {
            Result.success(apiProvider.getApiService().getHostFacts(hostId))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHostJobSummaries(
        hostId: Int,
        page: Int,
        pageSize: Int
    ): Result<JobHostSummaryResult> {
        return try {
            val response = apiProvider.getApiService().getHostJobSummaries(
                id = hostId,
                page = page,
                pageSize = pageSize
            )
            Result.success(
                JobHostSummaryResult(
                    summaries = response.results,
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
}

package com.example.aapremote.data

import com.example.aapremote.model.Host
import com.example.aapremote.model.JobHostSummary
import com.example.aapremote.network.AapApiProvider
import kotlinx.serialization.json.JsonElement

class HostRepository(private val apiProvider: AapApiProvider) {

    suspend fun getAllHosts(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getInventoryHosts(
        inventoryId: Int,
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHostFacts(hostId: Int): Result<Map<String, JsonElement>> {
        return try {
            Result.success(apiProvider.getApiService().getHostFacts(hostId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHostJobSummaries(
        hostId: Int,
        page: Int = 1,
        pageSize: Int = 20
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class HostListResult(
    val hosts: List<Host>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class JobHostSummaryResult(
    val summaries: List<JobHostSummary>,
    val hasMore: Boolean,
    val totalCount: Int
)

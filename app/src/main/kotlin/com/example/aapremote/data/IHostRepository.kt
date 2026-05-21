package com.example.aapremote.data

import kotlinx.serialization.json.JsonElement

interface IHostRepository {
    suspend fun getAllHosts(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<HostListResult>

    suspend fun getInventoryHosts(
        inventoryId: Int,
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<HostListResult>

    suspend fun getHostFacts(hostId: Int): Result<Map<String, JsonElement>>

    suspend fun getHostJobSummaries(
        hostId: Int,
        page: Int = 1,
        pageSize: Int = 20
    ): Result<JobHostSummaryResult>
}

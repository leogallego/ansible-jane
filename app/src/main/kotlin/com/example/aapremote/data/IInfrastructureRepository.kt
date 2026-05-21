package com.example.aapremote.data

import com.example.aapremote.model.Instance
import com.example.aapremote.model.PingResponse
import kotlinx.serialization.json.JsonElement

interface IInfrastructureRepository {
    suspend fun getInstances(
        page: Int = 1,
        pageSize: Int = 25
    ): Result<InstanceListResult>

    suspend fun getInstance(id: Int): Result<Instance>

    suspend fun getInstanceGroups(
        page: Int = 1,
        pageSize: Int = 25
    ): Result<InstanceGroupListResult>

    suspend fun ping(): Result<PingResponse>
    suspend fun getMeshTopology(): Result<JsonElement>
}

package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Instance
import io.github.leogallego.ansiblejane.model.PingResponse
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

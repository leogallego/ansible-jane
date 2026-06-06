package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Instance
import io.github.leogallego.ansiblejane.model.InstanceGroup
import io.github.leogallego.ansiblejane.model.PingResponse
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlinx.serialization.json.JsonElement

class InfrastructureRepository(private val apiProvider: IAapApiProvider) : IInfrastructureRepository {

    override suspend fun getInstances(
        page: Int,
        pageSize: Int
    ): Result<InstanceListResult> {
        return try {
            val response = apiProvider.getApiService().getInstances(
                page = page,
                pageSize = pageSize
            )
            Result.success(
                InstanceListResult(
                    instances = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInstance(id: Int): Result<Instance> {
        return try {
            Result.success(apiProvider.getApiService().getInstance(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getInstanceGroups(
        page: Int,
        pageSize: Int
    ): Result<InstanceGroupListResult> {
        return try {
            val response = apiProvider.getApiService().getInstanceGroups(
                page = page,
                pageSize = pageSize
            )
            Result.success(
                InstanceGroupListResult(
                    instanceGroups = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun ping(): Result<PingResponse> {
        return try {
            Result.success(apiProvider.getApiService().ping())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMeshTopology(): Result<JsonElement> {
        return try {
            Result.success(apiProvider.getApiService().getMeshTopology())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class InstanceListResult(
    val instances: List<Instance>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class InstanceGroupListResult(
    val instanceGroups: List<InstanceGroup>,
    val hasMore: Boolean,
    val totalCount: Int
)

package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.EdaActivation
import io.github.leogallego.ansiblejane.network.IAapApiProvider

class EdaActivationRepository(private val apiProvider: IAapApiProvider) : IEdaActivationRepository {

    override suspend fun getActivations(
        page: Int,
        pageSize: Int
    ): Result<EdaActivationListResult> {
        return try {
            val response = apiProvider.getEdaApiService().getActivations(
                page = page,
                pageSize = pageSize
            )
            Result.success(
                EdaActivationListResult(
                    activations = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActivation(id: Int): Result<EdaActivation> {
        return try {
            Result.success(apiProvider.getEdaApiService().getActivation(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class EdaActivationListResult(
    val activations: List<EdaActivation>,
    val hasMore: Boolean,
    val totalCount: Int
)

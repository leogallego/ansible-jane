package com.example.aapremote.data

import com.example.aapremote.model.EdaActivation
import com.example.aapremote.network.AapApiProvider

class EdaActivationRepository(private val apiProvider: AapApiProvider) {

    suspend fun getActivations(
        page: Int = 1,
        pageSize: Int = 20
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

    suspend fun getActivation(id: Int): Result<EdaActivation> {
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

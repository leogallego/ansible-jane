package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.EdaActivation

interface IEdaActivationRepository {
    suspend fun getActivations(
        page: Int = 1,
        pageSize: Int = 20
    ): Result<EdaActivationListResult>

    suspend fun getActivation(id: Int): Result<EdaActivation>
}

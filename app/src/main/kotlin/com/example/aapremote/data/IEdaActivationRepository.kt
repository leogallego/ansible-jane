package com.example.aapremote.data

import com.example.aapremote.model.EdaActivation

interface IEdaActivationRepository {
    suspend fun getActivations(
        page: Int = 1,
        pageSize: Int = 20
    ): Result<EdaActivationListResult>

    suspend fun getActivation(id: Int): Result<EdaActivation>
}

package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Credential

interface ICredentialRepository {
    suspend fun getCredentials(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<CredentialListResult>

    suspend fun getCredential(id: Int): Result<Credential>
}

package com.example.aapremote.data

import com.example.aapremote.model.Credential

interface ICredentialRepository {
    suspend fun getCredentials(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<CredentialListResult>

    suspend fun getCredential(id: Int): Result<Credential>
}

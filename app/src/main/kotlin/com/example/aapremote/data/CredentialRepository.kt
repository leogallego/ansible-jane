package com.example.aapremote.data

import com.example.aapremote.model.Credential
import com.example.aapremote.network.AapApiProvider

class CredentialRepository(private val apiProvider: AapApiProvider) {

    suspend fun getCredentials(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<CredentialListResult> {
        return try {
            val response = apiProvider.getApiService().getCredentials(
                page = page,
                pageSize = pageSize,
                search = search
            )
            Result.success(
                CredentialListResult(
                    credentials = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCredential(id: Int): Result<Credential> {
        return try {
            Result.success(apiProvider.getApiService().getCredential(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class CredentialListResult(
    val credentials: List<Credential>,
    val hasMore: Boolean,
    val totalCount: Int
)

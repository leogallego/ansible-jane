package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.Credential
import io.github.leogallego.ansiblejane.network.IAapApiProvider

class CredentialRepository(private val apiProvider: IAapApiProvider) : ICredentialRepository {

    override suspend fun getCredentials(
        page: Int,
        pageSize: Int,
        search: String?
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

    override suspend fun getCredential(id: Int): Result<Credential> {
        return try {
            Result.success(apiProvider.getApiService().getCredential(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

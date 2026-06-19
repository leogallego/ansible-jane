package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.*
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlin.coroutines.cancellation.CancellationException

class EdaReadOnlyRepository(private val apiProvider: IAapApiProvider) {

    suspend fun getRulebooks(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): Result<ListResult<EdaRulebook>> = runEdaPaginated {
        apiProvider.getEdaApiService().getRulebooks(page, pageSize, name)
    }

    suspend fun getDecisionEnvironments(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): Result<ListResult<EdaDecisionEnvironment>> = runEdaPaginated {
        apiProvider.getEdaApiService().getDecisionEnvironments(page, pageSize, name)
    }

    suspend fun getProjects(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): Result<ListResult<EdaProject>> = runEdaPaginated {
        apiProvider.getEdaApiService().getProjects(page, pageSize, name)
    }

    suspend fun getCredentials(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): Result<ListResult<EdaCredential>> = runEdaPaginated {
        apiProvider.getEdaApiService().getCredentials(page, pageSize, name)
    }

    suspend fun getCredentialTypes(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): Result<ListResult<EdaCredentialType>> = runEdaPaginated {
        apiProvider.getEdaApiService().getCredentialTypes(page, pageSize, name)
    }

    suspend fun getEventStreams(
        page: Int = 1,
        pageSize: Int = 20,
        name: String? = null
    ): Result<ListResult<EdaEventStream>> = runEdaPaginated {
        apiProvider.getEdaApiService().getEventStreams(page, pageSize, name)
    }

    suspend fun getUsers(
        page: Int = 1,
        pageSize: Int = 20
    ): Result<ListResult<EdaUser>> = runEdaPaginated {
        apiProvider.getEdaApiService().getUsers(page, pageSize)
    }

    private inline fun <T> runEdaPaginated(
        block: () -> PaginatedResponse<T>
    ): Result<ListResult<T>> = try {
        val response = block()
        Result.success(
            ListResult(
                items = response.results,
                hasMore = response.next != null,
                totalCount = response.count
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
}

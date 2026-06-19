package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.*
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlin.coroutines.cancellation.CancellationException

class PlatformRepository(private val apiProvider: IAapApiProvider) {

    suspend fun getOrganizations(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<PlatformOrganization>> = runPaginated {
        apiProvider.getPlatformApiService().getOrganizations(page, pageSize, search)
    }

    suspend fun getUsers(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<PlatformUser>> = runPaginated {
        apiProvider.getPlatformApiService().getUsers(page, pageSize, search)
    }

    suspend fun getTeams(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<PlatformTeam>> = runPaginated {
        apiProvider.getPlatformApiService().getTeams(page, pageSize, search)
    }

    suspend fun getRoleDefinitions(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<PlatformRoleDefinition>> = runPaginated {
        apiProvider.getPlatformApiService().getRoleDefinitions(page, pageSize, search)
    }

    suspend fun getAuthenticators(
        page: Int = 1,
        pageSize: Int = 25
    ): Result<ListResult<PlatformAuthenticator>> = runPaginated {
        apiProvider.getPlatformApiService().getAuthenticators(page, pageSize)
    }

    suspend fun getServices(
        page: Int = 1,
        pageSize: Int = 25
    ): Result<ListResult<PlatformService>> = runPaginated {
        apiProvider.getPlatformApiService().getServices(page, pageSize)
    }

    suspend fun getServiceClusters(
        page: Int = 1,
        pageSize: Int = 25
    ): Result<ListResult<PlatformServiceCluster>> = runPaginated {
        apiProvider.getPlatformApiService().getServiceClusters(page, pageSize)
    }

    private inline fun <T> runPaginated(
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

package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.*
import io.github.leogallego.ansiblejane.network.IAapApiProvider

class HubRepository(private val apiProvider: IAapApiProvider) {

    suspend fun getCollections(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): Result<ListResult<HubCollection>> = runV3Paginated {
        apiProvider.getHubApiService().getCollections(page, pageSize, search)
    }

    suspend fun getNamespaces(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): Result<ListResult<HubNamespace>> = runV3Paginated {
        apiProvider.getHubApiService().getNamespaces(page, pageSize, search)
    }

    suspend fun getCollectionVersions(
        page: Int = 1,
        pageSize: Int = 20,
        status: String? = null
    ): Result<ListResult<HubCollectionVersion>> = runV3Paginated {
        apiProvider.getHubApiService().getCollectionVersions(page, pageSize, status)
    }

    suspend fun getEeRepositories(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): Result<ListResult<HubEeRepository>> = runV3Paginated {
        apiProvider.getHubApiService().getEeRepositories(page, pageSize, search)
    }

    suspend fun getEeRegistries(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): Result<ListResult<HubEeRegistry>> = runV3Paginated {
        apiProvider.getHubApiService().getEeRegistries(page, pageSize, search)
    }

    suspend fun getUsers(
        page: Int = 1,
        pageSize: Int = 20
    ): Result<ListResult<HubUser>> = runV3Paginated {
        apiProvider.getHubApiService().getUsers(page, pageSize)
    }

    suspend fun getGroups(
        page: Int = 1,
        pageSize: Int = 20
    ): Result<ListResult<HubGroup>> = runV3Paginated {
        apiProvider.getHubApiService().getGroups(page, pageSize)
    }

    suspend fun getRoleDefinitions(
        page: Int = 1,
        pageSize: Int = 20
    ): Result<ListResult<HubRoleDefinition>> = runPaginated {
        apiProvider.getHubApiService().getRoleDefinitions(page, pageSize)
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
    } catch (e: Exception) {
        Result.failure(e)
    }

    private inline fun <T> runV3Paginated(
        block: () -> GalaxyV3Response<T>
    ): Result<ListResult<T>> = try {
        val response = block()
        Result.success(
            ListResult(
                items = response.data,
                hasMore = response.links.next != null,
                totalCount = response.meta.count
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }
}

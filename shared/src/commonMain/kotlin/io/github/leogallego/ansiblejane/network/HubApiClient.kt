package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class HubApiClient(private val client: HttpClient) {

    suspend fun getCollections(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): GalaxyV3Response<HubCollection> = client.get("v3/plugin/ansible/content/published/collections/index/") {
        parameter("offset", (page - 1) * pageSize)
        parameter("limit", pageSize)
        search?.let { parameter("keywords", it) }
    }.body()

    suspend fun getNamespaces(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): GalaxyV3Response<HubNamespace> = client.get("v3/namespaces/") {
        parameter("offset", (page - 1) * pageSize)
        parameter("limit", pageSize)
        search?.let { parameter("keywords", it) }
    }.body()

    suspend fun getCollectionVersions(
        page: Int = 1,
        pageSize: Int = 20,
        status: String? = null
    ): GalaxyV3Response<HubCollectionVersion> = client.get("_ui/v1/collection-versions/") {
        parameter("offset", (page - 1) * pageSize)
        parameter("limit", pageSize)
        status?.let { parameter("repository_label", it) }
    }.body()

    suspend fun getEeRepositories(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): GalaxyV3Response<HubEeRepository> = client.get("v3/plugin/execution-environments/repositories/") {
        parameter("offset", (page - 1) * pageSize)
        parameter("limit", pageSize)
        search?.let { parameter("name__icontains", it) }
    }.body()

    suspend fun getEeRegistries(
        page: Int = 1,
        pageSize: Int = 20,
        search: String? = null
    ): GalaxyV3Response<HubEeRegistry> = client.get("_ui/v1/execution-environments/registries/") {
        parameter("offset", (page - 1) * pageSize)
        parameter("limit", pageSize)
        search?.let { parameter("name__icontains", it) }
    }.body()

    suspend fun getUsers(
        page: Int = 1,
        pageSize: Int = 20
    ): GalaxyV3Response<HubUser> = client.get("_ui/v1/users/") {
        parameter("offset", (page - 1) * pageSize)
        parameter("limit", pageSize)
    }.body()

    suspend fun getGroups(
        page: Int = 1,
        pageSize: Int = 20
    ): GalaxyV3Response<HubGroup> = client.get("_ui/v1/groups/") {
        parameter("offset", (page - 1) * pageSize)
        parameter("limit", pageSize)
    }.body()

    suspend fun getRoleDefinitions(
        page: Int = 1,
        pageSize: Int = 20
    ): PaginatedResponse<HubRoleDefinition> = client.get("_ui/v2/role_definitions/") {
        parameter("page", page)
        parameter("limit", pageSize)
    }.body()
}

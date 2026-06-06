package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class PlatformApiClient(private val client: HttpClient) {

    suspend fun getOrganizations(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): PaginatedResponse<PlatformOrganization> = client.get("organizations/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
    }.body()

    suspend fun getUsers(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): PaginatedResponse<PlatformUser> = client.get("users/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
    }.body()

    suspend fun getTeams(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): PaginatedResponse<PlatformTeam> = client.get("teams/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
    }.body()

    suspend fun getRoleDefinitions(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): PaginatedResponse<PlatformRoleDefinition> = client.get("role_definitions/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
    }.body()

    suspend fun getAuthenticators(
        page: Int = 1,
        pageSize: Int = 25
    ): PaginatedResponse<PlatformAuthenticator> = client.get("authenticators/") {
        parameter("page", page)
        parameter("page_size", pageSize)
    }.body()

    suspend fun getServices(
        page: Int = 1,
        pageSize: Int = 25
    ): PaginatedResponse<PlatformService> = client.get("services/") {
        parameter("page", page)
        parameter("page_size", pageSize)
    }.body()

    suspend fun getServiceClusters(
        page: Int = 1,
        pageSize: Int = 25
    ): PaginatedResponse<PlatformServiceCluster> = client.get("service_clusters/") {
        parameter("page", page)
        parameter("page_size", pageSize)
    }.body()
}

package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.model.*
import retrofit2.http.GET
import retrofit2.http.Query

interface PlatformApiService {

    @GET("organizations/")
    suspend fun getOrganizations(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null
    ): PaginatedResponse<PlatformOrganization>

    @GET("users/")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null
    ): PaginatedResponse<PlatformUser>

    @GET("teams/")
    suspend fun getTeams(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null
    ): PaginatedResponse<PlatformTeam>

    @GET("role_definitions/")
    suspend fun getRoleDefinitions(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null
    ): PaginatedResponse<PlatformRoleDefinition>

    @GET("authenticators/")
    suspend fun getAuthenticators(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25
    ): PaginatedResponse<PlatformAuthenticator>

    @GET("services/")
    suspend fun getServices(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25
    ): PaginatedResponse<PlatformService>

    @GET("service_clusters/")
    suspend fun getServiceClusters(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25
    ): PaginatedResponse<PlatformServiceCluster>
}

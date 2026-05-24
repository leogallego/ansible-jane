package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EdaApiService {

    @GET("audit-rules/")
    suspend fun getAuditRules(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): PaginatedResponse<EdaRuleAudit>

    @GET("audit-rules/{id}/")
    suspend fun getAuditRule(@Path("id") id: Int): EdaRuleAudit

    @GET("activations/")
    suspend fun getActivations(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): PaginatedResponse<EdaActivation>

    @GET("activations/{id}/")
    suspend fun getActivation(@Path("id") id: Int): EdaActivation

    @GET("rulebooks/")
    suspend fun getRulebooks(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("name") name: String? = null
    ): PaginatedResponse<EdaRulebook>

    @GET("decision-environments/")
    suspend fun getDecisionEnvironments(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("name") name: String? = null
    ): PaginatedResponse<EdaDecisionEnvironment>

    @GET("projects/")
    suspend fun getProjects(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("name") name: String? = null
    ): PaginatedResponse<EdaProject>

    @GET("credentials/")
    suspend fun getCredentials(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("name") name: String? = null
    ): PaginatedResponse<EdaCredential>

    @GET("credential-types/")
    suspend fun getCredentialTypes(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("name") name: String? = null
    ): PaginatedResponse<EdaCredentialType>

    @GET("event-streams/")
    suspend fun getEventStreams(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("name") name: String? = null
    ): PaginatedResponse<EdaEventStream>

    @GET("users/")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): PaginatedResponse<EdaUser>
}

package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.model.EdaActivation
import io.github.leogallego.ansiblejane.model.EdaRuleAudit
import io.github.leogallego.ansiblejane.model.PaginatedResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EdaApiService {

    @GET("audit-rules/")
    suspend fun getAuditRules(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("search") search: String? = null
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
}

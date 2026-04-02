package com.example.aapremote.network

import com.example.aapremote.model.*
import retrofit2.http.*

interface AapApiService {

    @GET("me/")
    suspend fun getMe(): PaginatedResponse<User>

    @GET("job_templates/")
    suspend fun getJobTemplates(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("labels__name__icontains") labelsFilter: String? = null,
        @Query("order_by") orderBy: String = "-modified"
    ): PaginatedResponse<JobTemplate>

    @POST("job_templates/{id}/launch/")
    suspend fun launchJob(
        @Path("id") id: Int,
        @Body request: LaunchRequest = LaunchRequest()
    ): LaunchResponse

    @GET("jobs/{id}/")
    suspend fun getJob(@Path("id") id: Int): Job

    @GET("jobs/")
    suspend fun getJobs(
        @Query("order_by") orderBy: String = "-created",
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1
    ): PaginatedResponse<Job>
}

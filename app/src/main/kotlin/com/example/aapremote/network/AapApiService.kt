package com.example.aapremote.network

import com.example.aapremote.model.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

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

    @GET("jobs/{id}/stdout/")
    suspend fun getJobStdout(
        @Path("id") id: Int,
        @Query("format") format: String = "txt"
    ): okhttp3.ResponseBody

    @GET("jobs/")
    suspend fun getJobs(
        @Query("order_by") orderBy: String = "-created",
        @Query("page_size") pageSize: Int = 20,
        @Query("page") page: Int = 1,
        @Query("status") status: String? = null,
        @Query("or__status") orStatus: List<String>? = null
    ): PaginatedResponse<Job>

    @GET("schedules/")
    suspend fun getSchedules(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("order_by") orderBy: String = "-next_run"
    ): PaginatedResponse<Schedule>

    @PATCH("schedules/{id}/")
    suspend fun toggleSchedule(
        @Path("id") id: Int,
        @Body body: Map<String, Boolean>
    ): Schedule

    @GET("workflow_job_templates/")
    suspend fun getWorkflowJobTemplates(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("labels__name__icontains") labelsFilter: String? = null,
        @Query("order_by") orderBy: String = "-modified"
    ): PaginatedResponse<WorkflowJobTemplate>

    @POST("workflow_job_templates/{id}/launch/")
    suspend fun launchWorkflowJob(
        @Path("id") id: Int,
        @Body request: LaunchRequest = LaunchRequest()
    ): WorkflowLaunchResponse

    @GET("workflow_jobs/{id}/")
    suspend fun getWorkflowJob(@Path("id") id: Int): WorkflowJob

    @GET("workflow_jobs/{id}/workflow_nodes/")
    suspend fun getWorkflowNodes(
        @Path("id") id: Int,
        @Query("page_size") pageSize: Int = 200
    ): PaginatedResponse<WorkflowNode>
}

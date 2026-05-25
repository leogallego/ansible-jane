package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.model.*
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
        @Query("or__status") orStatus: List<String>? = null,
        @Query("search") search: String? = null,
        @Query("created__gte") createdAfter: String? = null
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
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 200
    ): PaginatedResponse<WorkflowNode>

    @GET("inventories/")
    suspend fun getInventories(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "-modified"
    ): PaginatedResponse<Inventory>

    @GET("inventories/{id}/")
    suspend fun getInventory(@Path("id") id: Int): Inventory

    @GET("inventories/{id}/hosts/")
    suspend fun getInventoryHosts(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<Host>

    @GET("hosts/")
    suspend fun getHosts(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<Host>

    @GET("hosts/{id}/ansible_facts/")
    suspend fun getHostFacts(@Path("id") id: Int): Map<String, kotlinx.serialization.json.JsonElement>

    @GET("hosts/{id}/job_host_summaries/")
    suspend fun getHostJobSummaries(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20,
        @Query("order_by") orderBy: String = "-created"
    ): PaginatedResponse<JobHostSummary>

    @GET("instances/")
    suspend fun getInstances(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("order_by") orderBy: String = "hostname"
    ): PaginatedResponse<Instance>

    @GET("instances/{id}/")
    suspend fun getInstance(@Path("id") id: Int): Instance

    @GET("instance_groups/")
    suspend fun getInstanceGroups(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25
    ): PaginatedResponse<InstanceGroup>

    @GET("ping/")
    suspend fun ping(): PingResponse

    @GET("mesh_visualizer/")
    suspend fun getMeshTopology(): kotlinx.serialization.json.JsonElement

    @GET("credentials/")
    suspend fun getCredentials(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<Credential>

    @GET("credentials/{id}/")
    suspend fun getCredential(@Path("id") id: Int): Credential

    @GET("projects/")
    suspend fun getProjects(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "-modified"
    ): PaginatedResponse<Project>

    @GET("projects/{id}/")
    suspend fun getProject(@Path("id") id: Int): Project

    @GET("execution_environments/")
    suspend fun getExecutionEnvironments(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25
    ): PaginatedResponse<ExecutionEnvironment>

    @GET("organizations/")
    suspend fun getOrganizations(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<Organization>

    @GET("users/")
    suspend fun getUsers(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "username"
    ): PaginatedResponse<User>

    @GET("teams/")
    suspend fun getTeams(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<Team>

    @GET("roles/")
    suspend fun getRoles(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null
    ): PaginatedResponse<Role>

    @GET("role_definitions/")
    suspend fun getRoleDefinitions(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null
    ): PaginatedResponse<RoleDefinition>

    @GET("groups/")
    suspend fun getGroups(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<Group>

    @GET("inventory_sources/")
    suspend fun getInventorySources(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<InventorySource>

    @GET("labels/")
    suspend fun getLabels(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<Label>

    @GET("credential_types/")
    suspend fun getCredentialTypes(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<CredentialType>

    @GET("notification_templates/")
    suspend fun getNotificationTemplates(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<NotificationTemplate>

    @GET("applications/")
    suspend fun getApplications(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("search") search: String? = null,
        @Query("order_by") orderBy: String = "name"
    ): PaginatedResponse<Application>

    @GET("tokens/")
    suspend fun getTokens(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("order_by") orderBy: String = "-created"
    ): PaginatedResponse<AapToken>

    @GET("settings/")
    suspend fun getSettings(): kotlinx.serialization.json.JsonElement

    @GET("config/")
    suspend fun getConfig(): kotlinx.serialization.json.JsonElement

    @GET("workflow_job_template_nodes/")
    suspend fun getWorkflowJobTemplateNodes(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("workflow_job_template") workflowJobTemplate: Int? = null
    ): PaginatedResponse<WorkflowJobTemplateNode>

    @GET("job_templates/{id}/survey_spec/")
    suspend fun getSurveySpec(@Path("id") id: Int): SurveySpec

    @GET("workflow_approvals/{id}/")
    suspend fun getWorkflowApproval(@Path("id") id: Int): WorkflowApproval

    @GET("workflow_approvals/")
    suspend fun getWorkflowApprovals(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 25,
        @Query("order_by") orderBy: String = "-created"
    ): PaginatedResponse<WorkflowApproval>

    @POST("workflow_approvals/{id}/approve/")
    suspend fun approveWorkflow(@Path("id") id: Int)

    @POST("workflow_approvals/{id}/deny/")
    suspend fun denyWorkflow(@Path("id") id: Int)
}

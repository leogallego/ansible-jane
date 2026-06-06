package io.github.leogallego.ansiblejane.network

import io.github.leogallego.ansiblejane.model.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement

class AapApiClient(private val client: HttpClient) {

    suspend fun getMe(): PaginatedResponse<User> =
        client.get("me/").body()

    suspend fun getJobTemplates(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        labelsFilter: String? = null,
        orderBy: String = "-modified"
    ): PaginatedResponse<JobTemplate> = client.get("job_templates/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        labelsFilter?.let { parameter("labels__name__icontains", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun launchJob(
        id: Int,
        request: LaunchRequest = LaunchRequest()
    ): LaunchResponse = client.post("job_templates/$id/launch/") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()

    suspend fun getJob(id: Int): Job =
        client.get("jobs/$id/").body()

    suspend fun getJobStdout(id: Int, format: String = "txt"): String =
        client.get("jobs/$id/stdout/") {
            parameter("format", format)
        }.bodyAsText()

    suspend fun getJobs(
        orderBy: String = "-created",
        pageSize: Int = 20,
        page: Int = 1,
        status: String? = null,
        orStatus: List<String>? = null,
        search: String? = null,
        createdAfter: String? = null
    ): PaginatedResponse<Job> = client.get("jobs/") {
        parameter("order_by", orderBy)
        parameter("page_size", pageSize)
        parameter("page", page)
        status?.let { parameter("status", it) }
        orStatus?.forEach { parameter("or__status", it) }
        search?.let { parameter("search", it) }
        createdAfter?.let { parameter("created__gte", it) }
    }.body()

    suspend fun getSchedules(
        page: Int = 1,
        pageSize: Int = 20,
        orderBy: String = "-next_run"
    ): PaginatedResponse<Schedule> = client.get("schedules/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        parameter("order_by", orderBy)
    }.body()

    suspend fun toggleSchedule(id: Int, body: Map<String, Boolean>): Schedule =
        client.patch("schedules/$id/") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.body()

    suspend fun getWorkflowJobTemplates(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        labelsFilter: String? = null,
        orderBy: String = "-modified"
    ): PaginatedResponse<WorkflowJobTemplate> = client.get("workflow_job_templates/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        labelsFilter?.let { parameter("labels__name__icontains", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun launchWorkflowJob(
        id: Int,
        request: LaunchRequest = LaunchRequest()
    ): WorkflowLaunchResponse = client.post("workflow_job_templates/$id/launch/") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }.body()

    suspend fun getWorkflowJob(id: Int): WorkflowJob =
        client.get("workflow_jobs/$id/").body()

    suspend fun getWorkflowNodes(
        id: Int,
        page: Int = 1,
        pageSize: Int = 200
    ): PaginatedResponse<WorkflowNode> = client.get("workflow_jobs/$id/workflow_nodes/") {
        parameter("page", page)
        parameter("page_size", pageSize)
    }.body()

    suspend fun getInventories(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "-modified"
    ): PaginatedResponse<Inventory> = client.get("inventories/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getInventory(id: Int): Inventory =
        client.get("inventories/$id/").body()

    suspend fun getInventoryHosts(
        id: Int,
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<Host> = client.get("inventories/$id/hosts/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getHosts(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<Host> = client.get("hosts/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getHostFacts(id: Int): Map<String, JsonElement> =
        client.get("hosts/$id/ansible_facts/").body()

    suspend fun getHostJobSummaries(
        id: Int,
        page: Int = 1,
        pageSize: Int = 20,
        orderBy: String = "-created"
    ): PaginatedResponse<JobHostSummary> = client.get("hosts/$id/job_host_summaries/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        parameter("order_by", orderBy)
    }.body()

    suspend fun getInstances(
        page: Int = 1,
        pageSize: Int = 25,
        orderBy: String = "hostname"
    ): PaginatedResponse<Instance> = client.get("instances/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        parameter("order_by", orderBy)
    }.body()

    suspend fun getInstance(id: Int): Instance =
        client.get("instances/$id/").body()

    suspend fun getInstanceGroups(
        page: Int = 1,
        pageSize: Int = 25
    ): PaginatedResponse<InstanceGroup> = client.get("instance_groups/") {
        parameter("page", page)
        parameter("page_size", pageSize)
    }.body()

    suspend fun ping(): PingResponse =
        client.get("ping/").body()

    suspend fun getMeshTopology(): JsonElement =
        client.get("mesh_visualizer/").body()

    suspend fun getCredentials(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<Credential> = client.get("credentials/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getCredential(id: Int): Credential =
        client.get("credentials/$id/").body()

    suspend fun getProjects(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "-modified"
    ): PaginatedResponse<Project> = client.get("projects/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getProject(id: Int): Project =
        client.get("projects/$id/").body()

    suspend fun getExecutionEnvironments(
        page: Int = 1,
        pageSize: Int = 25
    ): PaginatedResponse<ExecutionEnvironment> = client.get("execution_environments/") {
        parameter("page", page)
        parameter("page_size", pageSize)
    }.body()

    suspend fun getOrganizations(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<Organization> = client.get("organizations/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getUsers(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "username"
    ): PaginatedResponse<User> = client.get("users/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getTeams(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<Team> = client.get("teams/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getRoles(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): PaginatedResponse<Role> = client.get("roles/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
    }.body()

    suspend fun getRoleDefinitions(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): PaginatedResponse<RoleDefinition> = client.get("role_definitions/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
    }.body()

    suspend fun getGroups(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<Group> = client.get("groups/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getInventorySources(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<InventorySource> = client.get("inventory_sources/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getLabels(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<Label> = client.get("labels/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getCredentialTypes(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<CredentialType> = client.get("credential_types/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getNotificationTemplates(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<NotificationTemplate> = client.get("notification_templates/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getApplications(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null,
        orderBy: String = "name"
    ): PaginatedResponse<Application> = client.get("applications/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        search?.let { parameter("search", it) }
        parameter("order_by", orderBy)
    }.body()

    suspend fun getTokens(
        page: Int = 1,
        pageSize: Int = 25,
        orderBy: String = "-created"
    ): PaginatedResponse<AapToken> = client.get("tokens/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        parameter("order_by", orderBy)
    }.body()

    suspend fun getSettings(): JsonElement =
        client.get("settings/").body()

    suspend fun getConfig(): JsonElement =
        client.get("config/").body()

    suspend fun getWorkflowJobTemplateNodes(
        page: Int = 1,
        pageSize: Int = 25,
        workflowJobTemplate: Int? = null
    ): PaginatedResponse<WorkflowJobTemplateNode> = client.get("workflow_job_template_nodes/") {
        parameter("page", page)
        parameter("page_size", pageSize)
        workflowJobTemplate?.let { parameter("workflow_job_template", it) }
    }.body()

    suspend fun getSurveySpec(id: Int): SurveySpec =
        client.get("job_templates/$id/survey_spec/").body()

    suspend fun getWorkflowApproval(id: Int): WorkflowApproval =
        client.get("workflow_approvals/$id/").body()

    suspend fun getWorkflowApprovals(
        status: String? = null,
        page: Int = 1,
        pageSize: Int = 25,
        orderBy: String = "-created"
    ): PaginatedResponse<WorkflowApproval> = client.get("workflow_approvals/") {
        status?.let { parameter("status", it) }
        parameter("page", page)
        parameter("page_size", pageSize)
        parameter("order_by", orderBy)
    }.body()

    suspend fun approveWorkflow(id: Int) {
        client.post("workflow_approvals/$id/approve/")
    }

    suspend fun denyWorkflow(id: Int) {
        client.post("workflow_approvals/$id/deny/")
    }
}

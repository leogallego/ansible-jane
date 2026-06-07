package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.*
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlinx.serialization.json.JsonElement

class ControllerReadOnlyRepository(private val apiProvider: IAapApiProvider) {

    suspend fun getOrganizations(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<Organization>> = runPaginated {
        apiProvider.getApiService().getOrganizations(page, pageSize, search)
    }

    suspend fun getUsers(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<User>> = runPaginated {
        apiProvider.getApiService().getUsers(page, pageSize, search)
    }

    suspend fun getTeams(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<Team>> = runPaginated {
        apiProvider.getApiService().getTeams(page, pageSize, search)
    }

    suspend fun getRoles(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<Role>> = runPaginated {
        apiProvider.getApiService().getRoles(page, pageSize, search)
    }

    suspend fun getRoleDefinitions(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<RoleDefinition>> = runPaginated {
        apiProvider.getApiService().getRoleDefinitions(page, pageSize, search)
    }

    suspend fun getGroups(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<Group>> = runPaginated {
        apiProvider.getApiService().getGroups(page, pageSize, search)
    }

    suspend fun getInventorySources(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<InventorySource>> = runPaginated {
        apiProvider.getApiService().getInventorySources(page, pageSize, search)
    }

    suspend fun getLabels(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<Label>> = runPaginated {
        apiProvider.getApiService().getLabels(page, pageSize, search)
    }

    suspend fun getCredentialTypes(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<CredentialType>> = runPaginated {
        apiProvider.getApiService().getCredentialTypes(page, pageSize, search)
    }

    suspend fun getNotificationTemplates(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<NotificationTemplate>> = runPaginated {
        apiProvider.getApiService().getNotificationTemplates(page, pageSize, search)
    }

    suspend fun getApplications(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ListResult<Application>> = runPaginated {
        apiProvider.getApiService().getApplications(page, pageSize, search)
    }

    suspend fun getTokens(
        page: Int = 1,
        pageSize: Int = 25
    ): Result<ListResult<AapToken>> = runPaginated {
        apiProvider.getApiService().getTokens(page, pageSize)
    }

    suspend fun getSettings(): Result<JsonElement> = try {
        Result.success(apiProvider.getApiService().getSettings())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getConfig(): Result<JsonElement> = try {
        Result.success(apiProvider.getApiService().getConfig())
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getWorkflowJobTemplateNodes(
        page: Int = 1,
        pageSize: Int = 25,
        workflowJobTemplate: Int? = null
    ): Result<ListResult<WorkflowJobTemplateNode>> = runPaginated {
        apiProvider.getApiService().getWorkflowJobTemplateNodes(page, pageSize, workflowJobTemplate)
    }

    suspend fun getSurveySpec(id: Int): Result<SurveySpec> = try {
        Result.success(apiProvider.getApiService().getSurveySpec(id))
    } catch (e: Exception) {
        Result.failure(e)
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
}

data class ListResult<T>(
    val items: List<T>,
    val hasMore: Boolean,
    val totalCount: Int
)

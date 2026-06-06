package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.ExecutionEnvironment
import io.github.leogallego.ansiblejane.model.Project
import io.github.leogallego.ansiblejane.network.IAapApiProvider

class ProjectRepository(private val apiProvider: IAapApiProvider) : IProjectRepository {

    override suspend fun getProjects(
        page: Int,
        pageSize: Int,
        search: String?
    ): Result<ProjectListResult> {
        return try {
            val response = apiProvider.getApiService().getProjects(
                page = page,
                pageSize = pageSize,
                search = search
            )
            Result.success(
                ProjectListResult(
                    projects = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProject(id: Int): Result<Project> {
        return try {
            Result.success(apiProvider.getApiService().getProject(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExecutionEnvironments(
        page: Int,
        pageSize: Int
    ): Result<ExecutionEnvironmentListResult> {
        return try {
            val response = apiProvider.getApiService().getExecutionEnvironments(
                page = page,
                pageSize = pageSize
            )
            Result.success(
                ExecutionEnvironmentListResult(
                    executionEnvironments = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class ProjectListResult(
    val projects: List<Project>,
    val hasMore: Boolean,
    val totalCount: Int
)

data class ExecutionEnvironmentListResult(
    val executionEnvironments: List<ExecutionEnvironment>,
    val hasMore: Boolean,
    val totalCount: Int
)

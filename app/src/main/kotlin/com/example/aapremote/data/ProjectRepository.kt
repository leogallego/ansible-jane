package com.example.aapremote.data

import com.example.aapremote.model.ExecutionEnvironment
import com.example.aapremote.model.Project
import com.example.aapremote.network.AapApiProvider

class ProjectRepository(private val apiProvider: AapApiProvider) {

    suspend fun getProjects(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
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

    suspend fun getProject(id: Int): Result<Project> {
        return try {
            Result.success(apiProvider.getApiService().getProject(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExecutionEnvironments(
        page: Int = 1,
        pageSize: Int = 25
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

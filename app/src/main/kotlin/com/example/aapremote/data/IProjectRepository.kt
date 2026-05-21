package com.example.aapremote.data

import com.example.aapremote.model.Project

interface IProjectRepository {
    suspend fun getProjects(
        page: Int = 1,
        pageSize: Int = 25,
        search: String? = null
    ): Result<ProjectListResult>

    suspend fun getProject(id: Int): Result<Project>

    suspend fun getExecutionEnvironments(
        page: Int = 1,
        pageSize: Int = 25
    ): Result<ExecutionEnvironmentListResult>
}

package com.example.aapremote.data

import com.example.aapremote.model.LaunchRequest
import com.example.aapremote.model.WorkflowJob
import com.example.aapremote.model.WorkflowJobTemplate
import com.example.aapremote.model.WorkflowNode
import com.example.aapremote.network.AapApiService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WorkflowRepository(private val apiService: AapApiService) {

    suspend fun getWorkflowTemplates(
        page: Int = 1,
        search: String? = null,
        labelFilter: String? = null
    ): Result<WorkflowTemplateListResult> {
        return try {
            val response = apiService.getWorkflowJobTemplates(
                page = page,
                search = search,
                labelsFilter = labelFilter
            )
            Result.success(
                WorkflowTemplateListResult(
                    templates = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun launchWorkflow(templateId: Int, extraVars: String? = null): Result<Int> {
        return try {
            val request = LaunchRequest(extraVars = extraVars)
            val response = apiService.launchWorkflowJob(templateId, request)
            Result.success(response.workflowJob)
        } catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                400 -> "Invalid request. Check your extra variables."
                403 -> "You don't have permission to launch this workflow template."
                else -> "Launch failed (${e.code()}): ${e.message()}"
            }
            Result.failure(Exception(message))
        } catch (e: Exception) {
            Result.failure(Exception("Launch failed: ${e.message}"))
        }
    }

    suspend fun getWorkflowJobStatus(workflowJobId: Int): Result<WorkflowJob> {
        return try {
            Result.success(apiService.getWorkflowJob(workflowJobId))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get workflow job status: ${e.message}"))
        }
    }

    fun pollWorkflowJobStatus(workflowJobId: Int): Flow<WorkflowJob> = flow {
        while (true) {
            try {
                val job = apiService.getWorkflowJob(workflowJobId)
                emit(job)
                if (job.status.isTerminal) break
                delay(5000)
            } catch (e: Exception) {
                throw Exception("Failed to poll workflow job status: ${e.message}")
            }
        }
    }

    suspend fun getWorkflowNodes(workflowJobId: Int): Result<List<WorkflowNode>> {
        return try {
            val response = apiService.getWorkflowNodes(workflowJobId)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get workflow nodes: ${e.message}"))
        }
    }
}

data class WorkflowTemplateListResult(
    val templates: List<WorkflowJobTemplate>,
    val hasMore: Boolean,
    val totalCount: Int
)

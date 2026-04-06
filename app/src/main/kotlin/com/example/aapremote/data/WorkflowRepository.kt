package com.example.aapremote.data

import com.example.aapremote.model.LaunchRequest
import com.example.aapremote.model.WorkflowJob
import com.example.aapremote.model.WorkflowJobTemplate
import com.example.aapremote.model.WorkflowNode
import com.example.aapremote.network.AapApiProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WorkflowRepository(private val apiProvider: AapApiProvider) {

    suspend fun getWorkflowTemplates(
        page: Int = 1,
        search: String? = null,
        labelFilter: String? = null
    ): Result<WorkflowTemplateListResult> {
        return try {
            val response = apiProvider.getApiService().getWorkflowJobTemplates(
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
            val response = apiProvider.getApiService().launchWorkflowJob(templateId, request)
            Result.success(response.workflowJob)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkflowJobStatus(workflowJobId: Int): Result<WorkflowJob> {
        return try {
            Result.success(apiProvider.getApiService().getWorkflowJob(workflowJobId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun pollWorkflowJobStatus(workflowJobId: Int): Flow<WorkflowJob> = flow {
        while (true) {
            try {
                val job = apiProvider.getApiService().getWorkflowJob(workflowJobId)
                emit(job)
                if (job.status.isTerminal) break
                delay(5000)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun getWorkflowNodes(workflowJobId: Int): Result<List<WorkflowNode>> {
        return try {
            val response = apiProvider.getApiService().getWorkflowNodes(workflowJobId)
            Result.success(response.results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class WorkflowTemplateListResult(
    val templates: List<WorkflowJobTemplate>,
    val hasMore: Boolean,
    val totalCount: Int
)

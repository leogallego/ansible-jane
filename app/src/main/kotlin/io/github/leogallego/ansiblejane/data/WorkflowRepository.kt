package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.LaunchRequest
import io.github.leogallego.ansiblejane.model.WorkflowApproval
import io.github.leogallego.ansiblejane.model.WorkflowJob
import io.github.leogallego.ansiblejane.model.WorkflowJobTemplate
import io.github.leogallego.ansiblejane.model.WorkflowJobTemplateNode
import io.github.leogallego.ansiblejane.model.WorkflowNode
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WorkflowRepository(private val apiProvider: IAapApiProvider) : IWorkflowRepository {

    override suspend fun getWorkflowTemplates(
        page: Int,
        search: String?,
        labelFilter: String?
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

    override suspend fun launchWorkflow(templateId: Int, extraVars: String?): Result<Int> {
        return try {
            val request = LaunchRequest(extraVars = extraVars)
            val response = apiProvider.getApiService().launchWorkflowJob(templateId, request)
            Result.success(response.workflowJob)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWorkflowJobStatus(workflowJobId: Int): Result<WorkflowJob> {
        return try {
            Result.success(apiProvider.getApiService().getWorkflowJob(workflowJobId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun pollWorkflowJobStatus(workflowJobId: Int): Flow<WorkflowJob> = flow {
        while (true) {
            val job = apiProvider.getApiService().getWorkflowJob(workflowJobId)
            emit(job)
            if (job.status.isTerminal) break
            delay(5000)
        }
    }

    override suspend fun getWorkflowNodes(workflowJobId: Int): Result<List<WorkflowNode>> {
        return try {
            val all = mutableListOf<WorkflowNode>()
            var page = 1
            do {
                val response = apiProvider.getApiService().getWorkflowNodes(workflowJobId, page = page, pageSize = 200)
                all.addAll(response.results)
                page++
            } while (response.next != null)
            Result.success(all)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWorkflowTemplateNodes(templateId: Int): Result<List<WorkflowJobTemplateNode>> {
        return try {
            val all = mutableListOf<WorkflowJobTemplateNode>()
            var page = 1
            do {
                val response = apiProvider.getApiService().getWorkflowJobTemplateNodes(
                    page = page,
                    pageSize = 200,
                    workflowJobTemplate = templateId
                )
                all.addAll(response.results)
                page++
            } while (response.next != null)
            Result.success(all)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getWorkflowApproval(approvalId: Int): Result<WorkflowApproval> {
        return try {
            Result.success(apiProvider.getApiService().getWorkflowApproval(approvalId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPendingApprovals(page: Int, pageSize: Int): Result<PendingApprovalResult> {
        return try {
            val response = apiProvider.getApiService().getWorkflowApprovals(
                status = "pending",
                page = page,
                pageSize = pageSize
            )
            Result.success(
                PendingApprovalResult(
                    approvals = response.results,
                    hasMore = response.next != null,
                    totalCount = response.count
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun approveWorkflow(approvalId: Int): Result<Unit> {
        return try {
            apiProvider.getApiService().approveWorkflow(approvalId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun denyWorkflow(approvalId: Int): Result<Unit> {
        return try {
            apiProvider.getApiService().denyWorkflow(approvalId)
            Result.success(Unit)
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

data class PendingApprovalResult(
    val approvals: List<WorkflowApproval>,
    val hasMore: Boolean,
    val totalCount: Int
)

package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.data.IWorkflowRepository
import io.github.leogallego.ansiblejane.data.PendingApprovalResult
import io.github.leogallego.ansiblejane.data.WorkflowTemplateListResult
import io.github.leogallego.ansiblejane.model.WorkflowApproval
import io.github.leogallego.ansiblejane.model.WorkflowJob
import io.github.leogallego.ansiblejane.model.WorkflowJobTemplate
import io.github.leogallego.ansiblejane.model.WorkflowJobTemplateNode
import io.github.leogallego.ansiblejane.model.WorkflowNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeWorkflowRepository : IWorkflowRepository {
    var templates = listOf<WorkflowJobTemplate>()
    var workflowJob: WorkflowJob? = null
    var nodes = listOf<WorkflowNode>()
    var shouldFail = false
    var failureException: Exception = RuntimeException("Test error")
    var launchResult = 99
    var hasMore = false
    var lastRequestedPage = 0
    var lastSearchQuery: String? = null
    var lastLabelFilter: String? = null

    override suspend fun getWorkflowTemplates(page: Int, search: String?, labelFilter: String?): Result<WorkflowTemplateListResult> {
        lastRequestedPage = page
        lastSearchQuery = search
        lastLabelFilter = labelFilter
        if (shouldFail) return Result.failure(failureException)
        return Result.success(WorkflowTemplateListResult(templates, hasMore = hasMore, totalCount = templates.size))
    }

    override suspend fun launchWorkflow(templateId: Int, extraVars: String?): Result<Int> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(launchResult)
    }

    override suspend fun getWorkflowJobStatus(workflowJobId: Int): Result<WorkflowJob> {
        if (shouldFail) return Result.failure(failureException)
        return workflowJob?.let { Result.success(it) } ?: Result.failure(RuntimeException("Not found"))
    }

    override fun pollWorkflowJobStatus(workflowJobId: Int): Flow<WorkflowJob> = flow {
        if (shouldFail) throw failureException
        workflowJob?.let { emit(it) }
    }

    override suspend fun getWorkflowNodes(workflowJobId: Int): Result<List<WorkflowNode>> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(nodes)
    }

    var templateNodes = listOf<WorkflowJobTemplateNode>()

    override suspend fun getWorkflowTemplateNodes(templateId: Int): Result<List<WorkflowJobTemplateNode>> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(templateNodes)
    }

    var approvals = listOf<WorkflowApproval>()

    override suspend fun getWorkflowApproval(approvalId: Int): Result<WorkflowApproval> {
        if (shouldFail) return Result.failure(failureException)
        return approvals.find { it.id == approvalId }
            ?.let { Result.success(it) }
            ?: Result.failure(RuntimeException("Not found"))
    }

    override suspend fun getPendingApprovals(page: Int, pageSize: Int): Result<PendingApprovalResult> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(PendingApprovalResult(approvals, hasMore = false, totalCount = approvals.size))
    }

    override suspend fun approveWorkflow(approvalId: Int): Result<Unit> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(Unit)
    }

    override suspend fun denyWorkflow(approvalId: Int): Result<Unit> {
        if (shouldFail) return Result.failure(failureException)
        return Result.success(Unit)
    }
}

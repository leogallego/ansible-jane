package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.WorkflowApproval
import io.github.leogallego.ansiblejane.model.WorkflowJob
import io.github.leogallego.ansiblejane.model.WorkflowJobTemplateNode
import io.github.leogallego.ansiblejane.model.WorkflowNode
import kotlinx.coroutines.flow.Flow

interface IWorkflowRepository {
    suspend fun getWorkflowTemplates(
        page: Int = 1,
        search: String? = null,
        labelFilter: String? = null
    ): Result<WorkflowTemplateListResult>

    suspend fun launchWorkflow(templateId: Int, extraVars: String? = null): Result<Int>
    suspend fun getWorkflowJobStatus(workflowJobId: Int): Result<WorkflowJob>
    fun pollWorkflowJobStatus(workflowJobId: Int): Flow<WorkflowJob>
    suspend fun getWorkflowNodes(workflowJobId: Int): Result<List<WorkflowNode>>
    suspend fun getWorkflowTemplateNodes(templateId: Int): Result<List<WorkflowJobTemplateNode>>
    suspend fun getWorkflowApproval(approvalId: Int): Result<WorkflowApproval>
    suspend fun getPendingApprovals(page: Int = 1, pageSize: Int = 25): Result<PendingApprovalResult>
    suspend fun approveWorkflow(approvalId: Int): Result<Unit>
    suspend fun denyWorkflow(approvalId: Int): Result<Unit>
}

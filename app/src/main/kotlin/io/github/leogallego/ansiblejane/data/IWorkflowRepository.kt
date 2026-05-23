package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.WorkflowJob
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
}

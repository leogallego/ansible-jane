package io.github.leogallego.ansiblejane.presentation.workflows

import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.WorkflowJob
import io.github.leogallego.ansiblejane.model.WorkflowNode

sealed interface WorkflowJobStatusUiState {
    data object Loading : WorkflowJobStatusUiState
    data class Active(
        val workflowJob: WorkflowJob,
        val nodes: List<WorkflowNode> = emptyList()
    ) : WorkflowJobStatusUiState
    data class Completed(
        val workflowJob: WorkflowJob,
        val nodes: List<WorkflowNode> = emptyList()
    ) : WorkflowJobStatusUiState
    data class Error(val error: AppError) : WorkflowJobStatusUiState
}

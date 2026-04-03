package com.example.aapremote.presentation.workflows

import com.example.aapremote.model.AppError
import com.example.aapremote.model.WorkflowJob
import com.example.aapremote.model.WorkflowNode

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

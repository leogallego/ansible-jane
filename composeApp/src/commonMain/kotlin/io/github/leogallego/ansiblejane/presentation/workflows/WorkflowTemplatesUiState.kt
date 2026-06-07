package io.github.leogallego.ansiblejane.presentation.workflows

import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Label
import io.github.leogallego.ansiblejane.model.WorkflowJobTemplate

sealed interface WorkflowTemplatesUiState {
    data object Idle : WorkflowTemplatesUiState
    data object Loading : WorkflowTemplatesUiState
    data class Success(
        val templates: List<WorkflowJobTemplate>,
        val availableLabels: List<Label>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : WorkflowTemplatesUiState
    data class Error(val error: AppError) : WorkflowTemplatesUiState
}

sealed interface WorkflowLaunchState {
    data object Idle : WorkflowLaunchState
    data class Confirming(val template: WorkflowJobTemplate) : WorkflowLaunchState
    data class EnteringVars(val template: WorkflowJobTemplate) : WorkflowLaunchState
    data object Launching : WorkflowLaunchState
    data class Launched(val workflowJobId: Int) : WorkflowLaunchState
    data class LaunchError(val error: AppError) : WorkflowLaunchState
}

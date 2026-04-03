package com.example.aapremote.presentation.workflows

import com.example.aapremote.model.Label
import com.example.aapremote.model.WorkflowJobTemplate

sealed interface WorkflowTemplatesUiState {
    data object Idle : WorkflowTemplatesUiState
    data object Loading : WorkflowTemplatesUiState
    data class Success(
        val templates: List<WorkflowJobTemplate>,
        val availableLabels: List<Label>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : WorkflowTemplatesUiState
    data class Error(val message: String) : WorkflowTemplatesUiState
}

sealed interface WorkflowLaunchState {
    data object Idle : WorkflowLaunchState
    data class Confirming(val template: WorkflowJobTemplate) : WorkflowLaunchState
    data class EnteringVars(val template: WorkflowJobTemplate) : WorkflowLaunchState
    data object Launching : WorkflowLaunchState
    data class Launched(val workflowJobId: Int) : WorkflowLaunchState
    data class LaunchError(val message: String) : WorkflowLaunchState
}

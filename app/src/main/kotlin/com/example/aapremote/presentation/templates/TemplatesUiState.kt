package com.example.aapremote.presentation.templates

import com.example.aapremote.model.AppError
import com.example.aapremote.model.JobTemplate
import com.example.aapremote.model.Label

sealed interface TemplatesUiState {
    data object Idle : TemplatesUiState
    data object Loading : TemplatesUiState
    data class Success(
        val templates: List<JobTemplate>,
        val availableLabels: List<Label>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : TemplatesUiState
    data class Error(val error: AppError) : TemplatesUiState
}

sealed interface LaunchState {
    data object Idle : LaunchState
    data class Confirming(val template: JobTemplate) : LaunchState
    data class EnteringVars(val template: JobTemplate) : LaunchState
    data object Launching : LaunchState
    data class Launched(val jobId: Int) : LaunchState
    data class LaunchError(val error: AppError) : LaunchState
}

package io.github.leogallego.ansiblejane.presentation.templates

import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.JobTemplate
import io.github.leogallego.ansiblejane.model.Label

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

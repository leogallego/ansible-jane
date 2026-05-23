package io.github.leogallego.ansiblejane.presentation.jobs

import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Job
import io.github.leogallego.ansiblejane.model.JobStatus

sealed interface JobStatusUiState {
    data object Loading : JobStatusUiState
    data class Active(val job: Job, val stdout: String? = null) : JobStatusUiState
    data class Completed(val job: Job, val stdout: String? = null) : JobStatusUiState
    data class Error(val error: AppError) : JobStatusUiState
}

sealed interface RecentJobsUiState {
    data object Loading : RecentJobsUiState
    data class Success(
        val jobs: List<Job>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val activeFilters: Set<JobStatus> = emptySet()
    ) : RecentJobsUiState
    data class Error(val error: AppError) : RecentJobsUiState
}

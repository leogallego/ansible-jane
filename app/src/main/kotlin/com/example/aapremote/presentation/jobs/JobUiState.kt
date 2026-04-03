package com.example.aapremote.presentation.jobs

import com.example.aapremote.model.Job
import com.example.aapremote.model.JobStatus

sealed interface JobStatusUiState {
    data object Loading : JobStatusUiState
    data class Active(val job: Job, val stdout: String? = null) : JobStatusUiState
    data class Completed(val job: Job, val stdout: String? = null) : JobStatusUiState
    data class Error(val message: String) : JobStatusUiState
}

sealed interface RecentJobsUiState {
    data object Loading : RecentJobsUiState
    data class Success(
        val jobs: List<Job>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false,
        val activeFilters: Set<JobStatus> = emptySet()
    ) : RecentJobsUiState
    data class Error(val message: String) : RecentJobsUiState
}

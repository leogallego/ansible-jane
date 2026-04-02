package com.example.aapremote.presentation.jobs

import com.example.aapremote.model.Job

sealed interface JobStatusUiState {
    data object Loading : JobStatusUiState
    data class Active(val job: Job) : JobStatusUiState
    data class Completed(val job: Job) : JobStatusUiState
    data class Error(val message: String) : JobStatusUiState
}

sealed interface RecentJobsUiState {
    data object Loading : RecentJobsUiState
    data class Success(
        val jobs: List<Job>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : RecentJobsUiState
    data class Error(val message: String) : RecentJobsUiState
}

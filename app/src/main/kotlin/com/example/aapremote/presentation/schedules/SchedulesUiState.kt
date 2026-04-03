package com.example.aapremote.presentation.schedules

import com.example.aapremote.model.Schedule

sealed interface SchedulesUiState {
    data object Loading : SchedulesUiState
    data class Success(
        val schedules: List<Schedule>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : SchedulesUiState
    data class Error(val message: String) : SchedulesUiState
}

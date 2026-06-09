package io.github.leogallego.ansiblejane.presentation.schedules

import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Schedule

sealed interface SchedulesUiState {
    data object Loading : SchedulesUiState
    data class Success(
        val schedules: List<Schedule>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : SchedulesUiState
    data class Error(val error: AppError) : SchedulesUiState
}

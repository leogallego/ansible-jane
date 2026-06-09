package io.github.leogallego.ansiblejane.presentation.hosts

import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Host

sealed interface HostsUiState {
    data object Loading : HostsUiState
    data class Success(
        val hosts: List<Host>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : HostsUiState
    data class Empty(val message: String) : HostsUiState
    data class Error(val error: AppError) : HostsUiState
}

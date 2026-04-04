package com.example.aapremote.presentation.hosts

import com.example.aapremote.model.AppError
import com.example.aapremote.model.Host

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

package com.example.aapremote.presentation.hosts

import com.example.aapremote.model.AppError
import com.example.aapremote.model.Host

sealed interface InventoryHostsUiState {
    data object Loading : InventoryHostsUiState
    data class Success(
        val hosts: List<Host>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : InventoryHostsUiState
    data class Empty(val message: String) : InventoryHostsUiState
    data class Error(val error: AppError) : InventoryHostsUiState
}

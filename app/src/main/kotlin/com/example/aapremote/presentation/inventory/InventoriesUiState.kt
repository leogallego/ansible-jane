package com.example.aapremote.presentation.inventory

import com.example.aapremote.model.AppError
import com.example.aapremote.model.Inventory

sealed interface InventoriesUiState {
    data object Loading : InventoriesUiState
    data class Success(
        val inventories: List<Inventory>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : InventoriesUiState
    data class Empty(val message: String) : InventoriesUiState
    data class Error(val error: AppError) : InventoriesUiState
}

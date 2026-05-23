package io.github.leogallego.ansiblejane.presentation.inventory

import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Inventory

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

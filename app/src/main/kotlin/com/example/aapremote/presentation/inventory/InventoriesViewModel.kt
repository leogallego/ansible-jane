package com.example.aapremote.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.data.InventoryRepository
import com.example.aapremote.model.AppError
import com.example.aapremote.model.Inventory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InventoriesViewModel(
    private val inventoryRepository: InventoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoriesUiState>(InventoriesUiState.Loading)
    val uiState: StateFlow<InventoriesUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val allInventories = mutableListOf<Inventory>()

    init {
        loadInventories()
    }

    fun loadInventories() {
        currentPage = 1
        allInventories.clear()
        _uiState.value = InventoriesUiState.Loading
        viewModelScope.launch {
            fetchInventories()
        }
    }

    fun refresh() {
        currentPage = 1
        allInventories.clear()
        viewModelScope.launch {
            fetchInventories()
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current is InventoriesUiState.Success && current.hasMore && !current.isLoadingMore) {
            currentPage++
            _uiState.value = current.copy(isLoadingMore = true)
            viewModelScope.launch {
                fetchInventories(append = true)
            }
        }
    }

    private suspend fun fetchInventories(append: Boolean = false) {
        val result = inventoryRepository.getInventories(page = currentPage)
        result.fold(
            onSuccess = { inventoryResult ->
                if (append) {
                    allInventories.addAll(inventoryResult.inventories)
                } else {
                    allInventories.clear()
                    allInventories.addAll(inventoryResult.inventories)
                }
                if (allInventories.isEmpty()) {
                    _uiState.value = InventoriesUiState.Empty("No inventories found")
                } else {
                    _uiState.value = InventoriesUiState.Success(
                        inventories = allInventories.toList(),
                        hasMore = inventoryResult.hasMore
                    )
                }
            },
            onFailure = { error ->
                _uiState.value = InventoriesUiState.Error(AppError.from(error))
            }
        )
    }
}

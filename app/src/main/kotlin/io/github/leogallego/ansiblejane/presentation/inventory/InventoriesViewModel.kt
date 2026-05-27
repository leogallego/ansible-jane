package io.github.leogallego.ansiblejane.presentation.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IInventoryRepository
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Inventory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class InventoriesViewModel(
    private val inventoryRepository: IInventoryRepository,
    private val tokenManager: ITokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoriesUiState>(InventoriesUiState.Loading)
    val uiState: StateFlow<InventoriesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentPage = 1
    private val allInventories = mutableListOf<Inventory>()
    private var fetchJob: Job? = null
    private var searchJob: Job? = null
    private var currentSearch: String? = null

    init {
        viewModelScope.launch {
            tokenManager.activeInstance
                .distinctUntilChangedBy { it?.id }
                .collect { instance ->
                    fetchJob?.cancel()
                    if (instance != null) loadInventories()
                }
        }
    }

    fun loadInventories() {
        currentPage = 1
        allInventories.clear()
        _uiState.update { InventoriesUiState.Loading }
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchInventories()
        }
    }

    fun refresh() {
        currentPage = 1
        allInventories.clear()
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchInventories()
        }
    }

    fun search(query: String) {
        _searchQuery.update { query }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            currentSearch = query.ifBlank { null }
            currentPage = 1
            allInventories.clear()
            _uiState.update { InventoriesUiState.Loading }
            fetchInventories()
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current is InventoriesUiState.Success && current.hasMore && !current.isLoadingMore) {
            currentPage++
            _uiState.update { state ->
                (state as? InventoriesUiState.Success)?.copy(isLoadingMore = true) ?: state
            }
            fetchJob = viewModelScope.launch {
                fetchInventories(append = true)
            }
        }
    }

    private suspend fun fetchInventories(append: Boolean = false) {
        val result = inventoryRepository.getInventories(page = currentPage, search = currentSearch)
        result.fold(
            onSuccess = { inventoryResult ->
                if (append) {
                    allInventories.addAll(inventoryResult.inventories)
                } else {
                    allInventories.clear()
                    allInventories.addAll(inventoryResult.inventories)
                }
                _uiState.update {
                    if (allInventories.isEmpty()) {
                        InventoriesUiState.Empty("No inventories found")
                    } else {
                        InventoriesUiState.Success(
                            inventories = allInventories.toList(),
                            hasMore = inventoryResult.hasMore
                        )
                    }
                }
            },
            onFailure = { error ->
                _uiState.update { InventoriesUiState.Error(AppError.from(error)) }
            }
        )
    }
}

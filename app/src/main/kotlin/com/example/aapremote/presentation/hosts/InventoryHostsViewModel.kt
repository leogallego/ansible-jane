package com.example.aapremote.presentation.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.data.IHostRepository
import com.example.aapremote.model.AppError
import com.example.aapremote.model.Host
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InventoryHostsViewModel(
    private val hostRepository: IHostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryHostsUiState>(InventoryHostsUiState.Loading)
    val uiState: StateFlow<InventoryHostsUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val allHosts = mutableListOf<Host>()
    private var currentInventoryId: Int = 0
    private var currentSearch: String? = null
    private var searchJob: Job? = null

    fun loadHosts(inventoryId: Int) {
        currentInventoryId = inventoryId
        currentPage = 1
        allHosts.clear()
        _uiState.value = InventoryHostsUiState.Loading
        viewModelScope.launch {
            fetchHosts()
        }
    }

    fun refresh() {
        currentPage = 1
        allHosts.clear()
        viewModelScope.launch {
            fetchHosts()
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current is InventoryHostsUiState.Success && current.hasMore && !current.isLoadingMore) {
            currentPage++
            _uiState.value = current.copy(isLoadingMore = true)
            viewModelScope.launch {
                fetchHosts(append = true)
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            currentSearch = query.ifBlank { null }
            currentPage = 1
            allHosts.clear()
            _uiState.value = InventoryHostsUiState.Loading
            fetchHosts()
        }
    }

    private suspend fun fetchHosts(append: Boolean = false) {
        val result = hostRepository.getInventoryHosts(
            inventoryId = currentInventoryId,
            page = currentPage,
            search = currentSearch
        )
        result.fold(
            onSuccess = { hostResult ->
                if (append) {
                    allHosts.addAll(hostResult.hosts)
                } else {
                    allHosts.clear()
                    allHosts.addAll(hostResult.hosts)
                }
                if (allHosts.isEmpty()) {
                    _uiState.value = InventoryHostsUiState.Empty("No hosts found")
                } else {
                    _uiState.value = InventoryHostsUiState.Success(
                        hosts = allHosts.toList(),
                        hasMore = hostResult.hasMore
                    )
                }
            },
            onFailure = { error ->
                _uiState.value = InventoryHostsUiState.Error(AppError.from(error))
            }
        )
    }
}

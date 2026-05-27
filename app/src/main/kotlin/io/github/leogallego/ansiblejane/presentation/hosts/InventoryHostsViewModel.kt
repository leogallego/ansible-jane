package io.github.leogallego.ansiblejane.presentation.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IHostRepository
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Host
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
        _uiState.update { InventoryHostsUiState.Loading }
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
            _uiState.update { state ->
                (state as? InventoryHostsUiState.Success)?.copy(isLoadingMore = true) ?: state
            }
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
            _uiState.update { InventoryHostsUiState.Loading }
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
                _uiState.update {
                    if (allHosts.isEmpty()) InventoryHostsUiState.Empty("No hosts found")
                    else InventoryHostsUiState.Success(
                        hosts = allHosts.toList(),
                        hasMore = hostResult.hasMore
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { InventoryHostsUiState.Error(AppError.from(error)) }
            }
        )
    }
}

package io.github.leogallego.ansiblejane.presentation.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IHostRepository
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Host
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HostsViewModel(
    private val hostRepository: IHostRepository,
    private val tokenManager: ITokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<HostsUiState>(HostsUiState.Loading)
    val uiState: StateFlow<HostsUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var allHosts = mutableListOf<Host>()
    private var currentSearch: String? = null
    private var searchJob: Job? = null
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            tokenManager.activeInstance
                .distinctUntilChangedBy { it?.id }
                .collect { instance ->
                    fetchJob?.cancel()
                    if (instance != null) loadHosts()
                }
        }
    }

    fun loadHosts() {
        currentPage = 1
        allHosts.clear()
        _uiState.update { HostsUiState.Loading }
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchHosts()
        }
    }

    fun refresh() {
        currentPage = 1
        allHosts.clear()
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchHosts()
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current is HostsUiState.Success && current.hasMore && !current.isLoadingMore) {
            currentPage++
            _uiState.update { state ->
                (state as? HostsUiState.Success)?.copy(isLoadingMore = true) ?: state
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
            _uiState.update { HostsUiState.Loading }
            fetchHosts()
        }
    }

    private suspend fun fetchHosts(append: Boolean = false) {
        val result = hostRepository.getAllHosts(
            page = currentPage,
            search = currentSearch
        )

        result.fold(
            onSuccess = { listResult ->
                if (append) {
                    allHosts.addAll(listResult.hosts)
                } else {
                    allHosts = listResult.hosts.toMutableList()
                }

                _uiState.update {
                    if (allHosts.isEmpty()) HostsUiState.Empty("No hosts found")
                    else HostsUiState.Success(
                        hosts = allHosts.toList(),
                        hasMore = listResult.hasMore
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { HostsUiState.Error(AppError.from(error)) }
            }
        )
    }
}

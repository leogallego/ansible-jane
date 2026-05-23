package io.github.leogallego.ansiblejane.presentation.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IJobRepository
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Job
import io.github.leogallego.ansiblejane.model.JobStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class RecentJobsViewModel(
    private val jobRepository: IJobRepository,
    private val tokenManager: ITokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecentJobsUiState>(RecentJobsUiState.Loading)
    val uiState: StateFlow<RecentJobsUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val allJobs = mutableListOf<Job>()
    private var activeFilters = mutableSetOf<JobStatus>()
    private var fetchJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            tokenManager.activeInstance
                .distinctUntilChangedBy { it?.id }
                .collect { instance ->
                    fetchJob?.cancel()
                    if (instance != null) loadRecentJobs()
                }
        }
    }

    fun loadRecentJobs() {
        currentPage = 1
        allJobs.clear()
        _uiState.value = RecentJobsUiState.Loading
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchJobs()
        }
    }

    fun refresh() {
        currentPage = 1
        allJobs.clear()
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchJobs()
        }
    }

    fun toggleFilter(status: JobStatus) {
        if (status in activeFilters) {
            activeFilters.remove(status)
        } else {
            activeFilters.add(status)
        }
        currentPage = 1
        allJobs.clear()
        _uiState.value = RecentJobsUiState.Loading
        viewModelScope.launch {
            fetchJobs()
        }
    }

    fun clearFilters() {
        activeFilters.clear()
        currentPage = 1
        allJobs.clear()
        _uiState.value = RecentJobsUiState.Loading
        viewModelScope.launch {
            fetchJobs()
        }
    }

    fun getActiveFilters(): Set<JobStatus> = activeFilters.toSet()

    fun loadMore() {
        val current = _uiState.value
        if (current is RecentJobsUiState.Success && current.hasMore && !current.isLoadingMore) {
            currentPage++
            _uiState.value = current.copy(isLoadingMore = true)
            viewModelScope.launch {
                fetchJobs(append = true)
            }
        }
    }

    private suspend fun fetchJobs(append: Boolean = false) {
        val result = jobRepository.getRecentJobs(
            page = currentPage,
            statusFilters = activeFilters.toSet()
        )
        result.fold(
            onSuccess = { jobsResult ->
                if (append) {
                    allJobs.addAll(jobsResult.jobs)
                } else {
                    allJobs.clear()
                    allJobs.addAll(jobsResult.jobs)
                }
                _uiState.value = RecentJobsUiState.Success(
                    jobs = allJobs.toList(),
                    hasMore = jobsResult.hasMore,
                    activeFilters = activeFilters.toSet()
                )
            },
            onFailure = { error ->
                _uiState.value = RecentJobsUiState.Error(AppError.from(error))
            }
        )
    }
}

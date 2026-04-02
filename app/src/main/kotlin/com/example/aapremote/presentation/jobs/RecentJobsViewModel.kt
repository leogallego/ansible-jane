package com.example.aapremote.presentation.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.data.JobRepository
import com.example.aapremote.model.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecentJobsViewModel(
    private val jobRepository: JobRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecentJobsUiState>(RecentJobsUiState.Loading)
    val uiState: StateFlow<RecentJobsUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val allJobs = mutableListOf<Job>()

    init {
        loadRecentJobs()
    }

    fun loadRecentJobs() {
        currentPage = 1
        allJobs.clear()
        _uiState.value = RecentJobsUiState.Loading
        viewModelScope.launch {
            fetchJobs()
        }
    }

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
        val result = jobRepository.getRecentJobs(page = currentPage)
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
                    hasMore = jobsResult.hasMore
                )
            },
            onFailure = { error ->
                _uiState.value = RecentJobsUiState.Error(error.message ?: "Failed to load jobs")
            }
        )
    }
}

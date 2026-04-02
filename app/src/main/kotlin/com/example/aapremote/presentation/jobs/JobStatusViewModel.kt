package com.example.aapremote.presentation.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.data.JobRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class JobStatusViewModel(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: JobRepository
) : ViewModel() {

    private val jobId: Int = savedStateHandle.get<Int>("jobId") ?: -1

    private val _uiState = MutableStateFlow<JobStatusUiState>(JobStatusUiState.Loading)
    val uiState: StateFlow<JobStatusUiState> = _uiState.asStateFlow()

    init {
        if (jobId > 0) startPolling()
    }

    private fun startPolling() {
        viewModelScope.launch {
            jobRepository.pollJobStatus(jobId)
                .catch { e ->
                    _uiState.value = JobStatusUiState.Error(e.message ?: "Unknown error")
                }
                .collect { job ->
                    _uiState.value = if (job.status.isTerminal) {
                        JobStatusUiState.Completed(job)
                    } else {
                        JobStatusUiState.Active(job)
                    }
                }
        }
    }

    fun retry() {
        _uiState.value = JobStatusUiState.Loading
        startPolling()
    }
}

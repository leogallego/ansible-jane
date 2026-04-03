package com.example.aapremote.presentation.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.data.JobRepository
import com.example.aapremote.model.AppError
import kotlinx.coroutines.Job
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

    private var pollingJob: Job? = null

    fun startPolling() {
        if (jobId <= 0 || pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            jobRepository.pollJobStatus(jobId)
                .catch { e ->
                    _uiState.value = JobStatusUiState.Error(AppError.from(e))
                }
                .collect { job ->
                    val stdout = jobRepository.getJobStdout(jobId).getOrNull()
                    _uiState.value = if (job.status.isTerminal) {
                        JobStatusUiState.Completed(job, stdout)
                    } else {
                        JobStatusUiState.Active(job, stdout)
                    }
                }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun retry() {
        _uiState.value = JobStatusUiState.Loading
        startPolling()
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

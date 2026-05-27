package io.github.leogallego.ansiblejane.presentation.jobs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IJobRepository
import io.github.leogallego.ansiblejane.model.AppError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class JobStatusViewModel(
    savedStateHandle: SavedStateHandle,
    private val jobRepository: IJobRepository
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
                    _uiState.update { JobStatusUiState.Error(AppError.from(e)) }
                }
                .collect { job ->
                    val stdout = jobRepository.getJobStdout(jobId).getOrNull()
                    _uiState.update {
                        if (job.status.isTerminal) JobStatusUiState.Completed(job, stdout)
                        else JobStatusUiState.Active(job, stdout)
                    }
                }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun retry() {
        _uiState.update { JobStatusUiState.Loading }
        startPolling()
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

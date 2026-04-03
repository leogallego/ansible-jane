package com.example.aapremote.presentation.workflows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.data.JobRepository
import com.example.aapremote.data.WorkflowRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed interface NodeStdoutState {
    data object Loading : NodeStdoutState
    data class Loaded(val stdout: String) : NodeStdoutState
    data class Error(val message: String) : NodeStdoutState
}

class WorkflowJobStatusViewModel(
    savedStateHandle: SavedStateHandle,
    private val workflowRepository: WorkflowRepository,
    private val jobRepository: JobRepository
) : ViewModel() {

    private val workflowJobId: Int = savedStateHandle.get<Int>("workflowJobId") ?: -1

    private val _uiState = MutableStateFlow<WorkflowJobStatusUiState>(WorkflowJobStatusUiState.Loading)
    val uiState: StateFlow<WorkflowJobStatusUiState> = _uiState.asStateFlow()

    private val _expandedNodeId = MutableStateFlow<Int?>(null)
    val expandedNodeId: StateFlow<Int?> = _expandedNodeId.asStateFlow()

    private val _nodeStdout = MutableStateFlow<Map<Int, NodeStdoutState>>(emptyMap())
    val nodeStdout: StateFlow<Map<Int, NodeStdoutState>> = _nodeStdout.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling() {
        if (workflowJobId <= 0 || pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            workflowRepository.pollWorkflowJobStatus(workflowJobId)
                .catch { e ->
                    _uiState.value = WorkflowJobStatusUiState.Error(e.message ?: "Unknown error")
                }
                .collect { workflowJob ->
                    val nodes = workflowRepository.getWorkflowNodes(workflowJobId).getOrDefault(emptyList())
                    _uiState.value = if (workflowJob.status.isTerminal) {
                        WorkflowJobStatusUiState.Completed(workflowJob, nodes)
                    } else {
                        WorkflowJobStatusUiState.Active(workflowJob, nodes)
                    }
                }
        }
    }

    fun toggleNodeExpansion(jobId: Int) {
        if (_expandedNodeId.value == jobId) {
            _expandedNodeId.value = null
        } else {
            _expandedNodeId.value = jobId
            if (_nodeStdout.value[jobId] == null) {
                fetchNodeStdout(jobId)
            }
        }
    }

    private fun fetchNodeStdout(jobId: Int) {
        _nodeStdout.value = _nodeStdout.value + (jobId to NodeStdoutState.Loading)
        viewModelScope.launch {
            val result = jobRepository.getJobStdout(jobId)
            _nodeStdout.value = _nodeStdout.value + (jobId to result.fold(
                onSuccess = { stdout ->
                    if (stdout.isBlank()) NodeStdoutState.Loaded("No output available")
                    else NodeStdoutState.Loaded(stdout)
                },
                onFailure = { e -> NodeStdoutState.Error(e.message ?: "Failed to load output") }
            ))
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun retry() {
        _uiState.value = WorkflowJobStatusUiState.Loading
        startPolling()
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

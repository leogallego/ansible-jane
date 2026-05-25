package io.github.leogallego.ansiblejane.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IWorkflowRepository
import io.github.leogallego.ansiblejane.model.WorkflowApproval
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val approvals: List<WorkflowApproval> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val pendingCount: Int get() = approvals.size
}

class NotificationsViewModel(
    private val workflowRepository: IWorkflowRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var lastFetchTime: Long = 0L

    init {
        refresh()
    }

    fun refreshIfStale(maxAgeMs: Long = 30_000L) {
        if (System.currentTimeMillis() - lastFetchTime > maxAgeMs) {
            refresh()
        }
    }

    fun refresh() {
        val oldJob = refreshJob
        refreshJob = viewModelScope.launch {
            oldJob?.cancelAndJoin()
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            workflowRepository.getPendingApprovals(pageSize = 50).fold(
                onSuccess = { result ->
                    lastFetchTime = System.currentTimeMillis()
                    _uiState.value = NotificationsUiState(
                        approvals = result.approvals,
                        isLoading = false,
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load approvals"
                    )
                }
            )
        }
    }
}

package io.github.leogallego.ansiblejane.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IWorkflowRepository
import io.github.leogallego.ansiblejane.model.WorkflowApproval
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

    init {
        refresh()
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            workflowRepository.getPendingApprovals(pageSize = 50).fold(
                onSuccess = { result ->
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

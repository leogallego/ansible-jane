package io.github.leogallego.ansiblejane.presentation.approval

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IWorkflowRepository
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.WorkflowApproval
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ApprovalDetailUiState {
    data object Loading : ApprovalDetailUiState
    data class Ready(val approval: WorkflowApproval) : ApprovalDetailUiState
    data class ActionInProgress(val approval: WorkflowApproval, val action: String) : ApprovalDetailUiState
    data class Completed(val approval: WorkflowApproval, val action: String) : ApprovalDetailUiState
    data class Error(val error: AppError) : ApprovalDetailUiState
}

class ApprovalDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val workflowRepository: IWorkflowRepository
) : ViewModel() {

    private val approvalId: Int = savedStateHandle.get<Int>("approvalId") ?: -1

    private val _uiState = MutableStateFlow<ApprovalDetailUiState>(ApprovalDetailUiState.Loading)
    val uiState: StateFlow<ApprovalDetailUiState> = _uiState.asStateFlow()

    init {
        loadApproval()
    }

    fun loadApproval() {
        if (approvalId <= 0) {
            _uiState.update { ApprovalDetailUiState.Error(AppError.Unknown("Invalid approval ID")) }
            return
        }

        _uiState.update { ApprovalDetailUiState.Loading }
        viewModelScope.launch {
            workflowRepository.getWorkflowApproval(approvalId).fold(
                onSuccess = { approval ->
                    _uiState.update { ApprovalDetailUiState.Ready(approval) }
                },
                onFailure = { e ->
                    _uiState.update { ApprovalDetailUiState.Error(AppError.from(e)) }
                }
            )
        }
    }

    fun approve() {
        val currentState = _uiState.value
        val approval = when (currentState) {
            is ApprovalDetailUiState.Ready -> currentState.approval
            else -> return
        }

        _uiState.update { ApprovalDetailUiState.ActionInProgress(approval, "approve") }
        viewModelScope.launch {
            workflowRepository.approveWorkflow(approvalId).fold(
                onSuccess = {
                    _uiState.update { ApprovalDetailUiState.Completed(approval, "approved") }
                },
                onFailure = { e ->
                    _uiState.update { ApprovalDetailUiState.Error(AppError.from(e)) }
                }
            )
        }
    }

    fun deny() {
        val currentState = _uiState.value
        val approval = when (currentState) {
            is ApprovalDetailUiState.Ready -> currentState.approval
            else -> return
        }

        _uiState.update { ApprovalDetailUiState.ActionInProgress(approval, "deny") }
        viewModelScope.launch {
            workflowRepository.denyWorkflow(approvalId).fold(
                onSuccess = {
                    _uiState.update { ApprovalDetailUiState.Completed(approval, "denied") }
                },
                onFailure = { e ->
                    _uiState.update { ApprovalDetailUiState.Error(AppError.from(e)) }
                }
            )
        }
    }
}

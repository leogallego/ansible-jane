package io.github.leogallego.ansiblejane.presentation.workflows

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IWorkflowRepository
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.WorkflowJobTemplateNode
import java.net.URLDecoder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WorkflowTemplateDetailUiState {
    data object Loading : WorkflowTemplateDetailUiState
    data class Success(val nodes: List<WorkflowJobTemplateNode>) : WorkflowTemplateDetailUiState
    data class Error(val error: AppError) : WorkflowTemplateDetailUiState
}

sealed interface LaunchFromDetailState {
    data object Idle : LaunchFromDetailState
    data object Launching : LaunchFromDetailState
    data class Launched(val workflowJobId: Int) : LaunchFromDetailState
    data class Failed(val message: String) : LaunchFromDetailState
}

class WorkflowTemplateDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val workflowRepository: IWorkflowRepository
) : ViewModel() {

    val templateId: Int = savedStateHandle.get<Int>("templateId") ?: -1
    val templateName: String = savedStateHandle.get<String>("templateName")?.let {
        URLDecoder.decode(it, "UTF-8")
    } ?: ""

    private val _uiState = MutableStateFlow<WorkflowTemplateDetailUiState>(WorkflowTemplateDetailUiState.Loading)
    val uiState: StateFlow<WorkflowTemplateDetailUiState> = _uiState.asStateFlow()

    private val _launchState = MutableStateFlow<LaunchFromDetailState>(LaunchFromDetailState.Idle)
    val launchState: StateFlow<LaunchFromDetailState> = _launchState.asStateFlow()

    init {
        loadNodes()
    }

    private fun loadNodes() {
        if (templateId <= 0) return
        _uiState.value = WorkflowTemplateDetailUiState.Loading
        viewModelScope.launch {
            workflowRepository.getWorkflowTemplateNodes(templateId)
                .onSuccess { nodes ->
                    _uiState.value = WorkflowTemplateDetailUiState.Success(nodes)
                }
                .onFailure { e ->
                    _uiState.value = WorkflowTemplateDetailUiState.Error(AppError.from(e))
                }
        }
    }

    fun launch() {
        if (_launchState.value is LaunchFromDetailState.Launching) return
        _launchState.value = LaunchFromDetailState.Launching
        viewModelScope.launch {
            workflowRepository.launchWorkflow(templateId)
                .onSuccess { jobId ->
                    _launchState.value = LaunchFromDetailState.Launched(jobId)
                }
                .onFailure { e ->
                    _launchState.value = LaunchFromDetailState.Failed(e.message ?: "Launch failed")
                }
        }
    }

    fun resetLaunchState() {
        _launchState.value = LaunchFromDetailState.Idle
    }

    fun retry() {
        loadNodes()
    }
}

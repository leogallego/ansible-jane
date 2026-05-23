package io.github.leogallego.ansiblejane.presentation.workflows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.data.IWorkflowRepository
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Label
import io.github.leogallego.ansiblejane.model.WorkflowJobTemplate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class WorkflowTemplatesViewModel(
    private val workflowRepository: IWorkflowRepository,
    private val tokenManager: ITokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<WorkflowTemplatesUiState>(WorkflowTemplatesUiState.Idle)
    val uiState: StateFlow<WorkflowTemplatesUiState> = _uiState.asStateFlow()

    private val _launchState = MutableStateFlow<WorkflowLaunchState>(WorkflowLaunchState.Idle)
    val launchState: StateFlow<WorkflowLaunchState> = _launchState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var currentPage = 1
    private var currentSearch: String? = null
    private var currentLabelFilter: String? = null
    private var allTemplates = mutableListOf<WorkflowJobTemplate>()
    private var searchJob: Job? = null
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            tokenManager.activeInstance
                .distinctUntilChangedBy { it?.id }
                .collect { instance ->
                    fetchJob?.cancel()
                    if (instance != null) loadTemplates()
                }
        }
    }

    fun loadTemplates() {
        currentPage = 1
        allTemplates.clear()
        _uiState.value = WorkflowTemplatesUiState.Loading
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchTemplates()
        }
    }

    fun refresh() {
        currentPage = 1
        allTemplates.clear()
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchTemplates()
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current is WorkflowTemplatesUiState.Success && current.hasMore && !current.isLoadingMore) {
            currentPage++
            _uiState.value = current.copy(isLoadingMore = true)
            fetchJob = viewModelScope.launch {
                fetchTemplates(append = true)
            }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            currentSearch = query.ifBlank { null }
            currentPage = 1
            allTemplates.clear()
            _uiState.value = WorkflowTemplatesUiState.Loading
            fetchTemplates()
        }
    }

    fun filterByLabel(label: Label?) {
        currentLabelFilter = label?.name
        currentPage = 1
        allTemplates.clear()
        _uiState.value = WorkflowTemplatesUiState.Loading
        viewModelScope.launch {
            fetchTemplates()
        }
    }

    fun requestLaunch(template: WorkflowJobTemplate) {
        _launchState.value = if (template.askVariablesOnLaunch) {
            WorkflowLaunchState.EnteringVars(template)
        } else {
            WorkflowLaunchState.Confirming(template)
        }
    }

    fun confirmLaunch(extraVars: String? = null) {
        val template = when (val state = _launchState.value) {
            is WorkflowLaunchState.Confirming -> state.template
            is WorkflowLaunchState.EnteringVars -> state.template
            else -> return
        }

        _launchState.value = WorkflowLaunchState.Launching
        viewModelScope.launch {
            val result = workflowRepository.launchWorkflow(template.id, extraVars)
            _launchState.value = result.fold(
                onSuccess = { workflowJobId -> WorkflowLaunchState.Launched(workflowJobId) },
                onFailure = { error -> WorkflowLaunchState.LaunchError(AppError.from(error)) }
            )
        }
    }

    fun cancelLaunch() {
        _launchState.value = WorkflowLaunchState.Idle
    }

    fun resetLaunchState() {
        _launchState.value = WorkflowLaunchState.Idle
    }

    private suspend fun fetchTemplates(append: Boolean = false) {
        val result = workflowRepository.getWorkflowTemplates(
            page = currentPage,
            search = currentSearch,
            labelFilter = currentLabelFilter
        )

        result.fold(
            onSuccess = { listResult ->
                if (append) {
                    allTemplates.addAll(listResult.templates)
                } else {
                    allTemplates = listResult.templates.toMutableList()
                }

                val labels = allTemplates
                    .flatMap { it.summaryFields.labels.results }
                    .distinctBy { it.id }
                    .sortedBy { it.name }

                _uiState.value = WorkflowTemplatesUiState.Success(
                    templates = allTemplates.toList(),
                    availableLabels = labels,
                    hasMore = listResult.hasMore
                )
            },
            onFailure = { error ->
                _uiState.value = WorkflowTemplatesUiState.Error(AppError.from(error))
            }
        )
    }
}

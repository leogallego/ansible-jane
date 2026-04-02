package com.example.aapremote.presentation.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.data.TemplateRepository
import com.example.aapremote.model.JobTemplate
import com.example.aapremote.model.Label
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TemplatesViewModel(
    private val templateRepository: TemplateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TemplatesUiState>(TemplatesUiState.Idle)
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    private val _launchState = MutableStateFlow<LaunchState>(LaunchState.Idle)
    val launchState: StateFlow<LaunchState> = _launchState.asStateFlow()

    private var currentPage = 1
    private var currentSearch: String? = null
    private var currentLabelFilter: String? = null
    private var allTemplates = mutableListOf<JobTemplate>()
    private var searchJob: Job? = null

    init {
        loadTemplates()
    }

    fun loadTemplates() {
        currentPage = 1
        allTemplates.clear()
        _uiState.value = TemplatesUiState.Loading
        viewModelScope.launch {
            fetchTemplates()
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current is TemplatesUiState.Success && current.hasMore && !current.isLoadingMore) {
            currentPage++
            _uiState.value = current.copy(isLoadingMore = true)
            viewModelScope.launch {
                fetchTemplates(append = true)
            }
        }
    }

    fun search(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            currentSearch = query.ifBlank { null }
            currentPage = 1
            allTemplates.clear()
            _uiState.value = TemplatesUiState.Loading
            fetchTemplates()
        }
    }

    fun filterByLabel(label: Label?) {
        currentLabelFilter = label?.name
        currentPage = 1
        allTemplates.clear()
        _uiState.value = TemplatesUiState.Loading
        viewModelScope.launch {
            fetchTemplates()
        }
    }

    fun clearFilters() {
        currentSearch = null
        currentLabelFilter = null
        currentPage = 1
        allTemplates.clear()
        _uiState.value = TemplatesUiState.Loading
        viewModelScope.launch {
            fetchTemplates()
        }
    }

    fun requestLaunch(template: JobTemplate) {
        _launchState.value = if (template.askVariablesOnLaunch) {
            LaunchState.EnteringVars(template)
        } else {
            LaunchState.Confirming(template)
        }
    }

    fun confirmLaunch(extraVars: String? = null) {
        val template = when (val state = _launchState.value) {
            is LaunchState.Confirming -> state.template
            is LaunchState.EnteringVars -> state.template
            else -> return
        }

        _launchState.value = LaunchState.Launching
        viewModelScope.launch {
            val result = templateRepository.launchJob(template.id, extraVars)
            _launchState.value = result.fold(
                onSuccess = { jobId -> LaunchState.Launched(jobId) },
                onFailure = { error -> LaunchState.LaunchError(error.message ?: "Launch failed") }
            )
        }
    }

    fun cancelLaunch() {
        _launchState.value = LaunchState.Idle
    }

    fun resetLaunchState() {
        _launchState.value = LaunchState.Idle
    }

    private suspend fun fetchTemplates(append: Boolean = false) {
        val result = templateRepository.getTemplates(
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

                _uiState.value = TemplatesUiState.Success(
                    templates = allTemplates.toList(),
                    availableLabels = labels,
                    hasMore = listResult.hasMore
                )
            },
            onFailure = { error ->
                _uiState.value = TemplatesUiState.Error(error.message ?: "Failed to load templates")
            }
        )
    }
}

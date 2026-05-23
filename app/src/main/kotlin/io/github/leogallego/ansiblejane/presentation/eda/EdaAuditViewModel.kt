package io.github.leogallego.ansiblejane.presentation.eda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IEdaAuditRepository
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.EdaRuleAudit
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import retrofit2.HttpException

class EdaAuditViewModel(
    private val edaAuditRepository: IEdaAuditRepository,
    private val tokenManager: ITokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<EdaAuditUiState>(EdaAuditUiState.Loading)
    val uiState: StateFlow<EdaAuditUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private val allAuditRules = mutableListOf<EdaRuleAudit>()
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            tokenManager.activeInstance
                .distinctUntilChangedBy { it?.id }
                .collect { instance ->
                    fetchJob?.cancel()
                    if (instance != null) loadAuditRules()
                }
        }
    }

    fun loadAuditRules() {
        currentPage = 1
        allAuditRules.clear()
        _uiState.value = EdaAuditUiState.Loading
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchAuditRules()
        }
    }

    fun refresh() {
        currentPage = 1
        allAuditRules.clear()
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchAuditRules()
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current is EdaAuditUiState.Success && current.hasMore && !current.isLoadingMore) {
            currentPage++
            _uiState.value = current.copy(isLoadingMore = true)
            fetchJob = viewModelScope.launch {
                fetchAuditRules(append = true)
            }
        }
    }

    private suspend fun fetchAuditRules(append: Boolean = false) {
        val result = edaAuditRepository.getAuditRules(page = currentPage)
        result.fold(
            onSuccess = { auditResult ->
                if (append) {
                    allAuditRules.addAll(auditResult.auditRules)
                } else {
                    allAuditRules.clear()
                    allAuditRules.addAll(auditResult.auditRules)
                }
                if (allAuditRules.isEmpty()) {
                    _uiState.value = EdaAuditUiState.Empty("No EDA audit events")
                } else {
                    _uiState.value = EdaAuditUiState.Success(
                        auditRules = allAuditRules.toList(),
                        hasMore = auditResult.hasMore
                    )
                }
            },
            onFailure = { error ->
                if (error is HttpException && error.code() in listOf(404, 502, 503)) {
                    _uiState.value = EdaAuditUiState.Empty(
                        "EDA is not configured on this AAP instance"
                    )
                } else {
                    _uiState.value = EdaAuditUiState.Error(AppError.from(error))
                }
            }
        )
    }
}

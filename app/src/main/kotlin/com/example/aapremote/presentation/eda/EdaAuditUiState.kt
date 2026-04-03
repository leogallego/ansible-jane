package com.example.aapremote.presentation.eda

import com.example.aapremote.model.EdaRuleAudit

sealed interface EdaAuditUiState {
    data object Loading : EdaAuditUiState
    data class Success(
        val auditRules: List<EdaRuleAudit>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : EdaAuditUiState
    data class Empty(val message: String) : EdaAuditUiState
    data class Error(val message: String) : EdaAuditUiState
}

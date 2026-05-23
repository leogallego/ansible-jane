package io.github.leogallego.ansiblejane.presentation.eda

import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.EdaRuleAudit

sealed interface EdaAuditUiState {
    data object Loading : EdaAuditUiState
    data class Success(
        val auditRules: List<EdaRuleAudit>,
        val hasMore: Boolean,
        val isLoadingMore: Boolean = false
    ) : EdaAuditUiState
    data class Empty(val message: String) : EdaAuditUiState
    data class Error(val error: AppError) : EdaAuditUiState
}

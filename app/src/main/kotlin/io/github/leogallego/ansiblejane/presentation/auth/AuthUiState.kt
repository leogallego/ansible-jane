package io.github.leogallego.ansiblejane.presentation.auth

import io.github.leogallego.ansiblejane.model.AppError

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val username: String) : AuthUiState
    data class Error(val error: AppError) : AuthUiState
}

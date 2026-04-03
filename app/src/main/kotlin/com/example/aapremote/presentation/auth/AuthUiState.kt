package com.example.aapremote.presentation.auth

import com.example.aapremote.model.AppError

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val username: String) : AuthUiState
    data class Error(val error: AppError) : AuthUiState
}

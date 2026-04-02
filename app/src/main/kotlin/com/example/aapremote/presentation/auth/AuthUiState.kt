package com.example.aapremote.presentation.auth

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(val username: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

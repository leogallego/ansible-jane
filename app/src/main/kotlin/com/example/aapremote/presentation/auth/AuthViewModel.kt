package com.example.aapremote.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkExistingCredentials()
    }

    fun connect(baseUrl: String, token: String, trustSelfSigned: Boolean) {
        if (baseUrl.isBlank() || token.isBlank()) {
            _uiState.value = AuthUiState.Error("URL and token are required")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.validateCredentials(baseUrl, token, trustSelfSigned)
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user.username) },
                onFailure = { error -> AuthUiState.Error(error.message ?: "Unknown error") }
            )
        }
    }

    fun checkExistingCredentials() {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.checkExistingCredentials()
            _uiState.value = when {
                result == null -> AuthUiState.Idle
                result.isSuccess -> AuthUiState.Success(result.getOrThrow().username)
                else -> AuthUiState.Idle
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

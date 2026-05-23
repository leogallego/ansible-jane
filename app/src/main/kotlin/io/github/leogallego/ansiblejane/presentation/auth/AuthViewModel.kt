package io.github.leogallego.ansiblejane.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IAuthRepository
import io.github.leogallego.ansiblejane.model.AppError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: IAuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // checkExistingCredentials() is called from the composable, not init,
    // so add-instance mode can skip it

    fun connect(
        baseUrl: String,
        token: String,
        trustSelfSigned: Boolean,
        alias: String? = null,
        existingInstanceId: String? = null
    ) {
        if (baseUrl.isBlank() || token.isBlank()) {
            _uiState.value = AuthUiState.Error(
                AppError.Unknown(message = "URL and token are required")
            )
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.validateCredentials(
                baseUrl = baseUrl,
                token = token,
                trustSelfSigned = trustSelfSigned,
                alias = alias?.ifBlank { null },
                existingInstanceId = existingInstanceId
            )
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user.username) },
                onFailure = { error -> AuthUiState.Error(AppError.from(error)) }
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

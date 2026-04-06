package com.example.aapremote.presentation.settings

import com.example.aapremote.model.AapInstance

sealed interface SettingsUiState {
    data object Idle : SettingsUiState
    data object Loading : SettingsUiState
    data class Success(
        val instances: List<AapInstance>,
        val selectedInstance: AapInstance?,
        val selectedInstanceForDetails: AapInstance? = null
    ) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

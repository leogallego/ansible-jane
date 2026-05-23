package io.github.leogallego.ansiblejane.presentation.settings

import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.ui.components.TimeFormat

sealed interface SettingsUiState {
    data object Idle : SettingsUiState
    data object Loading : SettingsUiState
    data class Success(
        val instances: List<AapInstance>,
        val selectedInstance: AapInstance?,
        val selectedInstanceForDetails: AapInstance? = null,
        val timezoneId: String? = null,
        val timeFormat: TimeFormat = TimeFormat.SYSTEM
    ) : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

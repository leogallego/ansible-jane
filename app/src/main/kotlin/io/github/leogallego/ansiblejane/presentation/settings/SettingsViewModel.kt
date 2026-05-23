package io.github.leogallego.ansiblejane.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.data.IUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import io.github.leogallego.ansiblejane.ui.components.DateFormatter
import io.github.leogallego.ansiblejane.ui.components.TimeFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.ZoneId

class SettingsViewModel(
    private val tokenManager: ITokenManager,
    private val apiProvider: IAapApiProvider,
    private val userPreferences: IUserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tokenManager.instances,
                tokenManager.activeInstance,
                userPreferences.timezoneId,
                userPreferences.timeFormat
            ) { instances, active, timezone, timeFormat ->
                DateFormatter.zoneOverride = timezone?.let {
                    try { ZoneId.of(it) } catch (_: Exception) { null }
                }
                DateFormatter.timeFormat = timeFormat
                SettingsUiState.Success(
                    instances = instances,
                    selectedInstance = active,
                    timezoneId = timezone,
                    timeFormat = timeFormat
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun switchInstance(instanceId: String) {
        viewModelScope.launch {
            tokenManager.setActiveInstance(instanceId)
        }
    }

    fun removeInstance(instanceId: String) {
        viewModelScope.launch {
            apiProvider.evictInstance(instanceId)
            tokenManager.removeInstance(instanceId)
        }
    }

    fun showInstanceDetails(instanceId: String) {
        val current = _uiState.value
        if (current is SettingsUiState.Success) {
            val instance = current.instances.find { it.id == instanceId }
            _uiState.value = current.copy(selectedInstanceForDetails = instance)
        }
    }

    fun dismissDetails() {
        val current = _uiState.value
        if (current is SettingsUiState.Success) {
            _uiState.value = current.copy(selectedInstanceForDetails = null)
        }
    }

    fun setTimezone(zoneId: String?) {
        viewModelScope.launch {
            userPreferences.setTimezoneId(zoneId)
        }
    }

    fun setTimeFormat(format: TimeFormat) {
        viewModelScope.launch {
            userPreferences.setTimeFormat(format)
        }
    }
}

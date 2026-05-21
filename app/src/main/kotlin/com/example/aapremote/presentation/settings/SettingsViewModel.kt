package com.example.aapremote.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.data.ITokenManager
import com.example.aapremote.model.AapInstance
import com.example.aapremote.network.IAapApiProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val tokenManager: ITokenManager,
    private val apiProvider: IAapApiProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                tokenManager.instances,
                tokenManager.activeInstance
            ) { instances, active ->
                SettingsUiState.Success(
                    instances = instances,
                    selectedInstance = active
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
}

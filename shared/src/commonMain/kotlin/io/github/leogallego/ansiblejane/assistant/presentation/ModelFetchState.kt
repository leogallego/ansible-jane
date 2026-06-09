package io.github.leogallego.ansiblejane.assistant.presentation

sealed interface ModelFetchState {
    data object Idle : ModelFetchState
    data object Loading : ModelFetchState
    data class Success(val count: Int) : ModelFetchState
    data class Error(val message: String) : ModelFetchState
}

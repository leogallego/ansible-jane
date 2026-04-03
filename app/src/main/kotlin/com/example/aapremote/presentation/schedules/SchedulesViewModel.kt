package com.example.aapremote.presentation.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aapremote.data.ScheduleRepository
import com.example.aapremote.model.Schedule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SchedulesViewModel(
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SchedulesUiState>(SchedulesUiState.Loading)
    val uiState: StateFlow<SchedulesUiState> = _uiState.asStateFlow()

    private val _toggleError = MutableSharedFlow<String>()
    val toggleError: SharedFlow<String> = _toggleError.asSharedFlow()

    private var currentPage = 1
    private val allSchedules = mutableListOf<Schedule>()

    init {
        loadSchedules()
    }

    fun loadSchedules() {
        currentPage = 1
        allSchedules.clear()
        _uiState.value = SchedulesUiState.Loading
        viewModelScope.launch {
            fetchSchedules()
        }
    }

    fun refresh() {
        currentPage = 1
        allSchedules.clear()
        viewModelScope.launch {
            fetchSchedules()
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current is SchedulesUiState.Success && current.hasMore && !current.isLoadingMore) {
            currentPage++
            _uiState.value = current.copy(isLoadingMore = true)
            viewModelScope.launch {
                fetchSchedules(append = true)
            }
        }
    }

    fun toggleSchedule(schedule: Schedule) {
        val newEnabled = !schedule.enabled
        val index = allSchedules.indexOfFirst { it.id == schedule.id }
        if (index == -1) return

        allSchedules[index] = schedule.copy(enabled = newEnabled)
        updateSuccessState()

        viewModelScope.launch {
            val result = scheduleRepository.toggleSchedule(schedule.id, newEnabled)
            result.fold(
                onSuccess = { updated ->
                    allSchedules[index] = updated
                    updateSuccessState()
                },
                onFailure = { error ->
                    allSchedules[index] = schedule
                    updateSuccessState()
                    _toggleError.emit(error.message ?: "Failed to toggle schedule")
                }
            )
        }
    }

    private fun updateSuccessState() {
        val current = _uiState.value
        if (current is SchedulesUiState.Success) {
            _uiState.value = current.copy(schedules = allSchedules.toList())
        }
    }

    private suspend fun fetchSchedules(append: Boolean = false) {
        val result = scheduleRepository.getSchedules(page = currentPage)
        result.fold(
            onSuccess = { schedulesResult ->
                if (append) {
                    allSchedules.addAll(schedulesResult.schedules)
                } else {
                    allSchedules.clear()
                    allSchedules.addAll(schedulesResult.schedules)
                }
                _uiState.value = SchedulesUiState.Success(
                    schedules = allSchedules.toList(),
                    hasMore = schedulesResult.hasMore
                )
            },
            onFailure = { error ->
                _uiState.value = SchedulesUiState.Error(error.message ?: "Failed to load schedules")
            }
        )
    }
}

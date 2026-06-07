package io.github.leogallego.ansiblejane.presentation.schedules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.leogallego.ansiblejane.data.IScheduleRepository
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Schedule
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch

class SchedulesViewModel(
    private val scheduleRepository: IScheduleRepository,
    private val tokenManager: ITokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<SchedulesUiState>(SchedulesUiState.Loading)
    val uiState: StateFlow<SchedulesUiState> = _uiState.asStateFlow()

    private val _toggleError = MutableSharedFlow<String>()
    val toggleError: SharedFlow<String> = _toggleError.asSharedFlow()

    private var currentPage = 1
    private val allSchedules = mutableListOf<Schedule>()
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            tokenManager.activeInstance
                .distinctUntilChangedBy { it?.id }
                .collect { instance ->
                    fetchJob?.cancel()
                    if (instance != null) loadSchedules()
                }
        }
    }

    fun loadSchedules() {
        currentPage = 1
        allSchedules.clear()
        _uiState.update { SchedulesUiState.Loading }
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchSchedules()
        }
    }

    fun refresh() {
        currentPage = 1
        allSchedules.clear()
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            fetchSchedules()
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current is SchedulesUiState.Success && current.hasMore && !current.isLoadingMore) {
            currentPage++
            _uiState.update { state ->
                (state as? SchedulesUiState.Success)?.copy(isLoadingMore = true) ?: state
            }
            fetchJob = viewModelScope.launch {
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
        _uiState.update { current ->
            if (current is SchedulesUiState.Success) current.copy(schedules = allSchedules.toList())
            else current
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
                _uiState.update {
                    SchedulesUiState.Success(
                        schedules = allSchedules.toList(),
                        hasMore = schedulesResult.hasMore
                    )
                }
            },
            onFailure = { error ->
                _uiState.update { SchedulesUiState.Error(AppError.from(error)) }
            }
        )
    }
}

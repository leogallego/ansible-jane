package io.github.leogallego.ansiblejane.presentation.dashboard

import androidx.compose.runtime.Immutable
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Job
import io.github.leogallego.ansiblejane.model.Schedule

enum class HealthStatus { GREEN, YELLOW, RED }

data class DayJobStats(
    val label: String,
    val successful: Int,
    val failed: Int,
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState

    @Immutable
    data class Success(
        val activeJobsCount: Int,
        val failedCount24h: Int,
        val successfulCount24h: Int,
        val recentFailures: List<Job>,
        val healthStatus: HealthStatus,
        val inventoryCount: Int = 0,
        val hostCount: Int = 0,
        val templateCount: Int = 0,
        val projectCount: Int = 0,
        val edaActivationsCount: Int? = null,
        val edaActiveRulebooksCount: Int? = null,
        val jobHistory7d: List<DayJobStats> = emptyList(),
        val upcomingSchedules: List<Schedule> = emptyList(),
        val controllerVersion: String? = null,
        val edaVersion: String? = null,
        val gatewayVersion: String? = null,
        val licenseType: String? = null,
        val instanceUrl: String? = null,
        val instanceAlias: String? = null,
    ) : DashboardUiState

    data class Error(val error: AppError) : DashboardUiState
}

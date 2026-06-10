package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.ui.components.ThemeMode
import io.github.leogallego.ansiblejane.ui.components.TimeFormat
import kotlinx.coroutines.flow.Flow

enum class PollInterval(val minutes: Int, val displayName: String) {
    MINUTES_15(15, "15 minutes"),
    MINUTES_30(30, "30 minutes"),
    MINUTES_60(60, "1 hour")
}

interface IUserPreferencesRepository {
    val timezoneId: Flow<String?>
    val timeFormat: Flow<TimeFormat>
    val themeMode: Flow<ThemeMode>
    val approvalPollInterval: Flow<PollInterval>
    fun favoriteTemplateIds(instanceId: String): Flow<Set<Int>>
    fun approvalPollingEnabled(instanceId: String): Flow<Boolean>
    suspend fun setTimezoneId(zoneId: String?)
    suspend fun setTimeFormat(format: TimeFormat)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setApprovalPollInterval(interval: PollInterval)
    suspend fun setApprovalPollingEnabled(instanceId: String, enabled: Boolean)
    suspend fun toggleFavoriteTemplate(instanceId: String, templateId: Int)
}

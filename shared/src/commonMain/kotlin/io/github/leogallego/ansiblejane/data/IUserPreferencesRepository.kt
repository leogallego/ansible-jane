package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.ui.components.ThemeMode
import io.github.leogallego.ansiblejane.ui.components.TimeFormat
import kotlinx.coroutines.flow.Flow

interface IUserPreferencesRepository {
    val timezoneId: Flow<String?>
    val timeFormat: Flow<TimeFormat>
    val themeMode: Flow<ThemeMode>
    val favoriteTemplateIds: Flow<Set<Int>>
    suspend fun setTimezoneId(zoneId: String?)
    suspend fun setTimeFormat(format: TimeFormat)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun toggleFavoriteTemplate(templateId: Int)
}

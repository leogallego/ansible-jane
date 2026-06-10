package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.data.IUserPreferencesRepository
import io.github.leogallego.ansiblejane.ui.components.ThemeMode
import io.github.leogallego.ansiblejane.ui.components.TimeFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferencesRepository : IUserPreferencesRepository {
    private val _timezoneId = MutableStateFlow<String?>(null)
    override val timezoneId: Flow<String?> = _timezoneId

    private val _timeFormat = MutableStateFlow(TimeFormat.SYSTEM)
    override val timeFormat: Flow<TimeFormat> = _timeFormat

    override suspend fun setTimezoneId(zoneId: String?) {
        _timezoneId.value = zoneId
    }

    override suspend fun setTimeFormat(format: TimeFormat) {
        _timeFormat.value = format
    }

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: Flow<ThemeMode> = _themeMode

    override suspend fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }

    private val _favoriteTemplateIds = MutableStateFlow<Set<Int>>(emptySet())
    override val favoriteTemplateIds: Flow<Set<Int>> = _favoriteTemplateIds

    override suspend fun toggleFavoriteTemplate(templateId: Int) {
        _favoriteTemplateIds.value = if (templateId in _favoriteTemplateIds.value) {
            _favoriteTemplateIds.value - templateId
        } else {
            _favoriteTemplateIds.value + templateId
        }
    }
}

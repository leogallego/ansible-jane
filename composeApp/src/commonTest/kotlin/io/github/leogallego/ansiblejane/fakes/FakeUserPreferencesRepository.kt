package io.github.leogallego.ansiblejane.fakes

import io.github.leogallego.ansiblejane.data.IUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.PollInterval
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

    private val _pollInterval = MutableStateFlow(PollInterval.MINUTES_15)
    override val approvalPollInterval: Flow<PollInterval> = _pollInterval

    override suspend fun setApprovalPollInterval(interval: PollInterval) {
        _pollInterval.value = interval
    }

    private val _pollingEnabled = mutableMapOf<String, MutableStateFlow<Boolean>>()

    override fun approvalPollingEnabled(instanceId: String): Flow<Boolean> =
        _pollingEnabled.getOrPut(instanceId) { MutableStateFlow(true) }

    override suspend fun setApprovalPollingEnabled(instanceId: String, enabled: Boolean) {
        _pollingEnabled.getOrPut(instanceId) { MutableStateFlow(true) }.value = enabled
    }

    private val _favorites = mutableMapOf<String, MutableStateFlow<Set<Int>>>()

    override fun favoriteTemplateIds(instanceId: String): Flow<Set<Int>> =
        _favorites.getOrPut(instanceId) { MutableStateFlow(emptySet()) }

    override suspend fun toggleFavoriteTemplate(instanceId: String, templateId: Int) {
        val flow = _favorites.getOrPut(instanceId) { MutableStateFlow(emptySet()) }
        flow.value = if (templateId in flow.value) flow.value - templateId else flow.value + templateId
    }
}

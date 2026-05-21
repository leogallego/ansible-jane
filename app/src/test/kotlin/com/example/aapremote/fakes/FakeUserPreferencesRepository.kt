package com.example.aapremote.fakes

import com.example.aapremote.data.IUserPreferencesRepository
import com.example.aapremote.ui.components.TimeFormat
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
}

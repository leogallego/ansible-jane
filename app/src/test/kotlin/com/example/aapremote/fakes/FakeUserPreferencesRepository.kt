package com.example.aapremote.fakes

import com.example.aapremote.data.IUserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeUserPreferencesRepository : IUserPreferencesRepository {
    private val _timezoneId = MutableStateFlow<String?>(null)
    override val timezoneId: Flow<String?> = _timezoneId

    override suspend fun setTimezoneId(zoneId: String?) {
        _timezoneId.value = zoneId
    }
}

package com.example.aapremote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

interface IUserPreferencesRepository {
    val timezoneId: Flow<String?>
    suspend fun setTimezoneId(zoneId: String?)
}

class UserPreferencesRepository(
    private val context: Context
) : IUserPreferencesRepository {

    override val timezoneId: Flow<String?> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_TIMEZONE]
    }

    override suspend fun setTimezoneId(zoneId: String?) {
        context.userPreferencesDataStore.edit { prefs ->
            if (zoneId == null) {
                prefs.remove(KEY_TIMEZONE)
            } else {
                prefs[KEY_TIMEZONE] = zoneId
            }
        }
    }

    companion object {
        private val KEY_TIMEZONE = stringPreferencesKey("timezone_override")
    }
}

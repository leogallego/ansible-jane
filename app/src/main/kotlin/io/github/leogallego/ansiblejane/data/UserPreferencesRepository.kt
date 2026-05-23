package io.github.leogallego.ansiblejane.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.leogallego.ansiblejane.ui.components.ThemeMode
import io.github.leogallego.ansiblejane.ui.components.TimeFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "user_preferences"
)

interface IUserPreferencesRepository {
    val timezoneId: Flow<String?>
    val timeFormat: Flow<TimeFormat>
    val themeMode: Flow<ThemeMode>
    suspend fun setTimezoneId(zoneId: String?)
    suspend fun setTimeFormat(format: TimeFormat)
    suspend fun setThemeMode(mode: ThemeMode)
}

class UserPreferencesRepository(
    private val context: Context
) : IUserPreferencesRepository {

    override val timezoneId: Flow<String?> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_TIMEZONE]
    }

    override val timeFormat: Flow<TimeFormat> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_TIME_FORMAT]?.let {
            try { TimeFormat.valueOf(it) } catch (_: Exception) { TimeFormat.SYSTEM }
        } ?: TimeFormat.SYSTEM
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

    override suspend fun setTimeFormat(format: TimeFormat) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_TIME_FORMAT] = format.name
        }
    }

    override val themeMode: Flow<ThemeMode> = context.userPreferencesDataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE]?.let {
            try { ThemeMode.valueOf(it) } catch (_: Exception) { ThemeMode.SYSTEM }
        } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.userPreferencesDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    companion object {
        private val KEY_TIMEZONE = stringPreferencesKey("timezone_override")
        private val KEY_TIME_FORMAT = stringPreferencesKey("time_format")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}

package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.platform.DataStoreFactory
import io.github.leogallego.ansiblejane.ui.components.ThemeMode
import io.github.leogallego.ansiblejane.ui.components.TimeFormat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(
    dataStoreFactory: DataStoreFactory
) : IUserPreferencesRepository {

    private val dataStore = dataStoreFactory.createPreferencesDataStore("user_preferences")

    override val timezoneId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[KEY_TIMEZONE]
    }

    override val timeFormat: Flow<TimeFormat> = dataStore.data.map { prefs ->
        prefs[KEY_TIME_FORMAT]?.let {
            try { TimeFormat.valueOf(it) } catch (_: Exception) { TimeFormat.SYSTEM }
        } ?: TimeFormat.SYSTEM
    }

    override suspend fun setTimezoneId(zoneId: String?) {
        dataStore.edit { prefs ->
            if (zoneId == null) {
                prefs.remove(KEY_TIMEZONE)
            } else {
                prefs[KEY_TIMEZONE] = zoneId
            }
        }
    }

    override suspend fun setTimeFormat(format: TimeFormat) {
        dataStore.edit { prefs ->
            prefs[KEY_TIME_FORMAT] = format.name
        }
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE]?.let {
            try { ThemeMode.valueOf(it) } catch (_: Exception) { ThemeMode.SYSTEM }
        } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    override val approvalPollInterval: Flow<PollInterval> = dataStore.data.map { prefs ->
        prefs[KEY_POLL_INTERVAL]?.let {
            try { PollInterval.valueOf(it) } catch (_: Exception) { PollInterval.MINUTES_15 }
        } ?: PollInterval.MINUTES_15
    }

    override suspend fun setApprovalPollInterval(interval: PollInterval) {
        dataStore.edit { prefs ->
            prefs[KEY_POLL_INTERVAL] = interval.name
        }
    }

    override fun approvalPollingEnabled(instanceId: String): Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[pollingEnabledKey(instanceId)] != "false"
        }

    override suspend fun setApprovalPollingEnabled(instanceId: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[pollingEnabledKey(instanceId)] = enabled.toString()
        }
    }

    override fun favoriteTemplateIds(instanceId: String): Flow<Set<Int>> =
        dataStore.data.map { prefs ->
            prefs[favoritesKey(instanceId)]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toSet()
                ?: emptySet()
        }

    override suspend fun toggleFavoriteTemplate(instanceId: String, templateId: Int) {
        val key = favoritesKey(instanceId)
        dataStore.edit { prefs ->
            val current = prefs[key]
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?.toSet()
                ?: emptySet()
            val updated = if (templateId in current) current - templateId else current + templateId
            if (updated.isEmpty()) {
                prefs.remove(key)
            } else {
                prefs[key] = updated.joinToString(",")
            }
        }
    }

    companion object {
        private val KEY_TIMEZONE = stringPreferencesKey("timezone_override")
        private val KEY_TIME_FORMAT = stringPreferencesKey("time_format")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_POLL_INTERVAL = stringPreferencesKey("approval_poll_interval")

        private fun favoritesKey(instanceId: String) =
            stringPreferencesKey("favorite_templates_$instanceId")

        private fun pollingEnabledKey(instanceId: String) =
            stringPreferencesKey("approval_polling_enabled_$instanceId")
    }
}

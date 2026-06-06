package io.github.leogallego.ansiblejane.notification

import io.github.leogallego.ansiblejane.platform.DataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first

class ApprovalTracker(dataStoreFactory: DataStoreFactory) {

    private val dataStore = dataStoreFactory.createPreferencesDataStore("approval_tracker")

    companion object {
        private val KEY_SEEN_IDS = stringSetPreferencesKey("seen_approval_ids")
    }

    suspend fun getSeenIds(): Set<Int> {
        val prefs = dataStore.data.first()
        val strings = prefs[KEY_SEEN_IDS] ?: emptySet()
        return strings.mapNotNull { it.toIntOrNull() }.toSet()
    }

    suspend fun markSeen(ids: Set<Int>) {
        dataStore.edit { prefs ->
            val existing = prefs[KEY_SEEN_IDS] ?: emptySet()
            prefs[KEY_SEEN_IDS] = existing + ids.map { it.toString() }.toSet()
        }
    }

    suspend fun pruneIds(activeIds: Set<Int>) {
        dataStore.edit { prefs ->
            val existing = prefs[KEY_SEEN_IDS] ?: emptySet()
            val activeStrings = activeIds.map { it.toString() }.toSet()
            prefs[KEY_SEEN_IDS] = existing.intersect(activeStrings)
        }
    }
}

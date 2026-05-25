package io.github.leogallego.ansiblejane.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.approvalTrackerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "approval_tracker"
)

class ApprovalTracker(private val context: Context) {

    companion object {
        private val KEY_SEEN_IDS = stringSetPreferencesKey("seen_approval_ids")
    }

    /**
     * Returns the set of approval IDs that have already been seen.
     */
    suspend fun getSeenIds(): Set<Int> {
        val prefs = context.approvalTrackerDataStore.data.first()
        val strings = prefs[KEY_SEEN_IDS] ?: emptySet()
        return strings.mapNotNull { it.toIntOrNull() }.toSet()
    }

    /**
     * Marks the given approval IDs as seen.
     */
    suspend fun markSeen(ids: Set<Int>) {
        context.approvalTrackerDataStore.edit { prefs ->
            val existing = prefs[KEY_SEEN_IDS] ?: emptySet()
            prefs[KEY_SEEN_IDS] = existing + ids.map { it.toString() }.toSet()
        }
    }

    /**
     * Prunes IDs that are no longer in the active pending set.
     * This prevents the seen set from growing indefinitely.
     */
    suspend fun pruneIds(activeIds: Set<Int>) {
        context.approvalTrackerDataStore.edit { prefs ->
            val existing = prefs[KEY_SEEN_IDS] ?: emptySet()
            val activeStrings = activeIds.map { it.toString() }.toSet()
            prefs[KEY_SEEN_IDS] = existing.intersect(activeStrings)
        }
    }
}

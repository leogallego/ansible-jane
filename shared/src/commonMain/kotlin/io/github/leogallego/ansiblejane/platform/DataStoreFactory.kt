package io.github.leogallego.ansiblejane.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

expect class DataStoreFactory {
    fun createPreferencesDataStore(name: String): DataStore<Preferences>
}

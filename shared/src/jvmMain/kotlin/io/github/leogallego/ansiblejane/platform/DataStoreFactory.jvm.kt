package io.github.leogallego.ansiblejane.platform

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

actual class DataStoreFactory {

    private val stores = mutableMapOf<String, DataStore<Preferences>>()

    actual fun createPreferencesDataStore(name: String): DataStore<Preferences> {
        return stores.getOrPut(name) {
            val userHome = System.getProperty("user.home") ?: "."
            PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    "$userHome/.ansiblejane/datastore/$name.preferences_pb".toPath()
                }
            )
        }
    }
}

package io.github.leogallego.ansiblejane.platform

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

actual class DataStoreFactory(private val context: Context) {

    private val stores = mutableMapOf<String, DataStore<Preferences>>()

    actual fun createPreferencesDataStore(name: String): DataStore<Preferences> {
        return stores.getOrPut(name) {
            PreferenceDataStoreFactory.createWithPath(
                produceFile = {
                    context.filesDir.resolve("datastore/$name.preferences_pb")
                        .absolutePath.toPath()
                }
            )
        }
    }
}

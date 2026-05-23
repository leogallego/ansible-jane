package io.github.leogallego.ansiblejane

import android.app.Application
import io.github.leogallego.ansiblejane.assistant.assistantModule
import io.github.leogallego.ansiblejane.assistant.localToolsModule
import io.github.leogallego.ansiblejane.data.IUserPreferencesRepository
import io.github.leogallego.ansiblejane.data.dataModule
import io.github.leogallego.ansiblejane.network.networkModule
import io.github.leogallego.ansiblejane.presentation.presentationModule
import io.github.leogallego.ansiblejane.ui.components.DateFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.time.ZoneId

class AnsibleJaneApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AnsibleJaneApp)
            modules(networkModule, dataModule, presentationModule, localToolsModule, assistantModule)
        }

        val userPreferences: IUserPreferencesRepository by inject()
        appScope.launch {
            val savedZone = userPreferences.timezoneId.first()
            DateFormatter.zoneOverride = savedZone?.let {
                try { ZoneId.of(it) } catch (_: Exception) { null }
            }
            DateFormatter.timeFormat = userPreferences.timeFormat.first()
        }
    }
}

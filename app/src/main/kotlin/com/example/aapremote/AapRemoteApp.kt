package com.example.aapremote

import android.app.Application
import com.example.aapremote.assistant.assistantModule
import com.example.aapremote.assistant.localToolsModule
import com.example.aapremote.data.IUserPreferencesRepository
import com.example.aapremote.data.dataModule
import com.example.aapremote.network.networkModule
import com.example.aapremote.presentation.presentationModule
import com.example.aapremote.ui.components.DateFormatter
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

class AapRemoteApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AapRemoteApp)
            modules(networkModule, dataModule, presentationModule, localToolsModule, assistantModule)
        }

        val userPreferences: IUserPreferencesRepository by inject()
        appScope.launch {
            val savedZone = userPreferences.timezoneId.first()
            DateFormatter.zoneOverride = savedZone?.let {
                try { ZoneId.of(it) } catch (_: Exception) { null }
            }
        }
    }
}

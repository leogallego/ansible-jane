package io.github.leogallego.ansiblejane

import android.app.Application
import io.github.leogallego.ansiblejane.assistant.assistantModule
import io.github.leogallego.ansiblejane.assistant.localToolsModule
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import io.github.leogallego.ansiblejane.data.IUserPreferencesRepository
import io.github.leogallego.ansiblejane.data.dataModule
import io.github.leogallego.ansiblejane.notification.ApprovalNotificationManager
import io.github.leogallego.ansiblejane.notification.ApprovalPollingWorker
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
import java.util.concurrent.TimeUnit

class AnsibleJaneApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@AnsibleJaneApp)
            modules(dataModule, presentationModule, localToolsModule, assistantModule)
        }

        ApprovalNotificationManager.createChannel(this)

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ApprovalPollingWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ApprovalPollingWorker>(15, TimeUnit.MINUTES).build()
        )

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

package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.data.backup.BackupManager
import io.github.leogallego.ansiblejane.notification.ApprovalTracker
import io.github.leogallego.ansiblejane.platform.BackgroundWorker
import io.github.leogallego.ansiblejane.platform.ConnectivityObserver
import io.github.leogallego.ansiblejane.platform.DataStoreFactory
import io.github.leogallego.ansiblejane.platform.NotificationManager
import io.github.leogallego.ansiblejane.platform.SecureKeyStorage
import org.koin.dsl.bind
import org.koin.dsl.module

val sharedDataModule = module {
    single { ToolManifestRepository(get<DataStoreFactory>().createPreferencesDataStore("credentials")) } bind IToolManifestRepository::class
    single { TokenManager(get(), get(), get()) } bind ITokenManager::class
    single { UserPreferencesRepository(get()) } bind IUserPreferencesRepository::class
    single { ApprovalTracker(get()) }
    single { BackupManager() }
}

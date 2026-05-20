package com.example.aapremote.data

import com.example.aapremote.data.backup.BackupManager
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { TokenManager(androidContext()) }
    single { BackupManager() }
    single { ConnectivityObserver(androidContext()) }
    single { AuthRepository(get(), get()) }
    single { TemplateRepository(get<com.example.aapremote.network.AapApiProvider>()) }
    single { JobRepository(get<com.example.aapremote.network.AapApiProvider>()) }
    single { WorkflowRepository(get<com.example.aapremote.network.AapApiProvider>()) }
    single { ScheduleRepository(get<com.example.aapremote.network.AapApiProvider>()) }
    single { EdaAuditRepository(get<com.example.aapremote.network.AapApiProvider>()) }
    single { InventoryRepository(get<com.example.aapremote.network.AapApiProvider>()) }
    single { HostRepository(get<com.example.aapremote.network.AapApiProvider>()) }
    single { InfrastructureRepository(get<com.example.aapremote.network.AapApiProvider>()) }
    single { CredentialRepository(get<com.example.aapremote.network.AapApiProvider>()) }
    single { ProjectRepository(get<com.example.aapremote.network.AapApiProvider>()) }
    single { EdaActivationRepository(get<com.example.aapremote.network.AapApiProvider>()) }
}

package com.example.aapremote.data

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single { TokenManager(androidContext()) }
    single { ConnectivityObserver(androidContext()) }
    single { AuthRepository(get()) }
    single { TemplateRepository(get()) }
    single { JobRepository(get()) }
    single { WorkflowRepository(get()) }
    single { ScheduleRepository(get()) }
    single { EdaAuditRepository(get()) }
    single { InventoryRepository(get()) }
    single { HostRepository(get()) }
}

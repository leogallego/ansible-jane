package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.di.sharedNetworkModule
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import io.github.leogallego.ansiblejane.platform.BackgroundWorker
import io.github.leogallego.ansiblejane.platform.ConnectivityObserver
import io.github.leogallego.ansiblejane.platform.DataStoreFactory
import io.github.leogallego.ansiblejane.platform.NotificationManager
import io.github.leogallego.ansiblejane.platform.PlatformUtils
import io.github.leogallego.ansiblejane.platform.SecureKeyStorage
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val platformModule = module {
    single { DataStoreFactory(androidContext()) }
    single { SecureKeyStorage(androidContext()) }
    single { ConnectivityObserver(androidContext()) }
    single { BackgroundWorker(androidContext()) }
    single { NotificationManager(androidContext()) }
    single { PlatformUtils(androidContext()) }
}

val dataModule = module {
    includes(sharedDataModule, sharedNetworkModule, platformModule)
    single { AuthRepository(get(), get(), get()) } bind IAuthRepository::class
    single { TemplateRepository(get<IAapApiProvider>()) } bind ITemplateRepository::class
    single { JobRepository(get<IAapApiProvider>()) } bind IJobRepository::class
    single { WorkflowRepository(get<IAapApiProvider>()) } bind IWorkflowRepository::class
    single { ScheduleRepository(get<IAapApiProvider>()) } bind IScheduleRepository::class
    single { EdaAuditRepository(get<IAapApiProvider>()) } bind IEdaAuditRepository::class
    single { InventoryRepository(get<IAapApiProvider>()) } bind IInventoryRepository::class
    single { HostRepository(get<IAapApiProvider>()) } bind IHostRepository::class
    single { InfrastructureRepository(get<IAapApiProvider>()) } bind IInfrastructureRepository::class
    single { CredentialRepository(get<IAapApiProvider>()) } bind ICredentialRepository::class
    single { ProjectRepository(get<IAapApiProvider>()) } bind IProjectRepository::class
    single { EdaActivationRepository(get<IAapApiProvider>()) } bind IEdaActivationRepository::class
    single { ControllerReadOnlyRepository(get<IAapApiProvider>()) }
    single { EdaReadOnlyRepository(get<IAapApiProvider>()) }
    single { PlatformRepository(get<IAapApiProvider>()) }
}

package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.data.backup.BackupManager
import io.github.leogallego.ansiblejane.network.AapApiProvider
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
    includes(sharedDataModule, platformModule)
    single { BackupManager() }
    single { AuthRepository(get(), get(), get()) } bind IAuthRepository::class
    single { TemplateRepository(get<AapApiProvider>()) } bind ITemplateRepository::class
    single { JobRepository(get<AapApiProvider>()) } bind IJobRepository::class
    single { WorkflowRepository(get<AapApiProvider>()) } bind IWorkflowRepository::class
    single { ScheduleRepository(get<AapApiProvider>()) } bind IScheduleRepository::class
    single { EdaAuditRepository(get<AapApiProvider>()) } bind IEdaAuditRepository::class
    single { InventoryRepository(get<AapApiProvider>()) } bind IInventoryRepository::class
    single { HostRepository(get<AapApiProvider>()) } bind IHostRepository::class
    single { InfrastructureRepository(get<AapApiProvider>()) } bind IInfrastructureRepository::class
    single { CredentialRepository(get<AapApiProvider>()) } bind ICredentialRepository::class
    single { ProjectRepository(get<AapApiProvider>()) } bind IProjectRepository::class
    single { EdaActivationRepository(get<AapApiProvider>()) } bind IEdaActivationRepository::class
    single { ControllerReadOnlyRepository(get<AapApiProvider>()) }
    single { EdaReadOnlyRepository(get<AapApiProvider>()) }
    single { PlatformRepository(get<AapApiProvider>()) }
}

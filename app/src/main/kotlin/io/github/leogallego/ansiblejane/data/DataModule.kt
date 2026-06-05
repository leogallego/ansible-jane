package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.data.backup.BackupManager
import io.github.leogallego.ansiblejane.network.AapApiProvider
import io.github.leogallego.ansiblejane.notification.ApprovalNotificationManager
import io.github.leogallego.ansiblejane.notification.ApprovalTracker
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

val dataModule = module {
    single { ToolManifestRepository(androidContext().credentialsDataStore) } bind IToolManifestRepository::class
    single { TokenManager(androidContext(), get()) } bind ITokenManager::class
    single { UserPreferencesRepository(androidContext()) } bind IUserPreferencesRepository::class
    single { BackupManager() }
    single { ApprovalTracker(androidContext()) }
    single { ApprovalNotificationManager() }
    single { ConnectivityObserver(androidContext()) }
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

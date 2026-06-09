package io.github.leogallego.ansiblejane

import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.data.IToolManifestRepository
import io.github.leogallego.ansiblejane.data.backup.BackupManager
import io.github.leogallego.ansiblejane.fakes.FakeAssistantRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.fakes.FakeToolManifestRepository
import io.github.leogallego.ansiblejane.platform.PlatformUtils
import io.github.leogallego.ansiblejane.presentation.settings.BackupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun testKoinModule(
    tokenManager: ITokenManager = FakeTokenManager(),
    assistantRepository: IAssistantRepository = FakeAssistantRepository(),
    manifestRepository: IToolManifestRepository = FakeToolManifestRepository()
) = module {
    single<ITokenManager> { tokenManager }
    single<IAssistantRepository> { assistantRepository }
    single<IToolManifestRepository> { manifestRepository }
    single { BackupManager() }
    single { PlatformUtils(org.robolectric.RuntimeEnvironment.getApplication()) }
    viewModel { BackupViewModel(get(), get(), get()) }
}

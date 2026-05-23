package io.github.leogallego.ansiblejane

import io.github.leogallego.ansiblejane.assistant.data.IAssistantRepository
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.data.backup.BackupManager
import io.github.leogallego.ansiblejane.fakes.FakeAssistantRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.presentation.settings.BackupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun testKoinModule(
    tokenManager: ITokenManager = FakeTokenManager(),
    assistantRepository: IAssistantRepository = FakeAssistantRepository()
) = module {
    single<ITokenManager> { tokenManager }
    single<IAssistantRepository> { assistantRepository }
    single { BackupManager() }
    viewModel { BackupViewModel(get(), get(), get()) }
}

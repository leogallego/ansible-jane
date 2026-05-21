package com.example.aapremote

import com.example.aapremote.assistant.data.IAssistantRepository
import com.example.aapremote.data.ITokenManager
import com.example.aapremote.data.backup.BackupManager
import com.example.aapremote.fakes.FakeAssistantRepository
import com.example.aapremote.fakes.FakeTokenManager
import com.example.aapremote.presentation.settings.BackupViewModel
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

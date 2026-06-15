package io.github.leogallego.ansiblejane.assistant

import io.github.leogallego.ansiblejane.network.networkJson
import io.github.leogallego.ansiblejane.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val assistantModule = module {
    viewModel {
        SettingsViewModel(
            tokenManager = get(),
            apiProvider = get(),
            userPreferences = get(),
            assistantRepository = get(),
            mcpServerManager = get(),
            manifestRepository = get(),
            instanceDiscovery = get(),
            toolRouter = get(),
            httpClient = get(named("llm")),
            json = networkJson
        )
    }
}

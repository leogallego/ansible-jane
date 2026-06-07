package io.github.leogallego.ansiblejane.presentation

import io.github.leogallego.ansiblejane.presentation.settings.SettingsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appPresentationModule = module {
    viewModelOf(::SettingsViewModel)
}

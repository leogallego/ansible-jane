package io.github.leogallego.ansiblejane.platform

import org.koin.core.context.startKoin
import org.koin.core.module.Module

actual fun initializeApp(modules: List<Module>) {
    startKoin {
        modules(modules)
    }
}

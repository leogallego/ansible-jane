package io.github.leogallego.ansiblejane

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import io.github.leogallego.ansiblejane.data.sharedDataModule
import io.github.leogallego.ansiblejane.data.sharedRepositoryModule
import io.github.leogallego.ansiblejane.di.sharedNetworkModule
import io.github.leogallego.ansiblejane.presentation.presentationModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(desktopPlatformModule, sharedDataModule, sharedRepositoryModule, sharedNetworkModule, presentationModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Ansible Jane",
        state = WindowState(width = 1200.dp, height = 800.dp)
    ) {
        App()
    }
}

package io.github.leogallego.ansiblejane

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import aapremotecontrol.composeapp.generated.resources.Res
import aapremotecontrol.composeapp.generated.resources.icon
import io.github.leogallego.ansiblejane.assistant.di.sharedAssistantModule
import io.github.leogallego.ansiblejane.assistant.ui.AssistantScreen
import io.github.leogallego.ansiblejane.data.sharedDataModule
import org.jetbrains.compose.resources.painterResource
import io.github.leogallego.ansiblejane.data.sharedRepositoryModule
import io.github.leogallego.ansiblejane.di.sharedNetworkModule
import io.github.leogallego.ansiblejane.presentation.presentationModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(
            desktopPlatformModule,
            sharedDataModule,
            sharedRepositoryModule,
            sharedNetworkModule,
            sharedAssistantModule,
            presentationModule
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Ansible Jane",
        icon = painterResource(Res.drawable.icon),
        state = WindowState(width = 1200.dp, height = 800.dp)
    ) {
        App(
            assistantContent = { AssistantScreen() }
        )
    }
}

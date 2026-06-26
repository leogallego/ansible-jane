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
import io.github.leogallego.ansiblejane.presentation.auth.AuthViewModel
import io.github.leogallego.ansiblejane.ui.settings.SettingsScreen
import org.koin.compose.KoinContext
import org.koin.compose.viewmodel.koinViewModel
import io.github.leogallego.ansiblejane.assistant.engine.DebugLog
import org.koin.core.context.startKoin

fun main() = application {
    DebugLog.enabled = AppVersion.name.contains("-") ||
        System.getProperty("ansiblejane.debug") != null

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
        KoinContext {
            App(
                assistantContent = { AssistantScreen() },
                settingsContent = { onLogout, onNavigateBack, onAddInstance, initialTab ->
                    val authViewModel: AuthViewModel = koinViewModel()
                    SettingsScreen(
                        onLogout = {
                            authViewModel.logout()
                            onLogout()
                        },
                        onNavigateBack = onNavigateBack,
                        onAddInstance = onAddInstance,
                        initialTab = initialTab
                    )
                }
            )
        }
    }
}

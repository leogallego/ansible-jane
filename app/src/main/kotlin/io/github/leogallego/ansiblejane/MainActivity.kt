package io.github.leogallego.ansiblejane

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import io.github.leogallego.ansiblejane.assistant.ui.AssistantScreen
import io.github.leogallego.ansiblejane.ui.settings.SettingsScreen
import io.github.leogallego.ansiblejane.presentation.auth.AuthViewModel
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            App(
                modifier = Modifier.semantics { testTagsAsResourceId = true },
                assistantContent = { AssistantScreen() },
                settingsContent = { onLogout, onNavigateBack, onAddInstance ->
                    val authViewModel: AuthViewModel = koinViewModel()
                    SettingsScreen(
                        onLogout = {
                            authViewModel.logout()
                            onLogout()
                        },
                        onNavigateBack = onNavigateBack,
                        onAddInstance = onAddInstance
                    )
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
    }
}

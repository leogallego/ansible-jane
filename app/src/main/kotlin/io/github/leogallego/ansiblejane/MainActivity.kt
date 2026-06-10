package io.github.leogallego.ansiblejane

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.content.ContextCompat
import io.github.leogallego.ansiblejane.assistant.ui.AssistantScreen
import io.github.leogallego.ansiblejane.navigation.ApprovalDetailRoute
import io.github.leogallego.ansiblejane.navigation.JobStatusRoute
import io.github.leogallego.ansiblejane.navigation.WorkflowJobStatusRoute
import io.github.leogallego.ansiblejane.ui.settings.SettingsScreen
import io.github.leogallego.ansiblejane.presentation.auth.AuthViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {

    private val pendingDeepLink = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.toString()?.let { pendingDeepLink.value = it }

        enableEdgeToEdge()
        setContent {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {}
                LaunchedEffect(Unit) {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val deepLink by pendingDeepLink.collectAsState()

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
                },
                onHandleDeepLink = { navController ->
                    deepLink?.let { uri ->
                        val currentRoute = navController.currentDestination?.route
                        if (currentRoute != null && currentRoute.contains("MainRoute")) {
                            pendingDeepLink.value = null
                            parseDeepLink(uri)?.let { route ->
                                navController.navigate(route)
                            }
                        }
                    }
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.data?.toString()?.let { pendingDeepLink.value = it }
    }

    companion object {
        fun parseDeepLink(uri: String): Any? {
            val segments = uri.removePrefix("ansiblejane://").split("/")
            if (segments.size < 2) return null
            val id = segments[1].toIntOrNull() ?: return null
            return when (segments[0]) {
                "approval" -> ApprovalDetailRoute(id)
                "job" -> JobStatusRoute(id)
                "workflow" -> WorkflowJobStatusRoute(id)
                else -> null
            }
        }
    }
}

package io.github.leogallego.ansiblejane

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import io.github.leogallego.ansiblejane.data.IUserPreferencesRepository
import io.github.leogallego.ansiblejane.navigation.AppNavigation
import io.github.leogallego.ansiblejane.ui.components.ThemeMode
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme
import org.koin.compose.koinInject

@Composable
fun App(
    modifier: Modifier = Modifier,
    assistantContent: @Composable () -> Unit = {},
    settingsContent: @Composable (onLogout: () -> Unit, onNavigateBack: () -> Unit, onAddInstance: () -> Unit) -> Unit = { _, _, _ -> },
    onHandleDeepLink: ((NavHostController) -> Unit)? = null
) {
    val userPreferences: IUserPreferencesRepository = koinInject()
    val themeMode by userPreferences.themeMode.collectAsState(ThemeMode.SYSTEM)

    AnsibleJaneTheme(
        darkTheme = when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        }
    ) {
        AppNavigation(
            modifier = modifier,
            assistantContent = assistantContent,
            settingsContent = settingsContent,
            onHandleDeepLink = onHandleDeepLink
        )
    }
}

package io.github.leogallego.ansiblejane

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import io.github.leogallego.ansiblejane.data.IUserPreferencesRepository
import io.github.leogallego.ansiblejane.navigation.AppNavigation
import io.github.leogallego.ansiblejane.ui.components.ThemeMode
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme
import org.koin.compose.koinInject

@Composable
fun App(
    assistantContent: @Composable () -> Unit = {},
    settingsContent: @Composable (onLogout: () -> Unit, onNavigateBack: () -> Unit, onAddInstance: () -> Unit) -> Unit = { _, _, _ -> }
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
            modifier = Modifier.semantics { testTagsAsResourceId = true },
            assistantContent = assistantContent,
            settingsContent = settingsContent
        )
    }
}

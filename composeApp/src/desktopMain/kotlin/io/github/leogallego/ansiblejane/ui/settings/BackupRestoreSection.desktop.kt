package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.presentation.settings.BackupViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun BackupRestoreSection(
    viewModel: BackupViewModel
) {
    Text(
        text = "Backup & Restore is available on Android",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(16.dp)
    )
}

@Composable
actual fun ImportFromBackupButton(
    onNavigateToDashboard: () -> Unit
) {
    // Not available on desktop
}

package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.runtime.Composable
import io.github.leogallego.ansiblejane.presentation.settings.BackupViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
expect fun BackupRestoreSection(
    viewModel: BackupViewModel = koinViewModel()
)

@Composable
expect fun ImportFromBackupButton(
    onNavigateToDashboard: () -> Unit
)

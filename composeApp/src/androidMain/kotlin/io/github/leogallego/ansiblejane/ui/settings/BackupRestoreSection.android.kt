package io.github.leogallego.ansiblejane.ui.settings

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import io.github.leogallego.ansiblejane.presentation.settings.BackupUiState
import io.github.leogallego.ansiblejane.presentation.settings.BackupViewModel
import io.github.leogallego.ansiblejane.presentation.settings.ImportMode
import org.koin.compose.viewmodel.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
actual fun BackupRestoreSection(
    viewModel: BackupViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportPasswordDialog by remember { mutableStateOf(false) }
    var pendingImportData by remember { mutableStateOf<ByteArray?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            val data = (uiState as? BackupUiState.ExportReady)?.data ?: return@rememberLauncherForActivityResult
            writeToUri(context, uri, data)
            viewModel.dismiss()
            Toast.makeText(context, "Backup exported", Toast.LENGTH_SHORT).show()
        } else {
            viewModel.dismiss()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val data = readFromUri(context, uri)
            if (data != null) {
                pendingImportData = data
                showImportPasswordDialog = true
            } else {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is BackupUiState.ExportReady -> {
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                exportLauncher.launch("aapdroid-backup-$dateStr.aapdroid")
            }
            is BackupUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.dismiss()
            }
            is BackupUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.dismiss()
            }
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Backup & Restore",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Export credentials to transfer between devices or as a safety backup.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        val isLoading = uiState is BackupUiState.Exporting || uiState is BackupUiState.Importing

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { showExportDialog = true },
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .testTag("button_export_backup")
            ) {
                if (uiState is BackupUiState.Exporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Export")
                }
            }

            FilledTonalButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                enabled = !isLoading,
                modifier = Modifier
                    .weight(1f)
                    .testTag("button_import_backup")
            ) {
                if (uiState is BackupUiState.Importing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Import")
                }
            }
        }
    }

    if (showExportDialog) {
        ExportPasswordDialog(
            onConfirm = { password, includeLlm ->
                showExportDialog = false
                viewModel.exportBackup(password, includeLlm)
            },
            onDismiss = { showExportDialog = false }
        )
    }

    if (showImportPasswordDialog) {
        ImportPasswordDialog(
            onConfirm = { password ->
                showImportPasswordDialog = false
                pendingImportData?.let { viewModel.startImport(it, password) }
                pendingImportData = null
            },
            onDismiss = {
                showImportPasswordDialog = false
                pendingImportData = null
            }
        )
    }

    (uiState as? BackupUiState.ImportPreview)?.let { preview ->
        ImportPreviewScreen(
            preview = preview,
            onMerge = { viewModel.confirmImport(ImportMode.MERGE) },
            onReplace = { viewModel.confirmImport(ImportMode.REPLACE) },
            onDismiss = { viewModel.dismiss() }
        )
    }
}

@Composable
private fun ExportPasswordDialog(
    onConfirm: (password: String, includeLlm: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var includeLlm by remember { mutableStateOf(true) }
    var showMismatch by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Credentials") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter a password to encrypt the backup file.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; showMismatch = false },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    supportingText = if (password.isNotEmpty() && password.length < 8) {
                        { Text("At least 8 characters") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_export_password")
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; showMismatch = false },
                    label = { Text("Confirm password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    isError = showMismatch,
                    supportingText = if (showMismatch) {{ Text("Passwords don't match") }} else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_export_confirm_password")
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeLlm,
                        onCheckedChange = { includeLlm = it },
                        modifier = Modifier.testTag("switch_include_llm")
                    )
                    Text("Include LLM API keys", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (password != confirmPassword) {
                        showMismatch = true
                    } else {
                        onConfirm(password, includeLlm)
                    }
                },
                enabled = password.length >= 8
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ImportPasswordDialog(
    onConfirm: (password: String) -> Unit,
    onDismiss: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Credentials") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Enter the password used when exporting.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("field_import_password")
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(password) },
                enabled = password.isNotEmpty()
            ) {
                Text("Decrypt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImportPreviewScreen(
    preview: BackupUiState.ImportPreview,
    onMerge: () -> Unit,
    onReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Restore Backup",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "AAP Instances",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            preview.instances.forEach { inst ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        val instAlias = inst.alias
                        if (instAlias != null) {
                            Text(
                                text = instAlias,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                        Text(
                            text = inst.baseUrl,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = inst.apiVersion,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (inst.mcpEnabled) {
                                Text(
                                    text = "MCP enabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (inst.trustSelfSigned) {
                                Text(
                                    text = "Self-signed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        val mcpUrls = inst.mcpServerUrls
                        if (!mcpUrls.isNullOrEmpty()) {
                            Text(
                                text = mcpUrls.joinToString(", ") { it.label },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            preview.llmConfig?.let { config ->
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                Text(
                    text = "AI Assistant (global setting)",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Shared across all instances — not tied to any specific AAP server.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (config is io.github.leogallego.ansiblejane.assistant.data.LlmProviderConfig.OpenAiCompatible) {
                            Text(
                                text = config.model,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = config.url,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = config.apiKey?.let { key -> "API key: ••••${key.takeLast(4)}" } ?: "No API key",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Token mode: ${config.tokenSavingMode.displayName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            if (preview.hasExistingInstances && preview.duplicateCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${preview.duplicateCount} instance(s) already on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (preview.hasExistingInstances) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    OutlinedButton(
                        onClick = onReplace,
                        modifier = Modifier.weight(1f)
                    ) { Text("Replace existing") }
                    Button(
                        onClick = onMerge,
                        modifier = Modifier.weight(1f)
                    ) { Text("Add new") }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = onMerge,
                        modifier = Modifier.weight(1f)
                    ) { Text("Restore") }
                }
            }
        }
    }
}

@Composable
actual fun ImportFromBackupButton(
    onNavigateToDashboard: () -> Unit
) {
    val viewModel: BackupViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showPasswordDialog by remember { mutableStateOf(false) }
    var pendingData by remember { mutableStateOf<ByteArray?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val data = readFromUri(context, uri)
            if (data != null) {
                pendingData = data
                showPasswordDialog = true
            } else {
                Toast.makeText(context, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is BackupUiState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.dismiss()
                onNavigateToDashboard()
            }
            is BackupUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                viewModel.dismiss()
            }
            else -> {}
        }
    }

    TextButton(
        onClick = { importLauncher.launch(arrayOf("*/*")) },
        modifier = Modifier.testTag("button_import_backup_auth")
    ) {
        Icon(
            Icons.Default.FileDownload,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 4.dp)
        )
        Text("Import from backup")
    }

    if (showPasswordDialog) {
        ImportPasswordDialog(
            onConfirm = { password ->
                showPasswordDialog = false
                pendingData?.let { viewModel.startImport(it, password) }
                pendingData = null
            },
            onDismiss = {
                showPasswordDialog = false
                pendingData = null
            }
        )
    }

    (uiState as? BackupUiState.ImportPreview)?.let { preview ->
        ImportPreviewScreen(
            preview = preview,
            onMerge = { viewModel.confirmImport(ImportMode.MERGE) },
            onReplace = { viewModel.confirmImport(ImportMode.REPLACE) },
            onDismiss = { viewModel.dismiss() }
        )
    }
}

private fun writeToUri(context: Context, uri: Uri, data: ByteArray) {
    context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
}

private fun readFromUri(context: Context, uri: Uri): ByteArray? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    } catch (_: Exception) {
        null
    }
}

package com.example.aapremote.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.aapremote.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aapremote.model.AapInstance
import com.example.aapremote.presentation.settings.SettingsUiState
import com.example.aapremote.presentation.settings.SettingsViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    onNavigateBack: () -> Unit,
    onAddInstance: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var instanceToRemove by remember { mutableStateOf<AapInstance?>(null) }
    var showLogoutAllConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Instances",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is SettingsUiState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.instances.forEach { instance ->
                            val isActive = instance.id == state.selectedInstance?.id
                            InstanceCard(
                                instance = instance,
                                isActive = isActive,
                                onTap = {
                                    if (isActive) {
                                        viewModel.showInstanceDetails(instance.id)
                                    } else {
                                        viewModel.switchInstance(instance.id)
                                    }
                                },
                                onRemove = { instanceToRemove = instance }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    FilledTonalButton(
                        onClick = onAddInstance,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Add Instance")
                    }

                    // Instance details bottom sheet
                    state.selectedInstanceForDetails?.let { instance ->
                        InstanceDetailsBottomSheet(
                            instance = instance,
                            onDismiss = { viewModel.dismissDetails() }
                        )
                    }
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(24.dp))

            BackupRestoreSection()

            Spacer(modifier = Modifier.weight(1f))

            AboutSection()

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = { showLogoutAllConfirm = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Logout All")
            }
        }
    }

    // Confirmation dialog for instance removal
    instanceToRemove?.let { instance ->
        val isLastInstance = (uiState as? SettingsUiState.Success)?.instances?.size == 1
        AlertDialog(
            onDismissRequest = { instanceToRemove = null },
            title = { Text(if (isLastInstance) "Remove Last Instance" else "Remove Instance") },
            text = {
                Text(
                    if (isLastInstance)
                        "This is your only instance. Removing it will log you out completely."
                    else
                        "Remove \"${instance.displayLabel}\"? You will need to re-authenticate to use this instance again."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.removeInstance(instance.id)
                        instanceToRemove = null
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { instanceToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation dialog for logout all
    if (showLogoutAllConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutAllConfirm = false },
            title = { Text("Logout All") },
            text = {
                Text("Remove all instances and log out? You will need to re-authenticate each instance.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutAllConfirm = false
                        onLogout()
                    }
                ) {
                    Text("Logout All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutAllConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InstanceCard(
    instance: AapInstance,
    isActive: Boolean,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_ansible_platform),
                contentDescription = "Ansible Platform",
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = instance.alias ?: instance.hostname,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isActive) {
                        Spacer(modifier = Modifier.width(6.dp))
                        StatusPill("Active", MaterialTheme.colorScheme.primary)
                    }
                    if (instance.mcpEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        StatusPill("MCP", MaterialTheme.colorScheme.primary)
                    }
                }
                if (instance.alias != null) {
                    Text(
                        text = instance.baseUrl,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout ${instance.displayLabel}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstanceDetailsBottomSheet(
    instance: AapInstance,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Instance Details",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text("URL") },
                supportingContent = { Text(instance.baseUrl) }
            )

            if (instance.alias != null) {
                ListItem(
                    headlineContent = { Text("Alias") },
                    supportingContent = { Text(instance.alias) }
                )
            }

            ListItem(
                headlineContent = { Text("API Version") },
                supportingContent = { Text(instance.apiVersion) }
            )

            ListItem(
                headlineContent = { Text("Self-Signed Certificate") },
                supportingContent = {
                    Text(if (instance.trustSelfSigned) "Trusted" else "Not trusted")
                }
            )
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current
    val versionName = com.example.aapremote.BuildConfig.VERSION_NAME
    val versionCode = com.example.aapremote.BuildConfig.VERSION_CODE

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "AAPdroid",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "v$versionName ($versionCode)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Remote control for Ansible Automation Platform",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "GitHub",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/leogallego/aapdroid"))
                    )
                }
            )
            Text(
                text = "GPL-3.0 License",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusPill(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

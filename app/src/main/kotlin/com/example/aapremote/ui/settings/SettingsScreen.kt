package com.example.aapremote.ui.settings

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
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.instances.forEach { instance ->
                            val isActive = instance.id == state.selectedInstance?.id
                            InstancePill(
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

                        AssistChip(
                            onClick = onAddInstance,
                            label = { Text("Add Instance") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
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

            FilledTonalButton(
                onClick = onLogout,
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
        AlertDialog(
            onDismissRequest = { instanceToRemove = null },
            title = { Text("Remove Instance") },
            text = {
                Text("Remove \"${instance.displayLabel}\"? You will need to re-authenticate to use this instance again.")
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstancePill(
    instance: AapInstance,
    isActive: Boolean,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    FilterChip(
        selected = isActive,
        onClick = onTap,
        label = {
            Row {
                if (instance.alias != null) {
                    Text(
                        text = instance.alias,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = instance.hostname,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = instance.hostname,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_ansible_platform),
                contentDescription = "Ansible Platform",
                modifier = Modifier.size(20.dp),
                tint = androidx.compose.ui.graphics.Color.Unspecified
            )
        },
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(18.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove ${instance.displayLabel}",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    )
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

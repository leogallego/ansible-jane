package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.R
import io.github.leogallego.ansiblejane.model.AapInstance

@Composable
fun InstancesTab(
    instances: List<AapInstance>,
    selectedInstance: AapInstance?,
    selectedInstanceForDetails: AapInstance?,
    discoveryRefreshing: Boolean,
    discoveryError: String? = null,
    instanceEditSaving: Boolean = false,
    instanceEditError: String? = null,
    onSwitchInstance: (String) -> Unit,
    onRemoveInstance: (String) -> Unit,
    onShowDetails: (String) -> Unit,
    onDismissDetails: () -> Unit,
    onRefreshInstanceInfo: (String) -> Unit,
    onSaveInstanceEdits: (instanceId: String, token: String?, alias: String?, trustSelfSigned: Boolean) -> Unit,
    onAddInstance: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var instanceToRemove by remember { mutableStateOf<AapInstance?>(null) }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        instances.forEach { instance ->
            val isActive = instance.id == selectedInstance?.id
            InstanceCard(
                instance = instance,
                isActive = isActive,
                onTap = {
                    if (isActive) onShowDetails(instance.id)
                    else onSwitchInstance(instance.id)
                },
                onLongPress = { onShowDetails(instance.id) },
                onRemove = { instanceToRemove = instance }
            )
        }

        FilledTonalButton(
            onClick = onAddInstance,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .testTag("button_add_instance")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Add Instance")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        FilledTonalButton(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("button_logout_all")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Logout All")
        }
    }

    selectedInstanceForDetails?.let { instance ->
        InstanceDetailsBottomSheet(
            instance = instance,
            isRefreshing = discoveryRefreshing,
            isSaving = instanceEditSaving,
            errorMessage = discoveryError,
            editError = instanceEditError,
            onRefresh = { onRefreshInstanceInfo(instance.id) },
            onSave = { token, alias, trustSelfSigned ->
                onSaveInstanceEdits(instance.id, token, alias, trustSelfSigned)
            },
            onDismiss = onDismissDetails
        )
    }

    instanceToRemove?.let { instance ->
        val isLastInstance = instances.size == 1
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
                        onRemoveInstance(instance.id)
                        instanceToRemove = null
                    }
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { instanceToRemove = null }) { Text("Cancel") }
            }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout All") },
            text = {
                Text("Remove all AAP instances and log out? You will need to re-authenticate each instance.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    }
                ) { Text("Logout All") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InstanceCard(
    instance: AapInstance,
    isActive: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_instance_${instance.id}")
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            ),
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

            IconButton(
                onClick = onRemove,
                modifier = Modifier.testTag("button_remove_instance_${instance.id}")
            ) {
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
    isRefreshing: Boolean,
    isSaving: Boolean,
    errorMessage: String? = null,
    editError: String? = null,
    onRefresh: () -> Unit,
    onSave: (token: String?, alias: String?, trustSelfSigned: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var alias by remember(instance.id) { mutableStateOf(instance.alias ?: "") }
    var trustSelfSigned by remember(instance.id) { mutableStateOf(instance.trustSelfSigned) }
    var tokenResetActive by remember(instance.id) { mutableStateOf(false) }
    var newToken by remember(instance.id) { mutableStateOf("") }

    val hasChanges by remember {
        derivedStateOf {
            alias.trim().ifBlank { null } != instance.alias ||
                trustSelfSigned != instance.trustSelfSigned ||
                (tokenResetActive && newToken.isNotBlank())
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
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

            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text("Alias") },
                placeholder = { Text(instance.hostname) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("field_instance_alias")
            )

            if (!tokenResetActive) {
                FilledTonalButton(
                    onClick = { tokenResetActive = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("button_reset_token")
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Replace Token")
                }
            } else {
                Text(
                    text = "The current token will be replaced. Paste your new token below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                OutlinedTextField(
                    value = newToken,
                    onValueChange = { newToken = it },
                    label = { Text("New Token") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("field_instance_token")
                )
                TextButton(
                    onClick = {
                        tokenResetActive = false
                        newToken = ""
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .testTag("button_cancel_token_reset")
                ) {
                    Text("Cancel token reset")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Trust self-signed certificate",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Allow connections to servers with untrusted certificates",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = trustSelfSigned,
                    onCheckedChange = { trustSelfSigned = it },
                    modifier = Modifier.testTag("switch_trust_self_signed")
                )
            }

            FilledTonalButton(
                onClick = {
                    onSave(
                        if (tokenResetActive) newToken.trim() else null,
                        alias.trim().ifBlank { null },
                        trustSelfSigned
                    )
                },
                enabled = hasChanges && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("button_save_instance")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving...")
                } else {
                    Text("Save Changes")
                }
            }

            if (editError != null) {
                Text(
                    text = editError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            val info = instance.instanceInfo
            if (info != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ListItem(
                    headlineContent = { Text("Platform") },
                    supportingContent = {
                        Text(
                            when (info.platformType) {
                                "AAP" -> "Red Hat Ansible Automation Platform" +
                                    (info.aapVersion?.let { " $it" } ?: "")
                                "AWX" -> "AWX (upstream controller)"
                                "JEWEL" -> "Jewel (upstream gateway)"
                                else -> "Unknown"
                            }
                        )
                    }
                )
                if (info.controllerVersion.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("Controller") },
                        supportingContent = { Text(info.controllerVersion) }
                    )
                }
                if (info.gatewayVersion.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("Gateway") },
                        supportingContent = { Text(info.gatewayVersion) }
                    )
                }
                if (info.edaVersion.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text("EDA") },
                        supportingContent = { Text(info.edaVersion) }
                    )
                }
                ListItem(
                    headlineContent = { Text("Components") },
                    supportingContent = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            info.resolvedComponents.forEach { component ->
                                StatusPill(
                                    label = component.name.lowercase()
                                        .replaceFirstChar { it.uppercase() },
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                )
            } else {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ListItem(
                    headlineContent = { Text("Instance Info") },
                    supportingContent = { Text("Not yet discovered") }
                )
            }

            FilledTonalButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("button_refresh_instance_info")
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Discovering...")
                } else {
                    Text(if (info != null) "Refresh Instance Info" else "Discover Instance Info")
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
internal fun StatusPill(label: String, color: Color) {
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

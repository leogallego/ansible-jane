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
import androidx.compose.material.icons.filled.Dns
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*
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
            Text(stringResource(Res.string.instances_add))
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
            Text(stringResource(Res.string.instances_logout_all))
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
            title = { Text(if (isLastInstance) stringResource(Res.string.instances_remove_last_title) else stringResource(Res.string.instances_remove_title)) },
            text = {
                Text(
                    if (isLastInstance)
                        stringResource(Res.string.instances_remove_last_message)
                    else
                        stringResource(Res.string.instances_remove_message, instance.displayLabel)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRemoveInstance(instance.id)
                        instanceToRemove = null
                    }
                ) { Text(stringResource(Res.string.btn_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { instanceToRemove = null }) { Text(stringResource(Res.string.btn_cancel)) }
            }
        )
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(stringResource(Res.string.instances_logout_all_title)) },
            text = {
                Text(stringResource(Res.string.instances_logout_all_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutConfirm = false
                        onLogout()
                    }
                ) { Text(stringResource(Res.string.instances_logout_all)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text(stringResource(Res.string.btn_cancel)) }
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
                imageVector = Icons.Filled.Dns,
                contentDescription = stringResource(Res.string.cd_ansible_platform),
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
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
                        StatusPill(stringResource(Res.string.instances_status_active), MaterialTheme.colorScheme.primary)
                    }
                    if (instance.mcpEnabled) {
                        Spacer(modifier = Modifier.width(4.dp))
                        StatusPill(stringResource(Res.string.instances_status_mcp), MaterialTheme.colorScheme.primary)
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
                    contentDescription = stringResource(Res.string.cd_logout_instance, instance.displayLabel),
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
                text = stringResource(Res.string.instances_detail_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ListItem(
                headlineContent = { Text(stringResource(Res.string.label_url)) },
                supportingContent = { Text(instance.baseUrl) }
            )

            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text(stringResource(Res.string.instances_detail_alias)) },
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
                    Text(stringResource(Res.string.instances_detail_replace_token))
                }
            } else {
                Text(
                    text = stringResource(Res.string.instances_detail_replace_token_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                OutlinedTextField(
                    value = newToken,
                    onValueChange = { newToken = it },
                    label = { Text(stringResource(Res.string.instances_detail_new_token)) },
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
                    Text(stringResource(Res.string.instances_detail_cancel_token_reset))
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
                        text = stringResource(Res.string.instances_detail_trust_self_signed),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(Res.string.instances_detail_trust_self_signed_desc),
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
                    Text(stringResource(Res.string.instances_detail_saving))
                } else {
                    Text(stringResource(Res.string.instances_detail_save_changes))
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
                    headlineContent = { Text(stringResource(Res.string.label_platform)) },
                    supportingContent = {
                        Text(
                            when (info.platformType) {
                                "AAP" -> if (info.aapVersion != null) stringResource(Res.string.instances_platform_aap_version, info.aapVersion!!) else stringResource(Res.string.instances_platform_aap)
                                "AWX" -> stringResource(Res.string.instances_platform_awx)
                                "JEWEL" -> stringResource(Res.string.instances_platform_jewel)
                                else -> stringResource(Res.string.dashboard_platform_unknown)
                            }
                        )
                    }
                )
                if (info.controllerVersion.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.label_controller)) },
                        supportingContent = { Text(info.controllerVersion) }
                    )
                }
                if (info.gatewayVersion.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.label_gateway)) },
                        supportingContent = { Text(info.gatewayVersion) }
                    )
                }
                if (info.edaVersion.isNotBlank()) {
                    ListItem(
                        headlineContent = { Text(stringResource(Res.string.label_eda)) },
                        supportingContent = { Text(info.edaVersion) }
                    )
                }
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.label_components)) },
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
                    headlineContent = { Text(stringResource(Res.string.instances_info_title)) },
                    supportingContent = { Text(stringResource(Res.string.instances_info_not_discovered)) }
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
                    Text(stringResource(Res.string.instances_discovering))
                } else {
                    Text(if (info != null) stringResource(Res.string.instances_refresh_info) else stringResource(Res.string.instances_discover_info))
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

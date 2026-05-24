package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import io.github.leogallego.ansiblejane.model.AapComponent
import io.github.leogallego.ansiblejane.model.AapInstance

@Composable
fun InstancesTab(
    instances: List<AapInstance>,
    selectedInstance: AapInstance?,
    selectedInstanceForDetails: AapInstance?,
    discoveryRefreshing: Boolean,
    onSwitchInstance: (String) -> Unit,
    onRemoveInstance: (String) -> Unit,
    onShowDetails: (String) -> Unit,
    onDismissDetails: () -> Unit,
    onRefreshInstanceInfo: (String) -> Unit,
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
            onRefresh = { onRefreshInstanceInfo(instance.id) },
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
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
            if (instance.alias != null) {
                ListItem(
                    headlineContent = { Text("Alias") },
                    supportingContent = { Text(instance.alias) }
                )
            }
            if (instance.trustSelfSigned) {
                ListItem(
                    headlineContent = { Text("Self-Signed Certificate") },
                    supportingContent = { Text("Trusted") }
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
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Discovering...")
                } else {
                    Text(if (info != null) "Refresh Instance Info" else "Discover Instance Info")
                }
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

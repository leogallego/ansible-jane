package io.github.leogallego.ansiblejane.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.BuildConfig
import io.github.leogallego.ansiblejane.ui.components.TimeFormat
import java.time.ZoneId

@Composable
fun GeneralTab(
    timezoneId: String?,
    timeFormat: TimeFormat,
    onTimezoneSelected: (String?) -> Unit,
    onTimeFormatSelected: (TimeFormat) -> Unit,
    onClearHistory: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DisplaySection(
            currentTimezone = timezoneId,
            currentTimeFormat = timeFormat,
            onTimezoneSelected = onTimezoneSelected,
            onTimeFormatSelected = onTimeFormatSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        BackupRestoreSection()

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { showClearHistoryConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("button_clear_history")
        ) {
            Text("Clear Chat History")
        }

        Spacer(modifier = Modifier.height(24.dp))

        AboutSection()

        Spacer(modifier = Modifier.height(16.dp))

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

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Logout All") },
            text = {
                Text("Remove all instances and log out? You will need to re-authenticate each instance.")
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

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear Chat History") },
            text = {
                Text("This will remove all messages from the assistant chat. This cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryConfirm = false
                        onClearHistory()
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DisplaySection(
    currentTimezone: String?,
    currentTimeFormat: TimeFormat,
    onTimezoneSelected: (String?) -> Unit,
    onTimeFormatSelected: (TimeFormat) -> Unit
) {
    var showTimezonePicker by remember { mutableStateOf(false) }
    val timezoneDisplay = currentTimezone ?: "System (${ZoneId.systemDefault().id})"

    Text(
        text = "Display",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showTimezonePicker = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Timezone", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = timezoneDisplay,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Time format", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TimeFormat.entries.forEach { format ->
                    FilterChip(
                        selected = currentTimeFormat == format,
                        onClick = { onTimeFormatSelected(format) },
                        label = { Text(format.displayName) }
                    )
                }
            }
        }
    }

    if (showTimezonePicker) {
        TimezonePickerSheet(
            currentTimezone = currentTimezone,
            onSelect = {
                onTimezoneSelected(it)
                showTimezonePicker = false
            },
            onDismiss = { showTimezonePicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimezonePickerSheet(
    currentTimezone: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val systemZone = ZoneId.systemDefault().id

    val timezones = remember {
        listOf(null to "System ($systemZone)") + ZoneId.getAvailableZoneIds()
            .filter { it.contains("/") && !it.startsWith("SystemV") }
            .sorted()
            .map { it to it }
    }

    var searchQuery by remember { mutableStateOf("") }
    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) timezones
        else timezones.filter { (_, label) ->
            label.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Select Timezone",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search timezones...") },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )
            LazyColumn(modifier = Modifier.height(400.dp)) {
                items(
                    items = filtered,
                    key = { it.second }
                ) { (zoneId, label) ->
                    val isSelected = zoneId == currentTimezone
                    ListItem(
                        headlineContent = {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else null
                            )
                        },
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable { onSelect(zoneId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSection() {
    val context = LocalContext.current

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
                text = "Ansible Jane",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
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
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/leogallego/ansible-jane"))
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

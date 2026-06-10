package io.github.leogallego.ansiblejane.ui.settings

import io.github.leogallego.ansiblejane.platform.PlatformUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.AppVersion
import io.github.leogallego.ansiblejane.model.PollInterval
import io.github.leogallego.ansiblejane.ui.components.ThemeMode
import io.github.leogallego.ansiblejane.ui.components.TimeFormat
import java.time.ZoneId

@Composable
fun GeneralTab(
    timezoneId: String?,
    timeFormat: TimeFormat,
    themeMode: ThemeMode,
    pollInterval: PollInterval,
    approvalPollingEnabled: Boolean,
    onTimezoneSelected: (String?) -> Unit,
    onTimeFormatSelected: (TimeFormat) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onPollIntervalSelected: (PollInterval) -> Unit,
    onApprovalPollingToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        DisplaySection(
            currentTimezone = timezoneId,
            currentTimeFormat = timeFormat,
            currentThemeMode = themeMode,
            onTimezoneSelected = onTimezoneSelected,
            onTimeFormatSelected = onTimeFormatSelected,
            onThemeModeSelected = onThemeModeSelected
        )

        Spacer(modifier = Modifier.height(24.dp))

        NotificationSection(
            currentPollInterval = pollInterval,
            approvalPollingEnabled = approvalPollingEnabled,
            onPollIntervalSelected = onPollIntervalSelected,
            onApprovalPollingToggled = onApprovalPollingToggled
        )

        Spacer(modifier = Modifier.height(24.dp))

        BackupRestoreSection()

        Spacer(modifier = Modifier.height(24.dp))

        AboutSection()
    }
}

@Composable
private fun DisplaySection(
    currentTimezone: String?,
    currentTimeFormat: TimeFormat,
    currentThemeMode: ThemeMode,
    onTimezoneSelected: (String?) -> Unit,
    onTimeFormatSelected: (TimeFormat) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit
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
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TimeFormat.entries.forEachIndexed { index, format ->
                    SegmentedButton(
                        selected = currentTimeFormat == format,
                        onClick = { onTimeFormatSelected(format) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TimeFormat.entries.size
                        )
                    ) {
                        Text(format.displayName, style = MaterialTheme.typography.labelSmall)
                    }
                }
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
            Text("Theme", style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = currentThemeMode == mode,
                        onClick = { onThemeModeSelected(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.entries.size
                        )
                    ) {
                        Text(mode.displayName, style = MaterialTheme.typography.labelSmall)
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationSection(
    currentPollInterval: PollInterval,
    approvalPollingEnabled: Boolean,
    onPollIntervalSelected: (PollInterval) -> Unit,
    onApprovalPollingToggled: (Boolean) -> Unit
) {
    Text(
        text = "Notifications",
        style = MaterialTheme.typography.titleMedium
    )
    Spacer(modifier = Modifier.height(8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Approval notifications", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Poll for pending workflow approvals",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = approvalPollingEnabled,
                    onCheckedChange = onApprovalPollingToggled,
                    modifier = Modifier.testTag("switch_approval_polling")
                )
            }

            AnimatedVisibility(
                visible = approvalPollingEnabled,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Poll interval", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        PollInterval.entries.forEachIndexed { index, interval ->
                            SegmentedButton(
                                selected = currentPollInterval == interval,
                                onClick = { onPollIntervalSelected(interval) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = PollInterval.entries.size
                                ),
                                modifier = Modifier.testTag("button_poll_${interval.minutes}")
                            ) {
                                Text(interval.displayName, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Sound and vibration are configured in system notification settings.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun AboutSection() {
    val platformUtils: PlatformUtils = org.koin.compose.koinInject()
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
                text = "v${AppVersion.name} (${AppVersion.code})",
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
                    platformUtils.openUrl("https://github.com/leogallego/ansible-jane")
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

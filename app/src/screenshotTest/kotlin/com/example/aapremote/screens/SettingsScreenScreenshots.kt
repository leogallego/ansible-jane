package com.example.aapremote.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.example.aapremote.ui.theme.AapRemoteTheme

private data class InstancePreview(
    val alias: String?,
    val baseUrl: String,
    val isActive: Boolean,
    val mcpEnabled: Boolean = false
)

private val instances = listOf(
    InstancePreview("Production", "https://aap-prod.example.com", true, true),
    InstancePreview("Staging", "https://aap-staging.example.com", false),
    InstancePreview(null, "https://aap-dev.example.com", false)
)

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

@Composable
private fun InstanceCardPreview(instance: InstancePreview) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (instance.isActive) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (instance.isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = instance.alias ?: instance.baseUrl.removePrefix("https://"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (instance.isActive) {
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
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    instances: List<InstancePreview>,
    darkTheme: Boolean = false
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Text("Instances", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                instances.forEach { instance ->
                    InstanceCardPreview(instance)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Add Instance")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Backup & Restore", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text("Export")
                }
                FilledTonalButton(onClick = {}, modifier = Modifier.weight(1f)) {
                    Text("Import")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

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
                        text = "v1.2.4 (26052100)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Remote control for Ansible Automation Platform",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "GPL-3.0 License",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Logout All")
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Settings - Light",
    widthDp = 400, heightDp = 900
)
@Composable
fun SettingsScreenLight() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreenContent(instances)
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Settings - Dark",
    widthDp = 400, heightDp = 900
)
@Composable
fun SettingsScreenDark() {
    AapRemoteTheme(darkTheme = true, dynamicColor = false) {
        SettingsScreenContent(instances)
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Settings Single Instance",
    widthDp = 400, heightDp = 900
)
@Composable
fun SettingsSingleInstance() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreenContent(instances.take(1))
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Settings - Medium Width",
    widthDp = 610, heightDp = 900
)
@Composable
fun SettingsScreenMediumWidth() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreenContent(instances)
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Settings - Expanded Width",
    widthDp = 900, heightDp = 900
)
@Composable
fun SettingsScreenExpandedWidth() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreenContent(instances)
    }
}

@PreviewTest
@Preview(
    showBackground = true, fontScale = 1.5f, name = "Settings - Large Font",
    widthDp = 400, heightDp = 1000
)
@Composable
fun SettingsScreenLargeFont() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        SettingsScreenContent(instances.take(2))
    }
}

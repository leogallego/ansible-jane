package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.R
import java.net.URI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMcpServerSheet(
    onDismiss: () -> Unit,
    onAdd: (url: String, label: String, toolset: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var toolset by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.tools_mcp_add_server),
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.tools_mcp_server_name)) },
                placeholder = { Text("knowledge") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it; urlError = null },
                label = { Text(stringResource(R.string.tools_mcp_server_url)) },
                placeholder = { Text("https://mcp-server:3000/mcp") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = urlError != null,
                supportingText = urlError?.let { error -> { Text(error) } }
            )

            OutlinedTextField(
                value = toolset,
                onValueChange = { toolset = it },
                label = { Text(stringResource(R.string.tools_mcp_server_toolset)) },
                placeholder = { Text("job_management") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = {
                        val error = validateMcpUrl(url)
                        if (error != null) {
                            urlError = error
                        } else {
                            onAdd(url, name, toolset.ifBlank { null })
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_add_mcp_server"),
                    enabled = name.isNotBlank() && url.isNotBlank()
                ) { Text(stringResource(R.string.tools_mcp_add_server)) }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun validateMcpUrl(url: String): String? {
    val sanitized = url.trim().trimEnd('/')
    if (sanitized.isBlank()) return "URL is required"
    val uri = try {
        URI(sanitized)
    } catch (_: Exception) {
        return "Invalid URL format"
    }
    val scheme = uri.scheme?.lowercase()
    if (scheme !in listOf("https", "wss")) return "Only HTTPS and WSS URLs are supported"
    if (uri.host.isNullOrBlank()) return "URL must include a hostname"
    return null
}

package com.example.aapremote.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json

private const val MAX_EXTRA_VARS_SIZE = 64 * 1024

@Composable
fun ExtraVarsDialog(
    templateName: String,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var extraVars by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun validate(): Boolean {
        if (extraVars.isBlank()) {
            validationError = null
            return true
        }
        if (extraVars.toByteArray().size > MAX_EXTRA_VARS_SIZE) {
            validationError = "Extra variables exceed 64 KB limit"
            return false
        }
        return try {
            Json.parseToJsonElement(extraVars)
            validationError = null
            true
        } catch (e: Exception) {
            validationError = "Invalid JSON: ${e.message}"
            false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Launch \"$templateName\"") },
        text = {
            Column {
                Text(
                    text = "Enter extra variables (JSON):",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = extraVars,
                    onValueChange = {
                        extraVars = it
                        if (validationError != null) validate()
                    },
                    placeholder = { Text("{\"key\": \"value\"}") },
                    isError = validationError != null,
                    supportingText = validationError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validate()) {
                        onConfirm(extraVars.ifBlank { null })
                    }
                }
            ) {
                Text("Launch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

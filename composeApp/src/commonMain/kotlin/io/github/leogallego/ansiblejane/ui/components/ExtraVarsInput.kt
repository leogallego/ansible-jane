package io.github.leogallego.ansiblejane.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*
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

    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dialogScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "dialogAlpha"
    )

    LaunchedEffect(Unit) { visible = true }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.extra_vars_title, templateName)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.extra_vars_prompt),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = extraVars,
                    onValueChange = {
                        extraVars = it
                        if (validationError != null) validate()
                    },
                    placeholder = { Text(stringResource(Res.string.extra_vars_placeholder)) },
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
                Text(stringResource(Res.string.btn_launch))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.btn_cancel))
            }
        },
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
    )
}

package io.github.leogallego.ansiblejane.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

@Composable
private fun AuthScreenContent(
    baseUrl: String = "",
    token: String = "",
    alias: String = "",
    trustSelfSigned: Boolean = false,
    isLoading: Boolean = false,
    error: AppError? = null,
    isAddInstance: Boolean = false,
    isReAuth: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (isReAuth) "Re-authenticate" else "Ansible Jane",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = {},
            label = { Text("AAP Base URL") },
            placeholder = { Text("https://aap.example.com") },
            singleLine = true,
            enabled = !isReAuth,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = alias,
            onValueChange = {},
            label = { Text("Alias (optional)") },
            placeholder = { Text("e.g., Production") },
            singleLine = true,
            enabled = !isReAuth,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = token,
            onValueChange = {},
            label = { Text("AAP Personal API Token") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Accept self-signed certificate", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = trustSelfSigned, onCheckedChange = {})
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else if (error != null) {
            ErrorMessage(error = error, onRetry = null)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Retry")
            }
        } else {
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = baseUrl.isNotBlank() && token.isNotBlank()
            ) {
                Text(if (isReAuth) "Re-authenticate" else "Connect")
            }
        }

        if (isAddInstance) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Auth Empty - Light",
    widthDp = 400, heightDp = 900
)
@Composable
fun AuthScreenEmptyLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        AuthScreenContent()
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Auth Empty - Dark",
    widthDp = 400, heightDp = 900
)
@Composable
fun AuthScreenEmptyDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
        AuthScreenContent()
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Auth Filled",
    widthDp = 400, heightDp = 900
)
@Composable
fun AuthScreenFilled() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        AuthScreenContent(
            baseUrl = "https://aap.example.com",
            token = "my-api-token-123",
            alias = "Production"
        )
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Auth Loading",
    widthDp = 400, heightDp = 900
)
@Composable
fun AuthScreenLoading() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        AuthScreenContent(
            baseUrl = "https://aap.example.com",
            token = "my-api-token-123",
            isLoading = true
        )
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Auth Error",
    widthDp = 400, heightDp = 900
)
@Composable
fun AuthScreenError() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        AuthScreenContent(
            baseUrl = "https://aap.example.com",
            token = "bad-token",
            error = AppError.Auth()
        )
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Auth Self-Signed",
    widthDp = 400, heightDp = 900
)
@Composable
fun AuthScreenSelfSigned() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        AuthScreenContent(
            baseUrl = "https://aap.example.com",
            trustSelfSigned = true
        )
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Auth Add Instance",
    widthDp = 400, heightDp = 900
)
@Composable
fun AuthScreenAddInstance() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        AuthScreenContent(
            baseUrl = "https://aap.example.com",
            token = "my-token",
            isAddInstance = true
        )
    }
}

@PreviewTest
@Preview(
    showBackground = true, fontScale = 1.5f, name = "Auth - Large Font",
    widthDp = 400, heightDp = 1000
)
@Composable
fun AuthScreenLargeFont() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        AuthScreenContent(
            baseUrl = "https://aap.example.com",
            token = "my-token",
            alias = "Production"
        )
    }
}

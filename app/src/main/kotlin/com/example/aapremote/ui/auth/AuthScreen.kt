package com.example.aapremote.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aapremote.R
import com.example.aapremote.presentation.auth.AuthUiState
import com.example.aapremote.presentation.auth.AuthViewModel
import com.example.aapremote.ui.components.ErrorMessage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    onNavigateToDashboard: () -> Unit,
    preFilledUrl: String? = null,
    preFilledAlias: String? = null,
    reAuthInstanceId: String? = null,
    isAddInstance: Boolean = false,
    preFilledTrustSelfSigned: Boolean = false,
    viewModel: AuthViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isReAuth = reAuthInstanceId != null

    // Only check existing credentials on initial login, not when adding or re-authenticating
    LaunchedEffect(Unit) {
        if (!isAddInstance && reAuthInstanceId == null) {
            viewModel.checkExistingCredentials()
        }
    }

    var baseUrl by remember { mutableStateOf(preFilledUrl ?: "") }
    var token by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf(preFilledAlias ?: "") }
    var trustSelfSigned by remember { mutableStateOf(preFilledTrustSelfSigned) }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onNavigateToDashboard()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_ansible_platform),
            contentDescription = "Ansible Automation Platform",
            modifier = Modifier.size(96.dp),
            tint = androidx.compose.ui.graphics.Color.Unspecified
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isReAuth) "Re-authenticate" else "AAPdroid",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (isReAuth) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Token expired. Please enter a new API token.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("AAP Base URL") },
            placeholder = { Text("https://aap.example.com") },
            singleLine = true,
            enabled = !isReAuth,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            trailingIcon = {
                if (baseUrl.isNotEmpty() && !isReAuth) {
                    IconButton(onClick = { baseUrl = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Username }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = alias,
            onValueChange = { alias = it },
            label = { Text("Alias (optional)") },
            placeholder = { Text("e.g., Production") },
            singleLine = true,
            enabled = !isReAuth,
            trailingIcon = {
                if (alias.isNotEmpty() && !isReAuth) {
                    IconButton(onClick = { alias = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = token,
            onValueChange = { token = it },
            label = { Text("AAP Personal API Token") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                Row {
                    if (token.isNotEmpty()) {
                        IconButton(onClick = { token = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility
                                else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide token" else "Show token"
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentType = ContentType.Password }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Accept self-signed certificate",
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = trustSelfSigned,
                onCheckedChange = { trustSelfSigned = it }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        val onConnect = {
            viewModel.connect(
                baseUrl = baseUrl,
                token = token,
                trustSelfSigned = trustSelfSigned,
                alias = alias.ifBlank { null },
                existingInstanceId = reAuthInstanceId
            )
        }

        when (val state = uiState) {
            is AuthUiState.Loading -> {
                CircularProgressIndicator()
            }
            is AuthUiState.Error -> {
                ErrorMessage(
                    error = state.error,
                    onRetry = { onConnect() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onConnect() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = baseUrl.isNotBlank() && token.isNotBlank()
                ) {
                    Text(if (isReAuth) "Re-authenticate" else "Connect")
                }
            }
            else -> {
                Button(
                    onClick = { onConnect() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = baseUrl.isNotBlank() && token.isNotBlank()
                ) {
                    Text(if (isReAuth) "Re-authenticate" else "Connect")
                }
            }
        }
    }
}

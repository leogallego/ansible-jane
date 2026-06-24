package io.github.leogallego.ansiblejane.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import io.github.leogallego.ansiblejane.presentation.auth.AuthUiState
import io.github.leogallego.ansiblejane.presentation.auth.AuthViewModel
import io.github.leogallego.ansiblejane.presentation.settings.BackupViewModel
import io.github.leogallego.ansiblejane.presentation.settings.BackupUiState
import io.github.leogallego.ansiblejane.presentation.settings.ImportMode
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.settings.ImportFromBackupButton
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AuthScreen(
    onNavigateToDashboard: () -> Unit,
    onCancel: (() -> Unit)? = null,
    preFilledUrl: String? = null,
    preFilledAlias: String? = null,
    reAuthInstanceId: String? = null,
    isAddInstance: Boolean = false,
    preFilledTrustSelfSigned: Boolean = false,
    viewModel: AuthViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isReAuth = reAuthInstanceId != null

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

    val topBarTitle = when {
        isReAuth -> stringResource(Res.string.auth_title_reauth)
        isAddInstance -> stringResource(Res.string.auth_title_add_instance)
        else -> null
    }

    Scaffold(
        topBar = {
            if (onCancel != null && topBarTitle != null) {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text(topBarTitle) },
                    navigationIcon = {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.testTag("button_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.cd_back)
                            )
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (onCancel == null) Arrangement.Center else Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Devices,
                    contentDescription = stringResource(Res.string.cd_ansible_jane),
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isReAuth) stringResource(Res.string.auth_title_reauth) else stringResource(Res.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (isReAuth) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.auth_token_expired, preFilledAlias ?: preFilledUrl ?: stringResource(Res.string.auth_token_expired_fallback)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            val textFieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text(stringResource(Res.string.auth_label_url)) },
                placeholder = { Text(stringResource(Res.string.auth_placeholder_url)) },
                singleLine = true,
                enabled = !isReAuth,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                trailingIcon = {
                    if (baseUrl.isNotEmpty() && !isReAuth) {
                        IconButton(onClick = { baseUrl = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.cd_clear))
                        }
                    }
                },
                colors = textFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_url")
                    .semantics { contentType = ContentType.Username }
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = alias,
                onValueChange = { alias = it },
                label = { Text(stringResource(Res.string.auth_label_alias)) },
                placeholder = { Text(stringResource(Res.string.auth_placeholder_alias)) },
                singleLine = true,
                enabled = !isReAuth,
                trailingIcon = {
                    if (alias.isNotEmpty() && !isReAuth) {
                        IconButton(onClick = { alias = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.cd_clear))
                        }
                    }
                },
                colors = textFieldColors,
                modifier = Modifier.fillMaxWidth().testTag("field_alias")
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = token,
                onValueChange = { token = it },
                label = { Text(stringResource(Res.string.auth_label_token)) },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    Row {
                        if (token.isNotEmpty()) {
                            IconButton(onClick = { token = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(Res.string.cd_clear))
                            }
                        }
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) stringResource(Res.string.cd_hide_token) else stringResource(Res.string.cd_show_token)
                            )
                        }
                    }
                },
                colors = textFieldColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_token")
                    .semantics { contentType = ContentType.Password }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.auth_label_self_signed),
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = trustSelfSigned,
                    onCheckedChange = { trustSelfSigned = it },
                    modifier = Modifier.testTag("switch_self_signed")
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
                        onRetry = null
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onConnect() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = baseUrl.isNotBlank() && token.isNotBlank()
                    ) {
                        Text(if (isReAuth) stringResource(Res.string.auth_title_reauth) else stringResource(Res.string.btn_retry))
                    }
                }
                else -> {
                    Button(
                        onClick = { onConnect() },
                        modifier = Modifier.fillMaxWidth().testTag("button_connect"),
                        enabled = baseUrl.isNotBlank() && token.isNotBlank()
                    ) {
                        Text(if (isReAuth) stringResource(Res.string.auth_title_reauth) else stringResource(Res.string.btn_connect))
                    }
                }
            }

            if (!isReAuth && !isAddInstance) {
                Spacer(modifier = Modifier.height(16.dp))
                ImportFromBackupButton(onNavigateToDashboard = onNavigateToDashboard)
            }
        }
    }
}

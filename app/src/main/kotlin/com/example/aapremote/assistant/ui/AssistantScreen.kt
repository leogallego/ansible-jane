package com.example.aapremote.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.platform.testTag
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aapremote.assistant.presentation.AssistantUiState
import com.example.aapremote.assistant.presentation.AssistantViewModel
import com.example.aapremote.network.mcp.McpConnectionState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AssistantScreen(
    viewModel: AssistantViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    when (val state = uiState) {
        is AssistantUiState.Idle -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No active instance. Please log in first.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        is AssistantUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(
                        text = "Connecting to MCP servers...",
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        is AssistantUiState.Active -> {
            ActiveChatContent(
                state = state,
                onSendMessage = { viewModel.sendMessage(it) },
                onOpenSettings = { showSettings = true },
                modifier = Modifier.fillMaxSize()
            )
        }

        is AssistantUiState.Error -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.error.message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    if (showSettings) {
        val activeInstance = viewModel.activeInstance
        val connections = (uiState as? AssistantUiState.Active)?.connections ?: emptyMap()
        val currentLlmConfig by viewModel.llmConfig.collectAsStateWithLifecycle()
        val fetchedModels by viewModel.fetchedModels.collectAsStateWithLifecycle()
        val modelFetchState by viewModel.modelFetchState.collectAsStateWithLifecycle()
        AssistantSettingsSheet(
            mcpEnabled = activeInstance?.mcpEnabled ?: false,
            mcpServers = activeInstance?.mcpServerUrls ?: emptyList(),
            connections = connections,
            currentLlmConfig = currentLlmConfig,
            onToggleMcp = { viewModel.toggleMcpEnabled(it) },
            onAddMcpServer = { url, label -> viewModel.addMcpServer(url, label) },
            onRemoveMcpServer = { viewModel.removeMcpServer(it) },
            onToggleReadOnly = { url, readOnly -> viewModel.toggleServerReadOnly(url, readOnly) },
            fetchedModels = fetchedModels,
            modelFetchState = modelFetchState,
            onFetchModels = { url, key -> viewModel.fetchAvailableModels(url, key) },
            onClearFetchedModels = { viewModel.clearFetchedModels() },
            onSaveLlmConfig = { viewModel.updateLlmConfig(it) },
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
private fun ActiveChatContent(
    state: AssistantUiState.Active,
    onSendMessage: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var inputText by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }

    fun submit() {
        val text = inputText.text
        if (text.isNotBlank() && !state.isGenerating) {
            onSendMessage(text)
            inputText = TextFieldValue("")
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = modifier) {
        if (state.connections.isNotEmpty()) {
            val connected = state.connections.count { it.value is McpConnectionState.Connected }
            val total = state.connections.size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$connected/$total MCP servers connected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpenSettings) {
                    Text("Configure", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onOpenSettings) {
                    Text("Configure LLM", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ask me anything about your AAP instance.\nFor example: \"What job templates are available?\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(
                items = state.messages,
                key = { it.id },
                contentType = { it.role }
            ) { message ->
                ChatBubble(message = message, modifier = Modifier.padding(vertical = 2.dp))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("field_assistant_input")
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyUp && it.key == Key.Enter) {
                            submit()
                            true
                        } else false
                    },
                placeholder = { Text("Ask a question...") },
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submit() }),
            )

            IconButton(
                onClick = { submit() },
                modifier = Modifier.testTag("button_send"),
                enabled = inputText.text.isNotBlank() && !state.isGenerating
            ) {
                if (state.isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}

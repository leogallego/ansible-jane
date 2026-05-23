package io.github.leogallego.ansiblejane.assistant.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage
import io.github.leogallego.ansiblejane.assistant.engine.Role
import io.github.leogallego.ansiblejane.assistant.presentation.AssistantUiState
import io.github.leogallego.ansiblejane.assistant.presentation.AssistantViewModel
import io.github.leogallego.ansiblejane.network.mcp.McpConnectionState
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

@OptIn(ExperimentalLayoutApi::class)
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

    val imeVisible = WindowInsets.isImeVisible
    val messageCount = state.messages.size
    val hasStreamingSection = state.isGenerating
    val totalItems = messageCount + (if (hasStreamingSection) 1 else 0) +
        (if (messageCount == 0 && !hasStreamingSection) 1 else 0)

    LaunchedEffect(messageCount, state.isGenerating) {
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    LaunchedEffect(imeVisible) {
        if (imeVisible && totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = modifier.imePadding()) {
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
                TextButton(onClick = onOpenSettings, modifier = Modifier.testTag("button_configure")) {
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
                TextButton(onClick = onOpenSettings, modifier = Modifier.testTag("button_configure_llm")) {
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (state.messages.isEmpty() && state.streamingText == null) {
                item(key = "empty_placeholder") {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Hi, I'm Jane. I'm already connected and ready to help.\n\n" +
                                "Try asking me things like:\n" +
                                "- \"What job templates are available?\"\n" +
                                "- \"Show me the recent jobs\"\n" +
                                "- \"List the hosts in my inventory\"",
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
                when (message.role) {
                    Role.USER -> UserBubble(message = message)
                    Role.ASSISTANT -> AssistantMessage(
                        content = message.content,
                        source = message.source,
                        toolsUsed = message.toolsUsed
                    )
                    else -> AssistantMessage(content = message.content)
                }
            }

            if (state.isGenerating) {
                item(key = "streaming", contentType = "streaming") {
                    StreamingIndicator(statusText = state.streamingText ?: "Thinking...")
                }
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
                        if (it.type == KeyEventType.KeyUp &&
                            it.key == Key.Enter &&
                            it.isCtrlPressed
                        ) {
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

@Composable
private fun StreamingIndicator(
    statusText: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

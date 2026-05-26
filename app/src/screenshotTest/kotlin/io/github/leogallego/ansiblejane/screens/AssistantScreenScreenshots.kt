package io.github.leogallego.ansiblejane.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage
import io.github.leogallego.ansiblejane.assistant.engine.Role
import io.github.leogallego.ansiblejane.assistant.ui.AssistantMessage
import io.github.leogallego.ansiblejane.assistant.ui.UserBubble
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

private val chatMessages = listOf(
    ChatMessage(role = Role.USER, content = "List all job templates"),
    ChatMessage(role = Role.ASSISTANT, content = "Querying [list_job_templates]..."),
    ChatMessage(
        role = Role.ASSISTANT,
        content = "Found **3 job templates**:\n\n1. Deploy Production App\n2. Run Smoke Tests\n3. Backup Database\n\nWould you like to launch any of these?"
    ),
    ChatMessage(role = Role.USER, content = "Launch Deploy Production App"),
    ChatMessage(role = Role.ASSISTANT, content = "Querying [launch_job]..."),
    ChatMessage(
        role = Role.ASSISTANT,
        content = "Job launched successfully! Job ID: **#142**\nStatus: Running\n\nI'll keep monitoring. Would you like updates on the progress?"
    )
)

@Composable
private fun ChatBubble(message: ChatMessage) {
    when (message.role) {
        Role.USER -> UserBubble(message = message)
        else -> AssistantMessage(content = message.content)
    }
}

@Composable
private fun ChatInputBar(enabled: Boolean = true) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            placeholder = { Text("Ask about your AAP...") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            enabled = enabled
        )
        IconButton(onClick = {}, enabled = false) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send"
            )
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Assistant Chat - Light",
    widthDp = 400, heightDp = 900
)
@Composable
fun AssistantChatLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { message ->
                    ChatBubble(message = message)
                }
            }
            ChatInputBar()
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Assistant Chat - Dark",
    widthDp = 400, heightDp = 900
)
@Composable
fun AssistantChatDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages) { message ->
                    ChatBubble(message = message)
                }
            }
            ChatInputBar()
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Assistant Empty State",
    widthDp = 400, heightDp = 500
)
@Composable
fun AssistantEmptyState() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "AI Assistant",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Configure an LLM provider to get started",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = {}) {
                        Text("Configure")
                    }
                }
            }
            ChatInputBar(enabled = false)
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Assistant Loading",
    widthDp = 400, heightDp = 500
)
@Composable
fun AssistantLoading() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages.take(1)) { message ->
                    ChatBubble(message = message)
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }
            ChatInputBar(enabled = false)
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, fontScale = 1.5f, name = "Assistant - Large Font",
    widthDp = 400, heightDp = 900
)
@Composable
fun AssistantChatLargeFont() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(chatMessages.take(3)) { message ->
                    ChatBubble(message = message)
                }
            }
            ChatInputBar()
        }
    }
}

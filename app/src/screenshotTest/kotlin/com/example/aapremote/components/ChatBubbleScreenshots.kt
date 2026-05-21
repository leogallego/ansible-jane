package com.example.aapremote.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.example.aapremote.assistant.engine.ChatMessage
import com.example.aapremote.assistant.engine.Role
import com.example.aapremote.assistant.ui.ChatBubble
import com.example.aapremote.ui.theme.AapRemoteTheme

private val userMessage = ChatMessage(
    role = Role.USER,
    content = "List all job templates in production"
)

private val assistantMessage = ChatMessage(
    role = Role.ASSISTANT,
    content = "Here are the job templates in production:\n\n1. **Deploy App** - Deploys the application\n2. **Run Tests** - Executes test suite\n3. **Backup DB** - Database backup job"
)

private val toolMessage = ChatMessage(
    role = Role.ASSISTANT,
    content = "Querying [list_job_templates]..."
)

private val errorMessage = ChatMessage(
    role = Role.ASSISTANT,
    content = "Error: Failed to connect to AAP instance. Please check your credentials."
)

@PreviewTest
@Preview(showBackground = true, name = "Chat Conversation - Light")
@Composable
fun ChatBubbleConversationLight() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatBubble(message = userMessage)
            ChatBubble(message = toolMessage)
            ChatBubble(message = assistantMessage)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Chat Conversation - Dark")
@Composable
fun ChatBubbleConversationDark() {
    AapRemoteTheme(darkTheme = true, dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatBubble(message = userMessage)
            ChatBubble(message = toolMessage)
            ChatBubble(message = assistantMessage)
        }
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Error Bubble")
@Composable
fun ChatBubbleError() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        ChatBubble(message = errorMessage)
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "Chat Conversation - Large Font")
@Composable
fun ChatBubbleConversationLargeFont() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatBubble(message = userMessage)
            ChatBubble(message = assistantMessage)
        }
    }
}

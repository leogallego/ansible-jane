package io.github.leogallego.ansiblejane.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.assistant.engine.ChatMessage
import io.github.leogallego.ansiblejane.assistant.engine.Role
import io.github.leogallego.ansiblejane.assistant.ui.AssistantMessage
import io.github.leogallego.ansiblejane.assistant.ui.UserBubble
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

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

@Composable
private fun ChatBubble(message: ChatMessage) {
    when (message.role) {
        Role.USER -> UserBubble(message = message)
        else -> AssistantMessage(content = message.content)
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Chat Conversation - Light")
@Composable
fun ChatBubbleConversationLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        AssistantMessage(content = errorMessage.content)
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "Chat Conversation - Large Font")
@Composable
fun ChatBubbleConversationLargeFont() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChatBubble(message = userMessage)
            ChatBubble(message = assistantMessage)
        }
    }
}

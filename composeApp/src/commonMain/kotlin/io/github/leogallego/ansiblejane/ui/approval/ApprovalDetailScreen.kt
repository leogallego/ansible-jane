package io.github.leogallego.ansiblejane.ui.approval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import io.github.leogallego.ansiblejane.model.WorkflowApproval
import io.github.leogallego.ansiblejane.presentation.approval.ApprovalDetailUiState
import io.github.leogallego.ansiblejane.presentation.approval.ApprovalDetailViewModel
import io.github.leogallego.ansiblejane.ui.components.DateFormatter
import io.github.leogallego.ansiblejane.ui.components.DetailRowHorizontal
import io.github.leogallego.ansiblejane.ui.components.DetailScaffold
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.SkeletonCard
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ApprovalDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: ApprovalDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val haptic = LocalHapticFeedback.current

    var showConfirmDialog by remember { mutableStateOf<String?>(null) }

    DetailScaffold(title = "Workflow Approval", onNavigateBack = onNavigateBack) {
            when (val state = uiState) {
                is ApprovalDetailUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        SkeletonCard()
                    }
                }

                is ApprovalDetailUiState.Error -> {
                    ErrorMessage(
                        error = state.error,
                        onRetry = { viewModel.loadApproval() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ApprovalDetailUiState.Ready -> {
                    val isPending = state.approval.status == "pending"
                    ApprovalDetailContent(
                        approval = state.approval,
                        isActionInProgress = false,
                        showActions = isPending,
                        onApprove = { showConfirmDialog = "approve" },
                        onDeny = { showConfirmDialog = "deny" }
                    )
                }

                is ApprovalDetailUiState.ActionInProgress -> {
                    ApprovalDetailContent(
                        approval = state.approval,
                        isActionInProgress = true,
                        showActions = true,
                        onApprove = {},
                        onDeny = {}
                    )
                }

                is ApprovalDetailUiState.Completed -> {
                    ApprovalCompletedContent(
                        approval = state.approval,
                        action = state.action,
                        onNavigateBack = onNavigateBack
                    )
                }
            }
        }
    // Confirmation dialog is intentionally outside DetailScaffold
    showConfirmDialog?.let { action ->
        val approvalName = when (val state = uiState) {
            is ApprovalDetailUiState.Ready -> state.approval.name
            else -> "this approval"
        }

        AlertDialog(
            onDismissRequest = { showConfirmDialog = null },
            title = {
                Text(
                    text = if (action == "approve") "Approve Workflow?" else "Deny Workflow?"
                )
            },
            text = {
                Text(
                    text = if (action == "approve") {
                        "Are you sure you want to approve \"$approvalName\"?"
                    } else {
                        "Are you sure you want to deny \"$approvalName\"? This action cannot be undone."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showConfirmDialog = null
                        if (action == "approve") viewModel.approve() else viewModel.deny()
                    }
                ) {
                    Text(if (action == "approve") "Approve" else "Deny")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ApprovalDetailContent(
    approval: WorkflowApproval,
    isActionInProgress: Boolean,
    showActions: Boolean,
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Approval info card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = approval.name,
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                DetailRowHorizontal("Status", approval.status.replaceFirstChar { it.uppercase() })

                if (approval.created.isNotBlank()) {
                    DetailRowHorizontal("Created", DateFormatter.formatDateTime(approval.created))
                    if (approval.status == "pending") {
                        DetailRowHorizontal("Pending for", DateFormatter.formatDuration(approval.created))
                    }
                }

                approval.summaryFields.workflowJob?.let { job ->
                    job.launchedBy?.let { user ->
                        DetailRowHorizontal("Launched by", user.username)
                    }
                }

                approval.summaryFields.workflowJobTemplate?.let { template ->
                    DetailRowHorizontal("Workflow Template", template.name)
                }

                approval.summaryFields.workflowJob?.let { job ->
                    DetailRowHorizontal("Workflow Job", job.name)
                    DetailRowHorizontal("Job Status", job.status)
                }

                approval.summaryFields.approvedOrDeniedBy?.let { user ->
                    DetailRowHorizontal(
                        if (approval.status == "approved") "Approved by" else "Denied by",
                        user.username
                    )
                }

                if (approval.timedOut) {
                    DetailRowHorizontal("Timed Out", "Yes")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!showActions) {
            val displayStatus = approval.status.ifBlank { "unknown" }
            val (cardColor, contentColor, statusIcon, statusDescription) = when (displayStatus) {
                "approved" -> StatusCardStyle(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    Icons.Default.CheckCircle,
                    "Approved"
                )
                "denied" -> StatusCardStyle(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                    Icons.Default.Cancel,
                    "Denied"
                )
                else -> StatusCardStyle(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    Icons.Default.Info,
                    displayStatus.replaceFirstChar { it.uppercase() }
                )
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusDescription,
                        tint = contentColor
                    )
                    Text(
                        text = "This approval has been $displayStatus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
            }
        } else if (isActionInProgress) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("button_approve_action"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Approve")
                }

                OutlinedButton(
                    onClick = onDeny,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("button_deny_action"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Deny")
                }
            }
        }
    }
}

@Composable
private fun ApprovalCompletedContent(
    approval: WorkflowApproval,
    action: String,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val icon = if (action == "approved") Icons.Default.CheckCircle else Icons.Default.Cancel
        val color = if (action == "approved") {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Workflow ${action.replaceFirstChar { it.uppercase() }}",
            style = MaterialTheme.typography.headlineSmall,
            color = color
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = approval.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onNavigateBack) {
            Text("Go Back")
        }
    }
}

private data class StatusCardStyle(
    val cardColor: Color,
    val contentColor: Color,
    val icon: ImageVector,
    val description: String
)

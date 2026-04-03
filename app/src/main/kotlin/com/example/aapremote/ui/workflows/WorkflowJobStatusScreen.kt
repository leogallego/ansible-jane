package com.example.aapremote.ui.workflows

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.aapremote.model.WorkflowJob
import com.example.aapremote.model.WorkflowNode
import com.example.aapremote.presentation.workflows.NodeStdoutState
import com.example.aapremote.presentation.workflows.WorkflowJobStatusUiState
import com.example.aapremote.presentation.workflows.WorkflowJobStatusViewModel
import com.example.aapremote.ui.components.ErrorMessage
import com.example.aapremote.ui.components.JobStatusBadge
import com.example.aapremote.ui.components.WorkflowNodeItem
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowJobStatusScreen(
    onNavigateBack: () -> Unit,
    viewModel: WorkflowJobStatusViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val expandedNodeId by viewModel.expandedNodeId.collectAsStateWithLifecycle()
    val nodeStdout by viewModel.nodeStdout.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Workflow Job Status") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is WorkflowJobStatusUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is WorkflowJobStatusUiState.Error -> {
                    ErrorMessage(
                        message = state.message,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is WorkflowJobStatusUiState.Active -> {
                    WorkflowJobDetailContent(
                        workflowJob = state.workflowJob,
                        nodes = state.nodes,
                        isActive = true,
                        expandedNodeId = expandedNodeId,
                        nodeStdout = nodeStdout,
                        onToggleNode = { jobId -> viewModel.toggleNodeExpansion(jobId) }
                    )
                }
                is WorkflowJobStatusUiState.Completed -> {
                    WorkflowJobDetailContent(
                        workflowJob = state.workflowJob,
                        nodes = state.nodes,
                        isActive = false,
                        expandedNodeId = expandedNodeId,
                        nodeStdout = nodeStdout,
                        onToggleNode = { jobId -> viewModel.toggleNodeExpansion(jobId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkflowJobDetailContent(
    workflowJob: WorkflowJob,
    nodes: List<WorkflowNode>,
    isActive: Boolean,
    expandedNodeId: Int?,
    nodeStdout: Map<Int, NodeStdoutState>,
    onToggleNode: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = workflowJob.name,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Template: ${workflowJob.workflowJobTemplateName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        JobStatusBadge(status = workflowJob.status)
                        if (isActive) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(start = 8.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Details", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    workflowJob.started?.let { started ->
                        DetailRow("Started", started)
                    }
                    workflowJob.finished?.let { finished ->
                        DetailRow("Finished", finished)
                    }
                    workflowJob.elapsed?.let { elapsed ->
                        DetailRow("Elapsed", String.format("%.1f seconds", elapsed))
                    }
                    DetailRow("Job ID", "#${workflowJob.id}")
                }
            }
        }

        if (nodes.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sub-Jobs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        nodes.forEachIndexed { index, node ->
                            val jobId = node.summaryFields.job?.id
                            WorkflowNodeItem(
                                node = node,
                                isExpanded = jobId != null && jobId == expandedNodeId,
                                stdoutState = jobId?.let { nodeStdout[it] },
                                onToggleExpand = jobId?.let { id -> { onToggleNode(id) } }
                            )
                            if (index < nodes.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

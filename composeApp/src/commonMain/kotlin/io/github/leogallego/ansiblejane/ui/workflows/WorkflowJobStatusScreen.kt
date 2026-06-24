package io.github.leogallego.ansiblejane.ui.workflows

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import io.github.leogallego.ansiblejane.model.WorkflowJob
import io.github.leogallego.ansiblejane.model.WorkflowNode
import io.github.leogallego.ansiblejane.presentation.workflows.NodeStdoutState
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowJobStatusUiState
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowJobStatusViewModel
import io.github.leogallego.ansiblejane.ui.components.DetailRowHorizontal
import io.github.leogallego.ansiblejane.ui.components.DetailScaffold
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.JobStatusBadge
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkflowJobStatusScreen(
    onNavigateBack: () -> Unit,
    viewModel: WorkflowJobStatusViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val expandedNodeId by viewModel.expandedNodeId.collectAsState()
    val nodeStdout by viewModel.nodeStdout.collectAsState()

    DisposableEffect(Unit) {
        viewModel.startPolling()
        onDispose { viewModel.stopPolling() }
    }

    DetailScaffold(title = stringResource(Res.string.workflow_job_status_title), onNavigateBack = onNavigateBack) {
            when (val state = uiState) {
                is WorkflowJobStatusUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is WorkflowJobStatusUiState.Error -> {
                    ErrorMessage(
                        error = state.error,
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

@Composable
private fun WorkflowJobDetailContent(
    workflowJob: WorkflowJob,
    nodes: List<WorkflowNode>,
    isActive: Boolean,
    expandedNodeId: Int?,
    nodeStdout: Map<Int, NodeStdoutState>,
    onToggleNode: (Int) -> Unit
) {
    val orderedNodes = remember(nodes) { buildOrderedNodes(nodes) }

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
                        text = stringResource(Res.string.job_detail_template, workflowJob.workflowJobTemplateName),
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
                    Text(stringResource(Res.string.label_details), style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    workflowJob.started?.let { started ->
                        DetailRowHorizontal(stringResource(Res.string.job_detail_started), started)
                    }
                    workflowJob.finished?.let { finished ->
                        DetailRowHorizontal(stringResource(Res.string.job_detail_finished), finished)
                    }
                    workflowJob.elapsed?.let { elapsed ->
                        DetailRowHorizontal(stringResource(Res.string.job_detail_elapsed), String.format("%.1f seconds", elapsed))
                    }
                    DetailRowHorizontal(stringResource(Res.string.job_detail_job_id), "#${workflowJob.id}")
                }
            }
        }

        if (orderedNodes.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.workflow_nodes_header),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }

            itemsIndexed(
                items = orderedNodes,
                key = { _, item -> item.node.id }
            ) { index, orderedNode ->
                val jobId = orderedNode.node.summaryFields.job?.id

                WorkflowNodeCard(
                    orderedNode = orderedNode,
                    isExpanded = jobId != null && jobId == expandedNodeId,
                    stdoutState = jobId?.let { nodeStdout[it] },
                    onToggleExpand = jobId?.let { id -> { onToggleNode(id) } }
                )

                if (index < orderedNodes.lastIndex) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}


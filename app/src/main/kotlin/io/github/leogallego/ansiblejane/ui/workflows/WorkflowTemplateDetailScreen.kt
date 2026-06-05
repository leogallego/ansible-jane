package io.github.leogallego.ansiblejane.ui.workflows

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leogallego.ansiblejane.presentation.workflows.LaunchFromDetailState
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowTemplateDetailUiState
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowTemplateDetailViewModel
import io.github.leogallego.ansiblejane.ui.components.DetailScaffold
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WorkflowTemplateDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWorkflowJobStatus: (Int) -> Unit = {},
    viewModel: WorkflowTemplateDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val launchState by viewModel.launchState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(launchState) {
        when (val state = launchState) {
            is LaunchFromDetailState.Launched -> {
                viewModel.resetLaunchState()
                onNavigateToWorkflowJobStatus(state.workflowJobId)
            }
            is LaunchFromDetailState.Failed -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetLaunchState()
            }
            else -> {}
        }
    }

    DetailScaffold(
        title = "Workflow Template",
        onNavigateBack = onNavigateBack,
        titleContent = {
            Text(
                text = viewModel.templateName.ifBlank { "Workflow Template" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        floatingActionButton = {
            if (uiState is WorkflowTemplateDetailUiState.Success) {
                FloatingActionButton(
                    onClick = { viewModel.launch() },
                    modifier = Modifier.testTag("button_launch_workflow")
                ) {
                    if (launchState is LaunchFromDetailState.Launching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Launch workflow")
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
            when (val state = uiState) {
                is WorkflowTemplateDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is WorkflowTemplateDetailUiState.Error -> {
                    ErrorMessage(
                        error = state.error,
                        onRetry = { viewModel.retry() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is WorkflowTemplateDetailUiState.Success -> {
                    val orderedNodes = remember(state.nodes) {
                        buildOrderedTemplateNodes(state.nodes)
                    }

                    if (orderedNodes.isEmpty()) {
                        Text(
                            text = "No nodes in this workflow",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 8.dp)
                        ) {
                            itemsIndexed(
                                items = orderedNodes,
                                key = { _, item -> item.node.id }
                            ) { index, orderedNode ->
                                TemplateNodeCard(orderedNode = orderedNode)
                                if (index < orderedNodes.lastIndex) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun TemplateNodeCard(orderedNode: OrderedTemplateNode) {
    val node = orderedNode.node
    val incomingEdge = orderedNode.incomingEdge
    val name = node.summaryFields.unifiedJobTemplate?.name?.ifBlank { null }
        ?: node.identifier.ifBlank { "Node ${node.id}" }
    val jobType = node.summaryFields.unifiedJobTemplate?.unifiedJobType ?: ""

    Column(modifier = Modifier.fillMaxWidth()) {
        if (incomingEdge != null) {
            ConnectorSegment(type = incomingEdge)
        }

        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (jobType.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = jobType.replace("_", " ").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

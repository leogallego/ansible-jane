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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowTemplateDetailUiState
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowTemplateDetailViewModel
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import org.koin.compose.viewmodel.koinViewModel

private val connectorSuccessColor = Color(0xFF4CAF50)
private val connectorFailureColor = Color(0xFFF44336)
private val connectorAlwaysColor = Color(0xFF9E9E9E)

private fun ConnectorType.color(): Color = when (this) {
    ConnectorType.SUCCESS -> connectorSuccessColor
    ConnectorType.FAILURE -> connectorFailureColor
    ConnectorType.ALWAYS -> connectorAlwaysColor
}

private fun ConnectorType.label(): String = when (this) {
    ConnectorType.SUCCESS -> "On success"
    ConnectorType.FAILURE -> "On failure"
    ConnectorType.ALWAYS -> "Always"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowTemplateDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: WorkflowTemplateDetailViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = viewModel.templateName.ifBlank { "Workflow Template" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
            val color = incomingEdge.color()
            val label = incomingEdge.label()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(32.dp)
                        .drawBehind {
                            val centerX = size.width / 2f
                            drawLine(
                                color = color,
                                start = Offset(centerX, 0f),
                                end = Offset(centerX, size.height),
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = color,
                                radius = 4.dp.toPx(),
                                center = Offset(centerX, size.height / 2f)
                            )
                        }
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
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

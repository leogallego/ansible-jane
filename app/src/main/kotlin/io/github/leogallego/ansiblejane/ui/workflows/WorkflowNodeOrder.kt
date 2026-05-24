package io.github.leogallego.ansiblejane.ui.workflows

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import io.github.leogallego.ansiblejane.model.WorkflowJobTemplateNode
import io.github.leogallego.ansiblejane.model.WorkflowNode

enum class ConnectorType { SUCCESS, FAILURE, ALWAYS }

private val connectorSuccessColor = Color(0xFF4CAF50)
private val connectorFailureColor = Color(0xFFF44336)
private val connectorAlwaysColor = Color(0xFF9E9E9E)

fun ConnectorType.color(): Color = when (this) {
    ConnectorType.SUCCESS -> connectorSuccessColor
    ConnectorType.FAILURE -> connectorFailureColor
    ConnectorType.ALWAYS -> connectorAlwaysColor
}

fun ConnectorType.label(): String = when (this) {
    ConnectorType.SUCCESS -> "On success"
    ConnectorType.FAILURE -> "On failure"
    ConnectorType.ALWAYS -> "Always"
}

@Composable
fun ConnectorSegment(type: ConnectorType) {
    val color = type.color()
    val label = type.label()

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

data class OrderedNode(
    val node: WorkflowNode,
    val incomingEdge: ConnectorType?
)

data class OrderedTemplateNode(
    val node: WorkflowJobTemplateNode,
    val incomingEdge: ConnectorType?
)

fun buildOrderedNodes(nodes: List<WorkflowNode>): List<OrderedNode> {
    if (nodes.isEmpty()) return emptyList()
    val graphNodes = nodes.map { GraphNode(it.id, it.successNodes, it.failureNodes, it.alwaysNodes) }
    val (_, incomingEdge) = buildEdges(graphNodes)
    val order = topoSort(graphNodes)
    val byId = nodes.associateBy { it.id }
    return order.mapNotNull { id -> byId[id]?.let { OrderedNode(it, incomingEdge[id]) } }
}

fun buildOrderedTemplateNodes(nodes: List<WorkflowJobTemplateNode>): List<OrderedTemplateNode> {
    if (nodes.isEmpty()) return emptyList()
    val graphNodes = nodes.map { GraphNode(it.id, it.successNodes, it.failureNodes, it.alwaysNodes) }
    val (_, incomingEdge) = buildEdges(graphNodes)
    val order = topoSort(graphNodes)
    val byId = nodes.associateBy { it.id }
    return order.mapNotNull { id -> byId[id]?.let { OrderedTemplateNode(it, incomingEdge[id]) } }
}

private data class GraphNode(
    val id: Int,
    val successNodes: List<Int>,
    val failureNodes: List<Int>,
    val alwaysNodes: List<Int>
)

private val EDGE_PRIORITY = mapOf(
    ConnectorType.SUCCESS to 0,
    ConnectorType.ALWAYS to 1,
    ConnectorType.FAILURE to 2
)

private fun buildEdges(items: List<GraphNode>): Pair<Set<Int>, Map<Int, ConnectorType>> {
    val childIds = mutableSetOf<Int>()
    val incomingEdge = mutableMapOf<Int, ConnectorType>()

    fun setEdge(childId: Int, type: ConnectorType) {
        childIds += childId
        val existing = incomingEdge[childId]
        if (existing == null || EDGE_PRIORITY.getValue(type) > EDGE_PRIORITY.getValue(existing)) {
            incomingEdge[childId] = type
        }
    }

    for (item in items) {
        for (childId in item.successNodes) setEdge(childId, ConnectorType.SUCCESS)
        for (childId in item.alwaysNodes) setEdge(childId, ConnectorType.ALWAYS)
        for (childId in item.failureNodes) setEdge(childId, ConnectorType.FAILURE)
    }
    return childIds to incomingEdge
}

// AAP prevents cyclic workflows; cycles here produce undefined ordering but no crash.
private fun topoSort(items: List<GraphNode>): List<Int> {
    val byId = items.associateBy { it.id }
    val (childIds, _) = buildEdges(items)
    val roots = items.filter { it.id !in childIds }
    val visited = mutableSetOf<Int>()
    val order = mutableListOf<Int>()

    fun visit(id: Int) {
        if (id in visited) return
        visited += id
        val item = byId[id] ?: return
        order += id
        for (childId in item.successNodes) visit(childId)
        for (childId in item.failureNodes) visit(childId)
        for (childId in item.alwaysNodes) visit(childId)
    }

    for (root in roots) visit(root.id)
    for (item in items) if (item.id !in visited) { order += item.id; visited += item.id }
    return order
}

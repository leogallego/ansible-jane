package io.github.leogallego.ansiblejane.ui.workflows

import io.github.leogallego.ansiblejane.model.WorkflowJobTemplateNode
import io.github.leogallego.ansiblejane.model.WorkflowNode

enum class ConnectorType { SUCCESS, FAILURE, ALWAYS }

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

private fun buildEdges(items: List<GraphNode>): Pair<Set<Int>, Map<Int, ConnectorType>> {
    val childIds = mutableSetOf<Int>()
    val incomingEdge = mutableMapOf<Int, ConnectorType>()
    for (item in items) {
        for (childId in item.successNodes) { childIds += childId; incomingEdge[childId] = ConnectorType.SUCCESS }
        for (childId in item.failureNodes) { childIds += childId; incomingEdge[childId] = ConnectorType.FAILURE }
        for (childId in item.alwaysNodes) { childIds += childId; incomingEdge[childId] = ConnectorType.ALWAYS }
    }
    return childIds to incomingEdge
}

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

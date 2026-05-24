package io.github.leogallego.ansiblejane.ui.workflows

import io.github.leogallego.ansiblejane.model.WorkflowNode

enum class ConnectorType { SUCCESS, FAILURE, ALWAYS }

data class OrderedNode(
    val node: WorkflowNode,
    val incomingEdge: ConnectorType?
)

fun buildOrderedNodes(nodes: List<WorkflowNode>): List<OrderedNode> {
    if (nodes.isEmpty()) return emptyList()

    val byId = nodes.associateBy { it.id }
    val childIds = mutableSetOf<Int>()
    val incomingEdge = mutableMapOf<Int, ConnectorType>()

    for (node in nodes) {
        for (childId in node.successNodes) {
            childIds += childId
            incomingEdge[childId] = ConnectorType.SUCCESS
        }
        for (childId in node.failureNodes) {
            childIds += childId
            incomingEdge[childId] = ConnectorType.FAILURE
        }
        for (childId in node.alwaysNodes) {
            childIds += childId
            incomingEdge[childId] = ConnectorType.ALWAYS
        }
    }

    val roots = nodes.filter { it.id !in childIds }
    val visited = mutableSetOf<Int>()
    val result = mutableListOf<OrderedNode>()

    fun visit(id: Int) {
        if (id in visited) return
        visited += id
        val node = byId[id] ?: return
        result += OrderedNode(node, incomingEdge[id])

        for (childId in node.successNodes) visit(childId)
        for (childId in node.failureNodes) visit(childId)
        for (childId in node.alwaysNodes) visit(childId)
    }

    for (root in roots) visit(root.id)

    for (node in nodes) {
        if (node.id !in visited) {
            result += OrderedNode(node, incomingEdge[node.id])
            visited += node.id
        }
    }

    return result
}

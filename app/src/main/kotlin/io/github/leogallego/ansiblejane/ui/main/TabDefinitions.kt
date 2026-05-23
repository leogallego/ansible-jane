package io.github.leogallego.ansiblejane.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Assistant
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.ui.graphics.vector.ImageVector

data class Segment(
    val label: String,
    val isDefault: Boolean = false,
    val isImplemented: Boolean = false
)

sealed class TopLevelTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val segments: List<Segment>
) {
    data object Templates : TopLevelTab(
        route = "main/templates",
        label = "Templates",
        icon = Icons.Outlined.Description,
        selectedIcon = Icons.Filled.Description,
        segments = listOf(
            Segment(label = "Job Templates", isDefault = true, isImplemented = true),
            Segment(label = "Workflows", isImplemented = true)
        )
    )

    data object Infrastructure : TopLevelTab(
        route = "main/infrastructure",
        label = "Infrastructure",
        icon = Icons.Outlined.Dns,
        selectedIcon = Icons.Filled.Dns,
        segments = listOf(
            Segment(label = "Inventories", isDefault = true, isImplemented = true),
            Segment(label = "Hosts", isImplemented = true)
        )
    )

    data object Activity : TopLevelTab(
        route = "main/activity",
        label = "Activity",
        icon = Icons.Outlined.History,
        selectedIcon = Icons.Filled.History,
        segments = listOf(
            Segment(label = "Jobs", isDefault = true, isImplemented = true),
            Segment(label = "Schedules", isImplemented = true),
            Segment(label = "EDA Audit", isImplemented = true)
        )
    )

    data object Assistant : TopLevelTab(
        route = "main/assistant",
        label = "Jane",
        icon = Icons.Outlined.Assistant,
        selectedIcon = Icons.Filled.Assistant,
        segments = listOf(
            Segment(label = "Chat", isDefault = true, isImplemented = true)
        )
    )

    companion object {
        val entries: List<TopLevelTab> = listOf(Templates, Infrastructure, Activity, Assistant)
    }
}

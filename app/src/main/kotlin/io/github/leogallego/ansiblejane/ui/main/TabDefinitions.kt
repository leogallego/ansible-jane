package io.github.leogallego.ansiblejane.ui.main

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Assistant
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.History
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.leogallego.ansiblejane.R

data class Segment(
    val label: String,
    @param:StringRes val labelResId: Int,
    val isDefault: Boolean = false,
    val isImplemented: Boolean = false
)

sealed class TopLevelTab(
    val route: String,
    val label: String,
    @param:StringRes val labelResId: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    val segments: List<Segment>
) {
    data object Dashboard : TopLevelTab(
        route = "main/dashboard",
        label = "Dashboard",
        labelResId = R.string.tab_dashboard,
        icon = Icons.Outlined.Dashboard,
        selectedIcon = Icons.Filled.Dashboard,
        segments = listOf(
            Segment(label = "Overview", labelResId = R.string.segment_overview, isDefault = true, isImplemented = true)
        )
    )

    data object Templates : TopLevelTab(
        route = "main/templates",
        label = "Templates",
        labelResId = R.string.tab_templates,
        icon = Icons.Outlined.Description,
        selectedIcon = Icons.Filled.Description,
        segments = listOf(
            Segment(label = "Job Templates", labelResId = R.string.segment_job_templates, isDefault = true, isImplemented = true),
            Segment(label = "Workflows", labelResId = R.string.segment_workflows, isImplemented = true)
        )
    )

    data object Infrastructure : TopLevelTab(
        route = "main/infrastructure",
        label = "Infrastructure",
        labelResId = R.string.tab_infrastructure,
        icon = Icons.Outlined.Dns,
        selectedIcon = Icons.Filled.Dns,
        segments = listOf(
            Segment(label = "Inventories", labelResId = R.string.segment_inventories, isDefault = true, isImplemented = true),
            Segment(label = "Hosts", labelResId = R.string.segment_hosts, isImplemented = true)
        )
    )

    data object Activity : TopLevelTab(
        route = "main/activity",
        label = "Activity",
        labelResId = R.string.tab_activity,
        icon = Icons.Outlined.History,
        selectedIcon = Icons.Filled.History,
        segments = listOf(
            Segment(label = "Jobs", labelResId = R.string.segment_jobs, isDefault = true, isImplemented = true),
            Segment(label = "Schedules", labelResId = R.string.segment_schedules, isImplemented = true),
            Segment(label = "EDA", labelResId = R.string.segment_eda, isImplemented = true)
        )
    )

    data object Assistant : TopLevelTab(
        route = "main/assistant",
        label = "Jane",
        labelResId = R.string.tab_assistant,
        icon = Icons.Outlined.Assistant,
        selectedIcon = Icons.Filled.Assistant,
        segments = listOf(
            Segment(label = "Chat", labelResId = R.string.segment_chat, isDefault = true, isImplemented = true)
        )
    )

    companion object {
        val entries: List<TopLevelTab> = listOf(Dashboard, Templates, Infrastructure, Activity, Assistant)
    }
}

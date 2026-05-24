package io.github.leogallego.ansiblejane.navigation

import androidx.compose.runtime.Composable
import io.github.leogallego.ansiblejane.assistant.ui.AssistantScreen
import io.github.leogallego.ansiblejane.ui.components.PlaceholderScreen
import io.github.leogallego.ansiblejane.ui.hosts.HostsScreen
import io.github.leogallego.ansiblejane.ui.inventory.InventoriesScreen
import io.github.leogallego.ansiblejane.ui.jobs.RecentJobsScreen
import io.github.leogallego.ansiblejane.ui.main.Segment
import io.github.leogallego.ansiblejane.ui.main.TopLevelTab
import io.github.leogallego.ansiblejane.ui.eda.EdaAuditScreen
import io.github.leogallego.ansiblejane.ui.schedules.SchedulesScreen
import io.github.leogallego.ansiblejane.ui.templates.TemplateListScreen
import io.github.leogallego.ansiblejane.ui.workflows.WorkflowTemplateListScreen

@Composable
fun TabContent(
    tab: TopLevelTab,
    segment: Segment,
    onNavigateToJobStatus: (Int) -> Unit,
    onNavigateToWorkflowJobStatus: (Int) -> Unit = {}
) {
    if (!segment.isImplemented) {
        PlaceholderScreen(title = segment.label)
        return
    }

    when (tab) {
        is TopLevelTab.Templates -> {
            when (segment.label) {
                "Job Templates" -> TemplateListScreen(
                    onNavigateToJobStatus = onNavigateToJobStatus
                )
                "Workflows" -> WorkflowTemplateListScreen(
                    onNavigateToWorkflowJobStatus = onNavigateToWorkflowJobStatus
                )
                else -> PlaceholderScreen(title = segment.label)
            }
        }
        is TopLevelTab.Activity -> {
            when (segment.label) {
                "Jobs" -> RecentJobsScreen(
                    onNavigateToJobStatus = onNavigateToJobStatus
                )
                "Schedules" -> SchedulesScreen()
                "EDA" -> EdaAuditScreen()
                else -> PlaceholderScreen(title = segment.label)
            }
        }
        is TopLevelTab.Infrastructure -> {
            when (segment.label) {
                "Inventories" -> InventoriesScreen()
                "Hosts" -> HostsScreen()
                else -> PlaceholderScreen(title = segment.label)
            }
        }
        is TopLevelTab.Assistant -> {
            AssistantScreen()
        }
    }
}

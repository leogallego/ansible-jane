package io.github.leogallego.ansiblejane.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.leogallego.ansiblejane.assistant.ui.AssistantScreen
import io.github.leogallego.ansiblejane.ui.components.PlaceholderScreen
import io.github.leogallego.ansiblejane.ui.dashboard.DashboardScreen
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
    onNavigateToWorkflowJobStatus: (Int) -> Unit = {},
    onNavigateToWorkflowTemplateDetail: (Int, String) -> Unit = { _, _ -> }
) {
    if (!segment.isImplemented) {
        PlaceholderScreen(title = stringResource(segment.labelResId))
        return
    }

    when (tab) {
        is TopLevelTab.Dashboard -> {
            DashboardScreen(onNavigateToJobStatus = onNavigateToJobStatus)
        }
        is TopLevelTab.Templates -> {
            when (segment.label) {
                "Job Templates" -> TemplateListScreen(
                    onNavigateToJobStatus = onNavigateToJobStatus
                )
                "Workflows" -> WorkflowTemplateListScreen(
                    onNavigateToWorkflowJobStatus = onNavigateToWorkflowJobStatus,
                    onNavigateToTemplateDetail = onNavigateToWorkflowTemplateDetail
                )
                else -> PlaceholderScreen(title = stringResource(segment.labelResId))
            }
        }
        is TopLevelTab.Activity -> {
            when (segment.label) {
                "Jobs" -> RecentJobsScreen(
                    onNavigateToJobStatus = onNavigateToJobStatus
                )
                "Schedules" -> SchedulesScreen()
                "EDA" -> EdaAuditScreen()
                else -> PlaceholderScreen(title = stringResource(segment.labelResId))
            }
        }
        is TopLevelTab.Infrastructure -> {
            when (segment.label) {
                "Inventories" -> InventoriesScreen()
                "Hosts" -> HostsScreen()
                else -> PlaceholderScreen(title = stringResource(segment.labelResId))
            }
        }
        is TopLevelTab.Assistant -> {
            AssistantScreen()
        }
    }
}

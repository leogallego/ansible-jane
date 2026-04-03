package com.example.aapremote.navigation

import androidx.compose.runtime.Composable
import com.example.aapremote.ui.components.PlaceholderScreen
import com.example.aapremote.ui.jobs.RecentJobsScreen
import com.example.aapremote.ui.main.Segment
import com.example.aapremote.ui.main.TopLevelTab
import com.example.aapremote.ui.eda.EdaAuditScreen
import com.example.aapremote.ui.schedules.SchedulesScreen
import com.example.aapremote.ui.templates.TemplateListScreen
import com.example.aapremote.ui.workflows.WorkflowTemplateListScreen

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
                "Workflow Templates" -> WorkflowTemplateListScreen(
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
                "EDA Audit" -> EdaAuditScreen()
                else -> PlaceholderScreen(title = segment.label)
            }
        }
        is TopLevelTab.Infrastructure -> {
            PlaceholderScreen(title = segment.label)
        }
    }
}

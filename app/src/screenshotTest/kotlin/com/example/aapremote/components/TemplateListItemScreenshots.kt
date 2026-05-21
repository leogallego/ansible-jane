package com.example.aapremote.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.example.aapremote.model.JobTemplate
import com.example.aapremote.model.JobTemplateSummaryFields
import com.example.aapremote.model.Label
import com.example.aapremote.model.LabelSummary
import com.example.aapremote.model.UserCapabilities
import com.example.aapremote.ui.templates.TemplateListItem
import com.example.aapremote.ui.theme.AapRemoteTheme

private val templateWithLabels = JobTemplate(
    id = 1,
    name = "Deploy Production App",
    description = "Deploys the application to production servers using rolling update strategy",
    summaryFields = JobTemplateSummaryFields(
        labels = LabelSummary(
            count = 2,
            results = listOf(
                Label(id = 1, name = "production"),
                Label(id = 2, name = "critical")
            )
        ),
        userCapabilities = UserCapabilities(start = true)
    )
)

private val templateNoLabels = JobTemplate(
    id = 2,
    name = "Run Smoke Tests",
    description = "Executes smoke test suite against staging environment",
    summaryFields = JobTemplateSummaryFields(
        userCapabilities = UserCapabilities(start = true)
    )
)

private val templateNoLaunch = JobTemplate(
    id = 3,
    name = "Audit Compliance Report",
    description = "Generates compliance audit report for all managed hosts"
)

@PreviewTest
@Preview(showBackground = true, name = "Template With Labels - Light")
@Composable
fun TemplateListItemWithLabelsLight() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            TemplateListItem(template = templateWithLabels, onLaunch = {})
        }
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Template With Labels - Dark")
@Composable
fun TemplateListItemWithLabelsDark() {
    AapRemoteTheme(darkTheme = true, dynamicColor = false) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            TemplateListItem(template = templateWithLabels, onLaunch = {})
        }
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Template Variations")
@Composable
fun TemplateListItemVariations() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TemplateListItem(template = templateWithLabels, onLaunch = {})
            TemplateListItem(template = templateNoLabels, onLaunch = {})
            TemplateListItem(template = templateNoLaunch, onLaunch = {})
        }
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "Template - Large Font")
@Composable
fun TemplateListItemLargeFont() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            TemplateListItem(template = templateWithLabels, onLaunch = {})
        }
    }
}

package io.github.leogallego.ansiblejane.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.model.JobTemplate
import io.github.leogallego.ansiblejane.model.JobTemplateSummaryFields
import io.github.leogallego.ansiblejane.model.Label
import io.github.leogallego.ansiblejane.model.LabelSummary
import io.github.leogallego.ansiblejane.model.UserCapabilities
import io.github.leogallego.ansiblejane.ui.components.TemplateCard
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

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
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            TemplateCard(template = templateWithLabels, onClick = {}, onLaunch = {})
        }
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Template With Labels - Dark")
@Composable
fun TemplateListItemWithLabelsDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            TemplateCard(template = templateWithLabels, onClick = {}, onLaunch = {})
        }
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Template Variations")
@Composable
fun TemplateListItemVariations() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TemplateCard(template = templateWithLabels, onClick = {}, onLaunch = {})
            TemplateCard(template = templateNoLabels, onClick = {}, onLaunch = {})
            TemplateCard(template = templateNoLaunch, onClick = {}, onLaunch = {})
        }
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "Template - Large Font")
@Composable
fun TemplateListItemLargeFont() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            TemplateCard(template = templateWithLabels, onClick = {}, onLaunch = {})
        }
    }
}

package io.github.leogallego.ansiblejane.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.JobTemplate
import io.github.leogallego.ansiblejane.model.JobTemplateSummaryFields
import io.github.leogallego.ansiblejane.model.Label
import io.github.leogallego.ansiblejane.model.LabelSummary
import io.github.leogallego.ansiblejane.model.UserCapabilities
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.SearchBar
import io.github.leogallego.ansiblejane.ui.components.SkeletonCard
import io.github.leogallego.ansiblejane.ui.components.TemplateCard
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

private val sampleTemplates = listOf(
    JobTemplate(
        id = 1,
        name = "Deploy Production App",
        description = "Deploys the application to production servers",
        summaryFields = JobTemplateSummaryFields(
            labels = LabelSummary(
                count = 2,
                results = listOf(Label(1, "production"), Label(2, "critical"))
            ),
            userCapabilities = UserCapabilities(start = true)
        )
    ),
    JobTemplate(
        id = 2,
        name = "Run Smoke Tests",
        description = "Executes smoke test suite against staging",
        summaryFields = JobTemplateSummaryFields(
            labels = LabelSummary(
                count = 1,
                results = listOf(Label(3, "staging"))
            ),
            userCapabilities = UserCapabilities(start = true)
        )
    ),
    JobTemplate(
        id = 3,
        name = "Backup Database",
        description = "Creates database backup and uploads to S3",
        summaryFields = JobTemplateSummaryFields(
            userCapabilities = UserCapabilities(start = true)
        )
    ),
    JobTemplate(
        id = 4,
        name = "Security Audit",
        description = "Runs security compliance checks across all hosts"
    )
)

@PreviewTest
@Preview(
    showBackground = true, name = "Templates List - Light",
    widthDp = 400, heightDp = 900
)
@Composable
fun TemplateListLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(onSearch = {})
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleTemplates, key = { it.id }) { template ->
                    TemplateCard(template = template, onClick = {}, onLaunch = {})
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Templates List - Dark",
    widthDp = 400, heightDp = 900
)
@Composable
fun TemplateListDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(onSearch = {})
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleTemplates, key = { it.id }) { template ->
                    TemplateCard(template = template, onClick = {}, onLaunch = {})
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Templates Loading",
    widthDp = 400, heightDp = 900
)
@Composable
fun TemplateListLoading() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(onSearch = {})
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(5) { SkeletonCard() }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Templates Empty",
    widthDp = 400, heightDp = 500
)
@Composable
fun TemplateListEmpty() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(onSearch = {})
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No templates available",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Templates Error",
    widthDp = 400, heightDp = 500
)
@Composable
fun TemplateListError() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(onSearch = {})
            ErrorMessage(
                error = AppError.Network(),
                onRetry = {},
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Templates - Medium Width",
    widthDp = 610, heightDp = 900
)
@Composable
fun TemplateListMediumWidth() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(onSearch = {})
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleTemplates, key = { it.id }) { template ->
                    TemplateCard(template = template, onClick = {}, onLaunch = {})
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Templates - Expanded Width",
    widthDp = 900, heightDp = 900
)
@Composable
fun TemplateListExpandedWidth() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(onSearch = {})
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleTemplates, key = { it.id }) { template ->
                    TemplateCard(template = template, onClick = {}, onLaunch = {})
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, fontScale = 1.5f, name = "Templates - Large Font",
    widthDp = 400, heightDp = 900
)
@Composable
fun TemplateListLargeFont() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(onSearch = {})
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleTemplates.take(2), key = { it.id }) { template ->
                    TemplateCard(template = template, onClick = {}, onLaunch = {})
                }
            }
        }
    }
}

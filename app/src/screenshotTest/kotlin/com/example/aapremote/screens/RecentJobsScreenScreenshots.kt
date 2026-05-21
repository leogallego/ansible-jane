package com.example.aapremote.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.example.aapremote.model.AppError
import com.example.aapremote.model.Job
import com.example.aapremote.model.JobStatus
import com.example.aapremote.model.JobSummaryFields
import com.example.aapremote.model.JobTemplateRef
import com.example.aapremote.ui.components.ErrorMessage
import com.example.aapremote.ui.components.JobStatusBadge
import com.example.aapremote.ui.components.SkeletonCard
import com.example.aapremote.ui.components.StatusFilterChips
import com.example.aapremote.ui.theme.AapRemoteTheme

private val sampleJobs = listOf(
    Job(
        id = 1, name = "Deploy Web App #142",
        status = JobStatus.SUCCESSFUL,
        started = "2024-01-15T10:30:00Z",
        summaryFields = JobSummaryFields(jobTemplate = JobTemplateRef(1, "Deploy Web App"))
    ),
    Job(
        id = 2, name = "Run Tests #89",
        status = JobStatus.FAILED,
        started = "2024-01-15T09:15:00Z",
        summaryFields = JobSummaryFields(jobTemplate = JobTemplateRef(2, "Run Tests"))
    ),
    Job(
        id = 3, name = "Backup DB #56",
        status = JobStatus.RUNNING,
        started = "2024-01-15T11:00:00Z",
        summaryFields = JobSummaryFields(jobTemplate = JobTemplateRef(3, "Backup Database"))
    ),
    Job(
        id = 4, name = "Security Scan #33",
        status = JobStatus.PENDING,
        summaryFields = JobSummaryFields(jobTemplate = JobTemplateRef(4, "Security Audit"))
    )
)

@Composable
private fun JobItem(job: Job) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = job.jobTemplateName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                job.started?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            JobStatusBadge(status = job.status)
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Jobs List - Light",
    widthDp = 400, heightDp = 900
)
@Composable
fun RecentJobsListLight() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatusFilterChips(activeFilters = emptySet(), onToggleFilter = {})
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleJobs, key = { it.id }) { job ->
                    JobItem(job)
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Jobs List - Dark",
    widthDp = 400, heightDp = 900
)
@Composable
fun RecentJobsListDark() {
    AapRemoteTheme(darkTheme = true, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatusFilterChips(activeFilters = emptySet(), onToggleFilter = {})
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleJobs, key = { it.id }) { job ->
                    JobItem(job)
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Jobs Loading",
    widthDp = 400, heightDp = 500
)
@Composable
fun RecentJobsLoading() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(5) { SkeletonCard() }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Jobs Empty",
    widthDp = 400, heightDp = 500
)
@Composable
fun RecentJobsEmpty() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatusFilterChips(activeFilters = emptySet(), onToggleFilter = {})
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent jobs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Jobs Error",
    widthDp = 400, heightDp = 500
)
@Composable
fun RecentJobsError() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        ErrorMessage(
            error = AppError.Network(),
            onRetry = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Jobs With Filters",
    widthDp = 400, heightDp = 900
)
@Composable
fun RecentJobsWithFilters() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatusFilterChips(
                activeFilters = setOf(JobStatus.RUNNING, JobStatus.FAILED),
                onToggleFilter = {}
            )
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(
                    sampleJobs.filter { it.status in setOf(JobStatus.RUNNING, JobStatus.FAILED) },
                    key = { it.id }
                ) { job ->
                    JobItem(job)
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, fontScale = 1.5f, name = "Jobs - Large Font",
    widthDp = 400, heightDp = 900
)
@Composable
fun RecentJobsLargeFont() {
    AapRemoteTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatusFilterChips(activeFilters = emptySet(), onToggleFilter = {})
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleJobs.take(2), key = { it.id }) { job ->
                    JobItem(job)
                }
            }
        }
    }
}

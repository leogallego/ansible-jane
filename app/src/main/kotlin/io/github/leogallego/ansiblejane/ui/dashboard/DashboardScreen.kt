package io.github.leogallego.ansiblejane.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leogallego.ansiblejane.model.InstanceInfo
import io.github.leogallego.ansiblejane.model.Job
import io.github.leogallego.ansiblejane.model.Schedule
import io.github.leogallego.ansiblejane.presentation.dashboard.DashboardUiState
import io.github.leogallego.ansiblejane.presentation.dashboard.DashboardViewModel
import io.github.leogallego.ansiblejane.presentation.dashboard.DayJobStats
import io.github.leogallego.ansiblejane.presentation.dashboard.HealthStatus
import io.github.leogallego.ansiblejane.ui.components.DateFormatter
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.JobStatusBadge
import io.github.leogallego.ansiblejane.ui.components.SkeletonCard
import io.github.leogallego.ansiblejane.ui.components.pressScale
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToJobStatus: (Int) -> Unit,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState !is DashboardUiState.Loading) {
            isRefreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(4) { SkeletonCard() }
                }
            }
            is DashboardUiState.Error -> {
                ErrorMessage(
                    error = state.error,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is DashboardUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("list_dashboard"),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item(key = "jobs_header") {
                            SectionHeader("Jobs")
                        }

                        item(key = "stats") {
                            StatsRow(
                                activeCount = state.activeJobsCount,
                                failedCount = state.failedCount24h,
                                successfulCount = state.successfulCount24h,
                                healthStatus = state.healthStatus,
                            )
                        }

                        item(key = "failures_header") {
                            SectionHeader("Recent Failures")
                        }

                        if (state.recentFailures.isEmpty()) {
                            item(key = "no_failures") {
                                AllClearCard()
                            }
                        } else {
                            items(
                                items = state.recentFailures,
                                key = { "failure_${it.id}" }
                            ) { job ->
                                FailureItem(
                                    job = job,
                                    onClick = { onNavigateToJobStatus(job.id) },
                                )
                            }
                        }

                        if (state.edaActivationsCount != null) {
                            item(key = "eda_header") {
                                SectionHeader("Event-Driven Ansible")
                            }

                            item(key = "eda_stats") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    StatCard(
                                        count = state.edaActiveRulebooksCount ?: 0,
                                        label = "Running",
                                        color = AnsibleJaneTheme.statusColors.successfulDim,
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatCard(
                                        count = state.edaActivationsCount,
                                        label = "Activations",
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        item(key = "resources_header") {
                            SectionHeader("Resources")
                        }

                        item(key = "resources") {
                            ResourcesGrid(
                                inventoryCount = state.inventoryCount,
                                hostCount = state.hostCount,
                                templateCount = state.templateCount,
                                projectCount = state.projectCount,
                            )
                        }

                        if (state.jobHistory7d.isNotEmpty()) {
                            item(key = "chart_header") {
                                SectionHeader("Job Activity (7 days)")
                            }

                            item(key = "chart") {
                                JobHistoryChart(days = state.jobHistory7d)
                            }
                        }

                        if (state.upcomingSchedules.isNotEmpty()) {
                            item(key = "schedules_header") {
                                SectionHeader("Upcoming Schedules")
                            }

                            items(
                                items = state.upcomingSchedules,
                                key = { "schedule_${it.id}" }
                            ) { schedule ->
                                ScheduleItem(schedule = schedule)
                            }
                        }

                        item(key = "instance_header") {
                            SectionHeader("Instance")
                        }

                        item(key = "instance_info") {
                            InstanceInfoCard(
                                instanceInfo = state.instanceInfo,
                                healthStatus = state.healthStatus,
                                instanceUrl = state.instanceUrl,
                                instanceAlias = state.instanceAlias,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@Composable
private fun StatsRow(
    activeCount: Int,
    failedCount: Int,
    successfulCount: Int,
    healthStatus: HealthStatus,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            count = activeCount,
            label = "Active",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            count = failedCount,
            label = "Failed 24h",
            color = when (healthStatus) {
                HealthStatus.GREEN -> MaterialTheme.colorScheme.outline
                HealthStatus.YELLOW -> AnsibleJaneTheme.statusColors.healthDegraded
                HealthStatus.RED -> MaterialTheme.colorScheme.error
            },
            modifier = Modifier.weight(1f),
        )
        StatCard(
            count = successfulCount,
            label = "Passed 24h",
            color = AnsibleJaneTheme.statusColors.successfulDim,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.12f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AllClearCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AnsibleJaneTheme.statusColors.successfulDim.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = AnsibleJaneTheme.statusColors.successfulDim,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "No recent failures — all clear!",
                style = MaterialTheme.typography.bodyLarge,
                color = AnsibleJaneTheme.statusColors.successfulDim,
            )
        }
    }
}

@Composable
private fun FailureItem(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.jobTemplateName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                job.finished?.let {
                    Text(
                        text = DateFormatter.formatRelative(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            JobStatusBadge(status = job.status)
        }
    }
}

@Composable
private fun ResourcesGrid(
    inventoryCount: Int,
    hostCount: Int,
    templateCount: Int,
    projectCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ResourceCard(count = inventoryCount, label = "Inventories", modifier = Modifier.weight(1f))
            ResourceCard(count = hostCount, label = "Hosts", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ResourceCard(count = templateCount, label = "Templates", modifier = Modifier.weight(1f))
            ResourceCard(count = projectCount, label = "Projects", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResourceCard(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun JobHistoryChart(
    days: List<DayJobStats>,
    modifier: Modifier = Modifier
) {
    val successColor = AnsibleJaneTheme.statusColors.successfulDim
    val failColor = MaterialTheme.colorScheme.error
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChartLegendDot(color = successColor)
                Spacer(Modifier.width(4.dp))
                Text("Passed", style = MaterialTheme.typography.labelSmall, color = labelColor)
                Spacer(Modifier.width(12.dp))
                ChartLegendDot(color = failColor)
                Spacer(Modifier.width(4.dp))
                Text("Failed", style = MaterialTheme.typography.labelSmall, color = labelColor)
            }

            Spacer(Modifier.height(8.dp))

            val maxCount = days.maxOf { it.successful + it.failed }.coerceAtLeast(1)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (days.isEmpty()) return@Canvas
                val barGroupWidth = size.width / days.size
                val barWidth = barGroupWidth * 0.5f
                val bottomPad = 16.dp.toPx()

                days.forEachIndexed { i, day ->
                    val x = i * barGroupWidth + (barGroupWidth - barWidth) / 2
                    val totalHeight = size.height - bottomPad

                    val successHeight = (day.successful.toFloat() / maxCount) * totalHeight
                    val failHeight = (day.failed.toFloat() / maxCount) * totalHeight

                    drawRect(
                        color = successColor,
                        topLeft = Offset(x, size.height - bottomPad - successHeight - failHeight),
                        size = Size(barWidth, successHeight),
                    )
                    if (failHeight > 0) {
                        drawRect(
                            color = failColor,
                            topLeft = Offset(x, size.height - bottomPad - failHeight),
                            size = Size(barWidth, failHeight),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                days.forEach { day ->
                    Text(
                        text = day.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(8.dp)) {
        drawCircle(color = color, radius = size.minDimension / 2)
    }
}

@Composable
private fun ScheduleItem(
    schedule: Schedule,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                schedule.nextRun?.let {
                    Text(
                        text = "Next: ${DateFormatter.formatRelative(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun InstanceInfoCard(
    instanceInfo: InstanceInfo?,
    healthStatus: HealthStatus,
    instanceUrl: String?,
    instanceAlias: String?,
    modifier: Modifier = Modifier
) {
    val statusColors = AnsibleJaneTheme.statusColors
    val healthColor = when (healthStatus) {
        HealthStatus.GREEN -> statusColors.successfulDim
        HealthStatus.YELLOW -> statusColors.healthDegraded
        HealthStatus.RED -> MaterialTheme.colorScheme.error
    }
    val healthLabel = when (healthStatus) {
        HealthStatus.GREEN -> "Healthy"
        HealthStatus.YELLOW -> "Degraded"
        HealthStatus.RED -> "Critical"
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (instanceAlias != null) {
                InfoRow(label = "Instance", value = instanceAlias)
            }
            if (instanceUrl != null) {
                Text(
                    text = instanceUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (instanceInfo != null) {
                val platformLabel = when (instanceInfo.platformType) {
                    "AAP" -> "Red Hat AAP" + (instanceInfo.aapVersion?.let { " $it" } ?: "")
                    "AWX" -> "AWX (upstream)"
                    "JEWEL" -> "Jewel (upstream)"
                    else -> "Unknown"
                }
                InfoRow(label = "Platform", value = platformLabel)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                if (instanceInfo.controllerVersion.isNotBlank()) {
                    InfoRow(label = "Controller", value = instanceInfo.controllerVersion)
                }
                if (instanceInfo.gatewayVersion.isNotBlank()) {
                    InfoRow(label = "Gateway", value = instanceInfo.gatewayVersion)
                }
                if (instanceInfo.edaVersion.isNotBlank()) {
                    InfoRow(label = "EDA", value = instanceInfo.edaVersion)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(10.dp)) {
                    drawCircle(color = healthColor, radius = size.minDimension / 2)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = healthLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = healthColor,
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

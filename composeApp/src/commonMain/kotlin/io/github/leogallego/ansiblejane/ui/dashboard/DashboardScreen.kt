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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import io.github.leogallego.ansiblejane.model.InstanceInfo
import io.github.leogallego.ansiblejane.model.Job
import io.github.leogallego.ansiblejane.model.Schedule
import io.github.leogallego.ansiblejane.presentation.dashboard.DashboardUiState
import io.github.leogallego.ansiblejane.presentation.dashboard.DashboardViewModel
import io.github.leogallego.ansiblejane.presentation.dashboard.DayJobStats
import io.github.leogallego.ansiblejane.presentation.dashboard.HealthStatus
import io.github.leogallego.ansiblejane.ui.components.DateFormatter
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.JobStatusBadge
import io.github.leogallego.ansiblejane.ui.components.SkeletonCard
import io.github.leogallego.ansiblejane.ui.components.pressScale
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToJobStatus: (Int) -> Unit,
    viewModel: DashboardViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState !is DashboardUiState.Loading) {
            isRefreshing = false
        }
    }

    DashboardContent(
        uiState = uiState,
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refresh() },
        onRetry = { viewModel.refresh() },
        onNavigateToJobStatus = onNavigateToJobStatus,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardContent(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onRetry: () -> Unit = {},
    onNavigateToJobStatus: (Int) -> Unit = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(4) { SkeletonCard() }
                }
            }
            is DashboardUiState.Error -> {
                ErrorMessage(
                    error = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is DashboardUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
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
                            SectionHeader(stringResource(Res.string.dashboard_section_jobs))
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
                            SectionHeader(stringResource(Res.string.dashboard_section_recent_failures))
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
                                SectionHeader(stringResource(Res.string.dashboard_section_eda))
                            }

                            item(key = "eda_stats") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    StatCard(
                                        count = state.edaActiveRulebooksCount ?: 0,
                                        label = stringResource(Res.string.dashboard_stat_running),
                                        color = AnsibleJaneTheme.statusColors.successfulDim,
                                        modifier = Modifier.weight(1f),
                                    )
                                    StatCard(
                                        count = state.edaActivationsCount,
                                        label = stringResource(Res.string.dashboard_stat_activations),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        item(key = "resources_header") {
                            SectionHeader(stringResource(Res.string.dashboard_section_resources))
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
                                SectionHeader(stringResource(Res.string.dashboard_section_chart))
                            }

                            item(key = "chart") {
                                JobHistoryChart(days = state.jobHistory7d)
                            }
                        }

                        if (state.upcomingSchedules.isNotEmpty()) {
                            item(key = "schedules_header") {
                                SectionHeader(stringResource(Res.string.dashboard_section_schedules))
                            }

                            items(
                                items = state.upcomingSchedules,
                                key = { "schedule_${it.id}" }
                            ) { schedule ->
                                ScheduleItem(schedule = schedule)
                            }
                        }

                        item(key = "instance_header") {
                            SectionHeader(stringResource(Res.string.dashboard_section_instance))
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
        modifier = modifier.semantics { heading() },
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
            label = stringResource(Res.string.dashboard_stat_active),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            count = failedCount,
            label = stringResource(Res.string.dashboard_stat_failed_24h),
            color = when (healthStatus) {
                HealthStatus.GREEN -> MaterialTheme.colorScheme.outline
                HealthStatus.YELLOW -> AnsibleJaneTheme.statusColors.healthDegraded
                HealthStatus.RED -> MaterialTheme.colorScheme.error
            },
            modifier = Modifier.weight(1f),
        )
        StatCard(
            count = successfulCount,
            label = stringResource(Res.string.dashboard_stat_passed_24h),
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
                .padding(24.dp)
                .semantics(mergeDescendants = true) { },
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
                text = stringResource(Res.string.dashboard_all_clear),
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
            ResourceCard(count = inventoryCount, label = stringResource(Res.string.dashboard_resource_inventories), modifier = Modifier.weight(1f))
            ResourceCard(count = hostCount, label = stringResource(Res.string.dashboard_resource_hosts), modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ResourceCard(count = templateCount, label = stringResource(Res.string.dashboard_resource_templates), modifier = Modifier.weight(1f))
            ResourceCard(count = projectCount, label = stringResource(Res.string.dashboard_resource_projects), modifier = Modifier.weight(1f))
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

    val totalPassed = remember(days) { days.sumOf { it.successful } }
    val totalFailed = remember(days) { days.sumOf { it.failed } }
    val chartDescription = stringResource(Res.string.dashboard_chart_cd, totalPassed, totalFailed, days.size)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ChartLegendDot(color = successColor)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.dashboard_chart_passed), style = MaterialTheme.typography.labelSmall, color = labelColor)
                Spacer(Modifier.width(12.dp))
                ChartLegendDot(color = failColor)
                Spacer(Modifier.width(4.dp))
                Text(stringResource(Res.string.dashboard_chart_failed), style = MaterialTheme.typography.labelSmall, color = labelColor)
            }

            Spacer(Modifier.height(8.dp))

            val maxCount = days.maxOf { it.successful + it.failed }.coerceAtLeast(1)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .semantics { contentDescription = chartDescription }
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
                .padding(12.dp)
                .semantics(mergeDescendants = true) { },
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
                        text = stringResource(Res.string.dashboard_schedule_next, DateFormatter.formatRelative(it)),
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
        HealthStatus.GREEN -> stringResource(Res.string.dashboard_health_healthy)
        HealthStatus.YELLOW -> stringResource(Res.string.dashboard_health_degraded)
        HealthStatus.RED -> stringResource(Res.string.dashboard_health_critical)
    }
    val healthCd = stringResource(Res.string.dashboard_health_cd, healthLabel)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (instanceAlias != null) {
                InfoRow(label = stringResource(Res.string.label_instance), value = instanceAlias)
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
                    "AAP" -> instanceInfo.aapVersion?.let {
                        stringResource(Res.string.dashboard_platform_aap_version, it)
                    } ?: stringResource(Res.string.dashboard_platform_aap)
                    "AWX" -> stringResource(Res.string.dashboard_platform_awx)
                    "JEWEL" -> stringResource(Res.string.dashboard_platform_jewel)
                    else -> stringResource(Res.string.dashboard_platform_unknown)
                }
                InfoRow(label = stringResource(Res.string.label_platform), value = platformLabel)
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                if (instanceInfo.controllerVersion.isNotBlank()) {
                    InfoRow(label = stringResource(Res.string.label_controller), value = instanceInfo.controllerVersion)
                }
                if (instanceInfo.gatewayVersion.isNotBlank()) {
                    InfoRow(label = stringResource(Res.string.label_gateway), value = instanceInfo.gatewayVersion)
                }
                if (instanceInfo.edaVersion.isNotBlank()) {
                    InfoRow(label = stringResource(Res.string.label_eda), value = instanceInfo.edaVersion)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = healthCd
                },
            ) {
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

@PreviewLightDark
@Composable
private fun DashboardLoadingPreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        DashboardContent(uiState = DashboardUiState.Loading)
    }
}

@PreviewLightDark
@Composable
private fun DashboardErrorPreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        DashboardContent(uiState = DashboardUiState.Error(AppError.Network()))
    }
}

@PreviewLightDark
@Composable
private fun DashboardContentPreview() {
    AnsibleJaneTheme(dynamicColor = false) {
        DashboardContent(
            uiState = DashboardUiState.Success(
                activeJobsCount = 3,
                failedCount24h = 1,
                successfulCount24h = 12,
                recentFailures = emptyList(),
                healthStatus = HealthStatus.GREEN,
                inventoryCount = 5,
                hostCount = 42,
                templateCount = 18,
                projectCount = 7,
                jobHistory7d = listOf(
                    DayJobStats("Mon", 8, 1),
                    DayJobStats("Tue", 12, 0),
                    DayJobStats("Wed", 6, 2),
                    DayJobStats("Thu", 10, 1),
                    DayJobStats("Fri", 14, 0),
                    DayJobStats("Sat", 3, 0),
                    DayJobStats("Sun", 2, 0),
                ),
                instanceInfo = InstanceInfo(
                    controllerVersion = "4.6.0",
                    gatewayVersion = "2.6.0",
                    edaVersion = "1.1.0",
                    platformType = "AAP",
                    aapVersion = "2.6",
                ),
                instanceUrl = "https://aap.example.com",
                instanceAlias = "Production AAP",
            )
        )
    }
}

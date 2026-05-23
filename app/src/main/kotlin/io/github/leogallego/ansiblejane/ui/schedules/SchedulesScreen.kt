package io.github.leogallego.ansiblejane.ui.schedules

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leogallego.ansiblejane.model.Schedule
import io.github.leogallego.ansiblejane.ui.components.DateFormatter
import io.github.leogallego.ansiblejane.presentation.schedules.SchedulesUiState
import io.github.leogallego.ansiblejane.presentation.schedules.SchedulesViewModel
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.SkeletonCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(
    viewModel: SchedulesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is SchedulesUiState.Success || uiState is SchedulesUiState.Error) {
            isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toggleError.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is SchedulesUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(5) { SkeletonCard() }
                }
            }
            is SchedulesUiState.Error -> {
                ErrorMessage(
                    error = state.error,
                    onRetry = { viewModel.loadSchedules() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is SchedulesUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.schedules.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No schedules configured",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val listState = rememberLazyListState()

                        val shouldLoadMore by remember {
                            derivedStateOf {
                                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                lastVisibleItem >= state.schedules.size - 3
                            }
                        }

                        LaunchedEffect(shouldLoadMore) {
                            snapshotFlow { shouldLoadMore }
                                .collect { if (it) viewModel.loadMore() }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().testTag("list_schedules")
                        ) {
                            items(
                                items = state.schedules,
                                key = { it.id }
                            ) { schedule ->
                                ScheduleItem(
                                    schedule = schedule,
                                    onToggle = { viewModel.toggleSchedule(schedule) }
                                )
                            }

                            if (state.isLoadingMore) {
                                item {
                                    LinearProgressIndicator(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ScheduleItem(
    schedule: Schedule,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
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
                    text = schedule.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = schedule.templateName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                schedule.nextRun?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Next: ${DateFormatter.formatDateTime(it)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = schedule.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = Color(0xFF4CAF50),
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFF44336),
                    uncheckedThumbColor = Color.White,
                    uncheckedBorderColor = Color(0xFFF44336)
                ),
                modifier = Modifier.testTag("switch_schedule_${schedule.id}")
            )
        }
    }
}

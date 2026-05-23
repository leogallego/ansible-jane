package io.github.leogallego.ansiblejane.ui.jobs

import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import io.github.leogallego.ansiblejane.ui.components.StatusFilterChips
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.github.leogallego.ansiblejane.ui.components.pressScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leogallego.ansiblejane.model.Job
import io.github.leogallego.ansiblejane.presentation.jobs.RecentJobsUiState
import io.github.leogallego.ansiblejane.presentation.jobs.RecentJobsViewModel
import io.github.leogallego.ansiblejane.ui.components.DateFormatter
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.JobStatusBadge
import io.github.leogallego.ansiblejane.ui.components.SkeletonCard
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentJobsScreen(
    onNavigateToJobStatus: (Int) -> Unit,
    viewModel: RecentJobsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is RecentJobsUiState.Success || uiState is RecentJobsUiState.Error) {
            isRefreshing = false
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is RecentJobsUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(5) { SkeletonCard() }
                }
            }
            is RecentJobsUiState.Error -> {
                ErrorMessage(
                    error = state.error,
                    onRetry = { viewModel.loadRecentJobs() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is RecentJobsUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        StatusFilterChips(
                            activeFilters = state.activeFilters,
                            onToggleFilter = { viewModel.toggleFilter(it) }
                        )

                        if (state.jobs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (state.activeFilters.isNotEmpty()) {
                                        "No jobs match the selected filters"
                                    } else {
                                        "No recent jobs"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            val listState = rememberLazyListState()

                            val shouldLoadMore by remember {
                                derivedStateOf {
                                    val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                    lastVisibleItem >= state.jobs.size - 3
                                }
                            }

                            LaunchedEffect(shouldLoadMore) {
                                snapshotFlow { shouldLoadMore }
                                    .collect { if (it) viewModel.loadMore() }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f)
                                    .testTag("list_jobs")
                            ) {
                                items(
                                    items = state.jobs,
                                    key = { it.id }
                                ) { job ->
                                    RecentJobItem(
                                        job = job,
                                        onClick = { onNavigateToJobStatus(job.id) }
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
        }
    }
}

@Composable
private fun RecentJobItem(
    job: Job,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pressScale(interactionSource)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                job.started?.let {
                    Text(
                        text = DateFormatter.formatDateTime(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            JobStatusBadge(status = job.status)
        }
    }
}

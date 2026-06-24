package io.github.leogallego.ansiblejane.ui.eda

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import io.github.leogallego.ansiblejane.model.EdaRuleAudit
import io.github.leogallego.ansiblejane.model.JobStatus
import io.github.leogallego.ansiblejane.presentation.eda.EdaAuditUiState
import io.github.leogallego.ansiblejane.presentation.eda.EdaAuditViewModel
import io.github.leogallego.ansiblejane.ui.components.DateFormatter
import io.github.leogallego.ansiblejane.ui.components.EmptyState
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.JobStatusBadge
import io.github.leogallego.ansiblejane.ui.components.LoadMoreIndicator
import io.github.leogallego.ansiblejane.ui.components.LoadingList
import io.github.leogallego.ansiblejane.ui.components.PaginationEffect
import io.github.leogallego.ansiblejane.ui.components.SearchBar
import io.github.leogallego.ansiblejane.ui.components.pressScale
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdaAuditScreen(
    viewModel: EdaAuditViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedAuditRule by remember { mutableStateOf<EdaRuleAudit?>(null) }

    LaunchedEffect(uiState) {
        if (uiState !is EdaAuditUiState.Loading) {
            isRefreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is EdaAuditUiState.Loading -> {
                LoadingList()
            }
            is EdaAuditUiState.Error -> {
                ErrorMessage(
                    error = state.error,
                    onRetry = { viewModel.loadAuditRules() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is EdaAuditUiState.Empty -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    EmptyState(message = state.message)
                }
            }
            is EdaAuditUiState.Success -> {
                val searchQuery by viewModel.searchQuery.collectAsState()

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val listState = rememberLazyListState()

                    PaginationEffect(
                        listState = listState,
                        itemCount = state.auditRules.size,
                        onLoadMore = { viewModel.loadMore() }
                    )

                    Column(modifier = Modifier.fillMaxSize()) {
                        SearchBar(
                            onSearch = { viewModel.search(it) },
                            placeholder = stringResource(Res.string.search_audit_rules),
                            initialQuery = searchQuery,
                        )

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().weight(1f).testTag("list_eda_audit")
                        ) {
                            items(
                                items = state.auditRules,
                                key = { it.id }
                            ) { auditRule ->
                                EdaAuditItem(
                                    auditRule = auditRule,
                                    onClick = { selectedAuditRule = auditRule }
                                )
                            }

                            if (state.isLoadingMore) {
                                item { LoadMoreIndicator() }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedAuditRule?.let { auditRule ->
        EdaAuditDetailSheet(
            auditRule = auditRule,
            onDismiss = { selectedAuditRule = null }
        )
    }
}

@Composable
private fun EdaAuditItem(
    auditRule: EdaRuleAudit,
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
                    text = auditRule.displayRuleName,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (auditRule.displayRuleSetName.isNotEmpty()) {
                    Text(
                        text = auditRule.displayRuleSetName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = DateFormatter.formatDateTime(auditRule.firedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val jobStatus = try {
                JobStatus.valueOf(auditRule.status.uppercase())
            } catch (_: IllegalArgumentException) {
                null
            }
            if (jobStatus != null) {
                JobStatusBadge(status = jobStatus)
            } else {
                Text(
                    text = auditRule.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

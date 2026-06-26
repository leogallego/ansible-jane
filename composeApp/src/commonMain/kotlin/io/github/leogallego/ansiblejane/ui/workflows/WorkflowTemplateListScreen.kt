package io.github.leogallego.ansiblejane.ui.workflows

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import io.github.leogallego.ansiblejane.model.Label
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowLaunchState
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowTemplatesUiState
import io.github.leogallego.ansiblejane.presentation.workflows.WorkflowTemplatesViewModel
import io.github.leogallego.ansiblejane.ui.components.EmptyState
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.ExtraVarsDialog
import io.github.leogallego.ansiblejane.ui.components.LabelChips
import io.github.leogallego.ansiblejane.ui.components.LaunchConfirmDialog
import io.github.leogallego.ansiblejane.ui.components.LoadMoreIndicator
import io.github.leogallego.ansiblejane.ui.components.LoadingList
import io.github.leogallego.ansiblejane.ui.components.PaginationEffect
import io.github.leogallego.ansiblejane.ui.components.SearchBar
import io.github.leogallego.ansiblejane.ui.components.TemplateCard
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowTemplateListScreen(
    onNavigateToWorkflowJobStatus: (Int) -> Unit,
    onNavigateToTemplateDetail: (Int, String) -> Unit = { _, _ -> },
    viewModel: WorkflowTemplatesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val launchState by viewModel.launchState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var selectedLabel by remember { mutableStateOf<Label?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is WorkflowTemplatesUiState.Success || uiState is WorkflowTemplatesUiState.Error) {
            isRefreshing = false
        }
    }

    LaunchedEffect(launchState) {
        when (val state = launchState) {
            is WorkflowLaunchState.Launched -> {
                viewModel.resetLaunchState()
                onNavigateToWorkflowJobStatus(state.workflowJobId)
            }
            is WorkflowLaunchState.LaunchError -> {
                snackbarHostState.showSnackbar(state.error.message)
                viewModel.resetLaunchState()
            }
            else -> {}
        }
    }

    // Launch dialogs
    when (val state = launchState) {
        is WorkflowLaunchState.Confirming -> {
            LaunchConfirmDialog(
                templateName = state.template.name,
                onConfirm = { viewModel.confirmLaunch() },
                onDismiss = { viewModel.cancelLaunch() }
            )
        }
        is WorkflowLaunchState.EnteringVars -> {
            ExtraVarsDialog(
                templateName = state.template.name,
                onConfirm = { extraVars -> viewModel.confirmLaunch(extraVars) },
                onDismiss = { viewModel.cancelLaunch() }
            )
        }
        else -> {}
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                onSearch = { viewModel.search(it) },
                initialQuery = searchQuery
            )

            when (val state = uiState) {
                is WorkflowTemplatesUiState.Idle, is WorkflowTemplatesUiState.Loading -> {
                    LoadingList()
                }
                is WorkflowTemplatesUiState.Error -> {
                    ErrorMessage(
                        error = state.error,
                        onRetry = { viewModel.loadTemplates() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is WorkflowTemplatesUiState.Success -> {
                    LabelChips(
                        labels = state.availableLabels,
                        selectedLabel = selectedLabel,
                        onLabelSelected = { label ->
                            selectedLabel = label
                            viewModel.filterByLabel(label)
                        }
                    )

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            viewModel.refresh()
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (state.templates.isEmpty()) {
                            EmptyState(message = stringResource(Res.string.workflow_templates_empty))
                        } else {
                            val listState = rememberLazyListState()

                            if (state.hasMore) {
                                PaginationEffect(
                                    listState = listState,
                                    itemCount = state.templates.size,
                                    onLoadMore = { viewModel.loadMore() }
                                )
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = state.templates,
                                    key = { it.id }
                                ) { template ->
                                    TemplateCard(
                                        template = template,
                                        onClick = { onNavigateToTemplateDetail(template.id, template.name) },
                                        onLaunch = { viewModel.requestLaunch(template) },
                                        testTagPrefix = "button"
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

            // Show loading overlay during launch
            if (launchState is WorkflowLaunchState.Launching) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

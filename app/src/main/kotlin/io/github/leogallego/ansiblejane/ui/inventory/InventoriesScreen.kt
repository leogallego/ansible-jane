package io.github.leogallego.ansiblejane.ui.inventory

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.leogallego.ansiblejane.model.Inventory
import io.github.leogallego.ansiblejane.presentation.inventory.InventoriesUiState
import io.github.leogallego.ansiblejane.presentation.inventory.InventoriesViewModel
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.SkeletonCard
import io.github.leogallego.ansiblejane.ui.components.pressScale
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoriesScreen(
    viewModel: InventoriesViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedInventory by remember { mutableStateOf<Inventory?>(null) }

    LaunchedEffect(uiState) {
        if (uiState !is InventoriesUiState.Loading) {
            isRefreshing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is InventoriesUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(5) { SkeletonCard() }
                }
            }
            is InventoriesUiState.Error -> {
                ErrorMessage(
                    error = state.error,
                    onRetry = { viewModel.loadInventories() },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            is InventoriesUiState.Empty -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            is InventoriesUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.refresh()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    val listState = rememberLazyListState()

                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisibleItem >= state.inventories.size - 3
                        }
                    }

                    LaunchedEffect(shouldLoadMore) {
                        snapshotFlow { shouldLoadMore }
                            .collect { if (it) viewModel.loadMore() }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().testTag("list_inventories")
                    ) {
                        items(
                            items = state.inventories,
                            key = { it.id }
                        ) { inventory ->
                            InventoryItem(
                                inventory = inventory,
                                onClick = { selectedInventory = inventory }
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

    selectedInventory?.let { inventory ->
        InventoryDetailSheet(
            inventory = inventory,
            onDismiss = { selectedInventory = null }
        )
    }
}

@Composable
private fun InventoryItem(
    inventory: Inventory,
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
                    text = inventory.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (inventory.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = inventory.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = inventory.displayKind,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    )
                    inventory.summaryFields.organization?.let { org ->
                        Text(
                            text = org.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${inventory.totalHosts} hosts",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

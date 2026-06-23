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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.launch
import io.github.leogallego.ansiblejane.model.Host
import io.github.leogallego.ansiblejane.model.Inventory
import io.github.leogallego.ansiblejane.presentation.hosts.InventoryHostsUiState
import io.github.leogallego.ansiblejane.presentation.hosts.InventoryHostsViewModel
import io.github.leogallego.ansiblejane.ui.components.DetailRow
import io.github.leogallego.ansiblejane.ui.components.DetailSheetHeader
import io.github.leogallego.ansiblejane.ui.components.EmptyState
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.LoadMoreIndicator
import io.github.leogallego.ansiblejane.ui.components.LoadingList
import io.github.leogallego.ansiblejane.ui.components.PaginationEffect
import io.github.leogallego.ansiblejane.ui.components.SearchBar
import io.github.leogallego.ansiblejane.ui.components.pressScale
import io.github.leogallego.ansiblejane.ui.hosts.HostDetailSheet
import org.jetbrains.compose.resources.stringResource
import aapremotecontrol.composeapp.generated.resources.*
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryDetailSheet(
    inventory: Inventory,
    onDismiss: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = isExpanded)

    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        if (isExpanded) {
            InventoryHostsFullScreen(
                inventory = inventory,
                onClose = onDismiss
            )
        } else {
            InventoryDetailCompact(
                inventory = inventory,
                onExpand = {
                    isExpanded = true
                    scope.launch { sheetState.expand() }
                }
            )
        }
    }
}

@Composable
private fun InventoryDetailCompact(
    inventory: Inventory,
    onExpand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        DetailSheetHeader(title = inventory.name, onExpand = onExpand)
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = stringResource(Res.string.inventory_detail_type), value = inventory.displayKind)

        inventory.summaryFields.organization?.let {
            DetailRow(label = stringResource(Res.string.inventory_detail_organization), value = it.name)
        }

        DetailRow(label = stringResource(Res.string.inventory_detail_total_hosts), value = inventory.totalHosts.toString())
        DetailRow(label = stringResource(Res.string.inventory_detail_total_groups), value = inventory.totalGroups.toString())
        DetailRow(label = stringResource(Res.string.label_created), value = inventory.created)
        DetailRow(label = stringResource(Res.string.label_modified), value = inventory.modified)

        if (inventory.variables.isNotBlank() && inventory.variables != "{}") {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.label_variables),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = inventory.variables,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryHostsFullScreen(
    inventory: Inventory,
    onClose: () -> Unit
) {
    val viewModel: InventoryHostsViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    var selectedHost by remember { mutableStateOf<Host?>(null) }

    LaunchedEffect(inventory.id) {
        viewModel.loadHosts(inventory.id)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = inventory.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.cd_close))
                }
            }
        )

        SearchBar(
            onSearch = { viewModel.search(it) },
            placeholder = stringResource(Res.string.search_hosts)
        )

        when (val state = uiState) {
            is InventoryHostsUiState.Loading -> {
                LoadingList()
            }
            is InventoryHostsUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorMessage(
                        error = state.error,
                        onRetry = { viewModel.loadHosts(inventory.id) }
                    )
                }
            }
            is InventoryHostsUiState.Empty -> {
                EmptyState(message = state.message)
            }
            is InventoryHostsUiState.Success -> {
                val listState = rememberLazyListState()

                PaginationEffect(
                    listState = listState,
                    itemCount = state.hosts.size,
                    onLoadMore = { viewModel.loadMore() }
                )

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    items(
                        items = state.hosts,
                        key = { it.id }
                    ) { host ->
                        HostItem(
                            host = host,
                            onClick = { selectedHost = host }
                        )
                    }

                    if (state.isLoadingMore) {
                        item { LoadMoreIndicator() }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }

    selectedHost?.let { host ->
        HostDetailSheet(
            host = host,
            onDismiss = { selectedHost = null }
        )
    }
}

@Composable
private fun HostItem(
    host: Host,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val groups = host.summaryFields.groups?.results.orEmpty()

    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pressScale(interactionSource)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (host.enabled) Icons.Default.CheckCircle else Icons.Default.RemoveCircle,
                contentDescription = if (host.enabled) stringResource(Res.string.cd_inventory_host_enabled) else stringResource(Res.string.cd_inventory_host_disabled),
                tint = if (host.enabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (groups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = groups.first().name,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                        if (groups.size > 1) {
                            Text(
                                text = stringResource(Res.string.inventory_host_more, groups.size - 1),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}


package com.example.aapremote.ui.inventory

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
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.example.aapremote.model.Host
import com.example.aapremote.model.Inventory
import com.example.aapremote.presentation.hosts.InventoryHostsUiState
import com.example.aapremote.presentation.hosts.InventoryHostsViewModel
import com.example.aapremote.ui.components.ErrorMessage
import com.example.aapremote.ui.components.SearchBar
import com.example.aapremote.ui.components.SkeletonCard
import com.example.aapremote.ui.components.pressScale
import com.example.aapremote.ui.hosts.HostDetailSheet
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = inventory.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onExpand) {
                Icon(Icons.Default.OpenInFull, contentDescription = "Expand to full screen")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        DetailRow(label = "Type", value = inventory.displayKind)

        inventory.summaryFields.organization?.let {
            DetailRow(label = "Organization", value = it.name)
        }

        DetailRow(label = "Total Hosts", value = inventory.totalHosts.toString())
        DetailRow(label = "Total Groups", value = inventory.totalGroups.toString())
        DetailRow(label = "Created", value = inventory.created)
        DetailRow(label = "Modified", value = inventory.modified)

        if (inventory.variables.isNotBlank() && inventory.variables != "{}") {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Variables",
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        )

        SearchBar(
            onSearch = { viewModel.search(it) },
            placeholder = "Search hosts..."
        )

        when (val state = uiState) {
            is InventoryHostsUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(5) { SkeletonCard() }
                }
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
            is InventoryHostsUiState.Success -> {
                val listState = rememberLazyListState()

                val shouldLoadMore by remember {
                    derivedStateOf {
                        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        lastVisibleItem >= state.hosts.size - 3
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
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
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
                contentDescription = if (host.enabled) "Enabled" else "Disabled",
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
                                text = "+${groups.size - 1} more",
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

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

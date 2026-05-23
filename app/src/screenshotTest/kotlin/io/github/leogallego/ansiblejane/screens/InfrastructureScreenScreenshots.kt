package io.github.leogallego.ansiblejane.screens

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
import androidx.compose.material3.AssistChip
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
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.Host
import io.github.leogallego.ansiblejane.model.HostSummaryFields
import io.github.leogallego.ansiblejane.model.Inventory
import io.github.leogallego.ansiblejane.model.InventorySummaryFields
import io.github.leogallego.ansiblejane.model.InventorySummary
import io.github.leogallego.ansiblejane.model.OrganizationSummary
import io.github.leogallego.ansiblejane.ui.components.ErrorMessage
import io.github.leogallego.ansiblejane.ui.components.SearchBar
import io.github.leogallego.ansiblejane.ui.components.SkeletonCard
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

private val sampleInventories = listOf(
    Inventory(
        id = 1, name = "Production DC1",
        description = "Production datacenter 1 inventory",
        totalHosts = 150, totalGroups = 12,
        summaryFields = InventorySummaryFields(
            organization = OrganizationSummary(1, "Operations")
        )
    ),
    Inventory(
        id = 2, name = "Staging Cloud",
        description = "Staging environment in AWS",
        kind = "smart", totalHosts = 45, totalGroups = 6,
        summaryFields = InventorySummaryFields(
            organization = OrganizationSummary(1, "Operations")
        )
    ),
    Inventory(
        id = 3, name = "Development Lab",
        description = "Local development machines",
        totalHosts = 8, totalGroups = 3
    )
)

private val sampleHosts = listOf(
    Host(
        id = 1, name = "web-prod-01.example.com",
        description = "Primary web server",
        summaryFields = HostSummaryFields(
            inventory = InventorySummary(1, "Production DC1")
        )
    ),
    Host(
        id = 2, name = "db-prod-01.example.com",
        description = "Primary database server",
        summaryFields = HostSummaryFields(
            inventory = InventorySummary(1, "Production DC1")
        )
    ),
    Host(
        id = 3, name = "cache-prod-01.example.com",
        description = "Redis cache cluster node",
        summaryFields = HostSummaryFields(
            inventory = InventorySummary(1, "Production DC1")
        )
    )
)

@Composable
private fun InventoryItem(inventory: Inventory) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
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

@Composable
private fun HostItem(host: Host) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = host.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (host.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = host.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                host.summaryFields.inventory?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Inventory: ${it.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Inventories - Light",
    widthDp = 400, heightDp = 900
)
@Composable
fun InventoriesListLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sampleInventories, key = { it.id }) { inventory ->
                InventoryItem(inventory)
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Inventories - Dark",
    widthDp = 400, heightDp = 900
)
@Composable
fun InventoriesListDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sampleInventories, key = { it.id }) { inventory ->
                InventoryItem(inventory)
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Inventories Empty",
    widthDp = 400, heightDp = 500
)
@Composable
fun InventoriesEmpty() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No inventories found",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Hosts List - Light",
    widthDp = 400, heightDp = 900
)
@Composable
fun HostsListLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(onSearch = {}, placeholder = "Search hosts...")
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleHosts, key = { it.id }) { host ->
                    HostItem(host)
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Hosts List - Dark",
    widthDp = 400, heightDp = 900
)
@Composable
fun HostsListDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(onSearch = {}, placeholder = "Search hosts...")
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(sampleHosts, key = { it.id }) { host ->
                    HostItem(host)
                }
            }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Inventories Loading",
    widthDp = 400, heightDp = 500
)
@Composable
fun InventoriesLoading() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(5) { SkeletonCard() }
        }
    }
}

@PreviewTest
@Preview(
    showBackground = true, name = "Inventories Error",
    widthDp = 400, heightDp = 500
)
@Composable
fun InventoriesError() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        ErrorMessage(
            error = AppError.Server(statusCode = 500),
            onRetry = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@PreviewTest
@Preview(
    showBackground = true, fontScale = 1.5f, name = "Inventories - Large Font",
    widthDp = 400, heightDp = 900
)
@Composable
fun InventoriesLargeFont() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(sampleInventories.take(2), key = { it.id }) { inventory ->
                InventoryItem(inventory)
            }
        }
    }
}

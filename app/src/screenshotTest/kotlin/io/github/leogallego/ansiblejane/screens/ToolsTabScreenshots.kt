package io.github.leogallego.ansiblejane.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme

private data class McpServerPreview(
    val label: String,
    val url: String,
    val status: String,
    val dotColor: Color,
    val toolCount: Int = 0,
    val expanded: Boolean = false,
    val tools: List<ToolPreview> = emptyList()
)

private data class ToolPreview(
    val name: String,
    val description: String,
    val enabled: Boolean = true
)

private data class CategoryPreview(
    val name: String,
    val displayName: String,
    val tools: List<ToolPreview>,
    val expanded: Boolean = false
)

private val sampleMcpServers = listOf(
    McpServerPreview(
        "Jobs", "https://aap:8448/job_management/mcp",
        "Connected", Color(0xFF4CAF50), toolCount = 8,
        tools = listOf(
            ToolPreview("controller.job_templates_list", "List job templates"),
            ToolPreview("controller.jobs_list", "List jobs"),
            ToolPreview("controller.jobs_read", "Read job details")
        )
    ),
    McpServerPreview("Inventory", "https://aap:8448/inventory_management/mcp", "Connected", Color(0xFF4CAF50), 6),
    McpServerPreview("Monitoring", "https://aap:8448/system_monitoring/mcp", "Connecting…", Color(0xFFFF9800)),
    McpServerPreview("Security", "https://aap:8448/security_compliance/mcp", "Connection error", Color(0xFFF44336))
)

private val sampleCategories = listOf(
    CategoryPreview("JOBS", "Jobs", listOf(
        ToolPreview("list_job_templates", "List all job templates"),
        ToolPreview("launch_job", "Launch a job from template"),
        ToolPreview("get_job", "Get job details"),
        ToolPreview("get_job_stdout", "Get job output")
    )),
    CategoryPreview("INVENTORY", "Inventory", listOf(
        ToolPreview("list_hosts", "List all hosts"),
        ToolPreview("list_inventories", "List inventories"),
        ToolPreview("get_host_facts", "Get host facts")
    )),
    CategoryPreview("MONITORING", "Monitoring", listOf(
        ToolPreview("ping", "Ping AAP instance"),
        ToolPreview("list_instances", "List instances")
    )),
    CategoryPreview("EDA", "EDA", listOf(
        ToolPreview("list_eda_activations", "List EDA activations"),
        ToolPreview("list_eda_rulebooks", "List EDA rulebooks")
    ))
)

@Composable
private fun McpServerCardPreview(server: McpServerPreview) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(server.dotColor)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(server.label, style = MaterialTheme.typography.titleSmall)
                    Text(server.status, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = true, onCheckedChange = {})
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (server.expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (server.expanded && server.tools.isNotEmpty()) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider()
                    server.tools.forEach { tool ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tool.name, style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(tool.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = tool.enabled, onCheckedChange = {})
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySectionPreview(category: CategoryPreview) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 12.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(category.displayName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                Text("${category.tools.size}", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                if (category.expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = category.expanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.padding(start = 8.dp)) {
                category.tools.forEach { tool ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tool.name, style = MaterialTheme.typography.bodyMedium)
                            Text(tool.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = tool.enabled, onCheckedChange = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolsTabContent(
    servers: List<McpServerPreview>,
    categories: List<CategoryPreview>,
    mcpEnabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("MCP Servers", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable AAP MCP", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = mcpEnabled, onCheckedChange = {})
        }
        if (mcpEnabled) {
            servers.forEach { McpServerCardPreview(it) }
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Add MCP Server", modifier = Modifier.padding(start = 4.dp))
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text("Local Tools", style = MaterialTheme.typography.titleMedium)
        Text("26 tools across 4 categories", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        categories.forEach { CategorySectionPreview(it) }
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Tools Tab - Collapsed - Light", widthDp = 400, heightDp = 1000)
@Composable
fun ToolsTabCollapsedLight() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        ToolsTabContent(sampleMcpServers.map { it.copy(expanded = false) }, sampleCategories)
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Tools Tab - Collapsed - Dark", widthDp = 400, heightDp = 1000)
@Composable
fun ToolsTabCollapsedDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
        ToolsTabContent(sampleMcpServers.map { it.copy(expanded = false) }, sampleCategories)
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Tools Tab - MCP Expanded", widthDp = 400, heightDp = 1100)
@Composable
fun ToolsTabMcpExpanded() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        ToolsTabContent(
            sampleMcpServers.mapIndexed { i, s -> if (i == 0) s.copy(expanded = true) else s },
            sampleCategories
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Tools Tab - Category Expanded", widthDp = 400, heightDp = 1200)
@Composable
fun ToolsTabCategoryExpanded() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        ToolsTabContent(
            sampleMcpServers,
            sampleCategories.mapIndexed { i, c -> if (i == 0) c.copy(expanded = true) else c }
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Tools Tab - MCP Disabled", widthDp = 400, heightDp = 600)
@Composable
fun ToolsTabMcpDisabled() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        ToolsTabContent(sampleMcpServers, sampleCategories, mcpEnabled = false)
    }
}

@PreviewTest
@Preview(showBackground = true, fontScale = 1.5f, name = "Tools Tab - Large Font", widthDp = 400, heightDp = 1200)
@Composable
fun ToolsTabLargeFont() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        ToolsTabContent(sampleMcpServers.take(2), sampleCategories.take(2))
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Tools Tab - Category Expanded - Dark", widthDp = 400, heightDp = 1200)
@Composable
fun ToolsTabCategoryExpandedDark() {
    AnsibleJaneTheme(darkTheme = true, dynamicColor = false) {
        ToolsTabContent(
            sampleMcpServers,
            sampleCategories.mapIndexed { i, c -> if (i == 0) c.copy(expanded = true) else c }
        )
    }
}

@PreviewTest
@Preview(showBackground = true, name = "Tools Tab - Medium Width", widthDp = 610, heightDp = 1000)
@Composable
fun ToolsTabMediumWidth() {
    AnsibleJaneTheme(darkTheme = false, dynamicColor = false) {
        ToolsTabContent(sampleMcpServers, sampleCategories)
    }
}

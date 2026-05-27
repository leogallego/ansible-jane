# Tools Settings UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the Tools settings tab with expandable MCP server cards, per-tool enable/disable toggles, category-grouped local tools, and DataStore persistence for disabled tools.

**Architecture:** Two-section UI (MCP servers + local tools) backed by persisted disabled-tools state in DataStore. SettingsViewModel owns UI state; AssistantViewModel reads disabled set at message time and applies to per-message ToolRouter. No ToolRouter lifecycle changes.

**Tech Stack:** Jetpack Compose (Material 3), DataStore Preferences, Koin DI, Robolectric + Compose Testing + Screenshot Tests.

**Spec:** `docs/superpowers/specs/2026-05-26-tools-ui-redesign-design.md`

**Issue:** #196

**Branch:** Create `196-tools-ui-redesign` from `main`

---

## File Map

**New files:**
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/LocalToolUiState.kt` — UI models
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/McpServerCard.kt` — expandable card composable
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/AddMcpServerSheet.kt` — bottom sheet dialog
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/LocalToolsSection.kt` — category-grouped tools
- `app/src/screenshotTest/kotlin/io/github/leogallego/ansiblejane/screens/ToolsTabScreenshots.kt` — screenshot tests

**Modified files:**
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/theme/StatusColors.kt` — add `error`, `disconnected`
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ToolRouter.kt` — add `getCategoryForTool()`
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/data/IAssistantRepository.kt` — add disabled tools methods
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/data/AssistantRepository.kt` — implement persistence
- `app/src/test/kotlin/io/github/leogallego/ansiblejane/fakes/FakeAssistantRepository.kt` — add fake methods
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — add `getToolsForServer()`, `reconnectServer()`
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsUiState.kt` — add new fields
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsViewModel.kt` — add tool management methods
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/AssistantModule.kt` — inject local tools into SettingsViewModel
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/ToolsTab.kt` — full rewrite
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/SettingsScreen.kt` — pass new callbacks
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/presentation/AssistantViewModel.kt` — apply disabled tools
- `app/src/main/res/values/strings.xml` — add string resources
- `app/src/test/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ToolRouterTest.kt` — add getCategoryForTool tests
- `app/src/test/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsViewModelTest.kt` — add tool management tests
- `app/src/test/kotlin/io/github/leogallego/ansiblejane/ui/settings/SettingsScreenTest.kt` — add Tools tab UI tests
- `app/src/test/kotlin/io/github/leogallego/ansiblejane/TestData.kt` — add tool test fixtures

---

### Task 1: StatusColors — Add Error and Disconnected Tokens

**Files:**
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/theme/StatusColors.kt`

- [ ] **Step 1: Add new color tokens to StatusColors**

```kotlin
data class StatusColors(
    val successful: Color = Color(0xFF4CAF50),
    val successfulDim: Color = Color(0xFF2E7D32),
    val running: Color = Color(0xFFFF9800),
    val healthDegraded: Color = Color(0xFFE6A817),
    val error: Color = Color(0xFFF44336),
    val disconnected: Color = Color(0xFF9E9E9E),
)
```

- [ ] **Step 2: Build to verify no compilation errors**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/theme/StatusColors.kt
git commit -m "Add error and disconnected color tokens to StatusColors (#196)"
```

---

### Task 2: ToolRouter — Add getCategoryForTool Companion Function

**Files:**
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ToolRouter.kt`
- Modify: `app/src/test/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ToolRouterTest.kt`

- [ ] **Step 1: Write failing tests for getCategoryForTool**

Add to end of `ToolRouterTest.kt`:

```kotlin
// --- getCategoryForTool ---

@Test
fun `getCategoryForTool SHOULD return INVENTORY for list_hosts`() {
    assertEquals("INVENTORY", ToolRouter.getCategoryForTool("list_hosts"))
}

@Test
fun `getCategoryForTool SHOULD return JOBS for launch_job`() {
    assertEquals("JOBS", ToolRouter.getCategoryForTool("launch_job"))
}

@Test
fun `getCategoryForTool SHOULD return EDA for list_eda_activations`() {
    assertEquals("EDA", ToolRouter.getCategoryForTool("list_eda_activations"))
}

@Test
fun `getCategoryForTool SHOULD return PLATFORM for list_platform_users`() {
    assertEquals("PLATFORM", ToolRouter.getCategoryForTool("list_platform_users"))
}

@Test
fun `getCategoryForTool SHOULD return null for unknown tool`() {
    assertNull(ToolRouter.getCategoryForTool("nonexistent_tool"))
}

@Test
fun `getCategoryForTool SHOULD cover all local tool names in all categories`() {
    val allToolNames = listOf(
        "list_inventories", "list_hosts", "get_host_facts", "get_host_job_summaries",
        "list_groups", "list_inventory_sources", "list_labels",
        "list_job_templates", "launch_job", "get_job", "get_job_stdout", "list_jobs",
        "list_workflow_templates", "launch_workflow", "get_workflow_job",
        "list_schedules", "toggle_schedule", "list_workflow_nodes", "get_survey_spec",
        "list_pending_approvals", "approve_workflow", "deny_workflow",
        "list_instances", "get_instance", "list_instance_groups", "ping", "get_mesh_topology",
        "list_credentials", "get_credential", "list_credential_types",
        "list_projects", "get_project", "list_execution_environments",
        "list_notification_templates", "get_settings", "get_config",
        "list_organizations", "list_users", "list_teams",
        "list_roles", "list_role_definitions", "list_applications", "list_tokens",
        "list_platform_organizations", "list_platform_users", "list_platform_teams",
        "list_platform_role_definitions", "list_authenticators",
        "list_platform_services", "list_service_clusters",
        "list_eda_audit_rules", "list_eda_activations", "get_eda_activation",
        "list_eda_rulebooks", "list_eda_decision_environments",
        "list_eda_projects", "list_eda_credentials", "list_eda_credential_types",
        "list_eda_event_streams", "list_eda_users"
    )
    for (name in allToolNames) {
        assertNotNull(
            ToolRouter.getCategoryForTool(name),
            "Tool '$name' should have a category"
        )
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*ToolRouterTest.getCategoryForTool*" 2>&1 | tail -10`
Expected: FAIL — `getCategoryForTool` does not exist

- [ ] **Step 3: Implement getCategoryForTool**

Add to the `companion object` in `ToolRouter.kt` (after line 138):

```kotlin
fun getCategoryForTool(toolName: String): String? {
    return Category.entries.firstOrNull { toolName in it.localToolNames }?.name
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "*ToolRouterTest.getCategoryForTool*" 2>&1 | tail -10`
Expected: All 6 tests PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ToolRouter.kt \
       app/src/test/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ToolRouterTest.kt
git commit -m "Add getCategoryForTool companion function to ToolRouter (#196)"
```

---

### Task 3: Persistence Layer — Disabled Tools in DataStore

**Files:**
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/data/IAssistantRepository.kt`
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/data/AssistantRepository.kt`
- Modify: `app/src/test/kotlin/io/github/leogallego/ansiblejane/fakes/FakeAssistantRepository.kt`

- [ ] **Step 1: Add interface methods to IAssistantRepository**

Add at end of `IAssistantRepository` (before closing brace, line 22):

```kotlin
suspend fun saveDisabledTools(tools: Set<String>)
suspend fun getDisabledTools(): Set<String>
```

- [ ] **Step 2: Implement in AssistantRepository**

Add the DataStore key to the companion object (after line 43):

```kotlin
val KEY_DISABLED_TOOLS = stringPreferencesKey("disabled_tools")
```

Add implementations before the closing brace of the class:

```kotlin
override suspend fun saveDisabledTools(tools: Set<String>) {
    val encoded = json.encodeToString(
        kotlinx.serialization.builtins.SetSerializer(String.serializer()),
        tools
    )
    context.assistantDataStore.edit { prefs ->
        prefs[KEY_DISABLED_TOOLS] = encoded
    }
}

override suspend fun getDisabledTools(): Set<String> {
    val prefs = context.assistantDataStore.data.first()
    val encoded = prefs[KEY_DISABLED_TOOLS] ?: return emptySet()
    return try {
        json.decodeFromString(
            kotlinx.serialization.builtins.SetSerializer(String.serializer()),
            encoded
        )
    } catch (e: Exception) {
        Log.w(TAG, "Failed to deserialize disabled tools", e)
        emptySet()
    }
}
```

- [ ] **Step 3: Implement in FakeAssistantRepository**

Add to `FakeAssistantRepository`:

```kotlin
var savedDisabledTools: Set<String> = emptySet()

override suspend fun saveDisabledTools(tools: Set<String>) {
    savedDisabledTools = tools
}

override suspend fun getDisabledTools(): Set<String> = savedDisabledTools
```

- [ ] **Step 4: Build to verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/data/IAssistantRepository.kt \
       app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/data/AssistantRepository.kt \
       app/src/test/kotlin/io/github/leogallego/ansiblejane/fakes/FakeAssistantRepository.kt
git commit -m "Add disabled tools persistence to AssistantRepository (#196)"
```

---

### Task 4: McpServerManager — Add getToolsForServer and reconnectServer

**Files:**
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt`

- [ ] **Step 1: Add getToolsForServer method**

Add after `getAllTools()` (line 84):

```kotlin
fun getToolsForServer(label: String): List<McpTool> =
    synchronized(mcpTools) { mcpTools.filter { it.serverLabel == label } }
```

- [ ] **Step 2: Add reconnectServer method**

Add after `refreshConnections()` (line 91). This needs access to the instance, so store it as a field:

First, add a field after `mcpTools` (line 24):

```kotlin
private var currentInstance: AapInstance? = null
```

Update `connectAll` to store the instance (after line 27, before `disconnectAll()`):

```kotlin
currentInstance = instance
```

Then add the method:

```kotlin
suspend fun reconnectServer(label: String) {
    val instance = currentInstance ?: return
    val config = instance.mcpServerUrls?.find { it.label == label } ?: return

    // Disconnect existing
    clients[label]?.let { client ->
        try { client.disconnect() } catch (_: Exception) {}
        clients.remove(label)
    }
    synchronized(mcpTools) { mcpTools.removeAll { it.serverLabel == label } }
    _connections.update { it + (label to McpConnectionState.Connecting) }

    // Reconnect
    val httpClient = httpClientFactory(instance, config)
    connectServer(config, httpClient)
}
```

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt
git commit -m "Add getToolsForServer and reconnectServer to McpServerManager (#196)"
```

---

### Task 5: UI Models and State

**Files:**
- Create: `app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/LocalToolUiState.kt`
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsUiState.kt`
- Modify: `app/src/test/kotlin/io/github/leogallego/ansiblejane/TestData.kt`

- [ ] **Step 1: Create LocalToolUiState.kt**

```kotlin
package io.github.leogallego.ansiblejane.presentation.settings

data class LocalToolUiState(
    val name: String,
    val description: String,
    val category: String,
    val isEnabled: Boolean
)

data class McpToolUiState(
    val name: String,
    val description: String,
    val isEnabled: Boolean
)
```

- [ ] **Step 2: Add new fields to SettingsUiState.Ready**

Add after the existing `connections` field (line 42 in `SettingsUiState.kt`):

```kotlin
val localTools: List<LocalToolUiState> = emptyList(),
val mcpServerTools: Map<String, List<McpToolUiState>> = emptyMap(),
val expandedMcpServers: Set<String> = emptySet(),
val expandedCategories: Set<String> = emptySet(),
val disabledTools: Set<String> = emptySet(),
```

- [ ] **Step 3: Add test fixtures to TestData.kt**

Add at end of `TestData.kt`:

```kotlin
fun testMcpServerConfig(
    url: String = "https://aap.example.com:8448/job_management/mcp",
    label: String = "Jobs",
    enabled: Boolean = true,
    isAutoDetected: Boolean = true,
    readOnly: Boolean = true,
    toolset: String? = "job_management"
) = McpServerConfig(
    url = url, label = label, enabled = enabled,
    isAutoDetected = isAutoDetected, readOnly = readOnly, toolset = toolset
)

fun testLocalToolUiState(
    name: String = "list_hosts",
    description: String = "List all hosts in AAP inventory",
    category: String = "INVENTORY",
    isEnabled: Boolean = true
) = LocalToolUiState(
    name = name, description = description,
    category = category, isEnabled = isEnabled
)

fun testMcpToolUiState(
    name: String = "controller.hosts_list",
    description: String = "List hosts via MCP",
    isEnabled: Boolean = true
) = McpToolUiState(
    name = name, description = description, isEnabled = isEnabled
)
```

Add necessary imports to `TestData.kt`:

```kotlin
import io.github.leogallego.ansiblejane.model.McpServerConfig
import io.github.leogallego.ansiblejane.presentation.settings.LocalToolUiState
import io.github.leogallego.ansiblejane.presentation.settings.McpToolUiState
```

- [ ] **Step 4: Build to verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/LocalToolUiState.kt \
       app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsUiState.kt \
       app/src/test/kotlin/io/github/leogallego/ansiblejane/TestData.kt
git commit -m "Add UI models and state fields for tools redesign (#196)"
```

---

### Task 6: SettingsViewModel — Tool Management Methods

**Files:**
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsViewModel.kt`
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/AssistantModule.kt`
- Modify: `app/src/test/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsViewModelTest.kt`

- [ ] **Step 1: Write failing ViewModel tests**

Add to `SettingsViewModelTest.kt`. First add import and field:

```kotlin
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.assistant.tools.ToolSource
```

Then add tests at end:

```kotlin
@Test
fun `toggleToolEnabled SHOULD disable a local tool and persist`() = runTest {
    setUpViewModel()
    viewModel.uiState.test {
        skipItems(1)
        viewModel.toggleToolEnabled("list_hosts", ToolSource.LOCAL, false)
        val state = awaitItem() as SettingsUiState.Ready
        assertTrue("LOCAL:list_hosts" in state.disabledTools)
        assertTrue("LOCAL:list_hosts" in fakeAssistantRepository.savedDisabledTools)
    }
}

@Test
fun `toggleToolEnabled SHOULD re-enable a previously disabled tool`() = runTest {
    fakeAssistantRepository.savedDisabledTools = setOf("LOCAL:list_hosts")
    setUpViewModel()
    viewModel.uiState.test {
        skipItems(1)
        viewModel.toggleToolEnabled("list_hosts", ToolSource.LOCAL, true)
        val state = awaitItem() as SettingsUiState.Ready
        assertFalse("LOCAL:list_hosts" in state.disabledTools)
    }
}

@Test
fun `toggleToolEnabled SHOULD use MCP prefix for MCP tools`() = runTest {
    setUpViewModel()
    viewModel.uiState.test {
        skipItems(1)
        viewModel.toggleToolEnabled("controller.hosts_list", ToolSource.MCP, false)
        val state = awaitItem() as SettingsUiState.Ready
        assertTrue("MCP:controller.hosts_list" in state.disabledTools)
    }
}

@Test
fun `toggleExpandCategory SHOULD toggle category in expandedCategories`() = runTest {
    setUpViewModel()
    viewModel.uiState.test {
        skipItems(1)
        viewModel.toggleExpandCategory("JOBS")
        val expanded = awaitItem() as SettingsUiState.Ready
        assertTrue("JOBS" in expanded.expandedCategories)
        viewModel.toggleExpandCategory("JOBS")
        val collapsed = awaitItem() as SettingsUiState.Ready
        assertFalse("JOBS" in collapsed.expandedCategories)
    }
}

@Test
fun `toggleExpandMcpServer SHOULD toggle server in expandedMcpServers`() = runTest {
    setUpViewModel()
    viewModel.uiState.test {
        skipItems(1)
        viewModel.toggleExpandMcpServer("Jobs")
        val expanded = awaitItem() as SettingsUiState.Ready
        assertTrue("Jobs" in expanded.expandedMcpServers)
    }
}
```

Note: the existing `setUpViewModel()` method may need updating to inject `localTools` — check the actual test setup and adjust accordingly when implementing. The ViewModel constructor will gain a `localTools: List<LocalTool>` parameter.

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*SettingsViewModelTest.toggleTool*" --tests "*SettingsViewModelTest.toggleExpand*" 2>&1 | tail -10`
Expected: FAIL — methods don't exist

- [ ] **Step 3: Add localTools parameter to SettingsViewModel constructor**

Update constructor (line 30 of `SettingsViewModel.kt`):

```kotlin
class SettingsViewModel(
    private val tokenManager: ITokenManager,
    private val apiProvider: IAapApiProvider,
    private val userPreferences: IUserPreferencesRepository,
    private val assistantRepository: IAssistantRepository,
    private val mcpServerManager: McpServerManager,
    private val instanceDiscovery: InstanceDiscovery,
    private val httpClient: OkHttpClient,
    private val json: Json,
    private val localTools: List<LocalTool> = emptyList()
) : ViewModel() {
```

- [ ] **Step 4: Add init block to load local tools and disabled state**

Add a new `viewModelScope.launch` block in `init` (after line 117):

```kotlin
viewModelScope.launch {
    val disabled = assistantRepository.getDisabledTools()
    val toolUiStates = localTools.map { tool ->
        LocalToolUiState(
            name = tool.spec.name,
            description = tool.spec.description,
            category = ToolRouter.getCategoryForTool(tool.spec.name) ?: "OTHER",
            isEnabled = "LOCAL:${tool.spec.name}" !in disabled
        )
    }
    updateReady { copy(localTools = toolUiStates, disabledTools = disabled) }
}
```

Add import at top:

```kotlin
import io.github.leogallego.ansiblejane.assistant.engine.ToolRouter
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolSource
```

- [ ] **Step 5: Add mcpServerTools derivation in the existing connections combine block**

In the `combine` block (around line 88 where `connections` is used), add MCP tools derivation:

```kotlin
val mcpServerTools = connections.mapNotNull { (label, state) ->
    if (state is McpConnectionState.Connected) {
        val disabled = (current as? SettingsUiState.Ready)?.disabledTools ?: emptySet()
        label to mcpServerManager.getToolsForServer(label).map { tool ->
            McpToolUiState(
                name = tool.spec.name,
                description = tool.spec.description,
                isEnabled = "MCP:${tool.spec.name}" !in disabled
            )
        }
    } else null
}.toMap()
```

And add `mcpServerTools = mcpServerTools` to the `SettingsUiState.Ready(...)` constructor call.

Also preserve `expandedMcpServers`, `expandedCategories`, `localTools`, and `disabledTools` via the `current` state (same pattern as other preserved fields):

```kotlin
val preservedExpandedMcp = (current as? SettingsUiState.Ready)?.expandedMcpServers ?: emptySet()
val preservedExpandedCats = (current as? SettingsUiState.Ready)?.expandedCategories ?: emptySet()
val preservedLocalTools = (current as? SettingsUiState.Ready)?.localTools ?: emptyList()
val preservedDisabledTools = (current as? SettingsUiState.Ready)?.disabledTools ?: emptySet()
```

Pass them into the `SettingsUiState.Ready(...)` constructor.

- [ ] **Step 6: Add tool management methods**

Add after the `toggleServerReadOnly` method (after line 320):

```kotlin
fun toggleToolEnabled(toolName: String, source: ToolSource, enabled: Boolean) {
    val key = "${source.name}:$toolName"
    viewModelScope.launch {
        val current = (uiState.value as? SettingsUiState.Ready)?.disabledTools ?: emptySet()
        val updated = if (enabled) current - key else current + key
        assistantRepository.saveDisabledTools(updated)
        updateReady {
            copy(
                disabledTools = updated,
                localTools = localTools.map {
                    if (it.name == toolName && source == ToolSource.LOCAL) {
                        it.copy(isEnabled = enabled)
                    } else it
                },
                mcpServerTools = mcpServerTools.mapValues { (_, tools) ->
                    tools.map {
                        if (it.name == toolName && source == ToolSource.MCP) {
                            it.copy(isEnabled = enabled)
                        } else it
                    }
                }
            )
        }
    }
}

fun toggleExpandMcpServer(label: String) {
    updateReady {
        val updated = if (label in expandedMcpServers) {
            expandedMcpServers - label
        } else {
            expandedMcpServers + label
        }
        copy(expandedMcpServers = updated)
    }
}

fun toggleExpandCategory(category: String) {
    updateReady {
        val updated = if (category in expandedCategories) {
            expandedCategories - category
        } else {
            expandedCategories + category
        }
        copy(expandedCategories = updated)
    }
}

fun refreshMcpServer(label: String) {
    viewModelScope.launch {
        mcpServerManager.reconnectServer(label)
    }
}
```

- [ ] **Step 7: Update AssistantModule.kt Koin wiring**

Update the SettingsViewModel construction in `AssistantModule.kt` (line 72-83):

```kotlin
viewModel {
    SettingsViewModel(
        tokenManager = get(),
        apiProvider = get(),
        userPreferences = get(),
        assistantRepository = get(),
        mcpServerManager = get(),
        instanceDiscovery = get(),
        httpClient = get(named("llm")),
        json = networkJson,
        localTools = getAll<LocalTool>()
    )
}
```

- [ ] **Step 8: Update test setup to inject localTools**

Update `SettingsViewModelTest` setup to pass `localTools = emptyList()` (or a test list) to the ViewModel constructor.

- [ ] **Step 9: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "*SettingsViewModelTest*" 2>&1 | tail -15`
Expected: All tests PASS (existing + new)

- [ ] **Step 10: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsViewModel.kt \
       app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/AssistantModule.kt \
       app/src/test/kotlin/io/github/leogallego/ansiblejane/presentation/settings/SettingsViewModelTest.kt
git commit -m "Add tool management methods to SettingsViewModel (#196)"
```

---

### Task 7: String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add all new string resources**

Add inside the `<resources>` tag:

```xml
<!-- Tools tab - MCP -->
<string name="tools_mcp_servers">MCP Servers</string>
<string name="tools_mcp_description">Auto-detect at {instance}/{toolset}/mcp</string>
<string name="tools_mcp_status_connected">Connected</string>
<string name="tools_mcp_status_connecting">Connecting…</string>
<string name="tools_mcp_status_error">Connection error</string>
<string name="tools_mcp_status_disconnected">Not connected</string>
<string name="tools_mcp_no_tools">No tools discovered</string>
<string name="tools_mcp_add_server">Add MCP Server</string>
<string name="tools_mcp_server_name">Server name</string>
<string name="tools_mcp_server_url">Server URL</string>
<string name="tools_mcp_server_toolset">Toolset (optional)</string>
<string name="tools_mcp_refresh">Refresh</string>
<string name="tools_mcp_remove">Remove</string>
<string name="tools_mcp_read_only">Read Only</string>
<string name="tools_mcp_error_retry">Tap Refresh to retry</string>
<!-- Tools tab - Local -->
<string name="tools_local_title">Local Tools</string>
<string name="tools_local_description">%1$d tools across %2$d categories</string>
<string name="tools_category_jobs">Jobs</string>
<string name="tools_category_inventory">Inventory</string>
<string name="tools_category_monitoring">Monitoring</string>
<string name="tools_category_users">Users</string>
<string name="tools_category_security">Security</string>
<string name="tools_category_configuration">Configuration</string>
<string name="tools_category_eda">EDA</string>
<string name="tools_category_platform">Platform</string>
```

- [ ] **Step 2: Build to verify no XML errors**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "Add string resources for tools UI redesign (#196)"
```

---

### Task 8: McpServerCard Composable

**Files:**
- Create: `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/McpServerCard.kt`

- [ ] **Step 1: Create McpServerCard.kt**

Full composable with expandable card, status dot, tool list, action buttons. Reference the spec's Interaction & UX Patterns section for animation (`AnimatedVisibility`), click isolation (`.clickable` on Row, Switch separate), touch targets (48dp min), empty/loading states, and accessibility patterns.

Key patterns from existing AgentTab (`ui/settings/AgentTab.kt:220-316`):
- Status dot: `Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))`
- Header row: `Row(Modifier.fillMaxWidth().clickable { onToggleExpand() }.padding(16.dp))`
- Expand animation: `AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically())`
- Chevron: `Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown)`

Status colors from `AnsibleJaneTheme.statusColors`:
- Connected → `statusColors.successful`
- Connecting → `statusColors.running`
- Error → `statusColors.error`
- Disconnected → `statusColors.disconnected`

The composable signature should be:

```kotlin
@Composable
fun McpServerCard(
    server: McpServerConfig,
    connectionState: McpConnectionState?,
    tools: List<McpToolUiState>,
    expanded: Boolean,
    disabledTools: Set<String>,
    onToggleExpand: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleReadOnly: (Boolean) -> Unit,
    onToggleTool: (String, Boolean) -> Unit,
    onRefresh: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
)
```

testTags: `card_mcp_server_{label}`, `switch_mcp_server_{label}`, `switch_mcp_tool_{name}`, `button_mcp_refresh_{label}`, `button_mcp_remove_{label}`

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/McpServerCard.kt
git commit -m "Add McpServerCard expandable composable (#196)"
```

---

### Task 9: AddMcpServerSheet Composable

**Files:**
- Create: `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/AddMcpServerSheet.kt`

- [ ] **Step 1: Create AddMcpServerSheet.kt**

`ModalBottomSheet` with name, URL, toolset fields. Reference Kai's `AddMcpServerDialog` for layout (but without the headers/popular servers — those are out of scope).

Signature:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMcpServerSheet(
    onDismiss: () -> Unit,
    onAdd: (url: String, label: String, toolset: String?) -> Unit
)
```

Content: Three `OutlinedTextField`s (name, URL, toolset), Add/Cancel buttons. Add button disabled when name or URL blank. Local `remember` state for field values. testTag: `button_add_mcp_server` on the Add button.

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/AddMcpServerSheet.kt
git commit -m "Add AddMcpServerSheet bottom sheet composable (#196)"
```

---

### Task 10: LocalToolsSection Composable

**Files:**
- Create: `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/LocalToolsSection.kt`

- [ ] **Step 1: Create LocalToolsSection.kt**

Category-grouped collapsible sections. Category headers are clickable Rows (not Cards). Each expanded section shows tools with name + description + toggle.

Signature:

```kotlin
@Composable
fun LocalToolsSection(
    tools: List<LocalToolUiState>,
    expandedCategories: Set<String>,
    onToggleCategory: (String) -> Unit,
    onToggleTool: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
)
```

Internal structure:
- Group `tools` by `category` using `groupBy`
- Category display names from string resources (map `"JOBS"` → `R.string.tools_category_jobs`)
- Category order: JOBS, INVENTORY, MONITORING, USERS, SECURITY, CONFIGURATION, EDA, PLATFORM
- Each category: clickable Row with name + count badge + chevron, `AnimatedVisibility` for tool list
- testTags: `section_category_{name}`, `switch_local_tool_{name}`

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/LocalToolsSection.kt
git commit -m "Add LocalToolsSection composable with category grouping (#196)"
```

---

### Task 11: Rewrite ToolsTab and Wire SettingsScreen

**Files:**
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/ToolsTab.kt`
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Rewrite ToolsTab.kt**

Replace entire content. New signature:

```kotlin
@Composable
fun ToolsTab(
    mcpEnabled: Boolean,
    mcpServers: List<McpServerConfig>,
    connections: Map<String, McpConnectionState>,
    mcpServerTools: Map<String, List<McpToolUiState>>,
    localTools: List<LocalToolUiState>,
    expandedMcpServers: Set<String>,
    expandedCategories: Set<String>,
    disabledTools: Set<String>,
    onToggleMcp: (Boolean) -> Unit,
    onAddMcpServer: (url: String, label: String, toolset: String?) -> Unit,
    onRemoveMcpServer: (url: String) -> Unit,
    onToggleReadOnly: (url: String, readOnly: Boolean) -> Unit,
    onToggleToolEnabled: (name: String, source: ToolSource, enabled: Boolean) -> Unit,
    onToggleExpandMcpServer: (label: String) -> Unit,
    onToggleExpandCategory: (category: String) -> Unit,
    onRefreshMcpServer: (label: String) -> Unit,
    modifier: Modifier = Modifier
)
```

Body: vertical scroll Column with:
1. MCP section header + toggle
2. `if (mcpEnabled)` → forEach server: `McpServerCard(...)` + "Add MCP Server" `OutlinedButton` (opens `AddMcpServerSheet` via local `remember` state)
3. `HorizontalDivider` with 8dp vertical margin
4. `LocalToolsSection(...)`

Delete the old `ConnectionStatusIcon` private composable — replaced by status dot in `McpServerCard`.

- [ ] **Step 2: Update SettingsScreen.kt callback wiring**

Update `SettingsContent` params — add new callbacks:

```kotlin
onToggleToolEnabled: (String, ToolSource, Boolean) -> Unit,
onToggleExpandMcpServer: (String) -> Unit,
onToggleExpandCategory: (String) -> Unit,
onRefreshMcpServer: (String) -> Unit,
```

Update the `SettingsScreen` composable to wire from ViewModel:

```kotlin
onToggleToolEnabled = { name, source, enabled -> viewModel.toggleToolEnabled(name, source, enabled) },
onToggleExpandMcpServer = { viewModel.toggleExpandMcpServer(it) },
onToggleExpandCategory = { viewModel.toggleExpandCategory(it) },
onRefreshMcpServer = { viewModel.refreshMcpServer(it) },
```

Update the `SettingsTab.Tools ->` branch to pass new state and callbacks to `ToolsTab(...)`.

- [ ] **Step 3: Build to verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/ToolsTab.kt \
       app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/settings/SettingsScreen.kt
git commit -m "Rewrite ToolsTab and wire new callbacks in SettingsScreen (#196)"
```

---

### Task 12: AssistantViewModel — Apply Disabled Tools

**Files:**
- Modify: `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/presentation/AssistantViewModel.kt`

- [ ] **Step 1: Add disabled tools application after ToolRouter registration**

In `sendMessage()`, after line 146 (`toolRouter.registerMcpTools(mcpTools)`), add:

```kotlin
val disabledTools = repository.getDisabledTools()
for (entry in disabledTools) {
    val colonIndex = entry.indexOf(':')
    if (colonIndex > 0) {
        val sourceStr = entry.substring(0, colonIndex)
        val toolName = entry.substring(colonIndex + 1)
        val source = try { ToolSource.valueOf(sourceStr) } catch (_: Exception) { continue }
        toolRouter.setToolEnabled(toolName, source, false)
    }
}
```

- [ ] **Step 2: Build to verify compilation**

Run: `./gradlew assembleDebug 2>&1 | tail -5`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/presentation/AssistantViewModel.kt
git commit -m "Apply persisted disabled tools to per-message ToolRouter (#196)"
```

---

### Task 13: UI Tests — Tools Tab

**Files:**
- Modify: `app/src/test/kotlin/io/github/leogallego/ansiblejane/ui/settings/SettingsScreenTest.kt`

- [ ] **Step 1: Add Tools tab UI tests**

The existing test already has `Tools tab shows MCP Servers section` which should still pass. Add new tests. These tests need the Koin module to provide `localTools` — update `testKoinModule` if needed, or construct the ViewModel with fakes in test setup.

Add tests for:

```kotlin
@Test
fun `Tools tab shows Local Tools section`() {
    fakeTokenManager.setInstances(listOf(instance1))
    setUpScreen()
    composeTestRule.onNodeWithText("Tools").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Local Tools").assertIsDisplayed()
}

@Test
fun `Tools tab categories collapsed by default`() {
    fakeTokenManager.setInstances(listOf(instance1))
    setUpScreen()
    composeTestRule.onNodeWithText("Tools").performClick()
    composeTestRule.waitForIdle()
    // Category headers visible but individual tool names not visible
    composeTestRule.onNodeWithText("Jobs").assertIsDisplayed()
    composeTestRule.onNodeWithText("list_job_templates").assertDoesNotExist()
}
```

Add additional tests from the spec's Testing section (Tier 2 table). Focus on the highest-value tests first: section visibility, expand/collapse, tool toggle state changes.

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "*SettingsScreenTest*" 2>&1 | tail -15`
Expected: All tests PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/test/kotlin/io/github/leogallego/ansiblejane/ui/settings/SettingsScreenTest.kt
git commit -m "Add UI tests for Tools tab redesign (#196)"
```

---

### Task 14: Screenshot Tests

**Files:**
- Create: `app/src/screenshotTest/kotlin/io/github/leogallego/ansiblejane/screens/ToolsTabScreenshots.kt`

- [ ] **Step 1: Create screenshot test file**

Follow the pattern in `SettingsScreenScreenshots.kt` — self-contained preview composables with hardcoded data (no ViewModel dependency).

Create preview composables for: collapsed MCP servers (light + dark), expanded MCP card with tools, local tools with one category expanded (light + dark), large font variant, empty state.

Each preview uses `@PreviewTest` + `@Preview(...)` annotations and renders via `AnsibleJaneTheme`.

- [ ] **Step 2: Generate reference screenshots**

Run: `./gradlew updateDebugScreenshotTest 2>&1 | tail -10`
Expected: Reference screenshots generated in `app/src/screenshotTest/reference/`

- [ ] **Step 3: Validate screenshots match**

Run: `./gradlew validateDebugScreenshotTest 2>&1 | tail -10`
Expected: PASS — screenshots match references

- [ ] **Step 4: Commit**

```bash
git add app/src/screenshotTest/kotlin/io/github/leogallego/ansiblejane/screens/ToolsTabScreenshots.kt \
       app/src/screenshotTest/reference/
git commit -m "Add screenshot tests for Tools tab (#196)"
```

---

### Task 15: Full Test Suite and Cleanup

**Files:** None new — validation only.

- [ ] **Step 1: Run full test suite**

Run: `./scripts/test-all.sh 2>&1 | tail -20`
Expected: All stages pass (assembleDebug, testDebugUnitTest, validateDebugScreenshotTest, lintDebug)

- [ ] **Step 2: Fix any failures**

If any tests fail, fix them before proceeding.

- [ ] **Step 3: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "Fix test issues from full suite run (#196)"
```

---

### Task 16: PR

- [ ] **Step 1: Push branch and create PR**

```bash
git push -u origin 196-tools-ui-redesign
gh pr create --title "Redesign MCP/Tools settings UI (#196)" --body "$(cat <<'EOF'
## Summary
- Expandable MCP server cards with status dot, per-tool enable/disable toggles, connection state feedback
- Category-grouped local tools section (8 categories, 61 tools) with collapsible sections
- Persisted disabled tools state via DataStore (survives app restarts)
- Add MCP Server bottom sheet dialog
- New StatusColors tokens (error, disconnected)
- ToolRouter.getCategoryForTool() companion function
- McpServerManager.getToolsForServer() and reconnectServer()
- AssistantViewModel reads disabled set and applies to per-message ToolRouter

## Test plan
- [ ] 6 new ToolRouter unit tests (getCategoryForTool)
- [ ] 5 new SettingsViewModel unit tests (tool toggle, expand, persist)
- [ ] UI tests for MCP cards, local tools, tab navigation
- [ ] 8 screenshot test variants (light/dark, expanded, large font)
- [ ] Full test suite passes (scripts/test-all.sh)
- [ ] Manual: navigate to Settings > Tools, expand MCP cards, toggle tools, verify state persists after app restart

Closes #196

Assisted-by: Claude Opus 4.6 <noreply@anthropic.com>
EOF
)"
```

Assisted-by: Claude Opus 4.6 <noreply@anthropic.com>

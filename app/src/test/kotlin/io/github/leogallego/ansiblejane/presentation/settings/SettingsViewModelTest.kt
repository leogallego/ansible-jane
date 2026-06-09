package io.github.leogallego.ansiblejane.presentation.settings

import app.cash.turbine.test
import io.github.leogallego.ansiblejane.MainDispatcherRule
import io.github.leogallego.ansiblejane.fakes.FakeAapApiProvider
import io.github.leogallego.ansiblejane.fakes.FakeAssistantRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.fakes.FakeUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.assistant.tools.LocalTool
import io.github.leogallego.ansiblejane.assistant.tools.ToolResult
import io.github.leogallego.ansiblejane.assistant.tools.ToolSpec
import io.github.leogallego.ansiblejane.assistant.tools.ToolSource
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeTokenManager: FakeTokenManager
    private lateinit var fakeApiProvider: FakeAapApiProvider
    private lateinit var fakeUserPreferences: FakeUserPreferencesRepository
    private lateinit var fakeAssistantRepo: FakeAssistantRepository
    private lateinit var mcpServerManager: McpServerManager
    private val httpClient = HttpClient(MockEngine) {
        engine { addHandler { respond("{}") } }
    }
    private val json = Json { ignoreUnknownKeys = true }

    private val instance1 = AapInstance(
        id = "inst-1",
        baseUrl = "https://aap1.example.com",
        token = "token-1",
        alias = "Production"
    )

    private val instance2 = AapInstance(
        id = "inst-2",
        baseUrl = "https://aap2.example.com",
        token = "token-2",
        alias = "Staging"
    )

    @Before
    fun setup() {
        fakeTokenManager = FakeTokenManager()
        fakeApiProvider = FakeAapApiProvider()
        fakeUserPreferences = FakeUserPreferencesRepository()
        fakeAssistantRepo = FakeAssistantRepository()
        mcpServerManager = McpServerManager(
            ktorClientFactory = { _, _ ->
                HttpClient(MockEngine) { engine { addHandler { respond("") } } }
            }
        )
    }

    private fun fakeLocalTool(name: String) = object : LocalTool {
        override val spec = ToolSpec(name, "Description of $name", JsonObject(emptyMap()))
        override val isDestructive = false
        override suspend fun execute(args: JsonObject) = ToolResult(success = true)
    }

    private fun createViewModel(localTools: List<LocalTool> = emptyList()) = SettingsViewModel(
        tokenManager = fakeTokenManager,
        apiProvider = fakeApiProvider,
        userPreferences = fakeUserPreferences,
        assistantRepository = fakeAssistantRepo,
        mcpServerManager = mcpServerManager,
        manifestRepository = io.github.leogallego.ansiblejane.fakes.FakeToolManifestRepository(),
        instanceDiscovery = io.github.leogallego.ansiblejane.network.InstanceDiscovery(json),
        httpClient = httpClient,
        json = json,
        localTools = localTools
    )

    @Test
    fun `init emits Ready with instances from TokenManager`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))

        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Ready)
        val ready = state as SettingsUiState.Ready
        assertEquals(2, ready.instances.size)
        assertEquals("Production", ready.instances[0].alias)
        assertEquals("Staging", ready.instances[1].alias)
    }

    @Test
    fun `init with empty instances emits Ready with empty list`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Ready)
        val ready = state as SettingsUiState.Ready
        assertTrue(ready.instances.isEmpty())
        assertNull(ready.selectedInstance)
    }

    @Test
    fun `init sets active instance as selectedInstance`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        fakeTokenManager.setActiveInstanceDirect(instance2)

        val viewModel = createViewModel()

        val state = viewModel.uiState.value as SettingsUiState.Ready
        assertEquals("inst-2", state.selectedInstance?.id)
    }

    @Test
    fun `switchInstance updates active instance`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem() as SettingsUiState.Ready
            assertEquals("inst-1", initial.selectedInstance?.id)

            viewModel.switchInstance("inst-2")

            val updated = awaitItem() as SettingsUiState.Ready
            assertEquals("inst-2", updated.selectedInstance?.id)
        }
    }

    @Test
    fun `removeInstance removes instance and evicts from API provider`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem() as SettingsUiState.Ready
            assertEquals(2, initial.instances.size)

            viewModel.removeInstance("inst-1")

            val updated = awaitItem() as SettingsUiState.Ready
            assertEquals(1, updated.instances.size)
            assertEquals("inst-2", updated.instances[0].id)
            assertEquals(listOf("inst-1"), fakeApiProvider.evictedInstances)
        }
    }

    @Test
    fun `removeInstance with active instance switches to remaining`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.removeInstance("inst-1")

            val updated = awaitItem() as SettingsUiState.Ready
            assertEquals("inst-2", updated.selectedInstance?.id)
        }
    }

    @Test
    fun `showInstanceDetails sets selectedInstanceForDetails`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        val viewModel = createViewModel()

        val initial = viewModel.uiState.value as SettingsUiState.Ready
        assertNull(initial.selectedInstanceForDetails)

        viewModel.showInstanceDetails("inst-2")

        val updated = viewModel.uiState.value as SettingsUiState.Ready
        assertEquals("inst-2", updated.selectedInstanceForDetails?.id)
        assertEquals("Staging", updated.selectedInstanceForDetails?.alias)
    }

    @Test
    fun `showInstanceDetails with unknown ID sets null`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))
        val viewModel = createViewModel()

        viewModel.showInstanceDetails("unknown-id")

        val state = viewModel.uiState.value as SettingsUiState.Ready
        assertNull(state.selectedInstanceForDetails)
    }

    @Test
    fun `dismissDetails clears selectedInstanceForDetails`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))
        val viewModel = createViewModel()

        viewModel.showInstanceDetails("inst-1")
        val withDetails = viewModel.uiState.value as SettingsUiState.Ready
        assertEquals("inst-1", withDetails.selectedInstanceForDetails?.id)

        viewModel.dismissDetails()
        val dismissed = viewModel.uiState.value as SettingsUiState.Ready
        assertNull(dismissed.selectedInstanceForDetails)
    }

    @Test
    fun `dismissDetails when no details shown is a no-op`() = runTest {
        val viewModel = createViewModel()

        val before = viewModel.uiState.value
        viewModel.dismissDetails()
        val after = viewModel.uiState.value

        assertTrue(before is SettingsUiState.Ready)
        assertTrue(after is SettingsUiState.Ready)
        assertNull((after as SettingsUiState.Ready).selectedInstanceForDetails)
    }

    // --- Tool management ---

    @Test
    fun `init SHOULD load pre-existing disabled tools from repository`() = runTest {
        fakeAssistantRepo.savedDisabledTools = setOf("LOCAL:list_hosts", "MCP:controller.jobs_list")
        val tools = listOf(fakeLocalTool("list_hosts"), fakeLocalTool("list_inventories"))
        val viewModel = createViewModel(localTools = tools)

        viewModel.uiState.test {
            val state = awaitItem() as SettingsUiState.Ready
            assertEquals(setOf("LOCAL:list_hosts", "MCP:controller.jobs_list"), state.disabledTools)
            val hostTool = state.localTools.find { it.name == "list_hosts" }
            assertFalse("list_hosts should be disabled", hostTool?.isEnabled ?: true)
            val invTool = state.localTools.find { it.name == "list_inventories" }
            assertTrue("list_inventories should be enabled", invTool?.isEnabled ?: false)
        }
    }

    @Test
    fun `toggleToolEnabled SHOULD disable a local tool and persist`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.toggleToolEnabled("list_hosts", ToolSource.LOCAL, false)
            val state = awaitItem() as SettingsUiState.Ready
            assertTrue("LOCAL:list_hosts" in state.disabledTools)
            assertTrue("LOCAL:list_hosts" in fakeAssistantRepo.savedDisabledTools)
        }
    }

    @Test
    fun `toggleToolEnabled SHOULD re-enable a previously disabled tool`() = runTest {
        fakeAssistantRepo.savedDisabledTools = setOf("LOCAL:list_hosts")
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.toggleToolEnabled("list_hosts", ToolSource.LOCAL, true)
            val state = awaitItem() as SettingsUiState.Ready
            assertFalse("LOCAL:list_hosts" in state.disabledTools)
        }
    }

    @Test
    fun `toggleToolEnabled SHOULD use MCP prefix for MCP tools`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.toggleToolEnabled("controller.hosts_list", ToolSource.MCP, false)
            val state = awaitItem() as SettingsUiState.Ready
            assertTrue("MCP:controller.hosts_list" in state.disabledTools)
        }
    }

    @Test
    fun `toggleExpandCategory SHOULD toggle category in expandedCategories`() = runTest {
        val viewModel = createViewModel()
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
        val viewModel = createViewModel()
        viewModel.uiState.test {
            skipItems(1)
            viewModel.toggleExpandMcpServer("Jobs")
            val expanded = awaitItem() as SettingsUiState.Ready
            assertTrue("Jobs" in expanded.expandedMcpServers)
        }
    }
}

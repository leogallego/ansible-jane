package io.github.leogallego.ansiblejane.presentation.settings

import app.cash.turbine.test
import io.github.leogallego.ansiblejane.MainDispatcherRule
import io.github.leogallego.ansiblejane.fakes.FakeAapApiProvider
import io.github.leogallego.ansiblejane.fakes.FakeAssistantRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.fakes.FakeUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
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
    private val httpClient = OkHttpClient()
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
            httpClientFactory = { _, _ -> OkHttpClient() },
            json = json
        )
    }

    private fun createViewModel() = SettingsViewModel(
        tokenManager = fakeTokenManager,
        apiProvider = fakeApiProvider,
        userPreferences = fakeUserPreferences,
        assistantRepository = fakeAssistantRepo,
        mcpServerManager = mcpServerManager,
        instanceDiscovery = io.github.leogallego.ansiblejane.network.InstanceDiscovery(json),
        httpClient = httpClient,
        json = json
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
}

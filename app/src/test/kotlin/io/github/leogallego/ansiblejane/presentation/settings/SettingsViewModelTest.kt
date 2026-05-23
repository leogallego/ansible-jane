package io.github.leogallego.ansiblejane.presentation.settings

import app.cash.turbine.test
import io.github.leogallego.ansiblejane.MainDispatcherRule
import io.github.leogallego.ansiblejane.fakes.FakeAapApiProvider
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.fakes.FakeUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.AapInstance
import kotlinx.coroutines.test.runTest
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
    }

    @Test
    fun `init emits Success with instances from TokenManager`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))

        val viewModel = SettingsViewModel(fakeTokenManager, fakeApiProvider, fakeUserPreferences)

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Success)
        val success = state as SettingsUiState.Success
        assertEquals(2, success.instances.size)
        assertEquals("Production", success.instances[0].alias)
        assertEquals("Staging", success.instances[1].alias)
    }

    @Test
    fun `init with empty instances emits Success with empty list`() = runTest {
        val viewModel = SettingsViewModel(fakeTokenManager, fakeApiProvider, fakeUserPreferences)

        val state = viewModel.uiState.value
        assertTrue(state is SettingsUiState.Success)
        val success = state as SettingsUiState.Success
        assertTrue(success.instances.isEmpty())
        assertNull(success.selectedInstance)
    }

    @Test
    fun `init sets active instance as selectedInstance`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        fakeTokenManager.setActiveInstanceDirect(instance2)

        val viewModel = SettingsViewModel(fakeTokenManager, fakeApiProvider, fakeUserPreferences)

        val state = viewModel.uiState.value as SettingsUiState.Success
        assertEquals("inst-2", state.selectedInstance?.id)
    }

    @Test
    fun `switchInstance updates active instance`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        val viewModel = SettingsViewModel(fakeTokenManager, fakeApiProvider, fakeUserPreferences)

        viewModel.uiState.test {
            val initial = awaitItem() as SettingsUiState.Success
            assertEquals("inst-1", initial.selectedInstance?.id)

            viewModel.switchInstance("inst-2")

            val updated = awaitItem() as SettingsUiState.Success
            assertEquals("inst-2", updated.selectedInstance?.id)
        }
    }

    @Test
    fun `removeInstance removes instance and evicts from API provider`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        val viewModel = SettingsViewModel(fakeTokenManager, fakeApiProvider, fakeUserPreferences)

        viewModel.uiState.test {
            val initial = awaitItem() as SettingsUiState.Success
            assertEquals(2, initial.instances.size)

            viewModel.removeInstance("inst-1")

            val updated = awaitItem() as SettingsUiState.Success
            assertEquals(1, updated.instances.size)
            assertEquals("inst-2", updated.instances[0].id)
            assertEquals(listOf("inst-1"), fakeApiProvider.evictedInstances)
        }
    }

    @Test
    fun `removeInstance with active instance switches to remaining`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        val viewModel = SettingsViewModel(fakeTokenManager, fakeApiProvider, fakeUserPreferences)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.removeInstance("inst-1")

            val updated = awaitItem() as SettingsUiState.Success
            assertEquals("inst-2", updated.selectedInstance?.id)
        }
    }

    @Test
    fun `showInstanceDetails sets selectedInstanceForDetails`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        val viewModel = SettingsViewModel(fakeTokenManager, fakeApiProvider, fakeUserPreferences)

        // Wait for init to complete
        val initial = viewModel.uiState.value as SettingsUiState.Success
        assertNull(initial.selectedInstanceForDetails)

        viewModel.showInstanceDetails("inst-2")

        val updated = viewModel.uiState.value as SettingsUiState.Success
        assertEquals("inst-2", updated.selectedInstanceForDetails?.id)
        assertEquals("Staging", updated.selectedInstanceForDetails?.alias)
    }

    @Test
    fun `showInstanceDetails with unknown ID sets null`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))
        val viewModel = SettingsViewModel(fakeTokenManager, fakeApiProvider, fakeUserPreferences)

        viewModel.showInstanceDetails("unknown-id")

        val state = viewModel.uiState.value as SettingsUiState.Success
        assertNull(state.selectedInstanceForDetails)
    }

    @Test
    fun `dismissDetails clears selectedInstanceForDetails`() = runTest {
        fakeTokenManager.setInstances(listOf(instance1))
        val viewModel = SettingsViewModel(fakeTokenManager, fakeApiProvider, fakeUserPreferences)

        viewModel.showInstanceDetails("inst-1")
        val withDetails = viewModel.uiState.value as SettingsUiState.Success
        assertEquals("inst-1", withDetails.selectedInstanceForDetails?.id)

        viewModel.dismissDetails()
        val dismissed = viewModel.uiState.value as SettingsUiState.Success
        assertNull(dismissed.selectedInstanceForDetails)
    }

    @Test
    fun `dismissDetails when no details shown is a no-op`() = runTest {
        val viewModel = SettingsViewModel(fakeTokenManager, fakeApiProvider, fakeUserPreferences)

        val before = viewModel.uiState.value
        viewModel.dismissDetails()
        val after = viewModel.uiState.value

        assertTrue(before is SettingsUiState.Success)
        assertTrue(after is SettingsUiState.Success)
        assertNull((after as SettingsUiState.Success).selectedInstanceForDetails)
    }
}

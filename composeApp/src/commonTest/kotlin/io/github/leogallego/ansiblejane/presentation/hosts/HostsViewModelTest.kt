package io.github.leogallego.ansiblejane.presentation.hosts

import app.cash.turbine.test
import io.github.leogallego.ansiblejane.TestData
import io.github.leogallego.ansiblejane.fakes.FakeHostRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
@OptIn(ExperimentalCoroutinesApi::class)
class HostsViewModelTest {

    private lateinit var fakeRepo: FakeHostRepository
    private lateinit var fakeTokenManager: FakeTokenManager

    @BeforeTest
    fun setup() {
        setupMainDispatcher()
        fakeRepo = FakeHostRepository()
        fakeTokenManager = FakeTokenManager()
    }

    @AfterTest
    fun cleanup() {
        tearDownMainDispatcher()
    }

    private fun createViewModel(): HostsViewModel {
        return HostsViewModel(fakeRepo, fakeTokenManager)
    }

    @Test
    fun `initial state is Loading when no active instance`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(HostsUiState.Loading, awaitItem())
        }
    }

    @Test
    fun `loads hosts when active instance is set`() = runTest {
        fakeRepo.hosts = TestData.sampleHosts

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is HostsUiState.Success)
            val success = state as HostsUiState.Success
            assertEquals(3, success.hosts.size)
            assertEquals("host-1.example.com", success.hosts[0].name)
            assertFalse(success.hasMore)
        }
    }

    @Test
    fun `shows error state when loading fails`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Connection refused")

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is HostsUiState.Error)
            val error = state as HostsUiState.Error
            assertTrue(error.error is AppError.Unknown)
        }
    }

    @Test
    fun `shows empty state when no hosts returned`() = runTest {
        fakeRepo.hosts = emptyList()

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is HostsUiState.Empty)
            assertEquals("No hosts found", (state as HostsUiState.Empty).message)
        }
    }

    @Test
    fun `loadMore appends hosts when hasMore is true`() = runTest {
        fakeRepo.hosts = TestData.sampleHosts
        fakeRepo.hasMore = true

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val firstPage = awaitItem()
            assertTrue(firstPage is HostsUiState.Success)
            assertEquals(3, (firstPage as HostsUiState.Success).hosts.size)
            assertTrue(firstPage.hasMore)

            // Set up page 2 data
            fakeRepo.hosts = listOf(TestData.createHost(4))
            fakeRepo.hasMore = false

            viewModel.loadMore()

            val loadingMore = awaitItem()
            assertTrue(loadingMore is HostsUiState.Success)
            assertTrue((loadingMore as HostsUiState.Success).isLoadingMore)

            val secondPage = awaitItem()
            assertTrue(secondPage is HostsUiState.Success)
            assertEquals(4, (secondPage as HostsUiState.Success).hosts.size)
            assertFalse(secondPage.hasMore)
        }
    }

    @Test
    fun `loadMore does nothing when hasMore is false`() = runTest {
        fakeRepo.hosts = TestData.sampleHosts

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is HostsUiState.Success)
            assertFalse((state as HostsUiState.Success).hasMore)

            viewModel.loadMore()
            expectNoEvents()
        }
    }

    @Test
    fun `search triggers reload with filtered results`() = runTest {
        fakeRepo.hosts = TestData.sampleHosts

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        // Wait for initial load to complete
        val initial = viewModel.uiState.value
        assertTrue(initial is HostsUiState.Success)
        assertEquals(3, (initial as HostsUiState.Success).hosts.size)

        // Set up filtered results and trigger search
        fakeRepo.hosts = listOf(TestData.createHost(1, "webserver-01"))
        viewModel.search("webserver")

        // Advance past the 300ms debounce delay
        advanceUntilIdle()

        val result = viewModel.uiState.value
        assertTrue(result is HostsUiState.Success)
        assertEquals(1, (result as HostsUiState.Success).hosts.size)
        assertEquals("webserver-01", result.hosts[0].name)
    }

    @Test
    fun `search with blank query clears search filter`() = runTest {
        fakeRepo.hosts = TestData.sampleHosts

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        // Wait for initial load
        assertTrue(viewModel.uiState.value is HostsUiState.Success)

        // Search with blank should reload all hosts
        viewModel.search("")
        advanceUntilIdle()

        val result = viewModel.uiState.value
        assertTrue(result is HostsUiState.Success)
        assertEquals(3, (result as HostsUiState.Success).hosts.size)
    }

    @Test
    fun `refresh reloads hosts from page 1`() = runTest {
        fakeRepo.hosts = TestData.sampleHosts

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is HostsUiState.Success)

            fakeRepo.hosts = listOf(TestData.createHost(99, "refreshed-host"))
            viewModel.refresh()

            val refreshed = awaitItem()
            assertTrue(refreshed is HostsUiState.Success)
            assertEquals(1, (refreshed as HostsUiState.Success).hosts.size)
            assertEquals("refreshed-host", refreshed.hosts[0].name)
        }
    }

    @Test
    fun `reloads when active instance changes`() = runTest {
        fakeRepo.hosts = TestData.sampleHosts

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is HostsUiState.Success)

            fakeRepo.hosts = listOf(TestData.createHost(10, "new-instance-host"))
            fakeTokenManager.setInstances(listOf(TestData.testInstance2))

            val loading = awaitItem()
            assertEquals(HostsUiState.Loading, loading)

            val newState = awaitItem()
            assertTrue(newState is HostsUiState.Success)
            assertEquals(
                "new-instance-host",
                (newState as HostsUiState.Success).hosts[0].name
            )
        }
    }
}

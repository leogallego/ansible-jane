package com.example.aapremote.presentation.hosts

import app.cash.turbine.test
import com.example.aapremote.MainDispatcherRule
import com.example.aapremote.TestData
import com.example.aapremote.fakes.FakeHostRepository
import com.example.aapremote.model.AppError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
@OptIn(ExperimentalCoroutinesApi::class)
class InventoryHostsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepo: FakeHostRepository
    private lateinit var viewModel: InventoryHostsViewModel

    @Before
    fun setup() {
        fakeRepo = FakeHostRepository()
        viewModel = InventoryHostsViewModel(fakeRepo)
    }

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel.uiState.test {
            assertEquals(InventoryHostsUiState.Loading, awaitItem())
        }
    }

    @Test
    fun `loadHosts shows success with hosts`() = runTest {
        fakeRepo.inventoryHosts = TestData.sampleHosts

        viewModel.uiState.test {
            assertEquals(InventoryHostsUiState.Loading, awaitItem())

            viewModel.loadHosts(inventoryId = 1)

            val state = awaitItem()
            assertTrue(state is InventoryHostsUiState.Success)
            val success = state as InventoryHostsUiState.Success
            assertEquals(3, success.hosts.size)
            assertEquals("host-1.example.com", success.hosts[0].name)
            assertFalse(success.hasMore)
        }
    }

    @Test
    fun `loadHosts shows error when repository fails`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("API error")

        viewModel.uiState.test {
            assertEquals(InventoryHostsUiState.Loading, awaitItem())

            viewModel.loadHosts(inventoryId = 1)

            val state = awaitItem()
            assertTrue(state is InventoryHostsUiState.Error)
            val error = state as InventoryHostsUiState.Error
            assertTrue(error.error is AppError.Unknown)
        }
    }

    @Test
    fun `loadHosts shows empty state when no hosts returned`() = runTest {
        fakeRepo.inventoryHosts = emptyList()

        viewModel.uiState.test {
            assertEquals(InventoryHostsUiState.Loading, awaitItem())

            viewModel.loadHosts(inventoryId = 1)

            val state = awaitItem()
            assertTrue(state is InventoryHostsUiState.Empty)
            assertEquals("No hosts found", (state as InventoryHostsUiState.Empty).message)
        }
    }

    @Test
    fun `loadMore appends hosts when hasMore is true`() = runTest {
        fakeRepo.inventoryHosts = TestData.sampleHosts
        fakeRepo.inventoryHostsHasMore = true

        viewModel.uiState.test {
            assertEquals(InventoryHostsUiState.Loading, awaitItem())

            viewModel.loadHosts(inventoryId = 1)

            val firstPage = awaitItem()
            assertTrue(firstPage is InventoryHostsUiState.Success)
            assertEquals(3, (firstPage as InventoryHostsUiState.Success).hosts.size)
            assertTrue(firstPage.hasMore)

            // Set up page 2 data
            fakeRepo.inventoryHosts = listOf(TestData.createHost(4))
            fakeRepo.inventoryHostsHasMore = false

            viewModel.loadMore()

            val loadingMore = awaitItem()
            assertTrue(loadingMore is InventoryHostsUiState.Success)
            assertTrue((loadingMore as InventoryHostsUiState.Success).isLoadingMore)

            val secondPage = awaitItem()
            assertTrue(secondPage is InventoryHostsUiState.Success)
            assertEquals(4, (secondPage as InventoryHostsUiState.Success).hosts.size)
            assertFalse(secondPage.hasMore)
        }
    }

    @Test
    fun `loadMore does nothing when hasMore is false`() = runTest {
        fakeRepo.inventoryHosts = TestData.sampleHosts

        viewModel.uiState.test {
            assertEquals(InventoryHostsUiState.Loading, awaitItem())

            viewModel.loadHosts(inventoryId = 1)

            val state = awaitItem()
            assertTrue(state is InventoryHostsUiState.Success)
            assertFalse((state as InventoryHostsUiState.Success).hasMore)

            viewModel.loadMore()
            expectNoEvents()
        }
    }

    @Test
    fun `search triggers reload with filtered results`() = runTest {
        fakeRepo.inventoryHosts = TestData.sampleHosts

        viewModel.loadHosts(inventoryId = 1)

        // Wait for initial load to complete
        val initial = viewModel.uiState.value
        assertTrue(initial is InventoryHostsUiState.Success)
        assertEquals(3, (initial as InventoryHostsUiState.Success).hosts.size)

        // Set up filtered results and trigger search
        fakeRepo.inventoryHosts = listOf(TestData.createHost(1, "searched-host"))
        viewModel.search("searched")

        // Advance past the 300ms debounce delay
        advanceUntilIdle()

        val result = viewModel.uiState.value
        assertTrue(result is InventoryHostsUiState.Success)
        assertEquals(1, (result as InventoryHostsUiState.Success).hosts.size)
        assertEquals("searched-host", result.hosts[0].name)
    }

    @Test
    fun `search with blank query clears search filter`() = runTest {
        fakeRepo.inventoryHosts = TestData.sampleHosts

        viewModel.loadHosts(inventoryId = 1)

        // Wait for initial load
        assertTrue(viewModel.uiState.value is InventoryHostsUiState.Success)

        // Search with blank should reload all hosts
        viewModel.search("")
        advanceUntilIdle()

        val result = viewModel.uiState.value
        assertTrue(result is InventoryHostsUiState.Success)
        assertEquals(3, (result as InventoryHostsUiState.Success).hosts.size)
    }

    @Test
    fun `refresh reloads from page 1`() = runTest {
        fakeRepo.inventoryHosts = TestData.sampleHosts

        viewModel.uiState.test {
            assertEquals(InventoryHostsUiState.Loading, awaitItem())

            viewModel.loadHosts(inventoryId = 1)
            val initial = awaitItem()
            assertTrue(initial is InventoryHostsUiState.Success)

            fakeRepo.inventoryHosts = listOf(TestData.createHost(99, "refreshed"))
            viewModel.refresh()

            val refreshed = awaitItem()
            assertTrue(refreshed is InventoryHostsUiState.Success)
            assertEquals(1, (refreshed as InventoryHostsUiState.Success).hosts.size)
            assertEquals("refreshed", refreshed.hosts[0].name)
        }
    }

    @Test
    fun `loadHosts with different inventoryId resets state`() = runTest {
        fakeRepo.inventoryHosts = TestData.sampleHosts

        viewModel.uiState.test {
            assertEquals(InventoryHostsUiState.Loading, awaitItem())

            viewModel.loadHosts(inventoryId = 1)
            val first = awaitItem()
            assertTrue(first is InventoryHostsUiState.Success)

            fakeRepo.inventoryHosts = listOf(TestData.createHost(10, "inv2-host"))
            viewModel.loadHosts(inventoryId = 2)

            val loading = awaitItem()
            assertEquals(InventoryHostsUiState.Loading, loading)

            val second = awaitItem()
            assertTrue(second is InventoryHostsUiState.Success)
            assertEquals(1, (second as InventoryHostsUiState.Success).hosts.size)
            assertEquals("inv2-host", second.hosts[0].name)
        }
    }
}

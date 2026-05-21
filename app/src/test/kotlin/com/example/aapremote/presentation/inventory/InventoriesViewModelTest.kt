package com.example.aapremote.presentation.inventory

import app.cash.turbine.test
import com.example.aapremote.MainDispatcherRule
import com.example.aapremote.TestData
import com.example.aapremote.fakes.FakeInventoryRepository
import com.example.aapremote.fakes.FakeTokenManager
import com.example.aapremote.model.AppError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
class InventoriesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepo: FakeInventoryRepository
    private lateinit var fakeTokenManager: FakeTokenManager

    @Before
    fun setup() {
        fakeRepo = FakeInventoryRepository()
        fakeTokenManager = FakeTokenManager()
    }

    private fun createViewModel(): InventoriesViewModel {
        return InventoriesViewModel(fakeRepo, fakeTokenManager)
    }

    @Test
    fun `initial state is Loading when no active instance`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(InventoriesUiState.Loading, awaitItem())
        }
    }

    @Test
    fun `loads inventories when active instance is set`() = runTest {
        fakeRepo.inventories = TestData.sampleInventories

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InventoriesUiState.Success)
            val success = state as InventoriesUiState.Success
            assertEquals(3, success.inventories.size)
            assertEquals("Inventory 1", success.inventories[0].name)
            assertFalse(success.hasMore)
        }
    }

    @Test
    fun `shows error state when loading fails`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Network error")

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InventoriesUiState.Error)
            val error = state as InventoriesUiState.Error
            assertTrue(error.error is AppError.Unknown)
        }
    }

    @Test
    fun `shows empty state when no inventories returned`() = runTest {
        fakeRepo.inventories = emptyList()

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InventoriesUiState.Empty)
            assertEquals("No inventories found", (state as InventoriesUiState.Empty).message)
        }
    }

    @Test
    fun `loadMore appends inventories when hasMore is true`() = runTest {
        val page1 = listOf(TestData.createInventory(1), TestData.createInventory(2))
        val page2 = listOf(TestData.createInventory(3))

        fakeRepo.setResultForPage { page ->
            when (page) {
                1 -> Result.success(
                    com.example.aapremote.data.InventoryListResult(page1, hasMore = true, totalCount = 3)
                )
                2 -> Result.success(
                    com.example.aapremote.data.InventoryListResult(page2, hasMore = false, totalCount = 3)
                )
                else -> Result.failure(RuntimeException("Unexpected page $page"))
            }
        }

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val firstPage = awaitItem()
            assertTrue(firstPage is InventoriesUiState.Success)
            assertEquals(2, (firstPage as InventoriesUiState.Success).inventories.size)
            assertTrue(firstPage.hasMore)

            viewModel.loadMore()

            val loadingMore = awaitItem()
            assertTrue(loadingMore is InventoriesUiState.Success)
            assertTrue((loadingMore as InventoriesUiState.Success).isLoadingMore)

            val secondPage = awaitItem()
            assertTrue(secondPage is InventoriesUiState.Success)
            assertEquals(3, (secondPage as InventoriesUiState.Success).inventories.size)
            assertFalse(secondPage.hasMore)
            assertFalse(secondPage.isLoadingMore)
        }
    }

    @Test
    fun `loadMore does nothing when hasMore is false`() = runTest {
        fakeRepo.inventories = TestData.sampleInventories
        fakeRepo.hasMore = false

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is InventoriesUiState.Success)
            assertFalse((state as InventoriesUiState.Success).hasMore)

            viewModel.loadMore()
            expectNoEvents()
        }
    }

    @Test
    fun `refresh reloads inventories from page 1`() = runTest {
        fakeRepo.inventories = TestData.sampleInventories

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is InventoriesUiState.Success)
            assertEquals(3, (initial as InventoriesUiState.Success).inventories.size)

            fakeRepo.inventories = listOf(TestData.createInventory(99, "Updated"))
            viewModel.refresh()

            val refreshed = awaitItem()
            assertTrue(refreshed is InventoriesUiState.Success)
            assertEquals(1, (refreshed as InventoriesUiState.Success).inventories.size)
            assertEquals("Updated", refreshed.inventories[0].name)
        }
    }

    @Test
    fun `reloads when active instance changes`() = runTest {
        fakeRepo.inventories = TestData.sampleInventories

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is InventoriesUiState.Success)

            fakeRepo.inventories = listOf(TestData.createInventory(10, "New Instance Inventory"))
            fakeTokenManager.setInstances(listOf(TestData.testInstance2))

            // Should see Loading then Success with new data
            val loading = awaitItem()
            assertEquals(InventoriesUiState.Loading, loading)

            val newState = awaitItem()
            assertTrue(newState is InventoriesUiState.Success)
            assertEquals(
                "New Instance Inventory",
                (newState as InventoriesUiState.Success).inventories[0].name
            )
        }
    }
}

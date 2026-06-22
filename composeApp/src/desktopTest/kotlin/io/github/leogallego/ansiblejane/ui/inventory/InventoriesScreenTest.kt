package io.github.leogallego.ansiblejane.ui.inventory

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import io.github.leogallego.ansiblejane.TestData
import io.github.leogallego.ansiblejane.fakes.FakeInventoryRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.presentation.inventory.InventoriesViewModel
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class InventoriesScreenTest {

    private val fakeRepo = FakeInventoryRepository()
    private val fakeTokenManager = FakeTokenManager()

    @BeforeTest
    fun setUp() {
        setupMainDispatcher()
    }

    @AfterTest
    fun tearDown() {
        tearDownMainDispatcher()
    }

    @Test
    fun displays_inventory_list_with_names() = runComposeUiTest {
        fakeRepo.inventories = TestData.sampleInventories
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = InventoriesViewModel(fakeRepo, fakeTokenManager)

        setContent {
            MaterialTheme {
                InventoriesScreen(viewModel = viewModel)
            }
        }
        waitForIdle()

        onNodeWithText("Inventory 1").assertIsDisplayed()
        onNodeWithText("Inventory 2").assertIsDisplayed()
    }

    @Test
    fun shows_empty_state_when_no_inventories() = runComposeUiTest {
        fakeRepo.inventories = emptyList()
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = InventoriesViewModel(fakeRepo, fakeTokenManager)

        setContent {
            MaterialTheme {
                InventoriesScreen(viewModel = viewModel)
            }
        }
        waitForIdle()

        onNodeWithText("No inventories found").assertIsDisplayed()
    }

    @Test
    fun shows_error_with_retry_button() = runComposeUiTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Network error")
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = InventoriesViewModel(fakeRepo, fakeTokenManager)

        setContent {
            MaterialTheme {
                InventoriesScreen(viewModel = viewModel)
            }
        }
        waitForIdle()

        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun displays_host_count_chip() = runComposeUiTest {
        fakeRepo.inventories = listOf(TestData.createInventory(1, "Production"))
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = InventoriesViewModel(fakeRepo, fakeTokenManager)

        setContent {
            MaterialTheme {
                InventoriesScreen(viewModel = viewModel)
            }
        }
        waitForIdle()

        onNodeWithText("10 hosts").assertIsDisplayed()
    }
}

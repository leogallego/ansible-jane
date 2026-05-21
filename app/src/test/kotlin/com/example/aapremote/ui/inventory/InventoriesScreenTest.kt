package com.example.aapremote.ui.inventory

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.aapremote.MainDispatcherRule
import com.example.aapremote.TestData
import com.example.aapremote.fakes.FakeInventoryRepository
import com.example.aapremote.fakes.FakeTokenManager
import com.example.aapremote.presentation.inventory.InventoriesViewModel
import com.example.aapremote.ui.theme.AapRemoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InventoriesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepo = FakeInventoryRepository()
    private val fakeTokenManager = FakeTokenManager()

    private fun setUpScreen() {
        val viewModel = InventoriesViewModel(fakeRepo, fakeTokenManager)
        composeTestRule.setContent {
            AapRemoteTheme {
                InventoriesScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `displays inventory list with names`() {
        fakeRepo.inventories = TestData.sampleInventories
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("Inventory 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inventory 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Inventory 3").assertIsDisplayed()
    }

    @Test
    fun `shows empty state message when no inventories`() {
        fakeRepo.inventories = emptyList()
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("No inventories found").assertIsDisplayed()
    }

    @Test
    fun `shows error with retry button`() {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Network error")
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun `inventory items show kind chip`() {
        fakeRepo.inventories = listOf(TestData.createInventory(1, "Test Inv"))
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        // Default kind is "" which maps to "Regular"
        composeTestRule.onNodeWithText("Regular").assertIsDisplayed()
    }

    @Test
    fun `inventory items show host count`() {
        fakeRepo.inventories = listOf(TestData.createInventory(1, "Test Inv"))
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        // createInventory(1) sets totalHosts = 1 * 10 = 10
        composeTestRule.onNodeWithText("10 hosts").assertIsDisplayed()
    }
}

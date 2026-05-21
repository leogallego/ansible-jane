package com.example.aapremote.ui.hosts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.aapremote.MainDispatcherRule
import com.example.aapremote.TestData
import com.example.aapremote.fakes.FakeHostRepository
import com.example.aapremote.fakes.FakeTokenManager
import com.example.aapremote.presentation.hosts.HostsViewModel
import com.example.aapremote.ui.theme.AapRemoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HostsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepo = FakeHostRepository()
    private val fakeTokenManager = FakeTokenManager()

    private fun setUpScreen() {
        val viewModel = HostsViewModel(fakeRepo, fakeTokenManager)
        composeTestRule.setContent {
            AapRemoteTheme {
                HostsScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `displays host list with names`() {
        fakeRepo.hosts = TestData.sampleHosts
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("host-1.example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("host-2.example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("host-3.example.com").assertIsDisplayed()
    }

    @Test
    fun `shows search bar with placeholder`() {
        fakeRepo.hosts = TestData.sampleHosts
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("Search hosts...").assertIsDisplayed()
    }

    @Test
    fun `shows empty state message when no hosts`() {
        fakeRepo.hosts = emptyList()
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("No hosts found").assertIsDisplayed()
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
    fun `displays host description`() {
        fakeRepo.hosts = listOf(TestData.createHost(1, "myhost.example.com"))
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("myhost.example.com").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test host 1").assertIsDisplayed()
    }
}

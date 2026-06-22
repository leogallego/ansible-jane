package io.github.leogallego.ansiblejane.ui.hosts

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.runComposeUiTest
import io.github.leogallego.ansiblejane.TestData
import io.github.leogallego.ansiblejane.fakes.FakeHostRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.presentation.hosts.HostsViewModel
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class HostsScreenTest {

    private val fakeRepo = FakeHostRepository()
    private val fakeTokenManager = FakeTokenManager()

    @BeforeTest
    fun setUp() {
        setupMainDispatcher()
    }

    @AfterTest
    fun tearDown() {
        tearDownMainDispatcher()
    }

    private fun ComposeUiTest.setUpScreen(viewModel: HostsViewModel) {
        setContent { MaterialTheme { HostsScreen(viewModel = viewModel) } }
        waitForIdle()
    }

    @Test
    fun displays_host_list_with_names() = runComposeUiTest {
        fakeRepo.hosts = TestData.sampleHosts
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen(HostsViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("host-1.example.com").assertIsDisplayed()
        onNodeWithText("host-2.example.com").assertIsDisplayed()
        onNodeWithText("host-3.example.com").assertIsDisplayed()
    }

    @Test
    fun shows_search_bar_with_placeholder() = runComposeUiTest {
        fakeRepo.hosts = TestData.sampleHosts
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen(HostsViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("Search hosts...").assertIsDisplayed()
    }

    @Test
    fun shows_empty_state_when_no_hosts() = runComposeUiTest {
        fakeRepo.hosts = emptyList()
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen(HostsViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("No hosts found").assertIsDisplayed()
    }

    @Test
    fun shows_error_with_retry_button() = runComposeUiTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Network error")
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen(HostsViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun displays_host_description() = runComposeUiTest {
        fakeRepo.hosts = listOf(TestData.createHost(1, "myhost.example.com"))
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen(HostsViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("myhost.example.com").assertIsDisplayed()
        onNodeWithText("Test host 1").assertIsDisplayed()
    }
}

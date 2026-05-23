package io.github.leogallego.ansiblejane.ui.jobs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import io.github.leogallego.ansiblejane.MainDispatcherRule
import io.github.leogallego.ansiblejane.TestData
import io.github.leogallego.ansiblejane.fakes.FakeJobRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.model.JobStatus
import io.github.leogallego.ansiblejane.presentation.jobs.RecentJobsViewModel
import io.github.leogallego.ansiblejane.testJob
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecentJobsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepo = FakeJobRepository()
    private val fakeTokenManager = FakeTokenManager()

    private fun setUpScreen() {
        val viewModel = RecentJobsViewModel(fakeRepo, fakeTokenManager)
        composeTestRule.setContent {
            AnsibleJaneTheme {
                RecentJobsScreen(
                    onNavigateToJobStatus = {},
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `displays job list with names`() {
        fakeRepo.jobs = listOf(
            testJob(id = 1, name = "Job #100 Deploy", templateName = "Deploy Template", status = JobStatus.SUCCESSFUL),
            testJob(id = 2, name = "Job #101 Test", templateName = "Test Template", status = JobStatus.FAILED)
        )
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("Job #100 Deploy").assertIsDisplayed()
        composeTestRule.onNodeWithText("Job #101 Test").assertIsDisplayed()
    }

    @Test
    fun `shows job status badges`() {
        fakeRepo.jobs = listOf(
            testJob(id = 1, name = "Job #100 Deploy", templateName = "Deploy Template", status = JobStatus.SUCCESSFUL),
            testJob(id = 2, name = "Job #101 Test", templateName = "Test Template", status = JobStatus.FAILED)
        )
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onAllNodesWithText("Successful")[0].assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Failed")[0].assertIsDisplayed()
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
    fun `shows empty state message when no jobs`() {
        fakeRepo.jobs = emptyList()
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("No recent jobs").assertIsDisplayed()
    }

    @Test
    fun `shows job template name`() {
        fakeRepo.jobs = listOf(
            testJob(id = 1, name = "Deploy App", templateName = "Production Deploy")
        )
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("Production Deploy").assertIsDisplayed()
    }
}

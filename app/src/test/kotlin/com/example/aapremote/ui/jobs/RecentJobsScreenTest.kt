package com.example.aapremote.ui.jobs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.example.aapremote.MainDispatcherRule
import com.example.aapremote.TestData
import com.example.aapremote.fakes.FakeJobRepository
import com.example.aapremote.fakes.FakeTokenManager
import com.example.aapremote.model.JobStatus
import com.example.aapremote.presentation.jobs.RecentJobsViewModel
import com.example.aapremote.testJob
import com.example.aapremote.ui.theme.AapRemoteTheme
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
            AapRemoteTheme {
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

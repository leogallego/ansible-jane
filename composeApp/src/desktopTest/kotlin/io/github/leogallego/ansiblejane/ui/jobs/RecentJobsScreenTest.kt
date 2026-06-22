package io.github.leogallego.ansiblejane.ui.jobs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.runComposeUiTest
import io.github.leogallego.ansiblejane.TestData
import io.github.leogallego.ansiblejane.fakes.FakeJobRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.model.JobStatus
import io.github.leogallego.ansiblejane.presentation.jobs.RecentJobsViewModel
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import io.github.leogallego.ansiblejane.testInstance
import io.github.leogallego.ansiblejane.testJob
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class RecentJobsScreenTest {

    private val fakeRepo = FakeJobRepository()
    private val fakeTokenManager = FakeTokenManager()

    @BeforeTest
    fun setUp() {
        setupMainDispatcher()
    }

    @AfterTest
    fun tearDown() {
        tearDownMainDispatcher()
    }

    private fun ComposeUiTest.setUpScreen(viewModel: RecentJobsViewModel) {
        setContent {
            MaterialTheme {
                RecentJobsScreen(onNavigateToJobStatus = {}, viewModel = viewModel)
            }
        }
        waitForIdle()
    }

    @Test
    fun displays_job_list_with_names() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.jobs = listOf(
            testJob(id = 1, name = "Job #100 Deploy", templateName = "Deploy Template", status = JobStatus.SUCCESSFUL),
            testJob(id = 2, name = "Job #101 Test", templateName = "Test Template", status = JobStatus.FAILED)
        )
        setUpScreen(RecentJobsViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("Job #100 Deploy").assertIsDisplayed()
        onNodeWithText("Job #101 Test").assertIsDisplayed()
    }

    @Test
    fun shows_status_badges() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.jobs = listOf(
            testJob(id = 1, name = "Job #100 Deploy", templateName = "Deploy Template", status = JobStatus.SUCCESSFUL),
            testJob(id = 2, name = "Job #101 Test", templateName = "Test Template", status = JobStatus.FAILED)
        )
        setUpScreen(RecentJobsViewModel(fakeRepo, fakeTokenManager))

        onAllNodesWithText("Successful")[0].assertIsDisplayed()
        onAllNodesWithText("Failed")[0].assertIsDisplayed()
    }

    @Test
    fun shows_error_with_retry_button() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.shouldFail = true
        setUpScreen(RecentJobsViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun shows_empty_state_message() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.jobs = emptyList()
        setUpScreen(RecentJobsViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("No recent jobs").assertIsDisplayed()
    }

    @Test
    fun shows_job_template_name() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.jobs = listOf(
            testJob(id = 1, name = "Deploy App", templateName = "Production Deploy")
        )
        setUpScreen(RecentJobsViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("Production Deploy").assertIsDisplayed()
    }
}

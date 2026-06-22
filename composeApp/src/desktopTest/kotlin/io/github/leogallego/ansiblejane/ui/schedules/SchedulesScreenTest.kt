package io.github.leogallego.ansiblejane.ui.schedules

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.runComposeUiTest
import io.github.leogallego.ansiblejane.TestData
import io.github.leogallego.ansiblejane.fakes.FakeScheduleRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.presentation.schedules.SchedulesViewModel
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import io.github.leogallego.ansiblejane.testInstance
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SchedulesScreenTest {

    private val fakeRepo = FakeScheduleRepository()
    private val fakeTokenManager = FakeTokenManager()

    @BeforeTest
    fun setUp() {
        setupMainDispatcher()
    }

    @AfterTest
    fun tearDown() {
        tearDownMainDispatcher()
    }

    private fun ComposeUiTest.setUpScreen(viewModel: SchedulesViewModel) {
        setContent { MaterialTheme { SchedulesScreen(viewModel = viewModel) } }
        waitForIdle()
    }

    @Test
    fun displays_schedule_list_with_names() = runComposeUiTest {
        fakeRepo.schedules = TestData.sampleSchedules
        fakeTokenManager.setInstances(listOf(testInstance))
        setUpScreen(SchedulesViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("Schedule 1").assertIsDisplayed()
        onNodeWithText("Schedule 2").assertIsDisplayed()
    }

    @Test
    fun displays_template_names_for_schedules() = runComposeUiTest {
        fakeRepo.schedules = TestData.sampleSchedules
        fakeTokenManager.setInstances(listOf(testInstance))
        setUpScreen(SchedulesViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("Template for Schedule 1").assertIsDisplayed()
        onNodeWithText("Template for Schedule 2").assertIsDisplayed()
    }

    @Test
    fun shows_empty_state_when_no_schedules() = runComposeUiTest {
        fakeRepo.schedules = emptyList()
        fakeTokenManager.setInstances(listOf(testInstance))
        setUpScreen(SchedulesViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("No schedules configured").assertIsDisplayed()
    }

    @Test
    fun shows_error_with_retry_button() = runComposeUiTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Network error")
        fakeTokenManager.setInstances(listOf(testInstance))
        setUpScreen(SchedulesViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("Retry").assertIsDisplayed()
    }
}

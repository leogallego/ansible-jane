package com.example.aapremote.ui.schedules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.aapremote.MainDispatcherRule
import com.example.aapremote.TestData
import com.example.aapremote.fakes.FakeScheduleRepository
import com.example.aapremote.fakes.FakeTokenManager
import com.example.aapremote.presentation.schedules.SchedulesViewModel
import com.example.aapremote.ui.theme.AapRemoteTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SchedulesScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepo = FakeScheduleRepository()
    private val fakeTokenManager = FakeTokenManager()

    private fun setUpScreen() {
        val viewModel = SchedulesViewModel(fakeRepo, fakeTokenManager)
        composeTestRule.setContent {
            AapRemoteTheme {
                SchedulesScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `displays schedule list with names`() {
        fakeRepo.schedules = TestData.sampleSchedules
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("Schedule 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Schedule 2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Schedule 3").assertIsDisplayed()
    }

    @Test
    fun `displays template names for schedules`() {
        fakeRepo.schedules = TestData.sampleSchedules
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        // Schedule.templateName = summaryFields.unifiedJobTemplate?.name ?: name
        // createSchedule sets unifiedJobTemplate name = "Template for Schedule N"
        composeTestRule.onNodeWithText("Template for Schedule 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Template for Schedule 2").assertIsDisplayed()
    }

    @Test
    fun `shows empty state when no schedules`() {
        fakeRepo.schedules = emptyList()
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("No schedules configured").assertIsDisplayed()
    }

    @Test
    fun `shows error with retry button`() {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Network error")
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}

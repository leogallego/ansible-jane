package com.example.aapremote.ui.templates

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.aapremote.MainDispatcherRule
import com.example.aapremote.fakes.FakeTemplateRepository
import com.example.aapremote.fakes.FakeTokenManager
import com.example.aapremote.model.JobTemplate
import com.example.aapremote.model.JobTemplateSummaryFields
import com.example.aapremote.model.LabelSummary
import com.example.aapremote.model.UserCapabilities
import com.example.aapremote.presentation.templates.TemplatesViewModel
import com.example.aapremote.testInstance
import com.example.aapremote.testJobTemplate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TemplateListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepo = FakeTemplateRepository()
    private val fakeTokenManager = FakeTokenManager()

    private fun launchableTemplate(
        id: Int = 1,
        name: String = "Deploy App",
        description: String = "Deploy the application",
        askVariablesOnLaunch: Boolean = false
    ) = JobTemplate(
        id = id,
        name = name,
        description = description,
        askVariablesOnLaunch = askVariablesOnLaunch,
        summaryFields = JobTemplateSummaryFields(
            labels = LabelSummary(),
            userCapabilities = UserCapabilities(start = true)
        )
    )

    private fun setUpScreen(
        onNavigateToJobStatus: (Int) -> Unit = {}
    ) {
        val viewModel = TemplatesViewModel(fakeRepo, fakeTokenManager)
        composeTestRule.setContent {
            MaterialTheme {
                TemplateListScreen(
                    onNavigateToJobStatus = onNavigateToJobStatus,
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `shows loading skeletons when no instance is set`() {
        fakeRepo.templates = listOf(testJobTemplate())
        // Don't set instances, so ViewModel stays in Idle/Loading
        setUpScreen()

        // In Idle/Loading state, skeleton cards are shown (no template names)
        composeTestRule.onNodeWithText("Deploy App").assertDoesNotExist()
    }

    @Test
    fun `displays template list with names`() {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = listOf(
            testJobTemplate(id = 1, name = "Deploy App"),
            testJobTemplate(id = 2, name = "Configure Server")
        )
        setUpScreen()

        composeTestRule.onNodeWithText("Deploy App").assertIsDisplayed()
        composeTestRule.onNodeWithText("Configure Server").assertIsDisplayed()
    }

    @Test
    fun `shows empty state when no templates`() {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = emptyList()
        setUpScreen()

        composeTestRule.onNodeWithText("No templates available").assertIsDisplayed()
    }

    @Test
    fun `shows error state with retry`() {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.shouldFail = true
        setUpScreen()

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun `search bar is displayed`() {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = emptyList()
        setUpScreen()

        composeTestRule.onNodeWithText("Search templates...").assertIsDisplayed()
    }

    @Test
    fun `launch button shown for templates with start capability`() {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = listOf(launchableTemplate(id = 5, name = "My Template"))
        setUpScreen()

        composeTestRule.onNodeWithContentDescription("Launch My Template").assertIsDisplayed()
    }

    @Test
    fun `clicking launch shows confirmation dialog`() {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = listOf(launchableTemplate(id = 1, name = "Deploy App"))
        setUpScreen()

        composeTestRule.onNodeWithContentDescription("Launch Deploy App").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Launch \"Deploy App\"?").assertIsDisplayed()
    }

    @Test
    fun `extra vars dialog when template requires it`() {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = listOf(
            launchableTemplate(id = 1, name = "Parameterized Job", askVariablesOnLaunch = true)
        )
        setUpScreen()

        composeTestRule.onNodeWithContentDescription("Launch Parameterized Job").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Enter extra variables (JSON):").assertIsDisplayed()
    }
}

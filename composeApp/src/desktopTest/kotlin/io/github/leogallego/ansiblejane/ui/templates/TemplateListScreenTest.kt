package io.github.leogallego.ansiblejane.ui.templates

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.runComposeUiTest
import io.github.leogallego.ansiblejane.fakes.FakeTemplateRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.fakes.FakeUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.UserCapabilities
import io.github.leogallego.ansiblejane.presentation.templates.TemplatesViewModel
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import io.github.leogallego.ansiblejane.testInstance
import io.github.leogallego.ansiblejane.testJobTemplate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TemplateListScreenTest {

    private val fakeRepo = FakeTemplateRepository()
    private val fakeTokenManager = FakeTokenManager()
    private val fakeUserPrefs = FakeUserPreferencesRepository()

    @BeforeTest
    fun setUp() {
        setupMainDispatcher()
    }

    @AfterTest
    fun tearDown() {
        tearDownMainDispatcher()
    }

    private fun ComposeUiTest.setUpScreen(viewModel: TemplatesViewModel) {
        setContent {
            MaterialTheme {
                TemplateListScreen(onNavigateToJobStatus = {}, viewModel = viewModel)
            }
        }
        waitForIdle()
    }

    @Test
    fun does_not_show_template_data_when_no_instance() = runComposeUiTest {
        fakeRepo.templates = listOf(testJobTemplate())
        setUpScreen(TemplatesViewModel(fakeRepo, fakeTokenManager, fakeUserPrefs))

        onNodeWithText("Deploy App").assertDoesNotExist()
    }

    @Test
    fun displays_template_list_with_names() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = listOf(
            testJobTemplate(id = 1, name = "Deploy App"),
            testJobTemplate(id = 2, name = "Configure Server")
        )
        setUpScreen(TemplatesViewModel(fakeRepo, fakeTokenManager, fakeUserPrefs))

        onNodeWithText("Deploy App").assertIsDisplayed()
        onNodeWithText("Configure Server").assertIsDisplayed()
    }

    @Test
    fun shows_empty_state_when_no_templates() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = emptyList()
        setUpScreen(TemplatesViewModel(fakeRepo, fakeTokenManager, fakeUserPrefs))

        onNodeWithText("No templates available").assertIsDisplayed()
    }

    @Test
    fun shows_error_state_with_retry() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.shouldFail = true
        setUpScreen(TemplatesViewModel(fakeRepo, fakeTokenManager, fakeUserPrefs))

        onNodeWithText("Retry").assertIsDisplayed()
    }

    @Test
    fun search_bar_is_displayed() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = emptyList()
        setUpScreen(TemplatesViewModel(fakeRepo, fakeTokenManager, fakeUserPrefs))

        onNodeWithText("Search templates...").assertIsDisplayed()
    }

    @Test
    fun launch_button_shown_for_launchable_templates() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = listOf(testJobTemplate(id = 5, name = "My Template", userCapabilities = UserCapabilities(start = true)))
        setUpScreen(TemplatesViewModel(fakeRepo, fakeTokenManager, fakeUserPrefs))

        onNodeWithContentDescription("Launch My Template").assertIsDisplayed()
    }

    @Test
    fun clicking_launch_shows_confirmation_dialog() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = listOf(testJobTemplate(id = 1, name = "Deploy App", userCapabilities = UserCapabilities(start = true)))
        setUpScreen(TemplatesViewModel(fakeRepo, fakeTokenManager, fakeUserPrefs))

        onNodeWithContentDescription("Launch Deploy App").performClick()
        waitForIdle()

        onNodeWithText("Launch \"Deploy App\"?").assertIsDisplayed()
    }

    @Test
    fun extra_vars_dialog_when_template_requires_it() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeRepo.templates = listOf(
            testJobTemplate(id = 1, name = "Parameterized Job", askVariablesOnLaunch = true, userCapabilities = UserCapabilities(start = true))
        )
        setUpScreen(TemplatesViewModel(fakeRepo, fakeTokenManager, fakeUserPrefs))

        onNodeWithContentDescription("Launch Parameterized Job").performClick()
        waitForIdle()

        onNodeWithText("Enter extra variables (JSON):").assertIsDisplayed()
    }
}

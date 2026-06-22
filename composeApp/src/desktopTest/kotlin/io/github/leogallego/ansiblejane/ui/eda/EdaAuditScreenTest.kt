package io.github.leogallego.ansiblejane.ui.eda

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.runComposeUiTest
import io.github.leogallego.ansiblejane.TestData
import io.github.leogallego.ansiblejane.fakes.FakeEdaAuditRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.presentation.eda.EdaAuditViewModel
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class EdaAuditScreenTest {

    private val fakeRepo = FakeEdaAuditRepository()
    private val fakeTokenManager = FakeTokenManager()

    @BeforeTest
    fun setUp() {
        setupMainDispatcher()
    }

    @AfterTest
    fun tearDown() {
        tearDownMainDispatcher()
    }

    private fun ComposeUiTest.setUpScreen(viewModel: EdaAuditViewModel) {
        setContent { MaterialTheme { EdaAuditScreen(viewModel = viewModel) } }
        waitForIdle()
    }

    @Test
    fun displays_audit_rule_list() = runComposeUiTest {
        fakeRepo.auditRules = TestData.sampleEdaRuleAudits
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen(EdaAuditViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("rule-1").assertIsDisplayed()
        onNodeWithText("rule-2").assertIsDisplayed()
        onNodeWithText("rule-3").assertIsDisplayed()
    }

    @Test
    fun shows_empty_state_when_no_rules() = runComposeUiTest {
        fakeRepo.auditRules = emptyList()
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen(EdaAuditViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("No EDA audit events").assertIsDisplayed()
    }

    @Test
    fun shows_error_with_retry_button() = runComposeUiTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Network error")
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen(EdaAuditViewModel(fakeRepo, fakeTokenManager))

        onNodeWithText("Retry").assertIsDisplayed()
    }
}

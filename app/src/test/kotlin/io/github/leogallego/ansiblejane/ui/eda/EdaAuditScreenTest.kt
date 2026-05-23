package io.github.leogallego.ansiblejane.ui.eda

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.leogallego.ansiblejane.MainDispatcherRule
import io.github.leogallego.ansiblejane.TestData
import io.github.leogallego.ansiblejane.fakes.FakeEdaAuditRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.presentation.eda.EdaAuditViewModel
import io.github.leogallego.ansiblejane.ui.theme.AnsibleJaneTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EdaAuditScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeRepo = FakeEdaAuditRepository()
    private val fakeTokenManager = FakeTokenManager()

    private fun setUpScreen() {
        val viewModel = EdaAuditViewModel(fakeRepo, fakeTokenManager)
        composeTestRule.setContent {
            AnsibleJaneTheme {
                EdaAuditScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `displays audit rule list with rule names`() {
        fakeRepo.auditRules = TestData.sampleEdaRuleAudits
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        // EdaRuleAudit.displayRuleName = ruleName ?: name
        // createEdaRuleAudit sets ruleName = "rule-1", "rule-2", "rule-3"
        composeTestRule.onNodeWithText("rule-1").assertIsDisplayed()
        composeTestRule.onNodeWithText("rule-2").assertIsDisplayed()
        composeTestRule.onNodeWithText("rule-3").assertIsDisplayed()
    }

    @Test
    fun `shows empty state message when no audit rules`() {
        fakeRepo.auditRules = emptyList()
        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        setUpScreen()

        composeTestRule.onNodeWithText("No EDA audit events").assertIsDisplayed()
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

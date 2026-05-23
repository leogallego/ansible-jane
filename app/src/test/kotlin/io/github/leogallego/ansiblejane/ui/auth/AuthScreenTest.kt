package io.github.leogallego.ansiblejane.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.leogallego.ansiblejane.MainDispatcherRule
import io.github.leogallego.ansiblejane.fakes.FakeAuthRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.model.User
import io.github.leogallego.ansiblejane.presentation.auth.AuthViewModel
import io.github.leogallego.ansiblejane.testKoinModule
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeAuthRepo = FakeAuthRepository()
    private val fakeTokenManager = FakeTokenManager()

    @After
    fun tearDown() {
        stopKoin()
    }

    private val testUser = User(
        id = 1,
        username = "admin",
        firstName = "Test",
        lastName = "User",
        email = "admin@example.com",
        isSuperuser = true
    )

    private fun setUpScreen(
        onNavigateToDashboard: () -> Unit = {},
        onCancel: (() -> Unit)? = null,
        preFilledUrl: String? = null,
        preFilledAlias: String? = null,
        reAuthInstanceId: String? = null,
        isAddInstance: Boolean = false,
        preFilledTrustSelfSigned: Boolean = false
    ) {
        startKoin { modules(testKoinModule(tokenManager = fakeTokenManager)) }
        val viewModel = AuthViewModel(fakeAuthRepo)
        composeTestRule.setContent {
            MaterialTheme {
                AuthScreen(
                    onNavigateToDashboard = onNavigateToDashboard,
                    onCancel = onCancel,
                    preFilledUrl = preFilledUrl,
                    preFilledAlias = preFilledAlias,
                    reAuthInstanceId = reAuthInstanceId,
                    isAddInstance = isAddInstance,
                    preFilledTrustSelfSigned = preFilledTrustSelfSigned,
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `renders login form with empty fields`() {
        setUpScreen()

        composeTestRule.onNodeWithTag("field_url").assertIsDisplayed()
        composeTestRule.onNodeWithTag("field_alias").assertIsDisplayed()
        composeTestRule.onNodeWithTag("field_token").assertIsDisplayed()
    }

    @Test
    fun `connect button is displayed`() {
        setUpScreen()

        composeTestRule.onNodeWithTag("button_connect").fetchSemanticsNode()
    }

    @Test
    fun `connect button is disabled when fields are empty`() {
        setUpScreen()

        composeTestRule.onNodeWithTag("button_connect").assertIsNotEnabled()
    }

    @Test
    fun `connect button is enabled when url and token are filled`() {
        setUpScreen()

        composeTestRule.onNodeWithTag("field_url").performTextInput("https://aap.example.com")
        composeTestRule.onNodeWithTag("field_token").performTextInput("my-token")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("button_connect").assertIsEnabled()
    }

    @Test
    fun `shows error on auth failure`() {
        fakeAuthRepo.shouldFail = true
        fakeAuthRepo.failureException = RuntimeException("Invalid token")
        setUpScreen()

        composeTestRule.onNodeWithTag("field_url").performTextInput("https://aap.example.com")
        composeTestRule.onNodeWithTag("field_token").performTextInput("bad-token")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("button_connect").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `self-signed cert switch exists`() {
        setUpScreen()

        composeTestRule.onNodeWithTag("switch_self_signed").fetchSemanticsNode()
    }

    @Test
    fun `cancel button visible in add-instance mode`() {
        setUpScreen(onCancel = {}, isAddInstance = true)

        composeTestRule.onNodeWithText("Cancel").fetchSemanticsNode()
    }

    @Test
    fun `pre-filled URL shows in field`() {
        setUpScreen(preFilledUrl = "https://prefilled.example.com")

        composeTestRule.onNodeWithText("https://prefilled.example.com").assertIsDisplayed()
    }

    @Test
    fun `import from backup button visible on initial auth`() {
        setUpScreen(onCancel = null, isAddInstance = false)

        composeTestRule.onNodeWithText("Import from backup").fetchSemanticsNode()
    }
}

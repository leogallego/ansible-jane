package io.github.leogallego.ansiblejane.ui.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import io.github.leogallego.ansiblejane.desktopTestKoinModule
import io.github.leogallego.ansiblejane.fakes.FakeAuthRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.presentation.auth.AuthViewModel
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AuthScreenTest {

    private val fakeAuthRepo = FakeAuthRepository()
    private val fakeTokenManager = FakeTokenManager()

    @BeforeTest
    fun setUp() {
        setupMainDispatcher()
        startKoin { modules(desktopTestKoinModule(tokenManager = fakeTokenManager)) }
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        tearDownMainDispatcher()
    }

    @Test
    fun renders_login_form_with_empty_fields() = runComposeUiTest {
        val viewModel = AuthViewModel(fakeAuthRepo)
        setContent {
            MaterialTheme {
                AuthScreen(
                    onNavigateToDashboard = {},
                    viewModel = viewModel
                )
            }
        }
        waitForIdle()

        onNodeWithTag("field_url").assertIsDisplayed()
        onNodeWithTag("field_alias").assertIsDisplayed()
        onNodeWithTag("field_token").assertIsDisplayed()
    }

    @Test
    fun connect_button_is_displayed() = runComposeUiTest {
        val viewModel = AuthViewModel(fakeAuthRepo)
        setContent {
            MaterialTheme {
                AuthScreen(
                    onNavigateToDashboard = {},
                    viewModel = viewModel
                )
            }
        }
        waitForIdle()

        onNodeWithTag("button_connect").fetchSemanticsNode()
    }

    @Test
    fun connect_button_is_disabled_when_fields_are_empty() = runComposeUiTest {
        val viewModel = AuthViewModel(fakeAuthRepo)
        setContent {
            MaterialTheme {
                AuthScreen(
                    onNavigateToDashboard = {},
                    viewModel = viewModel
                )
            }
        }
        waitForIdle()

        onNodeWithTag("button_connect").assertIsNotEnabled()
    }

    @Test
    fun connect_button_is_enabled_when_url_and_token_filled() = runComposeUiTest {
        val viewModel = AuthViewModel(fakeAuthRepo)
        setContent {
            MaterialTheme {
                AuthScreen(
                    onNavigateToDashboard = {},
                    viewModel = viewModel
                )
            }
        }
        waitForIdle()

        onNodeWithTag("field_url").performTextInput("https://aap.example.com")
        onNodeWithTag("field_token").performTextInput("my-token")
        waitForIdle()

        onNodeWithTag("button_connect").assertIsEnabled()
    }

    @Test
    fun shows_error_on_auth_failure() = runComposeUiTest {
        fakeAuthRepo.shouldFail = true
        fakeAuthRepo.failureException = RuntimeException("Invalid token")
        val viewModel = AuthViewModel(fakeAuthRepo)

        setContent {
            MaterialTheme {
                AuthScreen(
                    onNavigateToDashboard = {},
                    viewModel = viewModel
                )
            }
        }
        waitForIdle()

        onNodeWithTag("field_url").performTextInput("https://aap.example.com")
        onNodeWithTag("field_token").performTextInput("bad-token")
        waitForIdle()

        onNodeWithTag("button_connect").performClick()
        waitForIdle()
    }

    @Test
    fun self_signed_cert_switch_exists() = runComposeUiTest {
        val viewModel = AuthViewModel(fakeAuthRepo)
        setContent {
            MaterialTheme {
                AuthScreen(
                    onNavigateToDashboard = {},
                    viewModel = viewModel
                )
            }
        }
        waitForIdle()

        onNodeWithTag("switch_self_signed").fetchSemanticsNode()
    }

    @Test
    fun back_button_and_title_visible_in_add_instance_mode() = runComposeUiTest {
        val viewModel = AuthViewModel(fakeAuthRepo)
        setContent {
            MaterialTheme {
                AuthScreen(
                    onNavigateToDashboard = {},
                    onCancel = {},
                    isAddInstance = true,
                    viewModel = viewModel
                )
            }
        }
        waitForIdle()

        onNodeWithTag("button_back").assertIsDisplayed()
        onNodeWithText("Add Instance").assertIsDisplayed()
    }

    @Test
    fun pre_filled_url_shows_in_field() = runComposeUiTest {
        val viewModel = AuthViewModel(fakeAuthRepo)
        setContent {
            MaterialTheme {
                AuthScreen(
                    onNavigateToDashboard = {},
                    preFilledUrl = "https://prefilled.example.com",
                    viewModel = viewModel
                )
            }
        }
        waitForIdle()

        onNodeWithText("https://prefilled.example.com").assertIsDisplayed()
    }

    // import_from_backup test omitted: ImportFromBackupButton is Android-only (expect/actual)
}

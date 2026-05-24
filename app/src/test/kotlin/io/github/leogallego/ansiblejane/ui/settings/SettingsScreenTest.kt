package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.leogallego.ansiblejane.MainDispatcherRule
import io.github.leogallego.ansiblejane.fakes.FakeAapApiProvider
import io.github.leogallego.ansiblejane.fakes.FakeAssistantRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.fakes.FakeUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import io.github.leogallego.ansiblejane.presentation.settings.SettingsViewModel
import io.github.leogallego.ansiblejane.testKoinModule
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakeTokenManager = FakeTokenManager()
    private val fakeApiProvider = FakeAapApiProvider()
    private val fakeUserPreferences = FakeUserPreferencesRepository()
    private val fakeAssistantRepo = FakeAssistantRepository()
    private val json = Json { ignoreUnknownKeys = true }
    private val mcpServerManager = McpServerManager(
        httpClientFactory = { OkHttpClient() },
        json = json
    )

    @After
    fun tearDown() {
        stopKoin()
    }

    private val instance1 = AapInstance(
        id = "inst-1",
        baseUrl = "https://aap1.example.com",
        token = "token-1",
        alias = "Production"
    )

    private val instance2 = AapInstance(
        id = "inst-2",
        baseUrl = "https://aap2.example.com",
        token = "token-2",
        alias = "Staging"
    )

    private fun setUpScreen(
        onLogout: () -> Unit = {},
        onNavigateBack: () -> Unit = {},
        onAddInstance: () -> Unit = {}
    ) {
        startKoin { modules(testKoinModule(tokenManager = fakeTokenManager)) }
        val viewModel = SettingsViewModel(
            tokenManager = fakeTokenManager,
            apiProvider = fakeApiProvider,
            userPreferences = fakeUserPreferences,
            assistantRepository = fakeAssistantRepo,
            mcpServerManager = mcpServerManager,
            httpClient = OkHttpClient(),
            json = json
        )
        composeTestRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = onLogout,
                    onNavigateBack = onNavigateBack,
                    onAddInstance = onAddInstance,
                    viewModel = viewModel
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `displays instance list on Instances tab`() {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        setUpScreen()

        composeTestRule.onNodeWithText("Instances").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Production").assertIsDisplayed()
        composeTestRule.onNodeWithText("Staging").assertIsDisplayed()
    }

    @Test
    fun `shows Add Instance button on Instances tab`() {
        fakeTokenManager.setInstances(listOf(instance1))
        setUpScreen()

        composeTestRule.onNodeWithText("Instances").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Add Instance").assertIsDisplayed()
    }

    @Test
    fun `shows Logout All button on Instances tab`() {
        fakeTokenManager.setInstances(listOf(instance1))
        setUpScreen()

        composeTestRule.onNodeWithText("Instances").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Logout All").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `logout all shows confirmation dialog on Instances tab`() {
        fakeTokenManager.setInstances(listOf(instance1))
        setUpScreen()

        composeTestRule.onNodeWithText("Instances").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Logout All").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Remove all AAP instances and log out?", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `back button present`() {
        fakeTokenManager.setInstances(listOf(instance1))
        setUpScreen()

        composeTestRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun `about section shows app name on General tab`() {
        fakeTokenManager.setInstances(listOf(instance1))
        setUpScreen()

        composeTestRule.onNodeWithText("Ansible Jane").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `backup restore section visible on General tab`() {
        fakeTokenManager.setInstances(listOf(instance1))
        setUpScreen()

        composeTestRule.onNodeWithText("Backup & Restore").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `active instance shows Active pill on Instances tab`() {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        setUpScreen()

        composeTestRule.onNodeWithText("Instances").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Active").assertIsDisplayed()
    }

    @Test
    fun `tab selector shows all four tabs`() {
        fakeTokenManager.setInstances(listOf(instance1))
        setUpScreen()

        composeTestRule.onNodeWithText("General").assertIsDisplayed()
        composeTestRule.onNodeWithText("Instances").assertIsDisplayed()
        composeTestRule.onNodeWithText("Agent").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tools").assertIsDisplayed()
    }

    @Test
    fun `Agent tab shows LLM Provider section`() {
        fakeTokenManager.setInstances(listOf(instance1))
        setUpScreen()

        composeTestRule.onNodeWithText("Agent").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("LLM Provider").assertIsDisplayed()
    }

    @Test
    fun `Tools tab shows MCP Servers section`() {
        fakeTokenManager.setInstances(listOf(instance1))
        setUpScreen()

        composeTestRule.onNodeWithText("Tools").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("MCP Servers").assertIsDisplayed()
    }
}

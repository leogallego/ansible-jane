package io.github.leogallego.ansiblejane.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import io.github.leogallego.ansiblejane.assistant.engine.ToolRouter
import io.github.leogallego.ansiblejane.desktopTestKoinModule
import io.github.leogallego.ansiblejane.fakes.FakeAapApiProvider
import io.github.leogallego.ansiblejane.fakes.FakeAssistantRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.fakes.FakeToolManifestRepository
import io.github.leogallego.ansiblejane.fakes.FakeUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.network.InstanceDiscovery
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import io.github.leogallego.ansiblejane.presentation.settings.SettingsViewModel
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {

    private val fakeTokenManager = FakeTokenManager()
    private val fakeApiProvider = FakeAapApiProvider()
    private val fakeUserPreferences = FakeUserPreferencesRepository()
    private val fakeAssistantRepo = FakeAssistantRepository()
    private val json = Json { ignoreUnknownKeys = true }
    private val mcpServerManager = McpServerManager(
        ktorClientFactory = { _, _ ->
            HttpClient(MockEngine) { engine { addHandler { respond("") } } }
        }
    )

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

    private fun createViewModel() = SettingsViewModel(
        tokenManager = fakeTokenManager,
        apiProvider = fakeApiProvider,
        userPreferences = fakeUserPreferences,
        assistantRepository = fakeAssistantRepo,
        mcpServerManager = mcpServerManager,
        manifestRepository = FakeToolManifestRepository(),
        instanceDiscovery = InstanceDiscovery(json),
        toolRouter = ToolRouter(repository = fakeAssistantRepo),
        json = json
    )

    @Test
    fun displays_instance_list_on_Instances_tab() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("Instances").performClick()
        waitForIdle()

        onNodeWithText("Production").assertIsDisplayed()
        onNodeWithText("Staging").assertIsDisplayed()
    }

    @Test
    fun shows_Add_Instance_button_on_Instances_tab() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("Instances").performClick()
        waitForIdle()

        onNodeWithText("Add Instance").assertIsDisplayed()
    }

    @Test
    fun shows_Logout_All_button_on_Instances_tab() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("Instances").performClick()
        waitForIdle()

        onNodeWithText("Logout All").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun logout_all_shows_confirmation_dialog() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("Instances").performClick()
        waitForIdle()

        onNodeWithText("Logout All").performScrollTo().performClick()
        waitForIdle()

        onNodeWithText("Remove all AAP instances and log out?", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun back_button_present() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun about_section_shows_app_name_on_General_tab() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("Ansible Jane").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun backup_restore_section_visible_on_General_tab() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("Backup & Restore is available on Android").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun active_instance_shows_Active_pill() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1, instance2))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("Instances").performClick()
        waitForIdle()

        onNodeWithText("Active").assertIsDisplayed()
    }

    @Test
    fun tab_selector_shows_all_four_tabs() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("General").assertIsDisplayed()
        onNodeWithText("Instances").assertIsDisplayed()
        onNodeWithText("Agent").assertIsDisplayed()
        onNodeWithText("Tools").assertIsDisplayed()
    }

    @Test
    fun Agent_tab_shows_LLM_Provider_section() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("Agent").performClick()
        waitForIdle()

        onNodeWithText("LLM Provider").assertIsDisplayed()
    }

    @Test
    fun Tools_tab_shows_MCP_Servers_section() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("Tools").performClick()
        waitForIdle()

        onNodeWithText("MCP Servers").assertIsDisplayed()
    }

    @Test
    fun Tools_tab_shows_Local_Tools_section() = runComposeUiTest {
        fakeTokenManager.setInstances(listOf(instance1))
        setContent {
            MaterialTheme {
                SettingsScreen(
                    onLogout = {},
                    onNavigateBack = {},
                    onAddInstance = {},
                    viewModel = createViewModel()
                )
            }
        }
        waitForIdle()

        onNodeWithText("Tools").performClick()
        waitForIdle()

        onNodeWithText("Local Tools").performScrollTo().assertIsDisplayed()
    }
}

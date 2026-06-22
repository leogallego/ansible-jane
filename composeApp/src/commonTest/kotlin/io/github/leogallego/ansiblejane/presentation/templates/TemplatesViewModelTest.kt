package io.github.leogallego.ansiblejane.presentation.templates

import app.cash.turbine.test
import io.github.leogallego.ansiblejane.fakes.FakeTemplateRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.fakes.FakeUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.testInstance
import io.github.leogallego.ansiblejane.testJobTemplate
import io.github.leogallego.ansiblejane.testLabel1
import io.github.leogallego.ansiblejane.testLabel2
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplatesViewModelTest {

    private lateinit var fakeRepo: FakeTemplateRepository
    private lateinit var fakeTokenManager: FakeTokenManager
    private lateinit var fakeUserPrefs: FakeUserPreferencesRepository
    private lateinit var viewModel: TemplatesViewModel

    @BeforeTest
    fun setup() {
        setupMainDispatcher()
        fakeRepo = FakeTemplateRepository()
        fakeTokenManager = FakeTokenManager()
        fakeUserPrefs = FakeUserPreferencesRepository()
    }

    @AfterTest
    fun cleanup() {
        tearDownMainDispatcher()
    }

    private fun createViewModel(): TemplatesViewModel {
        return TemplatesViewModel(fakeRepo, fakeTokenManager, fakeUserPrefs)
    }

    // --- Init / active instance ---

    @Test
    fun `initial state is Idle when no active instance`() = runTest {
        viewModel = createViewModel()
        assertEquals(TemplatesUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `auto-loads templates when active instance exists`() = runTest {
        val templates = listOf(testJobTemplate(id = 1), testJobTemplate(id = 2, name = "Backup DB"))
        fakeRepo.templates = templates

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is TemplatesUiState.Success)
            assertEquals(2, (state as TemplatesUiState.Success).templates.size)
        }
    }

    // --- loadTemplates ---

    @Test
    fun `loadTemplates success emits Success state`() = runTest {
        val templates = listOf(testJobTemplate(id = 1, name = "Deploy App"))
        fakeRepo.templates = templates

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is TemplatesUiState.Success)
            val success = state as TemplatesUiState.Success
            assertEquals(1, success.templates.size)
            assertEquals("Deploy App", success.templates[0].name)
            assertEquals(false, success.hasMore)
        }
    }

    @Test
    fun `loadTemplates error emits Error state`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Network failure")

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is TemplatesUiState.Error)
            assertTrue((state as TemplatesUiState.Error).error is AppError.Unknown)
        }
    }

    // --- Search ---

    @Test
    fun `search passes query to repository`() = runTest {
        fakeRepo.templates = listOf(testJobTemplate())

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        // Wait for initial load
        viewModel.uiState.test {
            expectMostRecentItem()
        }

        viewModel.search("deploy")
        advanceTimeBy(350)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is TemplatesUiState.Success)
            assertEquals("deploy", fakeRepo.lastSearchQuery)
        }
    }

    @Test
    fun `search with blank query clears search`() = runTest {
        fakeRepo.templates = listOf(testJobTemplate())

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.search("deploy")
        viewModel.search("")

        viewModel.uiState.test {
            expectMostRecentItem()
            assertEquals(null, fakeRepo.lastSearchQuery)
        }
    }

    // --- filterByLabel ---

    @Test
    fun `filterByLabel passes label name to repository`() = runTest {
        fakeRepo.templates = listOf(testJobTemplate(labels = listOf(testLabel1)))

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.filterByLabel(testLabel1)

        viewModel.uiState.test {
            expectMostRecentItem()
            assertEquals("production", fakeRepo.lastLabelFilter)
        }
    }

    @Test
    fun `filterByLabel with null clears filter`() = runTest {
        fakeRepo.templates = listOf(testJobTemplate())

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.filterByLabel(testLabel1)
        viewModel.filterByLabel(null)

        viewModel.uiState.test {
            expectMostRecentItem()
            assertEquals(null, fakeRepo.lastLabelFilter)
        }
    }

    // --- Labels extraction ---

    @Test
    fun `Success state extracts available labels from templates`() = runTest {
        fakeRepo.templates = listOf(
            testJobTemplate(id = 1, labels = listOf(testLabel1, testLabel2)),
            testJobTemplate(id = 2, labels = listOf(testLabel1))
        )

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as TemplatesUiState.Success
            // Labels should be deduplicated and sorted by name
            assertEquals(2, state.availableLabels.size)
            assertEquals("production", state.availableLabels[0].name)
            assertEquals("staging", state.availableLabels[1].name)
        }
    }

    // --- Launch flow ---

    @Test
    fun `requestLaunch with no vars shows Confirming state`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        val template = testJobTemplate(askVariablesOnLaunch = false)
        viewModel.requestLaunch(template)

        assertEquals(LaunchState.Confirming(template), viewModel.launchState.value)
    }

    @Test
    fun `requestLaunch with askVars shows EnteringVars state`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        val template = testJobTemplate(askVariablesOnLaunch = true)
        viewModel.requestLaunch(template)

        assertEquals(LaunchState.EnteringVars(template), viewModel.launchState.value)
    }

    @Test
    fun `confirmLaunch success emits Launched state`() = runTest {
        fakeRepo.launchResult = 99

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        val template = testJobTemplate()
        viewModel.requestLaunch(template)
        viewModel.confirmLaunch()

        viewModel.launchState.test {
            val state = expectMostRecentItem()
            assertEquals(LaunchState.Launched(99), state)
        }
    }

    @Test
    fun `confirmLaunch error emits LaunchError state`() = runTest {
        fakeRepo.shouldFail = true

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        val template = testJobTemplate()
        viewModel.requestLaunch(template)
        viewModel.confirmLaunch()

        viewModel.launchState.test {
            val state = expectMostRecentItem()
            assertTrue(state is LaunchState.LaunchError)
        }
    }

    @Test
    fun `cancelLaunch resets to Idle`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.requestLaunch(testJobTemplate())
        viewModel.cancelLaunch()

        assertEquals(LaunchState.Idle, viewModel.launchState.value)
    }

    @Test
    fun `resetLaunchState resets to Idle`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.requestLaunch(testJobTemplate())
        viewModel.confirmLaunch()
        viewModel.resetLaunchState()

        assertEquals(LaunchState.Idle, viewModel.launchState.value)
    }

    // --- loadMore ---

    @Test
    fun `loadMore increments page when hasMore is true`() = runTest {
        fakeRepo.templates = listOf(testJobTemplate())
        fakeRepo.hasMore = true

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        // Wait for initial load
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is TemplatesUiState.Success)
            assertTrue((state as TemplatesUiState.Success).hasMore)
        }

        fakeRepo.hasMore = false
        viewModel.loadMore()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is TemplatesUiState.Success)
            assertEquals(2, fakeRepo.lastRequestedPage)
        }
    }

    @Test
    fun `loadMore does nothing when hasMore is false`() = runTest {
        fakeRepo.templates = listOf(testJobTemplate())
        fakeRepo.hasMore = false

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
        }

        val callsBefore = fakeRepo.getTemplatesCalled
        viewModel.loadMore()
        assertEquals(callsBefore, fakeRepo.getTemplatesCalled)
    }

    // --- clearFilters ---

    @Test
    fun `clearFilters resets search query and reloads`() = runTest {
        fakeRepo.templates = listOf(testJobTemplate())

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.search("deploy")
        viewModel.filterByLabel(testLabel1)
        viewModel.clearFilters()

        assertEquals("", viewModel.searchQuery.value)
        assertEquals(null, fakeRepo.lastSearchQuery)
        assertEquals(null, fakeRepo.lastLabelFilter)
    }
}

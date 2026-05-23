package io.github.leogallego.ansiblejane.presentation.workflows

import app.cash.turbine.test
import io.github.leogallego.ansiblejane.MainDispatcherRule
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.fakes.FakeWorkflowRepository
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.testInstance
import io.github.leogallego.ansiblejane.testLabel1
import io.github.leogallego.ansiblejane.testLabel2
import io.github.leogallego.ansiblejane.testWorkflowJobTemplate
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkflowTemplatesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepo: FakeWorkflowRepository
    private lateinit var fakeTokenManager: FakeTokenManager
    private lateinit var viewModel: WorkflowTemplatesViewModel

    @Before
    fun setup() {
        fakeRepo = FakeWorkflowRepository()
        fakeTokenManager = FakeTokenManager()
    }

    private fun createViewModel(): WorkflowTemplatesViewModel {
        return WorkflowTemplatesViewModel(fakeRepo, fakeTokenManager)
    }

    // --- Init / active instance ---

    @Test
    fun `initial state is Idle when no active instance`() = runTest {
        viewModel = createViewModel()
        assertEquals(WorkflowTemplatesUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `auto-loads templates when active instance exists`() = runTest {
        fakeRepo.templates = listOf(
            testWorkflowJobTemplate(id = 1),
            testWorkflowJobTemplate(id = 2, name = "CI Pipeline")
        )

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is WorkflowTemplatesUiState.Success)
            assertEquals(2, (state as WorkflowTemplatesUiState.Success).templates.size)
        }
    }

    // --- loadTemplates ---

    @Test
    fun `loadTemplates success emits Success state`() = runTest {
        fakeRepo.templates = listOf(testWorkflowJobTemplate(id = 1, name = "Full Deploy"))

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is WorkflowTemplatesUiState.Success)
            val success = state as WorkflowTemplatesUiState.Success
            assertEquals(1, success.templates.size)
            assertEquals("Full Deploy", success.templates[0].name)
        }
    }

    @Test
    fun `loadTemplates error emits Error state`() = runTest {
        fakeRepo.shouldFail = true

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is WorkflowTemplatesUiState.Error)
            assertTrue((state as WorkflowTemplatesUiState.Error).error is AppError.Unknown)
        }
    }

    // --- Search ---

    @Test
    fun `search passes query to repository`() = runTest {
        fakeRepo.templates = listOf(testWorkflowJobTemplate())

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test { expectMostRecentItem() }

        viewModel.search("pipeline")
        advanceTimeBy(350)

        viewModel.uiState.test {
            expectMostRecentItem()
            assertEquals("pipeline", fakeRepo.lastSearchQuery)
        }
    }

    @Test
    fun `search with blank query clears search`() = runTest {
        fakeRepo.templates = listOf(testWorkflowJobTemplate())

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.search("pipeline")
        viewModel.search("")

        viewModel.uiState.test {
            expectMostRecentItem()
            assertEquals(null, fakeRepo.lastSearchQuery)
        }
    }

    // --- filterByLabel ---

    @Test
    fun `filterByLabel passes label name to repository`() = runTest {
        fakeRepo.templates = listOf(testWorkflowJobTemplate(labels = listOf(testLabel1)))

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.filterByLabel(testLabel1)

        viewModel.uiState.test {
            expectMostRecentItem()
            assertEquals("production", fakeRepo.lastLabelFilter)
        }
    }

    // --- Labels extraction ---

    @Test
    fun `Success state extracts available labels from templates`() = runTest {
        fakeRepo.templates = listOf(
            testWorkflowJobTemplate(id = 1, labels = listOf(testLabel1, testLabel2)),
            testWorkflowJobTemplate(id = 2, labels = listOf(testLabel1))
        )

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as WorkflowTemplatesUiState.Success
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

        val template = testWorkflowJobTemplate(askVariablesOnLaunch = false)
        viewModel.requestLaunch(template)

        assertEquals(WorkflowLaunchState.Confirming(template), viewModel.launchState.value)
    }

    @Test
    fun `requestLaunch with askVars shows EnteringVars state`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        val template = testWorkflowJobTemplate(askVariablesOnLaunch = true)
        viewModel.requestLaunch(template)

        assertEquals(WorkflowLaunchState.EnteringVars(template), viewModel.launchState.value)
    }

    @Test
    fun `confirmLaunch success emits Launched state`() = runTest {
        fakeRepo.launchResult = 77

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        val template = testWorkflowJobTemplate()
        viewModel.requestLaunch(template)
        viewModel.confirmLaunch()

        viewModel.launchState.test {
            val state = expectMostRecentItem()
            assertEquals(WorkflowLaunchState.Launched(77), state)
        }
    }

    @Test
    fun `confirmLaunch error emits LaunchError state`() = runTest {
        fakeRepo.shouldFail = true

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        val template = testWorkflowJobTemplate()
        viewModel.requestLaunch(template)
        viewModel.confirmLaunch()

        viewModel.launchState.test {
            val state = expectMostRecentItem()
            assertTrue(state is WorkflowLaunchState.LaunchError)
        }
    }

    @Test
    fun `cancelLaunch resets to Idle`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.requestLaunch(testWorkflowJobTemplate())
        viewModel.cancelLaunch()

        assertEquals(WorkflowLaunchState.Idle, viewModel.launchState.value)
    }

    @Test
    fun `resetLaunchState resets to Idle`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.requestLaunch(testWorkflowJobTemplate())
        viewModel.confirmLaunch()
        viewModel.resetLaunchState()

        assertEquals(WorkflowLaunchState.Idle, viewModel.launchState.value)
    }

    // --- loadMore ---

    @Test
    fun `loadMore increments page when hasMore is true`() = runTest {
        fakeRepo.templates = listOf(testWorkflowJobTemplate())
        fakeRepo.hasMore = true

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue((state as WorkflowTemplatesUiState.Success).hasMore)
        }

        fakeRepo.hasMore = false
        viewModel.loadMore()

        viewModel.uiState.test {
            expectMostRecentItem()
            assertEquals(2, fakeRepo.lastRequestedPage)
        }
    }

    @Test
    fun `loadMore does nothing when hasMore is false`() = runTest {
        fakeRepo.templates = listOf(testWorkflowJobTemplate())
        fakeRepo.hasMore = false

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test { expectMostRecentItem() }

        fakeRepo.lastRequestedPage = 0
        viewModel.loadMore()
        assertEquals(0, fakeRepo.lastRequestedPage)
    }
}

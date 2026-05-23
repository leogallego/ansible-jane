package io.github.leogallego.ansiblejane.presentation.workflows

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.leogallego.ansiblejane.MainDispatcherRule
import io.github.leogallego.ansiblejane.fakes.FakeJobRepository
import io.github.leogallego.ansiblejane.fakes.FakeWorkflowRepository
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.JobStatus
import io.github.leogallego.ansiblejane.testWorkflowJob
import io.github.leogallego.ansiblejane.testWorkflowNode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkflowJobStatusViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeWorkflowRepo: FakeWorkflowRepository
    private lateinit var fakeJobRepo: FakeJobRepository

    @Before
    fun setup() {
        fakeWorkflowRepo = FakeWorkflowRepository()
        fakeJobRepo = FakeJobRepository()
    }

    private fun createViewModel(workflowJobId: Int = 42): WorkflowJobStatusViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("workflowJobId" to workflowJobId))
        return WorkflowJobStatusViewModel(savedStateHandle, fakeWorkflowRepo, fakeJobRepo)
    }

    // --- Initial state ---

    @Test
    fun `initial state is Loading`() = runTest {
        val viewModel = createViewModel()
        assertEquals(WorkflowJobStatusUiState.Loading, viewModel.uiState.value)
    }

    // --- Polling with active workflow job ---

    @Test
    fun `startPolling with running workflow job emits Active state`() = runTest {
        val workflowJob = testWorkflowJob(id = 42, status = JobStatus.RUNNING)
        val nodes = listOf(
            testWorkflowNode(id = 1, jobId = 10, jobName = "Step 1", jobStatus = JobStatus.SUCCESSFUL),
            testWorkflowNode(id = 2, jobId = 11, jobName = "Step 2", jobStatus = JobStatus.RUNNING)
        )
        fakeWorkflowRepo.workflowJob = workflowJob
        fakeWorkflowRepo.nodes = nodes

        val viewModel = createViewModel(workflowJobId = 42)
        viewModel.startPolling()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is WorkflowJobStatusUiState.Active)
            val active = state as WorkflowJobStatusUiState.Active
            assertEquals(42, active.workflowJob.id)
            assertEquals(JobStatus.RUNNING, active.workflowJob.status)
            assertEquals(2, active.nodes.size)
        }
    }

    // --- Polling with completed workflow job ---

    @Test
    fun `startPolling with completed workflow job emits Completed state`() = runTest {
        val workflowJob = testWorkflowJob(
            id = 42,
            status = JobStatus.SUCCESSFUL,
            finished = "2024-01-15T10:05:00Z"
        )
        val nodes = listOf(
            testWorkflowNode(id = 1, jobId = 10, jobStatus = JobStatus.SUCCESSFUL)
        )
        fakeWorkflowRepo.workflowJob = workflowJob
        fakeWorkflowRepo.nodes = nodes

        val viewModel = createViewModel(workflowJobId = 42)
        viewModel.startPolling()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is WorkflowJobStatusUiState.Completed)
            val completed = state as WorkflowJobStatusUiState.Completed
            assertEquals(JobStatus.SUCCESSFUL, completed.workflowJob.status)
            assertEquals(1, completed.nodes.size)
        }
    }

    @Test
    fun `startPolling with failed workflow job emits Completed state`() = runTest {
        val workflowJob = testWorkflowJob(id = 42, status = JobStatus.FAILED, failed = true)
        fakeWorkflowRepo.workflowJob = workflowJob

        val viewModel = createViewModel(workflowJobId = 42)
        viewModel.startPolling()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is WorkflowJobStatusUiState.Completed)
            assertEquals(JobStatus.FAILED, (state as WorkflowJobStatusUiState.Completed).workflowJob.status)
        }
    }

    // --- Error handling ---

    @Test
    fun `startPolling error emits Error state`() = runTest {
        fakeWorkflowRepo.shouldFail = true
        fakeWorkflowRepo.failureException = RuntimeException("Connection failed")

        val viewModel = createViewModel(workflowJobId = 42)
        viewModel.startPolling()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is WorkflowJobStatusUiState.Error)
            assertTrue((state as WorkflowJobStatusUiState.Error).error is AppError.Unknown)
        }
    }

    // --- Invalid workflowJobId ---

    @Test
    fun `startPolling does nothing with invalid workflowJobId`() = runTest {
        val viewModel = createViewModel(workflowJobId = -1)
        viewModel.startPolling()

        assertEquals(WorkflowJobStatusUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `startPolling does nothing with zero workflowJobId`() = runTest {
        val viewModel = createViewModel(workflowJobId = 0)
        viewModel.startPolling()

        assertEquals(WorkflowJobStatusUiState.Loading, viewModel.uiState.value)
    }

    // --- Node expansion ---

    @Test
    fun `toggleNodeExpansion sets expanded node id`() = runTest {
        val viewModel = createViewModel()
        viewModel.toggleNodeExpansion(10)

        assertEquals(10, viewModel.expandedNodeId.value)
    }

    @Test
    fun `toggleNodeExpansion collapses when toggled again`() = runTest {
        val viewModel = createViewModel()
        viewModel.toggleNodeExpansion(10)
        viewModel.toggleNodeExpansion(10)

        assertNull(viewModel.expandedNodeId.value)
    }

    @Test
    fun `toggleNodeExpansion switches to new node`() = runTest {
        val viewModel = createViewModel()
        viewModel.toggleNodeExpansion(10)
        viewModel.toggleNodeExpansion(20)

        assertEquals(20, viewModel.expandedNodeId.value)
    }

    // --- Node stdout ---

    @Test
    fun `expanding node fetches stdout`() = runTest {
        fakeJobRepo.jobStdout = "PLAY [all] ***"

        val viewModel = createViewModel()
        viewModel.toggleNodeExpansion(10)

        viewModel.nodeStdout.test {
            val stdout = expectMostRecentItem()
            assertTrue(stdout[10] is NodeStdoutState.Loaded)
            assertEquals("PLAY [all] ***", (stdout[10] as NodeStdoutState.Loaded).stdout)
        }
    }

    @Test
    fun `expanding node with blank stdout shows no output message`() = runTest {
        fakeJobRepo.jobStdout = ""

        val viewModel = createViewModel()
        viewModel.toggleNodeExpansion(10)

        viewModel.nodeStdout.test {
            val stdout = expectMostRecentItem()
            assertTrue(stdout[10] is NodeStdoutState.Loaded)
            assertEquals("No output available", (stdout[10] as NodeStdoutState.Loaded).stdout)
        }
    }

    @Test
    fun `expanding node with stdout error shows Error state`() = runTest {
        fakeJobRepo.stdoutShouldFail = true
        fakeJobRepo.failureException = RuntimeException("Failed to fetch")

        val viewModel = createViewModel()
        viewModel.toggleNodeExpansion(10)

        viewModel.nodeStdout.test {
            val stdout = expectMostRecentItem()
            assertTrue(stdout[10] is NodeStdoutState.Error)
            assertEquals("Failed to fetch", (stdout[10] as NodeStdoutState.Error).message)
        }
    }

    @Test
    fun `re-expanding cached node does not refetch stdout`() = runTest {
        fakeJobRepo.jobStdout = "initial output"

        val viewModel = createViewModel()
        viewModel.toggleNodeExpansion(10)

        // Verify stdout loaded
        viewModel.nodeStdout.test {
            assertTrue(expectMostRecentItem()[10] is NodeStdoutState.Loaded)
        }

        // Collapse
        viewModel.toggleNodeExpansion(10)
        // Change stdout
        fakeJobRepo.jobStdout = "different output"
        // Re-expand
        viewModel.toggleNodeExpansion(10)

        // Should still have cached value
        viewModel.nodeStdout.test {
            val loaded = expectMostRecentItem()[10] as NodeStdoutState.Loaded
            assertEquals("initial output", loaded.stdout)
        }
    }

    // --- Retry ---

    @Test
    fun `retry resets to Loading and restarts polling`() = runTest {
        fakeWorkflowRepo.shouldFail = true
        val viewModel = createViewModel(workflowJobId = 42)
        viewModel.startPolling()

        viewModel.uiState.test {
            assertTrue(expectMostRecentItem() is WorkflowJobStatusUiState.Error)
        }

        fakeWorkflowRepo.shouldFail = false
        fakeWorkflowRepo.workflowJob = testWorkflowJob(id = 42, status = JobStatus.SUCCESSFUL)
        viewModel.retry()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is WorkflowJobStatusUiState.Completed)
        }
    }

    // --- stopPolling ---

    @Test
    fun `stopPolling does not crash when no polling active`() {
        val viewModel = createViewModel(workflowJobId = 42)
        viewModel.stopPolling()
        assertEquals(WorkflowJobStatusUiState.Loading, viewModel.uiState.value)
    }
}

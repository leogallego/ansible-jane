package io.github.leogallego.ansiblejane.presentation.jobs

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.leogallego.ansiblejane.MainDispatcherRule
import io.github.leogallego.ansiblejane.fakes.FakeJobRepository
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.JobStatus
import io.github.leogallego.ansiblejane.testJob
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class JobStatusViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepo: FakeJobRepository

    @Before
    fun setup() {
        fakeRepo = FakeJobRepository()
    }

    private fun createViewModel(jobId: Int = 42): JobStatusViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("jobId" to jobId))
        return JobStatusViewModel(savedStateHandle, fakeRepo)
    }

    // --- Initial state ---

    @Test
    fun `initial state is Loading`() = runTest {
        val viewModel = createViewModel()
        assertEquals(JobStatusUiState.Loading, viewModel.uiState.value)
    }

    // --- startPolling with active job ---

    @Test
    fun `startPolling with running job emits Active state`() = runTest {
        val runningJob = testJob(id = 42, status = JobStatus.RUNNING)
        fakeRepo.polledJob = runningJob
        fakeRepo.jobStdout = "PLAY [all] ***"

        val viewModel = createViewModel(jobId = 42)
        viewModel.startPolling()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is JobStatusUiState.Active)
            val active = state as JobStatusUiState.Active
            assertEquals(42, active.job.id)
            assertEquals(JobStatus.RUNNING, active.job.status)
            assertEquals("PLAY [all] ***", active.stdout)
        }
    }

    // --- startPolling with completed job ---

    @Test
    fun `startPolling with completed job emits Completed state`() = runTest {
        val completedJob = testJob(
            id = 42,
            status = JobStatus.SUCCESSFUL,
            finished = "2024-01-15T10:05:00Z",
            elapsed = 300.0
        )
        fakeRepo.polledJob = completedJob
        fakeRepo.jobStdout = "PLAY RECAP ***"

        val viewModel = createViewModel(jobId = 42)
        viewModel.startPolling()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is JobStatusUiState.Completed)
            val completed = state as JobStatusUiState.Completed
            assertEquals(42, completed.job.id)
            assertEquals(JobStatus.SUCCESSFUL, completed.job.status)
            assertEquals("PLAY RECAP ***", completed.stdout)
        }
    }

    @Test
    fun `startPolling with failed job emits Completed state`() = runTest {
        val failedJob = testJob(id = 42, status = JobStatus.FAILED, failed = true)
        fakeRepo.polledJob = failedJob

        val viewModel = createViewModel(jobId = 42)
        viewModel.startPolling()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is JobStatusUiState.Completed)
            assertEquals(JobStatus.FAILED, (state as JobStatusUiState.Completed).job.status)
        }
    }

    // --- Stdout ---

    @Test
    fun `stdout is null when getJobStdout fails`() = runTest {
        val runningJob = testJob(id = 42, status = JobStatus.RUNNING)
        fakeRepo.polledJob = runningJob
        fakeRepo.stdoutShouldFail = true

        val viewModel = createViewModel(jobId = 42)
        viewModel.startPolling()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is JobStatusUiState.Active)
            assertEquals(null, (state as JobStatusUiState.Active).stdout)
        }
    }

    // --- Error handling ---

    @Test
    fun `startPolling error emits Error state`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Connection refused")

        val viewModel = createViewModel(jobId = 42)
        viewModel.startPolling()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is JobStatusUiState.Error)
            assertTrue((state as JobStatusUiState.Error).error is AppError.Unknown)
        }
    }

    // --- Invalid jobId ---

    @Test
    fun `startPolling does nothing with invalid jobId`() = runTest {
        val viewModel = createViewModel(jobId = -1)
        viewModel.startPolling()

        assertEquals(JobStatusUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `startPolling does nothing with zero jobId`() = runTest {
        val viewModel = createViewModel(jobId = 0)
        viewModel.startPolling()

        assertEquals(JobStatusUiState.Loading, viewModel.uiState.value)
    }

    // --- Retry ---

    @Test
    fun `retry resets to Loading and restarts polling`() = runTest {
        fakeRepo.shouldFail = true
        val viewModel = createViewModel(jobId = 42)
        viewModel.startPolling()

        // Error state reached
        viewModel.uiState.test {
            assertTrue(expectMostRecentItem() is JobStatusUiState.Error)
        }

        // Now fix the error and retry
        fakeRepo.shouldFail = false
        fakeRepo.polledJob = testJob(id = 42, status = JobStatus.SUCCESSFUL)
        viewModel.retry()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is JobStatusUiState.Completed)
        }
    }

    // --- stopPolling ---

    @Test
    fun `stopPolling does not crash when no polling active`() {
        val viewModel = createViewModel(jobId = 42)
        // Should not throw
        viewModel.stopPolling()
        assertEquals(JobStatusUiState.Loading, viewModel.uiState.value)
    }
}

package io.github.leogallego.ansiblejane.presentation.jobs

import app.cash.turbine.test
import io.github.leogallego.ansiblejane.fakes.FakeJobRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.JobStatus
import io.github.leogallego.ansiblejane.testInstance
import io.github.leogallego.ansiblejane.testJob
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentJobsViewModelTest {

    private lateinit var fakeRepo: FakeJobRepository
    private lateinit var fakeTokenManager: FakeTokenManager
    private lateinit var viewModel: RecentJobsViewModel

    @BeforeTest
    fun setup() {
        setupMainDispatcher()
        fakeRepo = FakeJobRepository()
        fakeTokenManager = FakeTokenManager()
    }

    @AfterTest
    fun cleanup() {
        tearDownMainDispatcher()
    }

    private fun createViewModel(): RecentJobsViewModel {
        return RecentJobsViewModel(fakeRepo, fakeTokenManager)
    }

    // --- Init / active instance ---

    @Test
    fun `initial state is Loading`() = runTest {
        viewModel = createViewModel()
        assertEquals(RecentJobsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun `auto-loads jobs when active instance exists`() = runTest {
        val jobs = listOf(
            testJob(id = 1, status = JobStatus.SUCCESSFUL),
            testJob(id = 2, status = JobStatus.RUNNING)
        )
        fakeRepo.jobs = jobs

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is RecentJobsUiState.Success)
            assertEquals(2, (state as RecentJobsUiState.Success).jobs.size)
        }
    }

    // --- loadRecentJobs ---

    @Test
    fun `loadRecentJobs success emits Success state`() = runTest {
        val jobs = listOf(testJob(id = 1, name = "Deploy"))
        fakeRepo.jobs = jobs

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is RecentJobsUiState.Success)
            val success = state as RecentJobsUiState.Success
            assertEquals(1, success.jobs.size)
            assertEquals("Deploy", success.jobs[0].name)
            assertEquals(false, success.hasMore)
        }
    }

    @Test
    fun `loadRecentJobs error emits Error state`() = runTest {
        fakeRepo.shouldFail = true

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is RecentJobsUiState.Error)
            assertTrue((state as RecentJobsUiState.Error).error is AppError.Unknown)
        }
    }

    // --- toggleFilter ---

    @Test
    fun `toggleFilter adds status to active filters`() = runTest {
        fakeRepo.jobs = listOf(testJob(status = JobStatus.SUCCESSFUL))

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.toggleFilter(JobStatus.SUCCESSFUL)

        assertTrue(viewModel.getActiveFilters().contains(JobStatus.SUCCESSFUL))
    }

    @Test
    fun `toggleFilter removes status when toggled again`() = runTest {
        fakeRepo.jobs = listOf(testJob())

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.toggleFilter(JobStatus.SUCCESSFUL)
        viewModel.toggleFilter(JobStatus.SUCCESSFUL)

        assertTrue(viewModel.getActiveFilters().isEmpty())
    }

    @Test
    fun `toggleFilter reloads jobs with filter applied`() = runTest {
        fakeRepo.jobs = listOf(testJob())

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.toggleFilter(JobStatus.FAILED)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is RecentJobsUiState.Success)
            val success = state as RecentJobsUiState.Success
            assertEquals(setOf(JobStatus.FAILED), success.activeFilters)
        }
    }

    // --- clearFilters ---

    @Test
    fun `clearFilters removes all filters and reloads`() = runTest {
        fakeRepo.jobs = listOf(testJob())

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.toggleFilter(JobStatus.SUCCESSFUL)
        viewModel.toggleFilter(JobStatus.FAILED)
        viewModel.clearFilters()

        assertTrue(viewModel.getActiveFilters().isEmpty())

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is RecentJobsUiState.Success)
            assertEquals(emptySet<JobStatus>(), (state as RecentJobsUiState.Success).activeFilters)
        }
    }

    // --- loadMore ---

    @Test
    fun `loadMore increments page when hasMore is true`() = runTest {
        fakeRepo.jobs = listOf(testJob())
        fakeRepo.hasMore = true

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is RecentJobsUiState.Success)
            assertTrue((state as RecentJobsUiState.Success).hasMore)
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
        fakeRepo.jobs = listOf(testJob())
        fakeRepo.hasMore = false

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        // Wait for initial load to finish
        viewModel.uiState.test {
            expectMostRecentItem()
        }

        fakeRepo.lastRequestedPage = 0
        viewModel.loadMore()
        // loadMore should not have been called
        assertEquals(0, fakeRepo.lastRequestedPage)
    }

    // --- refresh ---

    @Test
    fun `refresh resets page and reloads`() = runTest {
        fakeRepo.jobs = listOf(testJob())

        fakeTokenManager.setInstances(listOf(testInstance))
        viewModel = createViewModel()

        viewModel.refresh()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is RecentJobsUiState.Success)
            assertEquals(1, fakeRepo.lastRequestedPage)
        }
    }
}

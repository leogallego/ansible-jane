package com.example.aapremote.presentation.schedules

import app.cash.turbine.test
import com.example.aapremote.MainDispatcherRule
import com.example.aapremote.TestData
import com.example.aapremote.fakes.FakeScheduleRepository
import com.example.aapremote.fakes.FakeTokenManager
import com.example.aapremote.model.AppError
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
class SchedulesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepo: FakeScheduleRepository
    private lateinit var fakeTokenManager: FakeTokenManager

    @Before
    fun setup() {
        fakeRepo = FakeScheduleRepository()
        fakeTokenManager = FakeTokenManager()
    }

    private fun createViewModel(): SchedulesViewModel {
        return SchedulesViewModel(fakeRepo, fakeTokenManager)
    }

    @Test
    fun `initial state is Loading when no active instance`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(SchedulesUiState.Loading, awaitItem())
        }
    }

    @Test
    fun `loads schedules when active instance is set`() = runTest {
        fakeRepo.schedules = TestData.sampleSchedules

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is SchedulesUiState.Success)
            val success = state as SchedulesUiState.Success
            assertEquals(3, success.schedules.size)
            assertEquals("Schedule 1", success.schedules[0].name)
            assertFalse(success.hasMore)
        }
    }

    @Test
    fun `shows error state when loading fails`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Server error")

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is SchedulesUiState.Error)
            val error = state as SchedulesUiState.Error
            assertTrue(error.error is AppError.Unknown)
        }
    }

    @Test
    fun `loadMore appends schedules when hasMore is true`() = runTest {
        fakeRepo.schedules = TestData.sampleSchedules
        fakeRepo.hasMore = true

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val firstPage = awaitItem()
            assertTrue(firstPage is SchedulesUiState.Success)
            assertEquals(3, (firstPage as SchedulesUiState.Success).schedules.size)
            assertTrue(firstPage.hasMore)

            // Set up page 2 data
            fakeRepo.schedules = listOf(TestData.createSchedule(4))
            fakeRepo.hasMore = false

            viewModel.loadMore()

            val loadingMore = awaitItem()
            assertTrue(loadingMore is SchedulesUiState.Success)
            assertTrue((loadingMore as SchedulesUiState.Success).isLoadingMore)

            val secondPage = awaitItem()
            assertTrue(secondPage is SchedulesUiState.Success)
            assertEquals(4, (secondPage as SchedulesUiState.Success).schedules.size)
            assertFalse(secondPage.hasMore)
        }
    }

    @Test
    fun `loadMore does nothing when hasMore is false`() = runTest {
        fakeRepo.schedules = TestData.sampleSchedules

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is SchedulesUiState.Success)
            assertFalse((state as SchedulesUiState.Success).hasMore)

            viewModel.loadMore()
            expectNoEvents()
        }
    }

    @Test
    fun `toggleSchedule updates schedule enabled state`() = runTest {
        val schedule = TestData.createSchedule(1, enabled = true)
        fakeRepo.schedules = listOf(schedule)

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is SchedulesUiState.Success)
            assertTrue((initial as SchedulesUiState.Success).schedules[0].enabled)

            viewModel.toggleSchedule(schedule)

            // With UnconfinedTestDispatcher, both the optimistic update and
            // server confirmation happen synchronously. Turbine may conflate
            // intermediate states, so we check the final state.
            val updated = expectMostRecentItem()
            assertTrue(updated is SchedulesUiState.Success)
            assertFalse((updated as SchedulesUiState.Success).schedules[0].enabled)
        }
    }

    @Test
    fun `toggleSchedule reverts on failure and emits error`() = runTest {
        val schedule = TestData.createSchedule(1, enabled = true)
        fakeRepo.schedules = listOf(schedule)
        fakeRepo.toggleShouldFail = true
        fakeRepo.toggleFailureException = RuntimeException("Toggle failed")

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is SchedulesUiState.Success)
            assertTrue((initial as SchedulesUiState.Success).schedules[0].enabled)

            viewModel.toggleSchedule(schedule)

            // After failure, the schedule should be reverted to original state
            val reverted = expectMostRecentItem()
            assertTrue(reverted is SchedulesUiState.Success)
            assertTrue((reverted as SchedulesUiState.Success).schedules[0].enabled)
        }

        // Verify error was emitted via toggleError SharedFlow
        viewModel.toggleError.test {
            // Trigger another toggle to get a fresh error emission
            fakeRepo.toggleShouldFail = true
            val schedule2 = (viewModel.uiState.value as SchedulesUiState.Success).schedules[0]
            viewModel.toggleSchedule(schedule2)
            val errorMessage = awaitItem()
            assertEquals("Toggle failed", errorMessage)
        }
    }

    @Test
    fun `refresh reloads schedules from page 1`() = runTest {
        fakeRepo.schedules = TestData.sampleSchedules

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is SchedulesUiState.Success)
            assertEquals(3, (initial as SchedulesUiState.Success).schedules.size)

            fakeRepo.schedules = listOf(TestData.createSchedule(99, "Updated Schedule"))
            viewModel.refresh()

            val refreshed = awaitItem()
            assertTrue(refreshed is SchedulesUiState.Success)
            assertEquals(1, (refreshed as SchedulesUiState.Success).schedules.size)
            assertEquals("Updated Schedule", refreshed.schedules[0].name)
        }
    }

    @Test
    fun `reloads when active instance changes`() = runTest {
        fakeRepo.schedules = TestData.sampleSchedules

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is SchedulesUiState.Success)

            fakeRepo.schedules = listOf(TestData.createSchedule(10, "New Instance Schedule"))
            fakeTokenManager.setInstances(listOf(TestData.testInstance2))

            val loading = awaitItem()
            assertEquals(SchedulesUiState.Loading, loading)

            val newState = awaitItem()
            assertTrue(newState is SchedulesUiState.Success)
            assertEquals(
                "New Instance Schedule",
                (newState as SchedulesUiState.Success).schedules[0].name
            )
        }
    }
}

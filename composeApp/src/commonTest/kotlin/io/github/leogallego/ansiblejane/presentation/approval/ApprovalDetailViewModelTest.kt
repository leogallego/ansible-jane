package io.github.leogallego.ansiblejane.presentation.approval

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.leogallego.ansiblejane.fakes.FakeWorkflowRepository
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.WorkflowApproval
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApprovalDetailViewModelTest {

    private lateinit var fakeRepo: FakeWorkflowRepository

    private val testApproval = WorkflowApproval(
        id = 42,
        name = "Deploy to production",
        status = "pending",
        created = "2024-01-15T10:00:00Z"
    )

    @BeforeTest
    fun setup() {
        setupMainDispatcher()
        fakeRepo = FakeWorkflowRepository()
    }

    @AfterTest
    fun cleanup() {
        tearDownMainDispatcher()
    }

    private fun createViewModel(approvalId: Int = 42): ApprovalDetailViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("approvalId" to approvalId))
        return ApprovalDetailViewModel(savedStateHandle, fakeRepo)
    }

    @Test
    fun `invalid approval ID emits Error state`() = runTest {
        val viewModel = createViewModel(approvalId = -1)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is ApprovalDetailUiState.Error)
            val error = (state as ApprovalDetailUiState.Error).error
            assertTrue(error is AppError.Unknown)
        }
    }

    @Test
    fun `zero approval ID emits Error state`() = runTest {
        val viewModel = createViewModel(approvalId = 0)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is ApprovalDetailUiState.Error)
        }
    }

    @Test
    fun `loadApproval success emits Ready state`() = runTest {
        fakeRepo.approvals = listOf(testApproval)
        val viewModel = createViewModel(approvalId = 42)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is ApprovalDetailUiState.Ready)
            assertEquals(testApproval, (state as ApprovalDetailUiState.Ready).approval)
        }
    }

    @Test
    fun `loadApproval failure emits Error state`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Network error")
        val viewModel = createViewModel(approvalId = 42)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is ApprovalDetailUiState.Error)
        }
    }

    @Test
    fun `loadApproval with nonexistent ID emits Error state`() = runTest {
        fakeRepo.approvals = listOf(testApproval)
        val viewModel = createViewModel(approvalId = 999)

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue(state is ApprovalDetailUiState.Error)
        }
    }

    @Test
    fun `approve transitions to Completed with approved action`() = runTest {
        fakeRepo.approvals = listOf(testApproval)
        val viewModel = createViewModel(approvalId = 42)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.approve()
            val state = expectMostRecentItem()
            assertTrue(state is ApprovalDetailUiState.Completed)
            val completed = state as ApprovalDetailUiState.Completed
            assertEquals("approved", completed.action)
            assertEquals(testApproval, completed.approval)
        }
    }

    @Test
    fun `deny transitions to Completed with denied action`() = runTest {
        fakeRepo.approvals = listOf(testApproval)
        val viewModel = createViewModel(approvalId = 42)

        viewModel.uiState.test {
            skipItems(1)
            viewModel.deny()
            val state = expectMostRecentItem()
            assertTrue(state is ApprovalDetailUiState.Completed)
            val completed = state as ApprovalDetailUiState.Completed
            assertEquals("denied", completed.action)
            assertEquals(testApproval, completed.approval)
        }
    }

    @Test
    fun `approve from non-Ready state is no-op`() = runTest {
        fakeRepo.shouldFail = true
        val viewModel = createViewModel(approvalId = 42)

        viewModel.uiState.test {
            val errorState = expectMostRecentItem()
            assertTrue(errorState is ApprovalDetailUiState.Error)
            viewModel.approve()
            expectNoEvents()
        }
    }

    @Test
    fun `deny from non-Ready state is no-op`() = runTest {
        fakeRepo.shouldFail = true
        val viewModel = createViewModel(approvalId = 42)

        viewModel.uiState.test {
            val errorState = expectMostRecentItem()
            assertTrue(errorState is ApprovalDetailUiState.Error)
            viewModel.deny()
            expectNoEvents()
        }
    }

    @Test
    fun `approve failure emits Error state`() = runTest {
        fakeRepo.approvals = listOf(testApproval)
        val viewModel = createViewModel(approvalId = 42)

        viewModel.uiState.test {
            skipItems(1)
            fakeRepo.shouldFail = true
            fakeRepo.failureException = RuntimeException("Server error")
            viewModel.approve()
            val state = expectMostRecentItem()
            assertTrue(state is ApprovalDetailUiState.Error)
        }
    }

    @Test
    fun `deny failure emits Error state`() = runTest {
        fakeRepo.approvals = listOf(testApproval)
        val viewModel = createViewModel(approvalId = 42)

        viewModel.uiState.test {
            skipItems(1)
            fakeRepo.shouldFail = true
            fakeRepo.failureException = RuntimeException("Server error")
            viewModel.deny()
            val state = expectMostRecentItem()
            assertTrue(state is ApprovalDetailUiState.Error)
        }
    }

    @Test
    fun `reload after error transitions back to Ready`() = runTest {
        fakeRepo.shouldFail = true
        val viewModel = createViewModel(approvalId = 42)

        viewModel.uiState.test {
            val errorState = expectMostRecentItem()
            assertTrue(errorState is ApprovalDetailUiState.Error)

            fakeRepo.shouldFail = false
            fakeRepo.approvals = listOf(testApproval)
            viewModel.loadApproval()

            val state = expectMostRecentItem()
            assertTrue(state is ApprovalDetailUiState.Ready)
            assertEquals(testApproval, (state as ApprovalDetailUiState.Ready).approval)
        }
    }
}

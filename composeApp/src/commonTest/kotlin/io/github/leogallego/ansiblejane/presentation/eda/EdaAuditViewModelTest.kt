package io.github.leogallego.ansiblejane.presentation.eda

import app.cash.turbine.test
import io.github.leogallego.ansiblejane.TestData
import io.github.leogallego.ansiblejane.fakes.FakeEdaAuditRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.model.AppError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.get
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EdaAuditViewModelTest {

    private lateinit var fakeRepo: FakeEdaAuditRepository
    private lateinit var fakeTokenManager: FakeTokenManager

    @BeforeTest
    fun setup() {
        setupMainDispatcher()
        fakeRepo = FakeEdaAuditRepository()
        fakeTokenManager = FakeTokenManager()
    }

    @AfterTest
    fun cleanup() {
        tearDownMainDispatcher()
    }

    private fun createViewModel(): EdaAuditViewModel {
        return EdaAuditViewModel(fakeRepo, fakeTokenManager)
    }

    private suspend fun createHttpException(code: Int): Exception {
        val client = HttpClient(MockEngine) {
            expectSuccess = true
            engine {
                addHandler { respond("", HttpStatusCode.fromValue(code)) }
            }
        }
        return try {
            client.get("/")
            error("Expected exception for status $code")
        } catch (e: ClientRequestException) {
            e
        } catch (e: ServerResponseException) {
            e
        } finally {
            client.close()
        }
    }

    @Test
    fun `initial state is Loading when no active instance`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(EdaAuditUiState.Loading, awaitItem())
        }
    }

    @Test
    fun `loads audit rules when active instance is set`() = runTest {
        fakeRepo.auditRules = TestData.sampleEdaRuleAudits

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is EdaAuditUiState.Success)
            val success = state as EdaAuditUiState.Success
            assertEquals(3, success.auditRules.size)
            assertEquals("Rule Audit 1", success.auditRules[0].name)
            assertFalse(success.hasMore)
        }
    }

    @Test
    fun `shows error state when loading fails with generic error`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = RuntimeException("Connection timeout")

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is EdaAuditUiState.Error)
            val error = state as EdaAuditUiState.Error
            assertTrue(error.error is AppError.Unknown)
        }
    }

    @Test
    fun `shows empty state when no audit rules returned`() = runTest {
        fakeRepo.auditRules = emptyList()

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is EdaAuditUiState.Empty)
            assertEquals("No EDA audit events", (state as EdaAuditUiState.Empty).message)
        }
    }

    @Test
    fun `treats 404 as EDA not configured empty state`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = createHttpException(404)

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is EdaAuditUiState.Empty)
            assertEquals(
                "EDA is not configured on this AAP instance",
                (state as EdaAuditUiState.Empty).message
            )
        }
    }

    @Test
    fun `treats 502 as EDA not configured empty state`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = createHttpException(502)

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is EdaAuditUiState.Empty)
            assertEquals(
                "EDA is not configured on this AAP instance",
                (state as EdaAuditUiState.Empty).message
            )
        }
    }

    @Test
    fun `treats 503 as EDA not configured empty state`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = createHttpException(503)

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is EdaAuditUiState.Empty)
            assertEquals(
                "EDA is not configured on this AAP instance",
                (state as EdaAuditUiState.Empty).message
            )
        }
    }

    @Test
    fun `treats 500 as server error, not empty state`() = runTest {
        fakeRepo.shouldFail = true
        fakeRepo.failureException = createHttpException(500)

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is EdaAuditUiState.Error)
            val error = state as EdaAuditUiState.Error
            assertTrue(error.error is AppError.Server)
        }
    }

    @Test
    fun `loadMore appends audit rules when hasMore is true`() = runTest {
        fakeRepo.auditRules = TestData.sampleEdaRuleAudits
        fakeRepo.hasMore = true

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val firstPage = awaitItem()
            assertTrue(firstPage is EdaAuditUiState.Success)
            assertEquals(3, (firstPage as EdaAuditUiState.Success).auditRules.size)
            assertTrue(firstPage.hasMore)

            // Set up page 2 data
            fakeRepo.auditRules = listOf(TestData.createEdaRuleAudit(4))
            fakeRepo.hasMore = false

            viewModel.loadMore()

            val loadingMore = awaitItem()
            assertTrue(loadingMore is EdaAuditUiState.Success)
            assertTrue((loadingMore as EdaAuditUiState.Success).isLoadingMore)

            val secondPage = awaitItem()
            assertTrue(secondPage is EdaAuditUiState.Success)
            assertEquals(4, (secondPage as EdaAuditUiState.Success).auditRules.size)
            assertFalse(secondPage.hasMore)
        }
    }

    @Test
    fun `refresh reloads audit rules from page 1`() = runTest {
        fakeRepo.auditRules = TestData.sampleEdaRuleAudits

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is EdaAuditUiState.Success)

            fakeRepo.auditRules = listOf(TestData.createEdaRuleAudit(99, "Refreshed Rule"))
            viewModel.refresh()

            val refreshed = awaitItem()
            assertTrue(refreshed is EdaAuditUiState.Success)
            assertEquals(1, (refreshed as EdaAuditUiState.Success).auditRules.size)
            assertEquals("Refreshed Rule", refreshed.auditRules[0].name)
        }
    }

    @Test
    fun `reloads when active instance changes`() = runTest {
        fakeRepo.auditRules = TestData.sampleEdaRuleAudits

        fakeTokenManager.setInstances(listOf(TestData.testInstance))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(initial is EdaAuditUiState.Success)

            fakeRepo.auditRules = listOf(TestData.createEdaRuleAudit(10, "New Instance Rule"))
            fakeTokenManager.setInstances(listOf(TestData.testInstance2))

            val loading = awaitItem()
            assertEquals(EdaAuditUiState.Loading, loading)

            val newState = awaitItem()
            assertTrue(newState is EdaAuditUiState.Success)
            assertEquals(
                "New Instance Rule",
                (newState as EdaAuditUiState.Success).auditRules[0].name
            )
        }
    }
}

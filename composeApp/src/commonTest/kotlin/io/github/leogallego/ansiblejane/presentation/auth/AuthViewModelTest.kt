package io.github.leogallego.ansiblejane.presentation.auth

import app.cash.turbine.test
import io.github.leogallego.ansiblejane.data.CredentialStatus
import io.github.leogallego.ansiblejane.fakes.FakeAuthRepository
import io.github.leogallego.ansiblejane.model.AppError
import io.github.leogallego.ansiblejane.model.User
import io.github.leogallego.ansiblejane.setupMainDispatcher
import io.github.leogallego.ansiblejane.tearDownMainDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthViewModelTest {

    private lateinit var fakeAuthRepo: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    private val testUser = User(
        id = 1,
        username = "admin",
        firstName = "Test",
        lastName = "User",
        email = "admin@example.com",
        isSuperuser = true
    )

    @BeforeTest
    fun setup() {
        setupMainDispatcher()
        fakeAuthRepo = FakeAuthRepository()
        viewModel = AuthViewModel(fakeAuthRepo)
    }

    @AfterTest
    fun cleanup() {
        tearDownMainDispatcher()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `connect with valid credentials transitions to Success`() = runTest {
        fakeAuthRepo.validateResult = Result.success(testUser)

        viewModel.connect(
            baseUrl = "https://aap.example.com",
            token = "valid-token",
            trustSelfSigned = false
        )

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Success)
        assertEquals("admin", (state as AuthUiState.Success).username)
    }

    @Test
    fun `connect with blank URL emits Error immediately`() = runTest {
        viewModel.connect(
            baseUrl = "",
            token = "some-token",
            trustSelfSigned = false
        )

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals(
            "URL and token are required",
            (state as AuthUiState.Error).error.message
        )
    }

    @Test
    fun `connect with blank token emits Error immediately`() = runTest {
        viewModel.connect(
            baseUrl = "https://aap.example.com",
            token = "",
            trustSelfSigned = false
        )

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        assertEquals(
            "URL and token are required",
            (state as AuthUiState.Error).error.message
        )
    }

    @Test
    fun `connect failure transitions to Error with AppError`() = runTest {
        fakeAuthRepo.shouldFail = true
        fakeAuthRepo.failureException = RuntimeException("Connection refused")

        viewModel.connect(
            baseUrl = "https://aap.example.com",
            token = "bad-token",
            trustSelfSigned = false
        )

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Error)
        val appError = (state as AuthUiState.Error).error
        assertTrue(appError is AppError.Unknown)
        assertEquals("Connection refused", appError.message)
    }

    @Test
    fun `resetState returns to Idle from Error`() = runTest {
        fakeAuthRepo.shouldFail = true
        viewModel.connect(
            baseUrl = "https://aap.example.com",
            token = "token",
            trustSelfSigned = false
        )
        assertTrue(viewModel.uiState.value is AuthUiState.Error)

        viewModel.resetState()
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `resetState returns to Idle from Success`() = runTest {
        fakeAuthRepo.validateResult = Result.success(testUser)
        viewModel.connect(
            baseUrl = "https://aap.example.com",
            token = "token",
            trustSelfSigned = false
        )
        assertTrue(viewModel.uiState.value is AuthUiState.Success)

        viewModel.resetState()
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `checkExistingCredentials with valid result transitions to Success`() = runTest {
        fakeAuthRepo.existingCredentialsResult = CredentialStatus.Valid(testUser)

        viewModel.checkExistingCredentials()

        val state = viewModel.uiState.value
        assertTrue(state is AuthUiState.Success)
        assertEquals("admin", (state as AuthUiState.Success).username)
    }

    @Test
    fun `checkExistingCredentials with null result stays Idle`() = runTest {
        fakeAuthRepo.existingCredentialsResult = CredentialStatus.NoCredentials

        viewModel.checkExistingCredentials()

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `checkExistingCredentials with failure returns to Idle`() = runTest {
        fakeAuthRepo.existingCredentialsResult =
            CredentialStatus.ValidationFailed(RuntimeException("Token expired"))

        viewModel.checkExistingCredentials()

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `logout transitions to Idle`() = runTest {
        fakeAuthRepo.validateResult = Result.success(testUser)
        viewModel.connect(
            baseUrl = "https://aap.example.com",
            token = "token",
            trustSelfSigned = false
        )
        assertTrue(viewModel.uiState.value is AuthUiState.Success)

        viewModel.logout()

        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `connect passes alias and existingInstanceId to repository`() = runTest {
        fakeAuthRepo.validateResult = Result.success(testUser)

        viewModel.connect(
            baseUrl = "https://aap.example.com",
            token = "token",
            trustSelfSigned = true,
            alias = "My Server",
            existingInstanceId = "existing-123"
        )

        assertTrue(viewModel.uiState.value is AuthUiState.Success)
    }
}

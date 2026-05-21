package com.example.aapremote.fakes

import com.example.aapremote.data.IAuthRepository
import com.example.aapremote.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : IAuthRepository {
    var validateResult: Result<User>? = null
    var shouldFail = false
    var failureException: Exception = RuntimeException("Test error")
    var existingCredentialsResult: Result<User>? = null
    private val _isLoggedIn = MutableStateFlow(false)

    override suspend fun validateCredentials(
        baseUrl: String,
        token: String,
        trustSelfSigned: Boolean,
        alias: String?,
        existingInstanceId: String?
    ): Result<User> {
        if (shouldFail) return Result.failure(failureException)
        return validateResult ?: Result.failure(RuntimeException("No result configured"))
    }

    override suspend fun reAuthenticate(instanceId: String, newToken: String): Result<User> {
        if (shouldFail) return Result.failure(failureException)
        return validateResult ?: Result.failure(RuntimeException("No result configured"))
    }

    override suspend fun checkExistingCredentials(): Result<User>? {
        return existingCredentialsResult
    }

    override suspend fun logoutInstance(instanceId: String) {
        _isLoggedIn.value = false
    }

    override suspend fun logout() {
        _isLoggedIn.value = false
    }

    override fun isLoggedIn(): Flow<Boolean> = _isLoggedIn

    fun setLoggedIn(loggedIn: Boolean) {
        _isLoggedIn.value = loggedIn
    }
}

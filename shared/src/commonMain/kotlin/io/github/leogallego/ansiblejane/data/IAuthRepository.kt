package io.github.leogallego.ansiblejane.data

import io.github.leogallego.ansiblejane.model.User
import kotlinx.coroutines.flow.Flow

sealed class CredentialStatus {
    data class Valid(val user: User) : CredentialStatus()
    data object NoCredentials : CredentialStatus()
    data class ValidationFailed(val error: Throwable) : CredentialStatus()
}

interface IAuthRepository {
    suspend fun validateCredentials(
        baseUrl: String,
        token: String,
        trustSelfSigned: Boolean,
        alias: String? = null,
        existingInstanceId: String? = null
    ): Result<User>

    suspend fun reAuthenticate(instanceId: String, newToken: String): Result<User>
    suspend fun checkExistingCredentials(): CredentialStatus
    suspend fun logoutInstance(instanceId: String)
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
}

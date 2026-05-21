package com.example.aapremote.data

import com.example.aapremote.model.User
import kotlinx.coroutines.flow.Flow

interface IAuthRepository {
    suspend fun validateCredentials(
        baseUrl: String,
        token: String,
        trustSelfSigned: Boolean,
        alias: String? = null,
        existingInstanceId: String? = null
    ): Result<User>

    suspend fun reAuthenticate(instanceId: String, newToken: String): Result<User>
    suspend fun checkExistingCredentials(): Result<User>?
    suspend fun logoutInstance(instanceId: String)
    suspend fun logout()
    fun isLoggedIn(): Flow<Boolean>
}

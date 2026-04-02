package com.example.aapremote.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: () -> String?
) : Interceptor {

    companion object {
        private val _unauthorizedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val unauthorizedEvent: SharedFlow<Unit> = _unauthorizedEvent.asSharedFlow()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider()
        val finalRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Content-Type", "application/json")
                .build()
        } else {
            request
        }

        val response = chain.proceed(finalRequest)

        if (response.code == 401) {
            _unauthorizedEvent.tryEmit(Unit)
        }

        return response
    }
}

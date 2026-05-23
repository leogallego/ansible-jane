package io.github.leogallego.ansiblejane.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val instanceIdProvider: () -> String?
) : Interceptor {

    constructor(tokenProvider: () -> String?) : this(tokenProvider, { null })

    companion object {
        private val _unauthorizedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val unauthorizedEvent: SharedFlow<String> = _unauthorizedEvent.asSharedFlow()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider()
        val finalRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(finalRequest)

        if (response.code == 401) {
            val instanceId = instanceIdProvider() ?: ""
            _unauthorizedEvent.tryEmit(instanceId)
        }

        return response
    }
}

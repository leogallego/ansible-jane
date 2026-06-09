package io.github.leogallego.ansiblejane.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AuthEvents {
    private val _unauthorizedEvent = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val unauthorizedEvent: SharedFlow<String> = _unauthorizedEvent.asSharedFlow()

    internal fun emitUnauthorized(instanceId: String) {
        _unauthorizedEvent.tryEmit(instanceId)
    }
}

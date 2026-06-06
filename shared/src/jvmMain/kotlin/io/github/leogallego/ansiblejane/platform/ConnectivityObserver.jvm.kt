package io.github.leogallego.ansiblejane.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class ConnectivityObserver {
    actual fun observe(): Flow<Boolean> = flowOf(true)
}

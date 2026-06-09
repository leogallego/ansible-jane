package io.github.leogallego.ansiblejane.platform

import kotlinx.coroutines.flow.Flow

expect class ConnectivityObserver {
    fun observe(): Flow<Boolean>
}

package io.github.leogallego.ansiblejane.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.coroutines.coroutineContext

actual class ConnectivityObserver {
    actual fun observe(): Flow<Boolean> = flow {
        while (coroutineContext.isActive) {
            val isConnected = checkConnectivity()
            emit(isConnected)
            delay(30_000)
        }
    }

    private suspend fun checkConnectivity(): Boolean = withContext(Dispatchers.IO) {
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), 5_000)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}

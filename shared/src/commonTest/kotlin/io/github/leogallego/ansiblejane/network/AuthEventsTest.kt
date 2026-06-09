package io.github.leogallego.ansiblejane.network

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthEventsTest {

    @Test
    fun emitUnauthorizedPublishesInstanceId() = runTest {
        AuthEvents.unauthorizedEvent.test {
            AuthEvents.emitUnauthorized("instance-123")
            assertEquals("instance-123", awaitItem())
        }
    }
}

package io.github.leogallego.ansiblejane.notification

import android.content.Intent
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.network.AapApiClient
import io.github.leogallego.ansiblejane.network.EdaApiClient
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import io.github.leogallego.ansiblejane.network.PlatformApiClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class ApprovalActionReceiverTest {

    private lateinit var fakeTokenManager: FakeTokenManager
    private val apiCalls = mutableListOf<String>()
    private lateinit var callLatch: CountDownLatch

    private val testInstance = AapInstance(
        id = "inst-1",
        baseUrl = "https://aap.example.com",
        token = "test-token"
    )

    private fun buildTrackingApiProvider(shouldFail: Boolean = false): IAapApiProvider {
        val mockEngine = MockEngine { request ->
            apiCalls.add(request.url.encodedPath)
            callLatch.countDown()
            if (shouldFail) {
                throw java.io.IOException("Connection refused")
            } else {
                respond("{}", status = HttpStatusCode.OK)
            }
        }
        val client = HttpClient(mockEngine)
        val apiClient = AapApiClient(client)
        return object : IAapApiProvider {
            override fun getApiService() = apiClient
            override fun getEdaApiService(): EdaApiClient = throw UnsupportedOperationException()
            override fun getPlatformApiService(): PlatformApiClient = throw UnsupportedOperationException()
            override fun evictInstance(instanceId: String) {}
        }
    }

    @Before
    fun setup() {
        fakeTokenManager = FakeTokenManager()
        apiCalls.clear()
        callLatch = CountDownLatch(1)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    private fun startKoinWith(apiProvider: IAapApiProvider = buildTrackingApiProvider()) {
        startKoin {
            modules(module {
                single<ITokenManager> { fakeTokenManager }
                single<IAapApiProvider> { apiProvider }
            })
        }
    }

    private fun buildIntent(action: String?, approvalId: Int = 42): Intent {
        return Intent(action).apply {
            putExtra(ApprovalActionReceiver.EXTRA_APPROVAL_ID, approvalId)
        }
    }

    @Test
    fun `unknown action is ignored`() {
        startKoinWith()
        val receiver = ApprovalActionReceiver()
        val context = RuntimeEnvironment.getApplication()

        receiver.onReceive(context, buildIntent("com.example.UNKNOWN_ACTION"))

        assertTrue("No API calls should be made for unknown action", apiCalls.isEmpty())
    }

    @Test
    fun `null action is ignored`() {
        startKoinWith()
        val receiver = ApprovalActionReceiver()
        val context = RuntimeEnvironment.getApplication()

        receiver.onReceive(context, buildIntent(null))

        assertTrue("No API calls should be made for null action", apiCalls.isEmpty())
    }

    @Test
    fun `negative approval ID is rejected`() {
        startKoinWith()
        fakeTokenManager.setInstances(listOf(testInstance))
        val receiver = ApprovalActionReceiver()
        val context = RuntimeEnvironment.getApplication()

        receiver.onReceive(context, buildIntent(ApprovalActionReceiver.ACTION_APPROVE, approvalId = -1))

        assertTrue("No API calls for negative ID", apiCalls.isEmpty())
    }

    @Test
    fun `zero approval ID is rejected`() {
        startKoinWith()
        fakeTokenManager.setInstances(listOf(testInstance))
        val receiver = ApprovalActionReceiver()
        val context = RuntimeEnvironment.getApplication()

        receiver.onReceive(context, buildIntent(ApprovalActionReceiver.ACTION_APPROVE, approvalId = 0))

        assertTrue("No API calls for zero ID", apiCalls.isEmpty())
    }

    @Test
    fun `approve action calls approveWorkflow endpoint`() {
        startKoinWith()
        fakeTokenManager.setInstances(listOf(testInstance))
        val receiver = ApprovalActionReceiver()
        val context = RuntimeEnvironment.getApplication()

        receiver.onReceive(context, buildIntent(ApprovalActionReceiver.ACTION_APPROVE, approvalId = 42))

        callLatch.await(5, TimeUnit.SECONDS)

        assertTrue(
            "Should call approve endpoint",
            apiCalls.any { it.contains("workflow_approvals/42/approve") }
        )
    }

    @Test
    fun `deny action calls denyWorkflow endpoint`() {
        startKoinWith()
        fakeTokenManager.setInstances(listOf(testInstance))
        val receiver = ApprovalActionReceiver()
        val context = RuntimeEnvironment.getApplication()

        receiver.onReceive(context, buildIntent(ApprovalActionReceiver.ACTION_DENY, approvalId = 42))

        callLatch.await(5, TimeUnit.SECONDS)

        assertTrue(
            "Should call deny endpoint",
            apiCalls.any { it.contains("workflow_approvals/42/deny") }
        )
    }

    @Test
    fun `no active instance shows error notification`() {
        startKoinWith()
        ApprovalNotificationManager.createChannel(RuntimeEnvironment.getApplication())
        val receiver = ApprovalActionReceiver()
        val context = RuntimeEnvironment.getApplication()

        receiver.onReceive(context, buildIntent(ApprovalActionReceiver.ACTION_APPROVE, approvalId = 42))

        val notifManager = shadowOf(
            context.getSystemService(android.app.NotificationManager::class.java)
        )
        // Poll for error notification instead of fixed sleep — production timeout is 5s,
        // so we poll up to 10s with short intervals to avoid flakiness on slow CI.
        var notification: android.app.Notification? = null
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            notification = notifManager.getNotification(42)
            if (notification != null) break
            Thread.sleep(200)
        }
        assertNotNull("Error notification should be posted", notification)
        val extras = notification!!.extras
        assertTrue(
            "Title should indicate failure",
            extras.getString("android.title")?.contains("Failed") == true
        )
    }

    @Test
    fun `API failure shows error notification with retry`() {
        startKoinWith(apiProvider = buildTrackingApiProvider(shouldFail = true))
        ApprovalNotificationManager.createChannel(RuntimeEnvironment.getApplication())
        fakeTokenManager.setInstances(listOf(testInstance))
        val receiver = ApprovalActionReceiver()
        val context = RuntimeEnvironment.getApplication()

        receiver.onReceive(context, buildIntent(ApprovalActionReceiver.ACTION_APPROVE, approvalId = 42))

        callLatch.await(5, TimeUnit.SECONDS)

        // Poll briefly — notification is posted right after the IOException is caught
        val notifManager = shadowOf(
            context.getSystemService(android.app.NotificationManager::class.java)
        )
        var notification: android.app.Notification? = null
        val deadline = System.currentTimeMillis() + 2_000
        while (System.currentTimeMillis() < deadline) {
            notification = notifManager.getNotification(42)
            if (notification != null) break
            Thread.sleep(50)
        }
        assertNotNull("Error notification should replace original", notification)
        assertEquals("Should have Retry action", 1, notification!!.actions.size)
        assertEquals("Retry", notification.actions[0].title)
    }
}

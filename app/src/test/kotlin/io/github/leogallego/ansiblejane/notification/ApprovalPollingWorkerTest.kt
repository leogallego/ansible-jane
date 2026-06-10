package io.github.leogallego.ansiblejane.notification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import io.github.leogallego.ansiblejane.data.ITokenManager
import io.github.leogallego.ansiblejane.data.IUserPreferencesRepository
import io.github.leogallego.ansiblejane.fakes.FakeTokenManager
import io.github.leogallego.ansiblejane.fakes.FakeUserPreferencesRepository
import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.model.PaginatedResponse
import io.github.leogallego.ansiblejane.model.WorkflowApproval
import io.github.leogallego.ansiblejane.network.AapApiClient
import io.github.leogallego.ansiblejane.network.EdaApiClient
import io.github.leogallego.ansiblejane.network.IAapApiProvider
import io.github.leogallego.ansiblejane.network.PlatformApiClient
import io.github.leogallego.ansiblejane.platform.DataStoreFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ApprovalPollingWorkerTest {

    private lateinit var fakeTokenManager: FakeTokenManager
    private lateinit var fakeUserPreferences: FakeUserPreferencesRepository
    private lateinit var approvalTracker: ApprovalTracker
    private lateinit var notificationManager: ApprovalNotificationManager
    private val json = Json { ignoreUnknownKeys = true }

    private val testInstance = AapInstance(
        id = "inst-1",
        baseUrl = "https://aap.example.com",
        token = "test-token"
    )

    private val approval1 = WorkflowApproval(id = 10, name = "Deploy prod")
    private val approval2 = WorkflowApproval(id = 20, name = "Deploy staging")

    @Before
    fun setup() {
        fakeTokenManager = FakeTokenManager()
        fakeUserPreferences = FakeUserPreferencesRepository()
        val context: Context = ApplicationProvider.getApplicationContext()
        approvalTracker = ApprovalTracker(DataStoreFactory(context))
        notificationManager = ApprovalNotificationManager()
        ApprovalNotificationManager.createChannel(context)
    }

    @After
    fun tearDown() {
        stopKoin()
        val context: Context = ApplicationProvider.getApplicationContext()
        context.filesDir.resolve("datastore").deleteRecursively()
    }

    private fun buildMockApiProvider(
        approvals: List<WorkflowApproval> = emptyList(),
        shouldFail: Boolean = false
    ): IAapApiProvider {
        val responseJson = json.encodeToString(
            PaginatedResponse(count = approvals.size, results = approvals)
        )
        val mockEngine = MockEngine { _ ->
            if (shouldFail) {
                respond("error", status = HttpStatusCode.InternalServerError)
            } else {
                respond(
                    content = responseJson,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(json) }
        }
        val apiClient = AapApiClient(client)
        return object : IAapApiProvider {
            override fun getApiService() = apiClient
            override fun getEdaApiService(): EdaApiClient = throw UnsupportedOperationException()
            override fun getPlatformApiService(): PlatformApiClient = throw UnsupportedOperationException()
            override fun evictInstance(instanceId: String) {}
        }
    }

    private fun startKoinWith(apiProvider: IAapApiProvider = buildMockApiProvider()) {
        startKoin {
            modules(module {
                single<ITokenManager> { fakeTokenManager }
                single<IAapApiProvider> { apiProvider }
                single<IUserPreferencesRepository> { fakeUserPreferences }
                single { approvalTracker }
                single { notificationManager }
            })
        }
    }

    private fun buildWorker(): ApprovalPollingWorker {
        val context: Context = ApplicationProvider.getApplicationContext()
        return TestListenableWorkerBuilder<ApprovalPollingWorker>(context).build()
    }

    @Test
    fun `no active instance returns success`() = runTest {
        startKoinWith()
        val worker = buildWorker()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `polling disabled returns success`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        fakeUserPreferences.setApprovalPollingEnabled("inst-1", false)
        startKoinWith()
        val worker = buildWorker()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `new approvals are marked as seen`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        startKoinWith(apiProvider = buildMockApiProvider(listOf(approval1, approval2)))
        val worker = buildWorker()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        val seenIds = approvalTracker.getSeenIds()
        assertTrue("Approval 10 should be marked seen", 10 in seenIds)
        assertTrue("Approval 20 should be marked seen", 20 in seenIds)
    }

    @Test
    fun `stale seen IDs are pruned`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        approvalTracker.markSeen(setOf(10, 99))
        startKoinWith(apiProvider = buildMockApiProvider(listOf(approval1)))
        val worker = buildWorker()

        worker.doWork()

        val seenIds = approvalTracker.getSeenIds()
        assertTrue("Active ID should be retained", 10 in seenIds)
        assertTrue("Stale ID should be pruned", 99 !in seenIds)
    }

    @Test
    fun `API failure returns retry`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        startKoinWith(apiProvider = buildMockApiProvider(shouldFail = true))
        val worker = buildWorker()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.retry(), result)
    }

    @Test
    fun `empty pending approvals returns success`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        startKoinWith(apiProvider = buildMockApiProvider(emptyList()))
        val worker = buildWorker()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun `polling enabled by default`() = runTest {
        fakeTokenManager.setInstances(listOf(testInstance))
        startKoinWith(apiProvider = buildMockApiProvider(listOf(approval1)))
        val worker = buildWorker()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue("Should poll when enabled by default", 10 in approvalTracker.getSeenIds())
    }
}

package io.github.leogallego.ansiblejane.assistant.data

import io.github.leogallego.ansiblejane.network.createPlatformHttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ModelFetcherTest {

    private lateinit var server: MockWebServer
    private lateinit var fetcher: ModelFetcher
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        fetcher = ModelFetcher(createPlatformHttpClient { expectSuccess = false }, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `SHOULD return sorted model IDs WHEN server returns valid response GIVEN models endpoint`() = runTest {
        server.enqueue(MockResponse()
            .setBody("""{"data":[{"id":"zmodel","object":"model"},{"id":"amodel","object":"model"}]}""")
            .setHeader("Content-Type", "application/json"))

        val result = fetcher.fetchModels(server.url("/v1").toString(), "test-key")

        assertTrue(result is ModelFetcher.Result.Success)
        assertEquals(listOf("amodel", "zmodel"), (result as ModelFetcher.Result.Success).models)
    }

    @Test
    fun `SHOULD send authorization header WHEN apiKey provided GIVEN valid request`() = runTest {
        server.enqueue(MockResponse()
            .setBody("""{"data":[]}""")
            .setHeader("Content-Type", "application/json"))

        fetcher.fetchModels(server.url("/v1").toString(), "my-secret-key")

        val recorded = server.takeRequest()
        assertEquals("Bearer my-secret-key", recorded.getHeader("Authorization"))
    }

    @Test
    fun `SHOULD not send authorization header WHEN apiKey is null GIVEN Ollama-like server`() = runTest {
        server.enqueue(MockResponse()
            .setBody("""{"data":[{"id":"llama3.1:8b","object":"model"}]}""")
            .setHeader("Content-Type", "application/json"))

        fetcher.fetchModels(server.url("/v1").toString(), null)

        val recorded = server.takeRequest()
        assertEquals(null, recorded.getHeader("Authorization"))
    }

    @Test
    fun `SHOULD return auth error WHEN server returns 401 GIVEN invalid key`() = runTest {
        server.enqueue(MockResponse().setResponseCode(401))

        val result = fetcher.fetchModels(server.url("/v1").toString(), "bad-key")

        assertTrue(result is ModelFetcher.Result.Error)
        assertTrue((result as ModelFetcher.Result.Error).message.contains("Authentication"))
    }

    @Test
    fun `SHOULD return auth error WHEN server returns 403 GIVEN forbidden key`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403))

        val result = fetcher.fetchModels(server.url("/v1").toString(), "bad-key")

        assertTrue(result is ModelFetcher.Result.Error)
        assertTrue((result as ModelFetcher.Result.Error).message.contains("Authentication"))
    }

    @Test
    fun `SHOULD return empty list WHEN data array is empty GIVEN valid response`() = runTest {
        server.enqueue(MockResponse()
            .setBody("""{"data":[]}""")
            .setHeader("Content-Type", "application/json"))

        val result = fetcher.fetchModels(server.url("/v1").toString(), "key")

        assertTrue(result is ModelFetcher.Result.Success)
        assertTrue((result as ModelFetcher.Result.Success).models.isEmpty())
    }

    @Test
    fun `SHOULD return error WHEN response is malformed JSON GIVEN broken server`() = runTest {
        server.enqueue(MockResponse()
            .setBody("not json at all")
            .setHeader("Content-Type", "application/json"))

        val result = fetcher.fetchModels(server.url("/v1").toString(), "key")

        assertTrue(result is ModelFetcher.Result.Error)
        assertTrue((result as ModelFetcher.Result.Error).message.contains("parse"))
    }

    @Test
    fun `SHOULD return server error WHEN status is 500 GIVEN server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val result = fetcher.fetchModels(server.url("/v1").toString(), "key")

        assertTrue(result is ModelFetcher.Result.Error)
        assertTrue((result as ModelFetcher.Result.Error).message.contains("500"))
    }

    @Test
    fun `SHOULD call models endpoint WHEN fetching GIVEN base URL`() = runTest {
        server.enqueue(MockResponse()
            .setBody("""{"data":[]}""")
            .setHeader("Content-Type", "application/json"))

        fetcher.fetchModels(server.url("/v1").toString(), "key")

        val recorded = server.takeRequest()
        assertTrue(recorded.path?.endsWith("/v1/models") == true)
    }
}

package com.example.aapremote.network.mcp

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class McpTransportTest {

    private lateinit var server: MockWebServer
    private lateinit var transport: McpTransport
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder().build()
        transport = McpTransport(client, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `postJsonRpc sends correct request body`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-03-26","capabilities":{},"serverInfo":{"name":"test","version":"1.0"}}}""")
                .setHeader("Content-Type", "application/json")
        )

        val request = JsonRpcRequest(id = 1, method = "initialize")
        transport.postJsonRpc(server.url("/mcp").toString(), request).toList()

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assert(recorded.body.readUtf8().contains("\"method\":\"initialize\""))
    }

    @Test
    fun `postJsonRpc parses JSON response`() = runTest {
        val responseBody = """{"jsonrpc":"2.0","id":1,"result":{"tools":[]}}"""
        server.enqueue(
            MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json")
        )

        val request = JsonRpcRequest(id = 1, method = "tools/list")
        val responses = transport.postJsonRpc(server.url("/mcp").toString(), request).toList()

        assertEquals(1, responses.size)
        assertNull(responses[0].error)
        assertNotNull(responses[0].result)
    }

    @Test
    fun `postJsonRpc includes session ID header when provided`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"jsonrpc":"2.0","id":1,"result":{}}""")
                .setHeader("Content-Type", "application/json")
        )

        val request = JsonRpcRequest(id = 1, method = "tools/list")
        transport.postJsonRpc(server.url("/mcp").toString(), request, sessionId = "test-session").toList()

        val recorded = server.takeRequest()
        assertEquals("test-session", recorded.getHeader("Mcp-Session-Id"))
    }

    @Test
    fun `postJsonRpc sends Accept header for both JSON and SSE`() = runTest {
        server.enqueue(
            MockResponse()
                .setBody("""{"jsonrpc":"2.0","id":1,"result":{}}""")
                .setHeader("Content-Type", "application/json")
        )

        val request = JsonRpcRequest(id = 1, method = "tools/list")
        transport.postJsonRpc(server.url("/mcp").toString(), request).toList()

        val recorded = server.takeRequest()
        val accept = recorded.getHeader("Accept")
        assert(accept?.contains("application/json") == true)
        assert(accept?.contains("text/event-stream") == true)
    }

    @Test
    fun `postNotification sends request without expecting response body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(202))

        val request = JsonRpcRequest(method = "notifications/initialized")
        transport.postNotification(server.url("/mcp").toString(), request)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assert(recorded.body.readUtf8().contains("notifications/initialized"))
    }

    @Test
    fun `postJsonRpc handles error response`() = runTest {
        val responseBody = """{"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"Method not found"}}"""
        server.enqueue(
            MockResponse()
                .setBody(responseBody)
                .setHeader("Content-Type", "application/json")
        )

        val request = JsonRpcRequest(id = 1, method = "unknown/method")
        val responses = transport.postJsonRpc(server.url("/mcp").toString(), request).toList()

        assertEquals(1, responses.size)
        assertNotNull(responses[0].error)
        assertEquals(-32601, responses[0].error?.code)
    }
}

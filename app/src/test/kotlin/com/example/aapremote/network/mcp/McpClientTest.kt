package com.example.aapremote.network.mcp

import com.example.aapremote.assistant.tools.ErrorType
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class McpClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: McpClient
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()

        val httpClient = OkHttpClient.Builder().build()
        val transport = McpTransport(httpClient, json)
        client = McpClient(server.url("/mcp").toString(), transport, json)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `connect sends initialize and notifications-initialized`() = runTest {
        enqueueInitializeResponse()
        enqueueNotificationResponse()
        enqueueToolsListResponse(emptyList())

        client.connect()

        val initRequest = server.takeRequest()
        assertTrue(initRequest.body.readUtf8().contains("\"method\":\"initialize\""))

        val notifyRequest = server.takeRequest()
        assertTrue(notifyRequest.body.readUtf8().contains("notifications/initialized"))
    }

    @Test
    fun `connect transitions to Connected state`() = runTest {
        enqueueInitializeResponse()
        enqueueNotificationResponse()
        enqueueToolsListResponse(listOf(
            """{"name":"controller.jobs_read","description":"List jobs","inputSchema":{"type":"object"}}"""
        ))

        client.connect()

        val state = client.connectionState.value
        assertTrue(state is McpConnectionState.Connected)
        assertEquals(1, (state as McpConnectionState.Connected).toolCount)
    }

    @Test
    fun `connect transitions to Error on failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        try {
            client.connect()
        } catch (_: Exception) {}

        val state = client.connectionState.value
        assertTrue(state is McpConnectionState.Error)
    }

    @Test
    fun `listTools returns tool definitions`() = runTest {
        enqueueInitializeResponse()
        enqueueNotificationResponse()
        enqueueToolsListResponse(listOf(
            """{"name":"controller.jobs_read","description":"List jobs","inputSchema":{"type":"object","properties":{}}}""",
            """{"name":"controller.inventories_read","description":"List inventories","inputSchema":{"type":"object"}}"""
        ))

        client.connect()

        val tools = client.tools.value
        assertEquals(2, tools.size)
        assertEquals("controller.jobs_read", tools[0].name)
        assertEquals("controller.inventories_read", tools[1].name)
    }

    @Test
    fun `callTool sends correct JSON-RPC and returns result`() = runTest {
        enqueueInitializeResponse()
        enqueueNotificationResponse()
        enqueueToolsListResponse(emptyList())

        client.connect()

        server.enqueue(
            MockResponse()
                .setBody("""{"jsonrpc":"2.0","id":4,"result":{"content":[{"type":"text","text":"job data here"}],"isError":false}}""")
                .setHeader("Content-Type", "application/json")
        )

        val args = buildJsonObject { put("id", 42) }
        val result = client.callTool("controller.jobs_read", args)

        assertFalse(result.isError)
        assertEquals("job data here", result.content[0].text)
    }

    @Test
    fun `callTool returns error result on JSON-RPC error`() = runTest {
        enqueueInitializeResponse()
        enqueueNotificationResponse()
        enqueueToolsListResponse(emptyList())

        client.connect()

        server.enqueue(
            MockResponse()
                .setBody("""{"jsonrpc":"2.0","id":4,"error":{"code":-32000,"message":"Unauthorized"}}""")
                .setHeader("Content-Type", "application/json")
        )

        val result = client.callTool("test", JsonObject(emptyMap()))

        assertTrue(result.isError)
    }

    @Test
    fun `disconnect resets state`() = runTest {
        enqueueInitializeResponse()
        enqueueNotificationResponse()
        enqueueToolsListResponse(emptyList())

        client.connect()
        client.disconnect()

        assertTrue(client.connectionState.value is McpConnectionState.Disconnected)
        assertTrue(client.tools.value.isEmpty())
    }

    @Test
    fun `mapJsonRpcError maps error codes correctly`() {
        assertEquals(ErrorType.AUTH_ERROR, McpClient.mapJsonRpcError(-32000))
        assertEquals(ErrorType.NOT_FOUND, McpClient.mapJsonRpcError(-32601))
        assertEquals(ErrorType.NOT_FOUND, McpClient.mapJsonRpcError(-32602))
        assertEquals(ErrorType.SERVER_ERROR, McpClient.mapJsonRpcError(-32603))
        assertEquals(ErrorType.SERVER_ERROR, McpClient.mapJsonRpcError(-32700))
    }

    @Test
    fun `mapHttpError maps status codes correctly`() {
        assertEquals(ErrorType.AUTH_ERROR, McpClient.mapHttpError(401))
        assertEquals(ErrorType.AUTH_ERROR, McpClient.mapHttpError(403))
        assertEquals(ErrorType.NOT_FOUND, McpClient.mapHttpError(404))
        assertEquals(ErrorType.SERVER_ERROR, McpClient.mapHttpError(500))
        assertEquals(ErrorType.SERVER_ERROR, McpClient.mapHttpError(503))
    }

    private fun enqueueInitializeResponse() {
        server.enqueue(
            MockResponse()
                .setBody("""{"jsonrpc":"2.0","id":1,"result":{"protocolVersion":"2025-03-26","capabilities":{},"serverInfo":{"name":"test-server","version":"1.0.0"}}}""")
                .setHeader("Content-Type", "application/json")
        )
    }

    private fun enqueueNotificationResponse() {
        server.enqueue(MockResponse().setResponseCode(202))
    }

    private fun enqueueToolsListResponse(tools: List<String>) {
        val toolsJson = tools.joinToString(",")
        server.enqueue(
            MockResponse()
                .setBody("""{"jsonrpc":"2.0","id":2,"result":{"tools":[$toolsJson]}}""")
                .setHeader("Content-Type", "application/json")
        )
    }
}

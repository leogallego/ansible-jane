package io.github.leogallego.ansiblejane.assistant.tools

import io.github.leogallego.ansiblejane.model.AapInstance
import io.github.leogallego.ansiblejane.model.McpServerConfig
import io.github.leogallego.ansiblejane.network.mcp.McpServerManager
import io.github.leogallego.ansiblejane.network.mcp.McpToolDefinition
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.sse.SSE
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.Proxy

class CachedMcpToolTest {

    private lateinit var server: MockWebServer
    private lateinit var manager: McpServerManager

    @Before
    fun setup() {
        server = MockWebServer()
        manager = McpServerManager(
            ktorClientFactory = { _, _ ->
                HttpClient(OkHttp) {
                    engine { config { proxy(Proxy.NO_PROXY) } }
                    install(SSE)
                }
            }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
        runBlocking { manager.disconnectAll() }
    }

    private fun mcpDispatcher(
        toolCallResponse: String? = null
    ): Dispatcher = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            if (request.method == "GET") return MockResponse().setResponseCode(405)

            val body = request.body.readUtf8()
            val id = extractId(body)

            return when {
                body.contains("\"method\":\"initialize\"") -> {
                    val result = """{"protocolVersion":"2025-03-26","capabilities":{"tools":{}},"serverInfo":{"name":"test-server","version":"1.0"}}"""
                    jsonResponse("""{"jsonrpc":"2.0","id":$id,"result":$result}""")
                }
                body.contains("notifications/initialized") ||
                body.contains("notifications/cancelled") ->
                    MockResponse().setResponseCode(202)
                body.contains("\"method\":\"tools/list\"") -> {
                    val tools = """[{"name":"ping","description":"Ping","inputSchema":{"type":"object","properties":{}}}]"""
                    jsonResponse("""{"jsonrpc":"2.0","id":$id,"result":{"tools":$tools}}""")
                }
                body.contains("\"method\":\"tools/call\"") -> {
                    val result = toolCallResponse
                        ?: """{"content":[{"type":"text","text":"pong"}],"isError":false}"""
                    jsonResponse("""{"jsonrpc":"2.0","id":$id,"result":$result}""")
                }
                else -> MockResponse().setResponseCode(404)
            }
        }

        private fun extractId(body: String): String {
            val match = Regex(""""id"\s*:\s*"([^"]+)"""").find(body)
            return match?.let { "\"${it.groupValues[1]}\"" } ?: "null"
        }
    }

    private fun jsonResponse(body: String) = MockResponse()
        .setHeader("Content-Type", "application/json")
        .setBody(body)

    @Test
    fun `execute calls ensureConnected and returns tool result`() = runBlocking {
        server.dispatcher = mcpDispatcher()
        server.start()

        val instance = AapInstance(
            id = "inst-1",
            baseUrl = "https://aap.example.com",
            token = "token-1",
            mcpServerUrls = listOf(
                McpServerConfig(url = server.url("/mcp").toString(), label = "test-server", enabled = true)
            )
        )
        manager.connectAll(instance)

        val tool = CachedMcpTool(
            mcpToolDef = McpToolDefinition("ping", "Ping"),
            serverLabel = "test-server",
            serverManager = manager
        )

        val result = tool.execute(JsonObject(emptyMap()))
        assertTrue(result.success)
        assertEquals("pong", result.data)
    }

    @Test
    fun `execute returns CONNECTION_ERROR when server unreachable`() = runBlocking {
        val tool = CachedMcpTool(
            mcpToolDef = McpToolDefinition("ping", "Ping"),
            serverLabel = "nonexistent-server",
            serverManager = manager
        )

        val result = tool.execute(JsonObject(emptyMap()))
        assertFalse(result.success)
        assertEquals(ErrorType.CONNECTION_ERROR, result.errorType)
        assertTrue(result.data!!.contains("Connection error"))
    }

    @Test
    fun `execute returns SERVER_ERROR when tool call isError`() = runBlocking {
        server.dispatcher = mcpDispatcher(
            toolCallResponse = """{"content":[{"type":"text","text":"something went wrong"}],"isError":true}"""
        )
        server.start()

        val instance = AapInstance(
            id = "inst-1",
            baseUrl = "https://aap.example.com",
            token = "token-1",
            mcpServerUrls = listOf(
                McpServerConfig(url = server.url("/mcp").toString(), label = "test-server", enabled = true)
            )
        )
        manager.connectAll(instance)

        val tool = CachedMcpTool(
            mcpToolDef = McpToolDefinition("ping", "Ping"),
            serverLabel = "test-server",
            serverManager = manager
        )

        val result = tool.execute(JsonObject(emptyMap()))
        assertFalse(result.success)
        assertEquals(ErrorType.SERVER_ERROR, result.errorType)
        assertEquals("something went wrong", result.data)
    }

    @Test
    fun `spec includes server label in description`() {
        val tool = CachedMcpTool(
            mcpToolDef = McpToolDefinition("ping", "Ping the server"),
            serverLabel = "my-mcp",
            serverManager = manager
        )

        assertTrue(tool.spec.description.startsWith("[my-mcp]"))
        assertEquals("ping", tool.spec.name)
    }

    @Test
    fun `isDestructive is true for write suffixes`() {
        val create = CachedMcpTool(
            mcpToolDef = McpToolDefinition("hosts_create", "Create host"),
            serverLabel = "s",
            serverManager = manager
        )
        assertTrue(create.isDestructive)

        val read = CachedMcpTool(
            mcpToolDef = McpToolDefinition("hosts_read", "List hosts"),
            serverLabel = "s",
            serverManager = manager
        )
        assertFalse(read.isDestructive)
    }

    @Test
    fun `execute passes arguments to tool call`() = runBlocking {
        server.dispatcher = mcpDispatcher()
        server.start()

        val instance = AapInstance(
            id = "inst-1",
            baseUrl = "https://aap.example.com",
            token = "token-1",
            mcpServerUrls = listOf(
                McpServerConfig(url = server.url("/mcp").toString(), label = "test-server", enabled = true)
            )
        )
        manager.connectAll(instance)

        val tool = CachedMcpTool(
            mcpToolDef = McpToolDefinition("ping", "Ping"),
            serverLabel = "test-server",
            serverManager = manager
        )

        val args = buildJsonObject {
            put("filter", JsonPrimitive("active"))
        }
        val result = tool.execute(args)
        assertTrue(result.success)
    }
}

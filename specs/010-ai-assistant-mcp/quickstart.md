# Quickstart: AI Assistant with MCP Integration

**Branch**: `010-ai-assistant-mcp` | **Phase**: A (Read-Only Assistant)

## Prerequisites

- Android Studio with Kotlin 2.2.10+
- An AAP 2.5+ instance with Gateway
- An MCP server running (e.g., `ansible/aap-mcp-server` connected to your AAP instance)
- An LLM endpoint (Ollama local, or OpenAI-compatible API)

## Build Setup

### 1. Add okhttp-sse dependency

In `gradle/libs.versions.toml`:
```toml
[libraries]
okhttp-sse = { group = "com.squareup.okhttp3", name = "okhttp-sse", version.ref = "okhttp" }
```

In `app/build.gradle.kts`:
```kotlin
implementation(libs.okhttp.sse)
```

### 2. Add test dependencies

```toml
[libraries]
junit = { group = "junit", name = "junit", version = "4.13.2" }
mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "okhttp" }
turbine = { group = "app.cash.turbine", name = "turbine", version = "1.1.0" }
coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
```

```kotlin
testImplementation(libs.junit)
testImplementation(libs.mockwebserver)
testImplementation(libs.turbine)
testImplementation(libs.coroutines.test)
```

## Implementation Order (Phase A)

### Layer 1: Foundation types (no dependencies between files)

1. `McpTypes.kt` — JSON-RPC request/response/error types, McpToolDefinition, McpToolResult
2. `LlmTypes.kt` — StreamEvent, LlmResult, ToolCall, ModelInfo
3. `ToolSpec.kt` — ToolSpec data class, Tool interface, ToolResult, ErrorType enum
4. `ChatMessage.kt` — ChatMessage data class, Role enum
5. `AssistantUiState.kt` — Sealed interface with Idle/Loading/Active/Error
6. `AssistantConfig.kt` — LlmProviderConfig sealed interface, config persistence

### Layer 2: MCP client (depends on Layer 1)

7. `McpTransport.kt` — OkHttp POST + SSE dual response handling, callbackFlow bridge
8. `McpClient.kt` — Connect/disconnect/listTools/callTool over McpTransport
9. `McpSession.kt` — Session state tracking, tool cache, connection state flow
10. `McpTool.kt` — Bridges Tool interface to McpClient.callTool()
11. `McpServerManager.kt` — Multi-server parallel connection, tool aggregation

### Layer 3: LLM provider (depends on Layer 1)

12. `LlmProvider.kt` — Interface definition
13. `OpenAiCompatibleProvider.kt` — Full implementation with streaming

### Layer 4: Engine (depends on Layers 2 + 3)

14. `ToolExecutor.kt` — Tool dispatch + result truncation
15. `ChatEngine.kt` — Orchestration loop

### Layer 5: Presentation + UI (depends on Layer 4)

16. `AssistantRepository.kt` — In-memory chat history, config access
17. `AssistantViewModel.kt` — Connects engine to UI state
18. `AssistantScreen.kt` — Chat UI composable
19. `ChatBubble.kt` — Message rendering
20. `AssistantSettingsSheet.kt` — LLM/MCP configuration

### Layer 6: Integration (modifies existing files)

21. `AapInstance.kt` — Add `mcpServerUrls` field
22. `TokenManager.kt` — Serialize new field
23. `TabDefinitions.kt` — Add Assistant tab
24. `MainNavigation.kt` — Route to AssistantScreen
25. `AapRemoteApp.kt` — Add `assistantModule` to Koin
26. `build.gradle.kts` + `libs.versions.toml` — Add dependencies

## Running Locally

### Start an MCP server

```bash
# Clone and run aap-mcp-server
git clone https://github.com/ansible/aap-mcp-server
cd aap-mcp-server
# Configure AAP_GATEWAY_URL, AAP_USERNAME, AAP_PASSWORD
npm start
# Server runs on http://localhost:3000/mcp
```

### Start an LLM (Ollama example)

```bash
ollama serve
ollama pull llama3.1:8b
# API available at http://localhost:11434/v1
```

### Configure in the app

1. Add/edit an AAP instance in Settings
2. In the "AI Assistant" section, add MCP server URL: `http://<your-ip>:3000/mcp`
3. Go to Assistant tab → Settings (gear icon)
4. Add LLM provider: OpenAI Compatible, URL `http://<your-ip>:11434/v1`, model `llama3.1:8b`
5. Start chatting: "What job templates are available?"

## Koin Module

```kotlin
val assistantModule = module {
    single { McpServerManager(httpClientFactory = { instance ->
        // Reuse AapApiProvider's buildClient pattern
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(
                tokenProvider = { instance.token },
                instanceIdProvider = { instance.id }
            ))
            .apply {
                if (instance.trustSelfSigned) {
                    val tm = CertTrustManager.createTrustAllManager()
                    sslSocketFactory(CertTrustManager.createSslSocketFactory(tm), tm)
                    hostnameVerifier { _, _ -> true }
                }
            }
            .build()
    }, json = networkJson) }

    factory { (provider: LlmProviderConfig) ->
        when (provider) {
            is LlmProviderConfig.OpenAiCompatible ->
                OpenAiCompatibleProvider(provider, get(), networkJson)
            // Phase D: Anthropic, GoogleAi, Local
            else -> throw UnsupportedOperationException("Provider not yet implemented")
        }
    }

    single { AssistantRepository(get()) }
    viewModelOf(::AssistantViewModel)
}
```

## Verification Checklist

- [ ] App builds with new `okhttp-sse` dependency
- [ ] Assistant tab appears as 4th bottom nav item
- [ ] MCP server URLs can be added to instance config
- [ ] MCP client connects and discovers tools
- [ ] LLM provider sends tool definitions and receives tool calls
- [ ] ChatEngine orchestrates multi-turn tool calling loop
- [ ] Streaming text appears word-by-word in chat
- [ ] Tool execution indicators show during tool calls
- [ ] Errors display gracefully (MCP down, LLM down, auth failure)
- [ ] Switching instances disconnects/reconnects MCP
- [ ] Chat history persists in-memory across tab switches (clears on app restart)

# Data Model: AI Assistant with MCP Integration

**Date**: 2026-05-08 | **Branch**: `010-ai-assistant-mcp`

## Entity Map

```
AapInstance (MODIFIED)          McpServerConfig (NEW)
├── mcpServerUrls ────────────> ├── url
│                               ├── label
│                               └── enabled
│
│   McpClient (NEW)             McpSession (NEW)
│   ├── serverConfig            ├── sessionId?
│   ├── httpClient              ├── tools: List<ToolSpec>
│   └── session ──────────────> └── state: McpConnectionState
│
│   ToolSpec (NEW)              Tool (interface, NEW)
│   ├── name                    ├── spec: ToolSpec
│   ├── description             └── execute(args) → ToolResult
│   └── parametersSchema
│
│   McpTool (NEW)               ToolResult (NEW)
│   ├── client: McpClient       ├── success
│   ├── mcpToolDef              ├── data
│   └── serverLabel             └── errorType?
│
│   LlmProvider (interface)     LlmProviderConfig (sealed)
│   ├── generate()              ├── OpenAiCompatible
│   ├── generateStream()        │   ├── url
│   ├── isAvailable()           │   ├── model
│   └── modelInfo()             │   └── apiKey?
│                               ├── Anthropic (Phase D)
│                               └── GoogleAi (Phase D)
│
│   ChatEngine (NEW)            ChatMessage (NEW)
│   ├── provider: LlmProvider   ├── role: Role (USER/ASSISTANT/TOOL/SYSTEM)
│   ├── toolExecutor            ├── content
│   └── processMessage()        ├── toolCalls?
│                               └── toolCallId?
│
│   AssistantViewModel (NEW)    AssistantUiState (NEW, sealed)
│   ├── chatEngine              ├── Idle
│   ├── mcpServerManager        ├── Loading
│   └── uiState: StateFlow     ├── Active (messages, isGenerating, connections)
│                               └── Error
```

## Entities

### Modified Entities

#### AapInstance

**File**: `app/src/main/kotlin/com/example/aapremote/model/AapInstance.kt`

| Field | Type | Change | Notes |
|-------|------|--------|-------|
| id | String | existing | |
| baseUrl | String | existing | |
| token | String | existing | |
| alias | String? | existing | |
| apiVersion | String | existing | |
| trustSelfSigned | Boolean | existing | |
| certFingerprint | String? | existing | |
| **mcpServerUrls** | **List\<McpServerConfig\>?** | **NEW** | Nullable for backward compat. Encrypted in DataStore. |

**Validation**: `mcpServerUrls` defaults to `null`. Existing instances load without migration. New field serialized as part of the encrypted instance JSON blob in TokenManager.

---

### New Entities — Network/MCP Layer

#### McpServerConfig

**File**: `app/src/main/kotlin/com/example/aapremote/model/AapInstance.kt` (co-located)

| Field | Type | Required | Default | Notes |
|-------|------|----------|---------|-------|
| url | String | yes | — | Full URL to MCP endpoint (e.g., `https://aap.example.com:8080/mcp`) |
| label | String | yes | — | User-facing label: "operations", "knowledge", custom |
| enabled | Boolean | no | true | Can disable without deleting |

**Validation**: `url` must be HTTPS (enforced by network security config). Label is free-text.

#### McpTypes (JSON-RPC 2.0)

**File**: `app/src/main/kotlin/com/example/aapremote/network/mcp/McpTypes.kt`

```kotlin
@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int? = null,          // null for notifications
    val method: String,
    val params: JsonObject? = null
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: JsonElement? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class McpToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

@Serializable
data class McpToolResult(
    val content: List<McpContent>,
    val isError: Boolean = false
)

@Serializable
data class McpContent(
    val type: String,             // "text", "image", "resource"
    val text: String? = null
)

@Serializable
data class McpInitializeResult(
    val protocolVersion: String,
    val capabilities: JsonObject,
    val serverInfo: McpServerInfo
)

@Serializable
data class McpServerInfo(
    val name: String,
    val version: String
)
```

#### McpConnectionState

**File**: `app/src/main/kotlin/com/example/aapremote/network/mcp/McpSession.kt`

```kotlin
sealed interface McpConnectionState {
    data object Disconnected : McpConnectionState
    data object Connecting : McpConnectionState
    data class Connected(val serverInfo: McpServerInfo, val toolCount: Int) : McpConnectionState
    data class Error(val message: String, val cause: Throwable? = null) : McpConnectionState
}
```

**State transitions**: Disconnected → Connecting → Connected | Error. Reconnect from Error → Connecting.

---

### New Entities — Tool Layer

#### ToolSpec

**File**: `app/src/main/kotlin/com/example/aapremote/assistant/tools/ToolSpec.kt`

| Field | Type | Notes |
|-------|------|-------|
| name | String | Tool name from MCP (e.g., `controller.jobs_read`) |
| description | String | Prefixed with server label: `[operations] List jobs` |
| parametersSchema | JsonObject | Raw JSON Schema from MCP — lossless passthrough |

**No validation rules** — schema is opaque to the app, passed directly to LLM provider.

#### Tool (interface)

**File**: `app/src/main/kotlin/com/example/aapremote/assistant/tools/ToolSpec.kt` (co-located)

```kotlin
interface Tool {
    val spec: ToolSpec
    suspend fun execute(args: Map<String, Any>): ToolResult
}
```

#### ToolResult

**File**: `app/src/main/kotlin/com/example/aapremote/assistant/tools/ToolSpec.kt` (co-located)

| Field | Type | Notes |
|-------|------|-------|
| success | Boolean | false when tool execution failed |
| data | String? | JSON string, smart-truncated to 20K chars |
| errorType | ErrorType? | Classified error for LLM remediation |

#### ErrorType

```kotlin
enum class ErrorType {
    CONNECTION_ERROR,   // MCP server unreachable
    AUTH_ERROR,         // 401/403 or JSON-RPC -32000
    NOT_FOUND,          // 404 or JSON-RPC -32601/-32602
    TIMEOUT,            // Request timed out
    SERVER_ERROR        // 500 or JSON-RPC -32603
}
```

---

### New Entities — LLM Layer

#### LlmProvider (interface)

**File**: `app/src/main/kotlin/com/example/aapremote/assistant/llm/LlmProvider.kt`

```kotlin
interface LlmProvider {
    suspend fun generate(messages: List<ChatMessage>, tools: List<ToolSpec>): LlmResult
    fun generateStream(messages: List<ChatMessage>, tools: List<ToolSpec>): Flow<StreamEvent>
    fun isAvailable(): Boolean
    fun modelInfo(): ModelInfo
}
```

#### StreamEvent (sealed interface)

**File**: `app/src/main/kotlin/com/example/aapremote/assistant/llm/LlmTypes.kt`

| Variant | Fields | Notes |
|---------|--------|-------|
| TextDelta | text: String | Incremental text chunk for typing effect |
| ToolCallStart | id: String, name: String | Tool call initiated — show indicator |
| ToolCallArgs | id: String, argsDelta: String | Streaming arguments (accumulate) |
| Done | result: LlmResult | Final result with complete text + tool calls |
| Error | cause: Throwable | Stream error |

#### LlmResult

| Field | Type | Notes |
|-------|------|-------|
| text | String? | Final text response (null if only tool calls) |
| toolCalls | List\<ToolCall\> | Empty list if text-only response |

#### ToolCall

| Field | Type | Notes |
|-------|------|-------|
| id | String | Provider-assigned tool call ID |
| name | String | Tool name matching ToolSpec.name |
| arguments | JsonObject | Parsed arguments from LLM |

#### ModelInfo

| Field | Type | Notes |
|-------|------|-------|
| name | String | Display name (e.g., "llama3.1:70b") |
| isLocal | Boolean | false for Phase A |

#### LlmProviderConfig (sealed interface)

**File**: `app/src/main/kotlin/com/example/aapremote/assistant/data/AssistantConfig.kt`

| Variant | Fields |
|---------|--------|
| OpenAiCompatible | url: String, model: String, apiKey: String? |
| Anthropic (Phase D) | apiKey: String, model: String, baseUrl: String |
| GoogleAi (Phase D) | apiKey: String, model: String |

**Storage**: Serialized to DataStore as JSON, encrypted via Tink. API keys stored encrypted separately from the config structure.

---

### New Entities — Engine Layer

#### ChatMessage

**File**: `app/src/main/kotlin/com/example/aapremote/assistant/engine/ChatMessage.kt`

| Field | Type | Notes |
|-------|------|-------|
| role | Role | USER, ASSISTANT, TOOL, SYSTEM |
| content | String | Message text or tool result |
| toolCalls | List\<ToolCall\>? | Only on ASSISTANT messages with tool calls |
| toolCallId | String? | Only on TOOL messages (references original call) |
| timestamp | Long | System.currentTimeMillis() |

#### Role

```kotlin
enum class Role { USER, ASSISTANT, TOOL, SYSTEM }
```

---

### New Entities — Presentation Layer

#### AssistantUiState (sealed interface)

**File**: `app/src/main/kotlin/com/example/aapremote/assistant/presentation/AssistantUiState.kt`

| Variant | Fields | Notes |
|---------|--------|-------|
| Idle | — | No active instance or MCP not configured |
| Loading | — | Connecting to MCP servers |
| Active | messages: List\<ChatMessage\>, isGenerating: Boolean, connections: Map\<String, McpConnectionState\>, inputText: String | Main chat state |
| Error | error: AppError | Fatal error (no MCP, no LLM configured) |

**Follows existing pattern**: Matches `TemplatesUiState`, `InventoriesUiState` sealed patterns used throughout the app.

## Relationship Diagram

```
TokenManager
  └── AapInstance
        └── mcpServerUrls: List<McpServerConfig>
              │
              ▼
        McpServerManager
          └── clients: Map<McpServerConfig, McpClient>
                └── McpClient
                      ├── McpSession (state, tools cache)
                      └── McpTransport (OkHttp POST/SSE)
                            │
                            ▼
                      tools: List<McpTool> (implements Tool)
                            │
                            ▼
        ToolExecutor (flat list of all Tools from all servers)
              │
              ▼
        ChatEngine
          ├── LlmProvider (OpenAiCompatibleProvider in Phase A)
          ├── ToolExecutor
          └── messages: List<ChatMessage>
              │
              ▼
        AssistantViewModel
          ├── chatEngine
          ├── mcpServerManager
          └── uiState: StateFlow<AssistantUiState>
              │
              ▼
        AssistantScreen (Compose)
```

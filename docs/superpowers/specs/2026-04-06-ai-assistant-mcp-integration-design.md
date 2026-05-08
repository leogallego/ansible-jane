# AI Assistant with MCP Integration — Design Spec

## Overview

Add an AI Assistant tab to AAP Remote Control that lets users interact with their AAP instances through natural language. The app acts as an **MCP client**, connecting to remote MCP servers (primarily the official `ansible/aap-mcp-server`) over Streamable HTTP. An LLM orchestrates tool calls between the user's query and MCP tool execution. No paid API subscriptions or proprietary dependencies — fully open source and free.

### Target MCP Servers

| Server | Purpose | Transport |
|--------|---------|-----------|
| [`ansible/aap-mcp-server`](https://github.com/ansible/aap-mcp-server) | Operations — jobs, templates, inventories, credentials across Controller, Gateway, EDA, Galaxy | Streamable HTTP (`/mcp`, `/{toolset}/mcp`) |
| [`ansible-know-mcp`](https://github.com/leogallego/ansible-know-mcp) | Knowledge — module docs, conceptual guides, troubleshooting search | STDIO (needs Streamable HTTP addition) |

The AAP MCP server dynamically generates tools from AAP OpenAPI schemas and supports toolset routing (`/mcp/job_management`, `/mcp/inventory_management`, etc.). Write operations are gated behind `ALLOW_WRITE_OPERATIONS` on the server side.

### Troubleshooting Flow (AAP MCP + ansible-know)

```
1. User sees failed job in app
2. AAP MCP → controller.jobs.read(id) → error output + failed module name
3. ansible-know → get_module_doc(failed_module) → correct params, examples
4. ansible-know → search_docs("error keywords") → relevant guides
5. LLM correlates error with docs → targeted troubleshooting advice
```

## Architecture

Four components:

1. **Chat UI** — 4th bottom tab ("Assistant") with a standard chat interface
2. **LLM Orchestrator (ChatEngine)** — Manages the conversation loop: user input → LLM → tool calls → MCP execution → LLM → final answer. Always owns the loop regardless of LLM backend.
3. **LLM Backend** — `LlmProvider` interface with three implementations in Phase A: `OpenAiCompatibleProvider` (Ollama, vLLM, Gemini via compatibility layer), `AnthropicProvider` (Claude), and `GeminiProvider` (native Google AI). `FallbackLlmProvider` wraps them with priority-ordered fallback. Phase D adds `LocalLlmProvider` (LiteRT-LM on-device).
4. **MCP Client** — Connects to N MCP servers per instance over Streamable HTTP, discovers tools, executes tool calls

### Data Flow

```
User types "What jobs failed today?"
  → ChatEngine sends to LLM with tool definitions (from MCP discovery)
  → LLM responds with tool_call: list_jobs(status=failed)
  → ChatEngine executes via MCP client → AAP MCP server → AAP API
  → Result returned to LLM (truncated to 20K chars)
  → LLM generates natural language answer
  → Displayed in Chat UI
```

### System Diagram

```
┌─────────────────────────────────────────┐
│  Android App                            │
│  ┌───────────┐    ┌──────────────────┐  │
│  │ Chat UI   │───>│ ChatEngine       │  │
│  │ (Tab 4)   │    │ (orchestration   │  │
│  └───────────┘    │  loop)           │  │
│                   └──┬─────────┬─────┘  │
│            LLM calls │         │ tool   │
│                      │         │ calls  │
│            ┌─────────▼───┐ ┌───▼──────┐ │
│            │ LlmProvider │ │ToolExec  │ │
│            │ (remote or  │ │ utor     │ │
│            │  local)     │ └───┬──────┘ │
│            └─────────────┘     │        │
│                   ┌────────────▼──────┐ │
│                   │ MCP Client        │ │
│                   │ (OkHttp+SSE)      │ │
│                   └──┬─────────────┬──┘ │
└──────────────────────┼─────────────┼────┘
                       │ HTTPS       │ HTTPS
             ┌─────────▼───┐  ┌──────▼────────┐
             │ AAP MCP     │  │ ansible-know  │
             │ Server      │  │ MCP Server    │
             │ (operations)│  │ (knowledge)   │
             └─────────┬───┘  └───────────────┘
                       │
             ┌─────────▼───┐
             │ AAP Instance│
             └─────────────┘
```

## Module Structure

```
app/src/main/kotlin/com/example/aapremote/
├── network/
│   ├── mcp/
│   │   ├── McpClient.kt             # Streamable HTTP MCP client (~130 lines)
│   │   ├── McpSession.kt            # Session state, tool cache, reconnect
│   │   ├── McpTransport.kt          # OkHttp transport (JSON + SSE dual handling)
│   │   ├── McpTypes.kt              # JSON-RPC 2.0 message types
│   │   └── McpServerManager.kt      # Multi-server connection manager
│   └── ... (existing Retrofit layer untouched)
│
├── assistant/
│   ├── engine/
│   │   ├── ChatEngine.kt            # Orchestration loop (LLM <-> MCP)
│   │   ├── ChatMessage.kt           # Message model (user/assistant/tool)
│   │   └── ToolExecutor.kt          # Routes tool calls to MCP, truncates results
│   ├── llm/
│   │   ├── LlmProvider.kt           # Interface: generate/generateStream
│   │   ├── LlmTypes.kt              # LlmResult, StreamEvent, ToolCall, ModelInfo
│   │   ├── OpenAiCompatibleProvider.kt  # Ollama, vLLM, llama.cpp, OpenAI, Gemini compat
│   │   ├── AnthropicProvider.kt     # Claude API (/v1/messages + tool_use)
│   │   ├── GeminiProvider.kt        # Google AI native (generativelanguage API)
│   │   └── FallbackLlmProvider.kt   # Priority-ordered provider chain with auto-fallback
│   ├── tools/
│   │   ├── ToolSpec.kt              # Canonical tool format (name, desc, JsonObject schema)
│   │   └── McpTool.kt               # Bridges Tool interface to McpClient.callTool()
│   ├── data/
│   │   ├── AssistantRepository.kt   # Chat history, settings persistence
│   │   └── AssistantConfig.kt       # Per-instance MCP URLs, global LLM preference
│   ├── presentation/
│   │   ├── AssistantViewModel.kt    # UI state, sends messages, streams responses
│   │   └── AssistantUiState.kt      # Chat state (messages, loading, connection)
│   └── ui/
│       ├── AssistantScreen.kt       # Chat screen composable
│       ├── ChatBubble.kt            # Message rendering (text, tool calls, errors)
│       └── AssistantSettingsSheet.kt # LLM/MCP config bottom sheet
│
├── ui/main/
│   └── TabDefinitions.kt            # MODIFIED: add Assistant tab
│
├── model/
│   └── AapInstance.kt                # MODIFIED: add mcpServerUrls field
```

**Phase D additions** (on-device LLM):
```
├── assistant/
│   ├── llm/
│   │   └── LocalLlmProvider.kt      # LiteRT-LM wrapper with OpenApiTool bridge
│   └── tools/
│       └── ToolAllowlist.kt          # Curated list of simple tools for local LLM
```

## Instance Configuration

Each AAP instance can have multiple MCP server URLs.

### AapInstance Model Update

```kotlin
data class AapInstance(
    val id: String,
    val baseUrl: String,
    val token: String,
    val alias: String?,
    val apiVersion: ApiVersion,
    val trustSelfSigned: Boolean,
    val certFingerprint: String?,
    val mcpServerUrls: List<McpServerConfig>?  // NEW - nullable, optional
)

data class McpServerConfig(
    val url: String,
    val label: String,            // "operations", "knowledge", user-defined
    val enabled: Boolean = true
)
```

### Auth Screen Changes

- New optional "MCP Servers" section under an expandable "AI Assistant" area
- Add/remove MCP server URLs with labels
- Validated on save: attempts MCP `initialize` handshake if provided (non-blocking — save succeeds with a warning if unreachable)
- Can be added/changed later from instance detail in Settings

### Serialization

- `mcpServerUrls` stored encrypted in DataStore alongside URL and token
- Backward compatible — defaults to `null` for existing instances

### Per-Instance MCP Behavior

- Instances without MCP URLs show "No MCP servers configured" in the Assistant tab with a configure button
- Switching active instance disconnects old MCP sessions and connects to the new instance's servers
- MCP auth reuses the same Bearer token stored per instance

## MCP Client

Lightweight Streamable HTTP implementation using OkHttp (no SDK dependency). Architecture validated by [Kai](https://github.com/SimonSchubert/Kai), which implements an identical approach in ~130 lines of Kotlin. Kai is Apache 2.0 licensed — we borrow patterns and types directly where solid.

### Capabilities

- **Connect** — POST `initialize` JSON-RPC to MCP server URL, receive `Mcp-Session-Id`, send `initialized` notification
- **Discover tools** — `tools/list` at connect time, cache tool definitions as `ToolSpec`
- **Execute tools** — `tools/call` with tool name + arguments, return result
- **Session management** — Track `Mcp-Session-Id`, handle expiry/reconnect
- **Disconnect** — HTTP DELETE to terminate session
- **Multi-server** — `McpServerManager` connects all enabled servers in parallel via `coroutineScope { async {} }`, isolates per-server failures

### Transport

- Single endpoint per MCP server (e.g., `https://aap.example.com:8080/mcp`)
- POST with `Content-Type: application/json`, `Accept: application/json, text/event-stream`
- Responses can be plain JSON or SSE stream — client handles both (check `Content-Type` header, branch to SSE parsing via `okhttp-sse` when `text/event-stream`)
- Auth: Bearer token from the AAP instance
- Reuses existing `AapApiProvider.buildClient()` for OkHttpClient with auth interceptor and self-signed cert TLS trust

### JSON-RPC Types (borrowed from Kai)

```kotlin
@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int,
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
```

### Connection Lifecycle

- Connects when Assistant tab is opened and active instance has MCP server URLs
- Disconnects when switching instances or leaving tab
- Auto-reconnects on session expiry
- Exposes `StateFlow<McpConnectionState>` (Disconnected, Connecting, Connected, Error) per server

## Tool Interface

Universal `Tool` pattern (validated by Kai). MCP tools, built-in tools (future), and cross-instance tools all implement the same interface. The LLM never knows the difference.

### Canonical Types

```kotlin
data class ToolSpec(
    val name: String,
    val description: String,
    val parametersSchema: JsonObject,  // Raw JSON Schema from MCP — lossless
)

interface Tool {
    val spec: ToolSpec
    suspend fun execute(args: Map<String, Any>): ToolResult
}

data class ToolResult(
    val success: Boolean,
    val data: String?,                 // JSON string, truncated
    val errorType: ErrorType? = null   // connection_error, auth_error, not_found, timeout, server_error
)

enum class ErrorType {
    CONNECTION_ERROR,  // MCP server unreachable
    AUTH_ERROR,        // 401/403 from AAP API
    NOT_FOUND,         // 404 from AAP API
    TIMEOUT,           // MCP or AAP call timed out
    SERVER_ERROR       // 500 from AAP API
}
```

### McpTool Bridge

```kotlin
class McpTool(
    private val client: McpClient,
    private val mcpToolDef: McpToolDefinition,
    private val serverLabel: String
) : Tool {
    override val spec = ToolSpec(
        name = mcpToolDef.name,
        description = "[$serverLabel] ${mcpToolDef.description}",
        parametersSchema = mcpToolDef.inputSchema  // Raw JsonObject passthrough
    )

    override suspend fun execute(args: Map<String, Any>): ToolResult {
        return try {
            val jsonArgs = args.toJsonObject()
            val result = client.callTool(mcpToolDef.name, jsonArgs)
            ToolResult(success = true, data = result.smartTruncate(maxChars))
        } catch (e: Exception) {
            ToolResult(success = false, data = null, errorType = e.toErrorType())
        }
    }
}
```

### Schema Passthrough

MCP `inputSchema` is preserved as raw `JsonObject` in `ToolSpec`. Conversion to provider-specific tool format happens inside each `LlmProvider`. The raw passthrough avoids lossy conversion and keeps nested schemas, enums, and complex types intact.

```kotlin
// ToolSpec → OpenAI format (inside OpenAiCompatibleProvider)
fun ToolSpec.toOpenAiTool(): JsonObject = buildJsonObject {
    put("type", "function")
    putJsonObject("function") {
        put("name", name)
        put("description", description)
        put("parameters", parametersSchema)
    }
}

// ToolSpec → Anthropic format (inside AnthropicProvider)
fun ToolSpec.toAnthropicTool(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description)
    put("input_schema", parametersSchema)  // Anthropic uses input_schema, not parameters
}

// ToolSpec → Gemini format (inside GeminiProvider)
fun ToolSpec.toGeminiTool(): JsonObject = buildJsonObject {
    put("name", name)
    put("description", description)
    put("parameters", parametersSchema)  // Gemini uses OpenAPI schema format
}
```

### Tool Result Handling

- **Remote LLM**: Truncate to 20K chars using smart truncation (keep first + last N chars, insert `[... X chars truncated ...]`)
- **Local LLM (Phase D)**: Truncate to 8K chars
- Structured error results flow to the LLM as tool responses — the LLM sees the error type and can suggest remediation (e.g., "Your token may have expired, try re-authenticating")

### Multi-Instance Namespacing (Phase C)

- Prefix tool names with sanitized instance alias: `prod_list_jobs`, `staging_list_jobs`
- Inject instance context into descriptions: `"[prod] List jobs from production AAP instance"`
- `McpTool` stores both original MCP name (for `callTool()`) and prefixed name (for LLM)
- Single flat tool list across all instances — enables cross-instance queries

## LLM Backend

### LlmProvider Interface

```kotlin
interface LlmProvider {
    suspend fun generate(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
    ): LlmResult

    fun generateStream(
        messages: List<ChatMessage>,
        tools: List<ToolSpec>,
    ): Flow<StreamEvent>

    fun isAvailable(): Boolean
    fun modelInfo(): ModelInfo
}

// Provider-agnostic types
sealed interface StreamEvent {
    data class TextDelta(val text: String) : StreamEvent
    data class ToolCallStart(val id: String, val name: String) : StreamEvent
    data class ToolCallArgs(val id: String, val argsDelta: String) : StreamEvent
    data class Done(val result: LlmResult) : StreamEvent
    data class Error(val cause: Throwable) : StreamEvent
}

data class LlmResult(
    val text: String?,
    val toolCalls: List<ToolCall>,
)

data class ToolCall(
    val id: String,
    val name: String,
    val arguments: JsonObject,
)

data class ModelInfo(val name: String, val isLocal: Boolean)
```

### Remote LLM Providers (Phase A)

Three provider implementations, all behind the same `LlmProvider` interface:

**OpenAI-Compatible** (`OpenAiCompatibleProvider`)
- `/v1/chat/completions` with `tools` parameter
- Covers: Ollama, vLLM, llama.cpp server, LocalAI, OpenAI, and any OpenAI-compatible endpoint
- Gemini also works via Google's OpenAI compatibility layer (`https://generativelanguage.googleapis.com/v1beta/openai/`)
- Streaming via SSE (`stream=true`), parsed with `okhttp-sse`
- User provides endpoint URL + model name + optional API key

**Anthropic** (`AnthropicProvider`)
- `/v1/messages` with `tool_use` content blocks — NOT OpenAI-compatible (different request/response format)
- Required for Claude (corporate-enabled model)
- Auth via `x-api-key` header (not Bearer token)
- Tool definitions use Anthropic's format: `{"name", "description", "input_schema"}` (schema passthrough from `ToolSpec.parametersSchema` works directly)
- Tool results returned as `tool_result` content blocks
- Streaming via SSE with `event: content_block_delta` messages
- DTO layer: `AnthropicChatRequest`, `AnthropicChatResponse`, `AnthropicToolUse`, `AnthropicToolResult`

**Google AI Native** (`GeminiProvider`)
- `generativelanguage.googleapis.com` REST API with `functionDeclarations`
- Required for Gemini (corporate-enabled model) when not using the OpenAI compatibility layer
- Tool definitions use `{"name", "description", "parameters"}` format with OpenAPI schema
- Tool results returned via `functionResponse` parts
- Streaming via SSE

Each provider converts `ToolSpec` to its native tool format internally — the `ChatEngine` never knows which provider is active.

### Provider Fallback Chain

`FallbackLlmProvider` wraps multiple providers with priority-ordered fallback (pattern from Kai):

```kotlin
class FallbackLlmProvider(
    private val providers: List<LlmProvider>  // Priority order
) : LlmProvider {
    override suspend fun generate(messages, tools): LlmResult {
        for (provider in providers) {
            if (!provider.isAvailable()) continue
            return try {
                provider.generate(messages, tools)
            } catch (e: LlmUnavailableException) {
                continue  // Try next provider
            }
        }
        throw NoProvidersAvailableException()
    }
    // generateStream() follows same pattern
}
```

User configures provider priority in Assistant settings (e.g., Claude → Gemini → Ollama). If Claude API is unreachable, falls through to Gemini, then to self-hosted Ollama. Phase D adds local LLM as the final fallback — works even when all remote providers are down.

### Provider Configuration

```kotlin
sealed interface LlmProviderConfig {
    data class OpenAiCompatible(
        val url: String,           // e.g., "http://192.168.1.10:11434/v1"
        val model: String,         // e.g., "llama3.1:70b"
        val apiKey: String? = null
    ) : LlmProviderConfig

    data class Anthropic(
        val apiKey: String,
        val model: String = "claude-sonnet-4-20250514",
        val baseUrl: String = "https://api.anthropic.com"
    ) : LlmProviderConfig

    data class GoogleAi(
        val apiKey: String,
        val model: String = "gemini-2.5-flash"
    ) : LlmProviderConfig
}
```

### Local LLM (Phase D — Offline/Air-Gap Mode)

- `com.google.ai.edge.litertlm:litertlm-android` (Google LiteRT-LM)
- **Model: Gemma 4 E2B** (~2.5 GB download) — only model with verified reliable tool calling at 2B params
- Positioned as **offline/air-gap mode**, not privacy mode (self-hosted remote provides equivalent privacy)
- **Curated tool allowlist** — only simple read-only tools with 1-3 flat string/boolean params (validated by Kai's production experience with Gemma on-device)
- **No LLM summarization** — render API results as structured UI cards directly. The local LLM only does tool dispatch, not response formatting. This eliminates half the model's workload and gives deterministic output.
- **Human confirmation required for ALL operations** when in local mode (tool calling accuracy at 2B is ~90% for simple calls — not reliable enough for unconfirmed infrastructure changes)
- Uses LiteRT-LM's `OpenApiTool` interface for dynamic MCP tool wrapping (not `@Tool` annotations, which are compile-time only)
- Manual tool calling mode (`automaticToolCalling = false`) — ChatEngine owns the loop, same as remote
- Retry-without-tools fallback on parser crash (ANTLR parser in LiteRT-LM crashes on malformed syntax)

```kotlin
// Phase D: Dynamic MCP tool → LiteRT-LM OpenApiTool bridge
class DynamicMcpTool(private val spec: ToolSpec) : OpenApiTool {
    override fun getToolDescriptionJsonString(): String =
        buildJsonObject {
            put("name", spec.name)
            put("description", spec.description)
            put("parameters", spec.parametersSchema)
        }.toString()

    override fun execute(paramsJsonString: String): String =
        error("Manual mode — ChatEngine handles execution")
}
```

### Global LLM Settings (DataStore)

- `providers`: List of configured `LlmProviderConfig` entries, each with:
  - `type`: `openai_compatible` / `anthropic` / `google_ai` / `local` (Phase D)
  - `enabled`: Boolean
  - `priority`: Int (lower = tried first)
  - Provider-specific fields (URL, model, API key — all encrypted via Tink)
- `fallbackEnabled`: Boolean (try next provider on failure)

LLM config is global, not per-instance. Users configure their available providers once (e.g., Claude with corporate API key, Gemini with corporate API key, Ollama on their server) and set priority order. The fallback chain handles provider unavailability automatically.

## Chat Engine & Orchestration

### Orchestration Loop

ChatEngine always owns the loop, regardless of LLM backend. This gives us identical behavior for remote and local, UI visibility into each tool call step, and control over max iterations / timeouts / confirmation flow.

```
1. User sends message
2. ChatEngine receives: user message + history + tool definitions (from MCP discovery)
3. Call provider.generateStream(messages, tools)
4. Collect StreamEvents, emit to UI (TextDelta for typing effect, ToolCallStart for indicators)
5. On Done event, if result contains toolCalls:
   a. For each toolCall, execute via ToolExecutor (routes to correct McpTool)
   b. Append tool results as tool-role messages
   c. Send updated conversation back to LLM (step 3)
   d. Repeat until LLM responds with text only (max 10 iterations)
6. Return final text response to ViewModel
```

### Safety Guardrails

**Phase A** — Read-only tools only (AAP MCP server defaults to `ALLOW_WRITE_OPERATIONS=false`)

**Phase B** — Write operations with confirmation:
- Tool calls tagged as `destructive` by MCP server (via tool annotations) require user confirmation
- UI shows confirmation card: "The assistant wants to launch job 'Patching' on inventory 'production'. Allow?"
- Approve/Deny flow — result fed back to LLM
- All operations confirmed in local LLM mode (Phase D) regardless of annotation

### Error Handling

- MCP unreachable → structured `ToolResult(success=false, errorType=CONNECTION_ERROR)` → LLM explains gracefully
- Tool call error → error type + message as tool result → LLM suggests remediation
- LLM timeout → "Response timed out" with retry button
- Iteration limit (10) → "I wasn't able to complete this request"
- Local LLM parser crash (Phase D) → retry without tools → plain text response

### Streaming

All providers emit the same `Flow<StreamEvent>` — the UI never knows which provider is active:
- **OpenAI-compatible**: SSE from `/v1/chat/completions?stream=true`, `delta` chunks
- **Anthropic**: SSE from `/v1/messages` with `stream=true`, `content_block_delta` events
- **Gemini**: SSE from `streamGenerateContent`, `candidates[0].content.parts` deltas
- **Local LLM (Phase D)**: LiteRT-LM callback
- All parsed via `okhttp-sse`, bridged to `Flow<StreamEvent>` via `callbackFlow`
- Tool execution: "thinking" indicator with tool name (e.g., "Querying failed jobs...")

## Chat UI

### Layout

```
┌──────────────────────────────┐
│ ● Connected to prod-aap      │  ← Connection status bar (per MCP server)
│ ● ansible-know               │
├──────────────────────────────┤
│                              │
│  ┌────────────────────────┐  │
│  │ Hi! I can help you     │  │  ← Welcome message
│  │ query your AAP instance│  │
│  └────────────────────────┘  │
│                              │
│        ┌──────────────────┐  │
│        │ What jobs failed │  │  ← User bubble (right)
│        │ today?           │  │
│        └──────────────────┘  │
│                              │
│  ┌────────────────────────┐  │
│  │ [gear] Querying jobs...│  │  ← Tool indicator (collapsible)
│  └────────────────────────┘  │
│                              │
│  ┌────────────────────────┐  │
│  │ 3 jobs failed today:   │  │  ← Assistant response
│  │ - Patching (prod)      │  │
│  │ - Backup (staging)     │  │
│  └────────────────────────┘  │
│                              │
├──────────────────────────────┤
│ [Ask about your AAP...  ][>]│  ← Input + send
├──────────────────────────────┤
│ Templates  Infra  Activity  │  ← Bottom nav with
│                   Assistant │    new 4th tab
└──────────────────────────────┘
```

### Components

- **Connection status bar** — Simple text list showing per-server status (e.g., "AAP MCP: connected", "ansible-know: disconnected"). Tappable for Assistant settings.
- **Message list** — LazyColumn, auto-scroll. User right-aligned, assistant left-aligned.
- **ChatBubble** — Plain text rendering in Phase A (markdown rendering deferred). Tool indicators inline as simple text labels.
- **Confirmation cards (Phase B)** — Inline card with Approve/Deny buttons, blocks conversation.
- **Input bar** — TextField + send button, disabled while generating, multi-line expand.
- **AssistantSettingsSheet** — Bottom sheet: LLM provider list (add/remove/reorder Claude, Gemini, Ollama, etc.), per-provider config fields (API key, URL, model), fallback toggle, MCP server status, clear history.

### State

- `AssistantUiState`: message list, input text, isGenerating, MCP connection states (per server), LLM config
- Chat history in-memory only for Phase A (persistence deferred)
- Last 100 messages in memory, older trimmed

## Phased Delivery

### Phase A — Read-Only Assistant

- Assistant tab with chat UI
- MCP client (Streamable HTTP, OkHttp + okhttp-sse)
- Multi-MCP server support per instance (operations + knowledge) — built as `List<McpClient>` from the start to avoid single-to-multi refactor later
- LLM orchestration loop (ChatEngine) with `LlmProvider` interface designed for all providers
- `OpenAiCompatibleProvider` — covers Ollama, vLLM, llama.cpp, OpenAI, and Gemini via Google's OpenAI compat layer
- MCP server URLs field on auth/settings screens with handshake validation on save
- LLM provider configuration in Assistant settings
- Chat history in-memory (100 messages, clears on app restart)
- Read-only tools only
- No confirmation flow

**Architecture note:** All interfaces, types (`ToolSpec`, `StreamEvent`, `LlmResult`, `ToolCall`), and the `ChatEngine` orchestration loop are designed from day one to support additional providers (Anthropic, Gemini native, local LLM) and the fallback chain. The `LlmProvider` interface, schema passthrough functions, and provider config model (`LlmProviderConfig` sealed interface) ship in Phase A even though only `OpenAiCompatibleProvider` is implemented. Adding providers is purely additive — new class, new Koin binding, no refactoring.

### Phase B — Read + Execute

Additive to A:
- Confirmation cards for destructive tool calls
- Tool annotation support (read `destructive` flag from MCP metadata)
- Approve/Deny flow in ChatEngine
- AAP MCP server enables `ALLOW_WRITE_OPERATIONS=true` — app changes minimal since tool discovery is dynamic

### Phase C — Multi-Instance + Multi-MCP

Additive to B:
- Multiple simultaneous MCP connections across instances (N servers x M instances)
- Tool namespacing: prefix with instance alias (`prod_list_jobs`, `staging_list_jobs`)
- Instance context injected into tool descriptions for LLM
- Cross-instance comparison queries via single flat tool list
- Instance routing based on user query or explicit selector

### Phase D — Additional Providers + Offline Mode

Additive to C. Consolidates all provider expansion into one effort:

**Additional remote providers:**
- `AnthropicProvider` — Claude API (`/v1/messages` + `tool_use` blocks), dedicated DTO layer, `x-api-key` auth, `content_block_delta` streaming
- `GeminiProvider` — Google AI native (`generativelanguage.googleapis.com`), `functionDeclarations` tool format, `functionResponse` results (alternative to OpenAI compat layer used in Phase A)
- `FallbackLlmProvider` — priority-ordered provider chain with auto-fallback
- Settings UI: add/remove/reorder providers, per-provider config (API key, URL, model)
- Corporate API key storage (encrypted via Tink)

**Local LLM (offline/air-gap mode):**
- `LocalLlmProvider` via LiteRT-LM (`litertlm-android`, Gemma 4 E2B)
- Model download on-demand (~2.5 GB)
- Curated tool allowlist (simple read-only, 1-3 flat params)
- Structured UI cards for results (no LLM summarization)
- Human confirmation for ALL operations
- Retry-without-tools fallback on parser crash
- Works without network to LLM server — only needs HTTPS to AAP instance + MCP servers
- Final fallback in the provider chain — works even when all remote providers are down

## Dependencies

### New Dependencies (Phase A)

| Library | Purpose | License |
|---------|---------|---------|
| OkHttp SSE (`com.squareup.okhttp3:okhttp-sse`) | SSE parsing for MCP Streamable HTTP responses | Apache 2.0 |

### Existing Dependencies (reused)

- OkHttp — MCP HTTP transport (reuses `AapApiProvider.buildClient()` with auth interceptor + TLS config)
- kotlinx-serialization — JSON-RPC message serialization
- Tink — encrypt MCP URLs and remote LLM API key
- DataStore — persist chat history and LLM config
- Koin — DI for new modules (`assistantModule`)
- Coroutines — async orchestration, `callbackFlow` for SSE-to-Flow bridge

### Deferred Dependencies (Phase D)

| Library | Purpose | License |
|---------|---------|---------|
| LiteRT-LM (`com.google.ai.edge.litertlm:litertlm-android`) | On-device LLM inference with tool calling | Apache 2.0 |

## Design Decisions

1. **No MCP SDK dependency** — Implement Streamable HTTP manually with OkHttp. The official Kotlin MCP SDK is untested on Android. The protocol is simple (4 JSON-RPC methods) and we already have OkHttp. Architecture validated by Kai (Apache 2.0), which implements an identical approach in ~130 lines of Kotlin. We borrow JSON-RPC types and patterns from Kai where solid.

2. **OkHttp over Ktor for MCP transport** — We already have OkHttp configured with auth interceptors, self-signed cert TLS trust, and Koin injection. Adding Ktor would introduce 5 new dependency groups for ~130 lines of transport code. Kai uses Ktor only because it's KMP (needs cross-platform HTTP) — our Android-only app doesn't need it. The `callbackFlow` bridge from OkHttp SSE callbacks to coroutines is ~10 lines.

3. **MCP server is remote, not in-app** — The MCP server runs alongside AAP infrastructure. The app is a client. The official `ansible/aap-mcp-server` handles all 4 AAP subsystems with dynamic tool generation from OpenAPI schemas.

4. **Multi-provider remote LLM with fallback, local as offline mode** — Phase A ships three remote providers: OpenAI-compatible (Ollama, vLLM, OpenAI), Anthropic (Claude), and Google AI (Gemini). Claude and Gemini are corporate-enabled models and first-class providers, not afterthoughts. A priority-ordered fallback chain (borrowed from Kai) tries providers in user-configured order — if Claude is unreachable, fall through to Gemini, then Ollama. On-device models (Phase D) are the final fallback for air-gapped/offline scenarios. Each provider has its own DTO layer for tool format conversion but shares the same `LlmProvider` interface.

5. **LLM config is global, MCP config is per-instance** — Users configure their LLM providers once (corporate Claude key, corporate Gemini key, self-hosted Ollama URL) with priority order. Each AAP instance has its own MCP servers. This matches reality: one set of LLM credentials, multiple AAP environments.

6. **Multi-MCP per instance** — Each instance can connect to multiple MCP servers (operations, knowledge, custom). Tool namespaces don't collide (`controller.*` vs `search_modules`). This enables the troubleshooting flow where AAP MCP provides error data and ansible-know provides module documentation.

7. **Universal Tool interface** — MCP tools, built-in tools, and future cross-instance tools all implement the same interface. The LLM never knows the tool's origin. Raw `JsonObject` schema passthrough avoids lossy conversion when bridging MCP schemas to LLM provider formats. Pattern borrowed from Kai.

8. **ChatEngine always owns the orchestration loop** — Both remote (we parse tool_calls from response) and local (LiteRT-LM manual mode, `automaticToolCalling = false`) use the same loop. This gives identical behavior, UI visibility into tool call steps, and control over confirmations.

9. **Structured error types in tool results** — Errors never throw from `Tool.execute()`. They return `ToolResult(success=false, errorType=AUTH_ERROR, data="Token expired")` so the LLM can suggest remediation. Classified as: connection, auth, not_found, timeout, server error.

10. **Anthropic provider as first-class, not bolted on** — Claude uses a different API format (`/v1/messages` with `tool_use` blocks) than OpenAI-compatible (`/v1/chat/completions` with `tools`). Rather than trying to shim Claude through an OpenAI adapter, we implement `AnthropicProvider` as a dedicated provider with its own DTO layer (like Kai does). This ensures full compatibility with Claude's tool calling format, streaming events, and future features. The `ToolSpec.parametersSchema` passthrough works directly as Anthropic's `input_schema`.

11. **Phased delivery with dynamic tool discovery** — The app discovers tools from MCP servers at connect time. Adding Phase B/C/D capabilities is primarily additive — no refactoring between phases. The `LlmProvider` interface is designed from day one to support both remote and local backends.

## References

- [Kai](https://github.com/SimonSchubert/Kai) — Apache 2.0 KMP app with hand-rolled MCP client, universal Tool interface, multi-provider LLM support. Validated our MCP client and tool interface design. Patterns and types borrowed where solid.
- [PennywiseAI](https://github.com/sarim2000/pennywiseai-tracker) — On-device LLM (LiteRT-LM, Qwen 2.5 1.5B) with streaming via Flow. Validated dynamic system prompt injection for domain context.
- [ansible/aap-mcp-server](https://github.com/ansible/aap-mcp-server) — Official AAP MCP server. Streamable HTTP, dynamic tool generation, toolset routing. Primary integration target.
- [ansible-know-mcp](https://github.com/leogallego/ansible-know-mcp) — Ansible module docs and troubleshooting MCP server. Knowledge layer complement to AAP MCP.

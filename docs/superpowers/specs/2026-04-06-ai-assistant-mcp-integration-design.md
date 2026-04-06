# AI Assistant with MCP Integration — Design Spec

## Overview

Add an AI Assistant tab to AAP Remote Control that lets users interact with their AAP instances through natural language. The system connects to a remote AAP MCP server over Streamable HTTP and uses either an on-device or self-hosted LLM to orchestrate tool calls. No paid API subscriptions or proprietary dependencies — fully open source and free.

## Architecture

Four components:

1. **Chat UI** — 4th bottom tab ("Assistant") with a standard chat interface
2. **LLM Orchestrator (ChatEngine)** — Manages the conversation loop: user input → LLM → tool calls → MCP execution → LLM → final answer
3. **LLM Backends** — `LlmProvider` interface with two implementations: `LocalLlmProvider` (Google AI Edge FC SDK, 3 tiers) and `RemoteLlmProvider` (OpenAI-compatible endpoint)
4. **MCP Client** — Connects to AAP MCP server over Streamable HTTP, discovers tools, executes tool calls

### Data Flow

```
User types "What jobs failed today?"
  → ChatEngine sends to LLM with tool definitions
  → LLM responds with tool_call: list_jobs(status=failed)
  → ChatEngine executes via MCP client → AAP MCP server → AAP API
  → Result returned to LLM
  → LLM generates natural language answer
  → Displayed in Chat UI
```

### System Diagram

```
┌─────────────────────────────────────────┐
│  Android App                            │
│  ┌───────────┐    ┌──────────────────┐  │
│  │ Chat UI   │───>│ LLM Orchestrator │  │
│  │ (Tab 4)   │    │ (on-device or    │  │
│  └───────────┘    │  remote LLM)     │  │
│                   └────────┬─────────┘  │
│                            │ tool calls │
│                   ┌────────▼─────────┐  │
│                   │ MCP Client       │  │
│                   │ (Streamable HTTP) │  │
│                   └────────┬─────────┘  │
└────────────────────────────┼────────────┘
                             │ HTTPS
                   ┌─────────▼─────────┐
                   │ AAP MCP Server    │
                   │ (alongside AAP)   │
                   └─────────┬─────────┘
                             │
                   ┌─────────▼─────────┐
                   │ AAP Instance      │
                   └───────────────────┘
```

## Module Structure

```
app/src/main/kotlin/com/example/aapremote/
├── network/
│   ├── mcp/
│   │   ├── McpClient.kt             # Streamable HTTP MCP client
│   │   ├── McpSession.kt            # Session state, tool cache
│   │   ├── McpTransport.kt          # HTTP transport layer (OkHttp)
│   │   └── McpTypes.kt              # JSON-RPC message types
│   └── ... (existing Retrofit layer untouched)
│
├── assistant/
│   ├── engine/
│   │   ├── ChatEngine.kt            # Orchestration loop (LLM <-> MCP)
│   │   ├── ChatMessage.kt           # Message model (user/assistant/tool)
│   │   └── ToolExecutor.kt          # Routes tool calls to MCP client
│   ├── llm/
│   │   ├── LlmProvider.kt           # Interface: sendChat(messages, tools) -> response
│   │   ├── LocalLlmProvider.kt      # On-device (AI Edge FC SDK)
│   │   ├── RemoteLlmProvider.kt     # Remote (OpenAI-compatible API)
│   │   └── LlmModelRegistry.kt      # Available models (low/mid/high/remote)
│   ├── data/
│   │   ├── AssistantRepository.kt   # Chat history, settings persistence
│   │   └── AssistantConfig.kt       # Per-instance MCP URL, LLM preference
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
│   └── AapInstance.kt                # MODIFIED: add mcpServerUrl field
```

## Instance Configuration

Each AAP instance can optionally have its own MCP server URL.

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
    val mcpServerUrl: String?         // NEW - nullable, optional
)
```

### Auth Screen Changes

- New optional "MCP Server URL" field under an expandable "AI Assistant" section
- Validated on save: attempts MCP `initialize` handshake if provided (non-blocking — save succeeds with a warning if unreachable)
- Can be added/changed later from instance detail in Settings

### Serialization

- `mcpServerUrl` stored encrypted in DataStore alongside URL and token
- Backward compatible — defaults to `null` for existing instances

### Per-Instance MCP Behavior

- Instances without MCP URL show "No MCP server configured" in the Assistant tab with a configure button
- Switching active instance disconnects the old MCP session and connects to the new one
- MCP auth reuses the same Bearer token stored per instance

## MCP Client

Lightweight Streamable HTTP implementation using OkHttp (no SDK dependency initially).

### Capabilities

- **Connect** — POST `initialize` JSON-RPC to MCP server URL, receive `Mcp-Session-Id`, send `initialized` notification
- **Discover tools** — `tools/list` at connect time, cache tool definitions
- **Execute tools** — `tools/call` with tool name + arguments, return result
- **Session management** — Track `Mcp-Session-Id`, handle expiry/reconnect
- **Disconnect** — HTTP DELETE to terminate session

### Transport

- Single endpoint per MCP server (e.g., `https://aap.example.com:8080/mcp`)
- POST with `Content-Type: application/json`, `Accept: application/json, text/event-stream`
- Responses can be plain JSON or SSE stream — client handles both
- Auth: Bearer token from the AAP instance

### Connection Lifecycle

- Connects when Assistant tab is opened and active instance has `mcpServerUrl`
- Disconnects when switching instances or leaving tab
- Auto-reconnects on session expiry
- Exposes `StateFlow<McpConnectionState>` (Disconnected, Connecting, Connected, Error)

## LLM Backends

### LlmProvider Interface

```kotlin
interface LlmProvider {
    suspend fun chat(
        messages: List<ChatMessage>,
        tools: List<ToolDefinition>
    ): LlmResponse  // contains text and/or tool_calls

    fun isAvailable(): Boolean
    fun modelName(): String
}
```

### Local LLM Tiers (Google AI Edge FC SDK)

| Tier | Model | Size (Q4) | RAM | Min Device | Formatter |
|------|-------|-----------|-----|------------|-----------|
| Low | Gemma 2B | ~1.2 GB | ~1.8 GB | 4 GB RAM | GemmaFormatter |
| Mid | Llama 3.2 3B | ~1.7 GB | ~2.1 GB | 6 GB RAM | LlamaFormatter |
| High | Llama 3.1 8B | ~4.5 GB | ~5.5 GB | 10 GB RAM | LlamaFormatter |

- Models downloaded on-demand (not bundled in APK) from Hugging Face or Google AI Edge model hub
- Stored in app internal storage
- Runtime RAM check recommends tier, warns if underpowered
- Download progress shown in UI
- Model URLs hardcoded per tier in `LlmModelRegistry` — updated via app releases

### Remote LLM

- User provides OpenAI-compatible endpoint (e.g., `http://192.168.1.10:11434/v1` for Ollama)
- Standard `/v1/chat/completions` with `tools` parameter
- Configurable model name (e.g., `llama3.1:70b`)
- Optional API key field (encrypted)

### Global Settings (DataStore)

- `llmMode`: local_low / local_mid / local_high / remote
- `remoteLlmUrl`: String?
- `remoteLlmModel`: String?
- `remoteLlmApiKey`: String? (encrypted)

LLM config is global, not per-instance.

## Chat Engine & Orchestration

### Orchestration Loop

```
1. User sends message
2. ChatEngine receives: user message + history + tool definitions (from MCP)
3. Send to LlmProvider.chat(messages, tools)
4. If response contains tool_calls:
   a. For each tool_call, execute via McpClient.callTool(name, args)
   b. Append tool results as tool-role messages
   c. Send updated conversation back to LLM (step 3)
   d. Repeat until LLM responds with text only (max 10 iterations)
5. Return final text response to ViewModel
```

### Safety Guardrails (Phase B)

- Tool calls tagged as `destructive` by MCP server (via tool annotations) require user confirmation
- UI shows confirmation card: "The assistant wants to launch job 'Patching' on inventory 'production'. Allow?"
- Approve/Deny flow — result fed back to LLM
- Phase A skips confirmation (all tools read-only)

### Error Handling

- MCP unreachable → error passed to LLM as tool result, it responds gracefully
- Tool call error → error message as tool result, LLM explains to user
- LLM timeout → "Response timed out" with retry button
- Iteration limit (10) → "I wasn't able to complete this request"

### Streaming

- Remote LLM: SSE from `/v1/chat/completions?stream=true`
- Local LLM: AI Edge callback
- Tool execution: "thinking" indicator with tool name (e.g., "Querying failed jobs...")

## Chat UI

### Layout

```
┌──────────────────────────────┐
│ * Connected to prod-aap      │  <- Connection status bar
├──────────────────────────────┤
│                              │
│  ┌────────────────────────┐  │
│  │ Hi! I can help you     │  │  <- Welcome message
│  │ query your AAP instance│  │
│  └────────────────────────┘  │
│                              │
│        ┌──────────────────┐  │
│        │ What jobs failed │  │  <- User bubble (right)
│        │ today?           │  │
│        └──────────────────┘  │
│                              │
│  ┌────────────────────────┐  │
│  │ [gear] Querying jobs...│  │  <- Tool indicator
│  └────────────────────────┘  │
│                              │
│  ┌────────────────────────┐  │
│  │ 3 jobs failed today:   │  │  <- Assistant response
│  │ - Patching (prod)      │  │
│  │ - Backup (staging)     │  │
│  └────────────────────────┘  │
│                              │
├──────────────────────────────┤
│ [Ask about your AAP...  ][>]│  <- Input + send
├──────────────────────────────┤
│ Templates Activity Infra    │  <- Bottom nav with
│                   Assistant │    new 4th tab
└──────────────────────────────┘
```

### Components

- **Connection status bar** — MCP state + active instance name, tappable for Assistant settings. Red/yellow/green indicator.
- **Message list** — LazyColumn, auto-scroll. User right-aligned, assistant left-aligned.
- **ChatBubble** — Markdown rendering. Tool indicators inline, collapsed after completion (expandable for debug).
- **Confirmation cards (Phase B)** — Inline card with Approve/Deny buttons, blocks conversation.
- **Input bar** — TextField + send button, disabled while generating, multi-line expand.
- **AssistantSettingsSheet** — Bottom sheet: LLM mode picker, remote config fields, model download progress, clear history.

### State

- `AssistantUiState`: message list, input text, isGenerating, MCP connection state, LLM config
- Chat history persisted to DataStore per-instance, survives restart
- Last 100 messages in memory, older trimmed

## Phased Delivery

### Phase A — Read-Only Assistant

- Assistant tab with chat UI
- MCP client (Streamable HTTP)
- LLM orchestration loop (ChatEngine)
- Local LLM (3 tiers) + remote LLM support
- MCP server URL field on auth/settings screens
- LLM configuration in Assistant settings
- Read-only tools only (list jobs, get status, list templates, etc.)
- No confirmation flow

### Phase B — Read + Execute

Additive to A:
- Confirmation cards for destructive tool calls
- Tool annotation support (read `destructive` flag from MCP metadata)
- Approve/Deny flow in ChatEngine
- MCP server adds write tools (launch job, toggle schedule, etc.) — app changes minimal since tool discovery is dynamic

### Phase C — Cross-Instance Orchestrator

Additive to B:
- Multiple simultaneous MCP connections (one per configured instance)
- ChatEngine aware of instance context, tools namespaced by instance
- Instance routing based on user query or explicit selector
- Cross-instance comparison queries
- Tool definitions include instance labels for LLM context

## Dependencies

### New Dependencies

| Library | Purpose | License |
|---------|---------|---------|
| Google AI Edge FC SDK (`localagents-fc`) | On-device LLM with tool calling | Apache 2.0 |
| MediaPipe LLM Inference (`tasks-genai`) | Local model inference runtime | Apache 2.0 |
| OkHttp SSE (`okhttp-sse`) | SSE parsing for MCP/LLM streaming | Apache 2.0 |

### Existing Dependencies (reused)

- OkHttp — MCP HTTP transport
- kotlinx-serialization — JSON-RPC message serialization
- Tink — encrypt MCP URL and remote LLM API key
- DataStore — persist chat history and LLM config
- Koin — DI for new modules
- Coroutines — async orchestration

### No New Dependencies Required For

- MCP client (OkHttp + kotlinx-serialization)
- Remote LLM client (OkHttp + kotlinx-serialization)
- Chat UI (Compose, already in project)

## Design Decisions

1. **No MCP SDK dependency** — Implement Streamable HTTP manually with OkHttp. The official Kotlin SDK is untested on Android. The protocol is simple enough (4 JSON-RPC methods) and we already have OkHttp. Can swap in the SDK later if it matures.

2. **MCP server is remote, not in-app** — The MCP server runs alongside AAP infrastructure. The app is a client. This avoids duplicating AAP API logic and keeps the MCP server reusable by other clients (Claude, CLI tools).

3. **LLM config is global, MCP config is per-instance** — Users have one LLM preference (their local model or self-hosted server), but each AAP instance has its own MCP server. This matches reality: you have one inference setup but multiple AAP environments.

4. **Google AI Edge FC SDK for local inference** — It's the only production-ready Android library with built-in tool-calling formatters for Gemma and Llama models. Apache 2.0 licensed, no subscriptions.

5. **OpenAI-compatible API for remote LLM** — De facto standard supported by Ollama, vLLM, llama.cpp server, LocalAI, and others. No vendor lock-in.

6. **Phased delivery with dynamic tool discovery** — The app discovers tools from the MCP server at connect time. Adding Phase B/C capabilities is primarily an MCP server change, not an app change. The app just needs confirmation UI (B) and multi-connection management (C).

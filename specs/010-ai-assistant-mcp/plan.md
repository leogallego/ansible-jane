# Implementation Plan: AI Assistant with MCP Integration

**Branch**: `010-ai-assistant-mcp` | **Date**: 2026-05-08 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/010-ai-assistant-mcp/spec.md`

## Summary

Add an AI Assistant tab to AAP Remote Control that acts as an MCP client, connecting to remote MCP servers (primarily `ansible/aap-mcp-server`) over Streamable HTTP. An LLM orchestrates tool calls between user queries and MCP tool execution. Phase A delivers: chat UI, hand-rolled MCP client (OkHttp + SSE), multi-MCP server support per instance, `OpenAiCompatibleProvider` (covers Ollama, vLLM, Gemini via compat), in-memory chat history, read-only tools only.

## Technical Context

**Language/Version**: Kotlin 2.2.10, JVM 17, compileSdk 36, minSdk 31
**Primary Dependencies**: Jetpack Compose (Material 3 BOM 2026.03.01), Retrofit 2.11.0, OkHttp 4.12.0, kotlinx-serialization-json 1.9.0, Koin 4.1.1, Coroutines 1.10.2
**New Dependencies**: `com.squareup.okhttp3:okhttp-sse:4.12.0` (SSE parsing for MCP Streamable HTTP + LLM streaming)
**Storage**: Jetpack DataStore (Preferences) 1.2.1 + Tink 1.20.0 (AES-256-GCM encryption for MCP URLs and LLM API keys)
**Testing**: JUnit 4 + MockWebServer 4.12.0 + Turbine 1.1.0 + kotlinx-coroutines-test 1.10.2 (see research.md R3)
**Target Platform**: Android 12+ (API 31+)
**Project Type**: Mobile app (Android)
**Performance Goals**: MCP tool call ≤60s, LLM response ≤120s, SSE streaming read infinite, max 10 tool iterations (see research.md R5)
**Constraints**: Single `ComponentActivity`, HTTPS-only (network security config), all credentials encrypted via Tink/Keystore, no paid API subscriptions required
**Scale/Scope**: Phase A adds ~20 new Kotlin files across 6 packages (network/mcp, assistant/engine, assistant/llm, assistant/tools, assistant/data, assistant/ui)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Kotlin-Only | PASS | All new code is Kotlin. No Java dependencies introduced. |
| II. Compose-First UI | PASS | AssistantScreen, ChatBubble, AssistantSettingsSheet are all Composables. No XML, Fragments, or new Activities. |
| III. MVVM with UDF | PASS | AssistantViewModel exposes `StateFlow<AssistantUiState>`. UI state follows Idle/Loading/Success/Error sealed pattern. ChatEngine is the domain layer, not in the ViewModel. |
| IV. Security-First | PASS | MCP URLs and LLM API keys encrypted via DataStore + Tink. Bearer token reused from existing auth interceptor. HTTPS-only enforced by network security config. No hardcoded URLs or tokens. |
| V. Lean Dependencies | PASS | Only 1 new dependency: `okhttp-sse` (same OkHttp version already in use). MCP client is hand-rolled (~130 lines), no MCP SDK. LLM providers use raw OkHttp/kotlinx-serialization — no LLM SDK. |
| VI. API-Driven Design | PASS | App is a thin MCP client. All operations go through MCP servers → AAP API. New network layer (`network/mcp/`) follows same Koin module pattern as existing `networkModule`. |
| Technology Constraints | PASS | Koin DI, Retrofit + KotlinX Serialization, Coroutines + Flow, DataStore + Tink — all existing stack. No prohibited alternatives introduced. |
| Development Workflow | PASS | Architecture layers respected: Network (mcp/) → Data (assistant/data/) → Presentation (assistant/presentation/) → UI (assistant/ui/). New `assistantModule` Koin module follows existing pattern. Feature-based packaging maintained. |

**GATE RESULT: PASS — no violations.**

## Project Structure

### Documentation (this feature)

```text
specs/010-ai-assistant-mcp/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (from /speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/kotlin/com/example/aapremote/
├── network/
│   ├── mcp/
│   │   ├── McpClient.kt             # Streamable HTTP MCP client
│   │   ├── McpSession.kt            # Session state, tool cache, reconnect
│   │   ├── McpTransport.kt          # OkHttp transport (JSON + SSE dual handling)
│   │   ├── McpTypes.kt              # JSON-RPC 2.0 message types
│   │   └── McpServerManager.kt      # Multi-server connection manager
│   └── ... (existing Retrofit layer untouched)
│
├── assistant/
│   ├── engine/
│   │   ├── ChatEngine.kt            # Orchestration loop (LLM ↔ MCP)
│   │   ├── ChatMessage.kt           # Message model (user/assistant/tool)
│   │   └── ToolExecutor.kt          # Routes tool calls to MCP, truncates results
│   ├── llm/
│   │   ├── LlmProvider.kt           # Interface: generate/generateStream
│   │   ├── LlmTypes.kt              # LlmResult, StreamEvent, ToolCall, ModelInfo
│   │   └── OpenAiCompatibleProvider.kt  # Ollama, vLLM, llama.cpp, OpenAI, Gemini compat
│   ├── tools/
│   │   ├── ToolSpec.kt              # Canonical tool format (name, desc, JsonObject schema)
│   │   └── McpTool.kt               # Bridges Tool interface to McpClient.callTool()
│   ├── data/
│   │   ├── AssistantRepository.kt   # In-memory chat history, LLM config persistence
│   │   └── AssistantConfig.kt       # Per-instance MCP URLs, global LLM provider settings
│   ├── presentation/
│   │   ├── AssistantViewModel.kt    # UI state, sends messages, streams responses
│   │   └── AssistantUiState.kt      # Chat state sealed interface
│   └── ui/
│       ├── AssistantScreen.kt       # Chat screen composable
│       ├── ChatBubble.kt            # Message rendering (text, tool indicators, errors)
│       └── AssistantSettingsSheet.kt # LLM/MCP config bottom sheet
│
├── model/
│   └── AapInstance.kt                # MODIFIED: add mcpServerUrls field
│
├── ui/main/
│   └── TabDefinitions.kt            # MODIFIED: add Assistant tab
│
├── navigation/
│   └── MainNavigation.kt            # MODIFIED: route to AssistantScreen
│
├── data/
│   └── TokenManager.kt              # MODIFIED: serialize mcpServerUrls field
│
└── AapRemoteApp.kt                  # MODIFIED: add assistantModule to Koin
```

**Structure Decision**: Feature-based packaging under `assistant/` with sub-packages by layer (engine, llm, tools, data, presentation, ui). MCP networking under `network/mcp/` follows existing pattern where networking infra lives under `network/`. Modified files are minimal: 5 existing files receive small additions.

## Complexity Tracking

> No constitution violations to justify — all PASS.

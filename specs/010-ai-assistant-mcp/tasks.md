# Tasks: AI Assistant with MCP Integration (Phase A)

**Input**: Design documents from `/specs/010-ai-assistant-mcp/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Included — research.md R3 defines test strategy (JUnit 4 + MockWebServer + Turbine). Tests placed in a dedicated phase after implementation.

**Organization**: Tasks grouped by user story mapped to the 6-layer implementation order from quickstart.md. Each user story represents an independently testable increment.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1–US4)
- Exact file paths included in descriptions

## User Stories (derived from Phase A scope)

| Story | Title | Priority | Summary |
|-------|-------|----------|---------|
| US1 | MCP Server Connection & Tool Discovery | P1 | Configure MCP servers, connect over Streamable HTTP, discover tools |
| US2 | LLM Streaming Communication | P1 | Send messages to OpenAI-compatible LLMs, receive streaming responses |
| US3 | Tool-Assisted Chat | P1 | Orchestrate the tool-calling loop between LLM and MCP |
| US4 | Chat Interface & App Integration | P1 | Full chat UI with streaming text, tool indicators, settings, navigation |

All stories are P1 (required for Phase A MVP). Dependency order: US1 ∥ US2 → US3 → US4.

## Path Conventions

- Source: `app/src/main/kotlin/io/github/leogallego/ansiblejane/`
- Tests: `app/src/test/kotlin/io/github/leogallego/ansiblejane/`
- Build: `app/build.gradle.kts`, `gradle/libs.versions.toml`

---

## Phase 1: Setup

**Purpose**: Add dependencies and create package structure for all new code

- [x] T001 Add `okhttp-sse` library entry in `gradle/libs.versions.toml` and implementation dependency in `app/build.gradle.kts`
- [x] T002 Add test dependencies (`junit`, `mockwebserver`, `turbine`, `coroutines-test`) in `gradle/libs.versions.toml` and `app/build.gradle.kts`
- [x] T003 Create package directories: `network/mcp/`, `assistant/engine/`, `assistant/llm/`, `assistant/tools/`, `assistant/data/`, `assistant/presentation/`, `assistant/ui/`

---

## Phase 2: Foundation Types (Blocking Prerequisites)

**Purpose**: Data classes, enums, sealed interfaces with zero cross-dependencies. Every later phase depends on these.

**CRITICAL**: No user story work can begin until this phase is complete.

- [x] T004 [P] Create MCP JSON-RPC types in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpTypes.kt` — `JsonRpcRequest`, `JsonRpcResponse`, `JsonRpcError`, `McpToolDefinition`, `McpToolResult`, `McpContent`, `McpInitializeResult`, `McpServerInfo` (all `@Serializable`, see data-model.md)
- [x] T005 [P] Create LLM types in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/llm/LlmTypes.kt` — `StreamEvent` sealed interface (`TextDelta`, `ToolCallStart`, `ToolCallArgs`, `Done`, `Error`), `LlmResult`, `ToolCall`, `ModelInfo` (see data-model.md and contracts/llm-provider.md)
- [x] T006 [P] Create tool types in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/tools/ToolSpec.kt` — `ToolSpec` data class, `Tool` interface, `ToolResult` data class, `ErrorType` enum (see data-model.md)
- [x] T007 [P] Create chat message types in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ChatMessage.kt` — `ChatMessage` data class (`role`, `content`, `toolCalls`, `toolCallId`, `timestamp`), `Role` enum (`USER`, `ASSISTANT`, `TOOL`, `SYSTEM`) (see data-model.md)
- [x] T008 [P] Create UI state in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/presentation/AssistantUiState.kt` — sealed interface with `Idle`, `Loading`, `Active` (`messages`, `isGenerating`, `connections`, `inputText`), `Error` (follows existing `TemplatesUiState` pattern)
- [x] T009 [P] Create assistant config types in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/data/AssistantConfig.kt` — `LlmProviderConfig` sealed interface with `OpenAiCompatible` variant (`url`, `model`, `apiKey?`), config persistence via DataStore + Tink (see data-model.md)

**Checkpoint**: All foundation types compile. No runtime behavior yet.

---

## Phase 3: User Story 1 — MCP Server Connection & Tool Discovery (P1)

**Goal**: App connects to MCP servers configured on an AAP instance, discovers available tools, and tracks connection state per server.

**Independent Test**: Configure MCP server URL on an AAP instance → app POSTs `initialize` + `tools/list` → tools appear in `McpServerManager.getAllTools()` → connection state is `Connected`.

**Dependencies**: Phase 2 complete (McpTypes, ToolSpec)

### Implementation

- [x] T010 [US1] Create MCP transport layer in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpTransport.kt` — OkHttp POST for JSON-RPC, SSE `callbackFlow` bridge for `text/event-stream` responses, dual response handling (check `Content-Type` header), separate SSE OkHttpClient with infinite read/call timeout via `baseClient.newBuilder()` (see research.md R2)
- [x] T011 [US1] Create MCP session tracker in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpSession.kt` — `McpConnectionState` sealed interface (`Disconnected`, `Connecting`, `Connected`, `Error`), `MutableStateFlow<McpConnectionState>`, tool cache as `StateFlow<List<McpToolDefinition>>`, optional `Mcp-Session-Id` tracking (see data-model.md)
- [x] T012 [US1] Create MCP client in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpClient.kt` — `connect()` (POST `initialize` → `notifications/initialized` → `listTools()`), `disconnect()`, `listTools()` (POST `tools/list` with pagination), `callTool(name, arguments)` with error mapping (IOException→CONNECTION_ERROR, 401/403→AUTH_ERROR, 404→NOT_FOUND, timeout→TIMEOUT, 500+→SERVER_ERROR). Uses McpTransport and McpSession. (see contracts/mcp-client.md)
- [x] T013 [US1] Create MCP tool bridge in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/tools/McpTool.kt` — implements `Tool` interface, wraps `McpClient.callTool()`, prefixes description with server label `[$label]`, converts `Map<String,Any>` args to `JsonObject`, catches exceptions → `ToolResult(success=false, errorType=...)` (see spec.md McpTool Bridge section)
- [x] T014 [US1] Create multi-server manager in `app/src/main/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpServerManager.kt` — `connectAll(instance)` launches parallel connections via `coroutineScope { async {} }`, isolates per-server failures, `disconnectAll()`, `getAllTools()` returns flat `List<McpTool>`, `connections: StateFlow<Map<String, McpConnectionState>>` (see contracts/mcp-client.md)
- [x] T015 [US1] Add `mcpServerUrls` field to `app/src/main/kotlin/io/github/leogallego/ansiblejane/model/AapInstance.kt` — `mcpServerUrls: List<McpServerConfig>?` (nullable for backward compat), co-locate `McpServerConfig` data class (`url`, `label`, `enabled`) with `@Serializable` annotation
- [x] T016 [US1] Update serialization in `app/src/main/kotlin/io/github/leogallego/ansiblejane/data/TokenManager.kt` — serialize/deserialize `mcpServerUrls` field in the encrypted instance JSON blob, ensure backward compatibility (defaults to `null` for existing instances)

**Checkpoint**: `McpServerManager.connectAll(instance)` connects to configured MCP servers, `getAllTools()` returns discovered tools, `connections` StateFlow emits per-server state. Verifiable with MockWebServer.

---

## Phase 4: User Story 2 — LLM Streaming Communication (P1)

**Goal**: Send conversation messages with tool definitions to an OpenAI-compatible LLM endpoint and receive streaming SSE responses parsed into `StreamEvent` flow.

**Independent Test**: Configure Ollama endpoint → send a simple message → receive `StreamEvent.TextDelta` chunks → `StreamEvent.Done` with final `LlmResult`.

**Dependencies**: Phase 2 complete (LlmTypes, ToolSpec, ChatMessage). Can run in parallel with Phase 3.

### Implementation

- [x] T017 [P] [US2] Create LLM provider interface in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/llm/LlmProvider.kt` — `generate(messages, tools): LlmResult`, `generateStream(messages, tools): Flow<StreamEvent>`, `isAvailable(): Boolean`, `modelInfo(): ModelInfo` (see contracts/llm-provider.md)
- [x] T018 [US2] Implement OpenAI-compatible provider in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/llm/OpenAiCompatibleProvider.kt` — POST `{url}/chat/completions`, `ToolSpec.toOpenAiTool()` conversion, non-streaming response parsing (`choices[0].message.content` + `tool_calls`), SSE streaming parsing (`delta.content` → `TextDelta`, `delta.tool_calls` → `ToolCallStart`/`ToolCallArgs`, `[DONE]` → `Done`), error mapping (401→LlmAuthException, 429→LlmRateLimitException, 500+→LlmServerException, timeout→LlmTimeoutException), optional `Authorization: Bearer {apiKey}` header (see contracts/llm-provider.md)

**Checkpoint**: `OpenAiCompatibleProvider.generateStream()` returns a `Flow<StreamEvent>` that emits text deltas and tool call events. Verifiable with MockWebServer serving recorded SSE responses.

---

## Phase 5: User Story 3 — Tool-Assisted Chat (P1) MVP Core

**Goal**: ChatEngine orchestrates the full loop: user question → LLM with tool defs → tool calls → MCP execution → results back to LLM → final answer. Max 10 iterations.

**Independent Test**: Send "What jobs failed today?" → ChatEngine calls LLM → LLM returns `tool_call: controller.jobs_read` → ToolExecutor dispatches to McpTool → result appended → LLM responds with natural language summary.

**Dependencies**: Phase 3 (US1) + Phase 4 (US2) both complete.

### Implementation

- [x] T019 [US3] Create tool executor in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ToolExecutor.kt` — `execute(toolCall): ToolResult`, find tool by name in flat list, convert `JsonObject` args to `Map<String,Any>`, call `tool.execute(args)`, smart-truncate result to `maxResultChars` (20K: keep first 60% + `[... N chars truncated ...]` + last 40%), return `ToolResult(success=false, errorType=NOT_FOUND)` if tool not found (see contracts/chat-engine.md)
- [x] T020 [US3] Create chat engine in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ChatEngine.kt` — `processMessage(userMessage, history, tools): Flow<ChatEvent>`, orchestration loop: prepend system prompt → append user message → `provider.generateStream()` → collect `StreamEvent`s → re-emit as `ChatEvent`s → on `Done` with `toolCalls`: execute each via `ToolExecutor` → emit `ToolExecuting`/`ToolResult` → append tool results as TOOL messages → loop (max 10 iterations) → on text-only or limit: emit `AssistantMessage` → flow completes. `ChatEvent` sealed interface: `TextDelta`, `ToolExecuting`, `ToolResult`, `AssistantMessage`, `Error`. System prompt from contracts/chat-engine.md. (see contracts/chat-engine.md)

**Checkpoint**: `ChatEngine.processMessage()` returns a `Flow<ChatEvent>` that correctly loops through tool calls. Verifiable with fake `LlmProvider` + fake `ToolExecutor`.

---

## Phase 6: User Story 4 — Chat Interface & App Integration (P1)

**Goal**: User sees an "Assistant" tab (4th bottom nav item), opens a chat interface, types questions, sees streaming responses with tool execution indicators, and can configure LLM/MCP settings.

**Independent Test**: Open app → tap Assistant tab → type "What job templates exist?" → see streaming text response with tool indicator → open settings → configure LLM provider → switch instances → MCP reconnects.

**Dependencies**: Phase 5 (US3) complete.

### Implementation

- [x] T021 [US4] Create assistant repository in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/data/AssistantRepository.kt` — in-memory chat history (`MutableList<ChatMessage>`, max 100 messages, trim oldest), `addMessage()`, `getHistory()`, `clearHistory()`, LLM config access via DataStore
- [x] T022 [US4] Create assistant ViewModel in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/presentation/AssistantViewModel.kt` — `StateFlow<AssistantUiState>`, `sendMessage(text)` launches `ChatEngine.processMessage()` in `viewModelScope`, collects `ChatEvent` flow to update UI state, `updateInputText()`, observes active instance changes via `distinctUntilChangedBy` to disconnect/reconnect MCP (follows `TemplatesViewModel` pattern), creates `ChatEngine` with current `LlmProvider` + `ToolExecutor` from `McpServerManager.getAllTools()`
- [x] T023 [P] [US4] Create chat bubble composable in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/ui/ChatBubble.kt` — renders `ChatMessage` with role-based alignment (user right, assistant left), text content, tool execution indicators (tool name + "querying..." text), error styling, timestamp
- [x] T024 [US4] Create assistant screen composable in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/ui/AssistantScreen.kt` — connection status bar (per-server status text, tappable for settings), `LazyColumn` message list with auto-scroll, welcome message when empty, `ChatBubble` for each message, input bar (`TextField` + send button, disabled while generating), gear icon for settings sheet
- [x] T025 [US4] Create settings bottom sheet in `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/ui/AssistantSettingsSheet.kt` — `ModalBottomSheet` with: LLM provider config (URL, model, API key fields for OpenAI-compatible), MCP server connection status list, clear history button
- [x] T026 [US4] Add Assistant tab to `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/main/TabDefinitions.kt` — new `data object Assistant : TopLevelTab(...)` with icon and label, add to `companion object { val entries }` list as 4th item
- [x] T027 [US4] Add navigation route in `app/src/main/kotlin/io/github/leogallego/ansiblejane/navigation/MainNavigation.kt` — add `composable` route for Assistant tab pointing to `AssistantScreen`
- [x] T028 [US4] Create Koin `assistantModule` and register in `app/src/main/kotlin/io/github/leogallego/ansiblejane/AapRemoteApp.kt` — `single { McpServerManager(...) }` with `httpClientFactory` reusing `AapApiProvider.buildClient()` pattern (AuthInterceptor + CertTrustManager), `factory` for `OpenAiCompatibleProvider`, `single { AssistantRepository(...) }`, `viewModelOf(::AssistantViewModel)`, add module to `startKoin { modules(...) }` (see quickstart.md Koin Module section)

**Checkpoint**: Full assistant feature functional end-to-end. All 11 items from quickstart.md verification checklist pass.

---

## Phase 7: Tests

**Purpose**: Unit and integration tests for highest-value components per research.md R3 priority table.

- [x] T029 [P] Write MCP client tests in `app/src/test/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpClientTest.kt` — MockWebServer with recorded JSON responses for `initialize`, `tools/list`, `tools/call`; test connection lifecycle, tool discovery, tool execution, error mapping (401→AUTH_ERROR, timeout→TIMEOUT, etc.), dual response handling (JSON vs SSE)
- [x] T030 [P] Write MCP transport tests in `app/src/test/kotlin/io/github/leogallego/ansiblejane/network/mcp/McpTransportTest.kt` — MockWebServer with SSE fixtures, test `callbackFlow` bridge, dual content-type handling, timeout behavior
- [x] T031 [P] Write OpenAI provider tests in `app/src/test/kotlin/io/github/leogallego/ansiblejane/assistant/llm/OpenAiCompatibleProviderTest.kt` — MockWebServer with recorded streaming SSE responses, test `TextDelta` emission, `ToolCallStart`/`ToolCallArgs` accumulation, `Done` with final `LlmResult`, error responses (401, 429, 500), `toOpenAiTool()` conversion
- [x] T032 [P] Write ChatEngine tests in `app/src/test/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ChatEngineTest.kt` — fake `LlmProvider` + fake `ToolExecutor`, test: single-turn text response, multi-turn tool calling loop, max iteration limit (10), tool not found error, LLM error propagation. Use Turbine for `Flow<ChatEvent>` assertions.
- [x] T033 [P] Write ToolExecutor tests in `app/src/test/kotlin/io/github/leogallego/ansiblejane/assistant/engine/ToolExecutorTest.kt` — test tool dispatch by name, `NOT_FOUND` for unknown tool, smart truncation (verify 60/40 split, boundary at `maxResultChars`), `JsonObject` → `Map<String,Any>` conversion

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Verification, error edge cases, and cleanup

- [x] T034 Run quickstart.md verification checklist — verify all 11 items: app builds, tab appears, MCP URLs configurable, MCP connects, LLM sends/receives, ChatEngine orchestrates, streaming works, tool indicators show, errors display gracefully, instance switching reconnects, chat history persists across tab switches
- [x] T035 Test error edge cases — MCP server down during chat, LLM unreachable, auth token expired mid-conversation, malformed JSON-RPC response, SSE stream interrupted, concurrent instance switch during generation

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1: Setup ──────────────────────────────┐
                                              │
Phase 2: Foundation Types ───────────────────┤ (blocks all user stories)
                                              │
            ┌─────────────────────────────────┘
            │
            ├── Phase 3: US1 (MCP Client) ──────┐
            │                                    │
            ├── Phase 4: US2 (LLM Provider) ────┤ (US1 ∥ US2)
            │                                    │
            │   Phase 5: US3 (Chat Engine) ──────┤ (depends on US1 + US2)
            │                                    │
            │   Phase 6: US4 (Chat UI) ──────────┤ (depends on US3)
            │                                    │
            │   Phase 7: Tests ──────────────────┤ (after implementation)
            │                                    │
            │   Phase 8: Polish ─────────────────┘ (after all)
```

### User Story Dependencies

- **US1 (MCP Client)**: Starts after Phase 2. No dependency on other stories.
- **US2 (LLM Provider)**: Starts after Phase 2. No dependency on other stories. **Can run in parallel with US1.**
- **US3 (Chat Engine)**: Depends on US1 + US2 both complete. Core orchestration.
- **US4 (Chat UI)**: Depends on US3. Wires everything into the app.

### Within Each Phase

- Foundation types (Phase 2): All 6 files are independent → all [P]
- MCP client (Phase 3): McpTransport → McpSession → McpClient → McpTool → McpServerManager (sequential within layer)
- LLM provider (Phase 4): LlmProvider interface → OpenAiCompatibleProvider (sequential)
- Chat engine (Phase 5): ToolExecutor → ChatEngine (sequential)
- Chat UI (Phase 6): Repository → ViewModel → ChatBubble [P] → Screen → Settings → Tab → Nav → Koin
- Tests (Phase 7): All test files are independent → all [P]

### Parallel Opportunities

```
Phase 2: T004 ∥ T005 ∥ T006 ∥ T007 ∥ T008 ∥ T009  (6 files, zero deps)
Phase 3 ∥ Phase 4: US1 and US2 can run in parallel  (different packages)
Phase 6: T023 can run in parallel with T021-T022    (ChatBubble has no deps on ViewModel)
Phase 7: T029 ∥ T030 ∥ T031 ∥ T032 ∥ T033          (5 test files, zero deps)
```

---

## Parallel Example: Foundation Types (Phase 2)

```
# All 6 foundation files can be created simultaneously:
T004: McpTypes.kt          (network/mcp/)
T005: LlmTypes.kt          (assistant/llm/)
T006: ToolSpec.kt           (assistant/tools/)
T007: ChatMessage.kt        (assistant/engine/)
T008: AssistantUiState.kt   (assistant/presentation/)
T009: AssistantConfig.kt    (assistant/data/)
```

## Parallel Example: US1 + US2 (Phases 3-4)

```
# MCP client and LLM provider have no cross-dependencies:
Phase 3 (US1): T010 → T011 → T012 → T013 → T014 → T015 → T016
Phase 4 (US2): T017 → T018
# These two chains can execute concurrently
```

---

## Implementation Strategy

### MVP First (Phase 1-6, sequential)

1. Complete Phase 1: Setup (T001-T003)
2. Complete Phase 2: Foundation types (T004-T009, parallel)
3. Complete Phase 3: US1 MCP Client (T010-T016) ∥ Phase 4: US2 LLM Provider (T017-T018)
4. Complete Phase 5: US3 Chat Engine (T019-T020)
5. Complete Phase 6: US4 Chat UI & Integration (T021-T028)
6. **STOP and VALIDATE**: Run quickstart.md verification checklist (T034)
7. Complete Phase 7: Tests (T029-T033, parallel)
8. Complete Phase 8: Polish (T035)

### Incremental Validation Points

| After Phase | What's Testable |
|-------------|-----------------|
| Phase 2 | App compiles with all new types |
| Phase 3 | MCP connects to MockWebServer, discovers tools |
| Phase 4 | LLM sends/receives via MockWebServer |
| Phase 5 | ChatEngine loops tool calls with fakes |
| Phase 6 | Full end-to-end with real MCP + LLM servers |

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks
- [Story] label maps task to user story for traceability
- All file paths are relative to `app/src/main/kotlin/io/github/leogallego/ansiblejane/`
- Test file paths are relative to `app/src/test/kotlin/io/github/leogallego/ansiblejane/`
- Commit after each task or logical group
- Phase A scope only — no Anthropic/Gemini/Local providers (deferred to Phase D per spec)

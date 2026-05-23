# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ansible Jane is a lightweight Android app for managing Ansible Automation Platform (AAP). Users authenticate with their AAP instance, browse job templates, launch playbooks, monitor job status, and interact with Jane, an AI assistant that works with local models (Ollama, llama.cpp) or frontier providers (OpenAI-compatible, Gemini, OpenRouter) through tool-use and MCP.

Full specification is in `idea.md`.

## Tech Stack (Strictly Enforced)

- **Language:** Kotlin only. No Java.
- **UI:** Jetpack Compose with Material 3 (Material You). No XML layouts, no Fragments. Single `ComponentActivity`.
- **Architecture:** MVVM with Unidirectional Data Flow. ViewModels expose UI state via `StateFlow`.
- **Networking:** Retrofit + Kotlin Serialization + Coroutines.
- **DI:** Koin (not Hilt).
- **Security:** Jetpack DataStore + Tink (Android Keystore-backed) for token/URL storage. HTTPS only via `network_security_config.xml`. `EncryptedSharedPreferences` is deprecated — do not use.

## AAP Requirements

- **Minimum version:** AAP 2.5+ with Gateway
- All API access goes through the AAP Gateway, which proxies to Controller and EDA Controller
- Controller endpoints use `/api/v2/` base path
- EDA endpoints use `/api/eda/v1/` base path

## AAP API Endpoints

All authenticated requests use header: `Authorization: Bearer <TOKEN>`

| Feature | Method | Endpoint |
|---------|--------|----------|
| Validate credentials | GET | `/api/v2/me/` |
| Get templates | GET | `/api/v2/job_templates/` |
| Launch job | POST | `/api/v2/job_templates/{id}/launch/` |
| Job status | GET | `/api/v2/jobs/{id}/` |
| Get token | POST | `/api/v2/tokens/` |

## Architecture Layers

- **Network Layer:** Retrofit interface `AapApiService`, OkHttpClient with auth interceptor, Koin `networkModule`
- **Data Layer:** `TokenManager` (DataStore + Tink encryption), repositories
- **Presentation:** ViewModels with `StateFlow<UiState>` (Idle, Loading, Success, Error pattern)
- **UI:** Compose screens reacting to ViewModel state

## AI Assistant Architecture

The AI assistant (`assistant/` package) provides natural-language interaction with AAP via tool-use LLMs.

### Tool System

Two tool sources, unified via `Tool` interface (`tools/ToolSpec.kt`):

- **Local tools** (`tools/local/`) — 26 tools that call AAP APIs directly via Retrofit. Zero latency, no MCP server required. Examples: `list_job_templates`, `launch_job`, `get_host_facts`, `ping`.
- **MCP tools** (`tools/McpTool.kt`) — dynamically discovered from connected MCP servers. Used for resources not covered by local tools (users, teams, notifications, etc.).

### ToolRouter (`engine/ToolRouter.kt`)

Category-based query routing that selects relevant tools per user message:

- **7 categories**: INVENTORY, JOBS, MONITORING, USERS, SECURITY, CONFIGURATION, EDA — each with keyword sets, resource prefixes, and local tool names.
- **Query matching**: splits user message into words, matches against category keywords, returns tools from matched categories only.
- **Ranking**: scores tools by keyword overlap with query, boosts `list_`/`ping` tools, penalizes destructive tools.
- **Overlap mapping** (`OVERLAP_MAPPING`): when a local tool exists, its MCP equivalent is auto-disabled to avoid duplicates.
- **Read-only enforcement**: `McpServerConfig.readOnly` filters out write actions (`_create`, `_update`, `_delete`, `_launch`, etc.) from read-only MCP servers.
- **Per-tool enable/disable**: `setToolEnabled()` / `isToolEnabled()` for user-facing tool management UI.

### Engine Pipeline (`engine/`)

- **ChatEngine** — agentic loop: sends user message + tool schemas to LLM, executes tool calls, re-sends results until LLM produces a text response. Max 10 iterations.
- **ToolExecutor** — executes tool calls with 30s timeout, 2-minute result cache, array capping (max 10 items), and smart truncation (8K char limit).
- **Token optimization** — 3-tier token saving mode (Standard / Token Saver / Minimal) controls schema detail, tool count caps, and conversation compaction.

## AI Agent Skills

The `skills/` directory contains SKILL.md reference files for Android/Kotlin development. Read the relevant skill file before writing or reviewing code in that area. The full inventory with when-to-use guidance is in `docs/skills-reference.md`.

Quick lookup:
- **Compose UI/state/layout** — `compose-skill/`, `compose-state-*`, `compose-modifier-and-layout-style/`
- **Compose performance** — `compose-recomposition-performance/`, `compose-stability-diagnostics/`, `compose-state-deferred-reads/`
- **Compose animations** — `compose-animations/`
- **Compose side effects** — `compose-side-effects/`
- **Compose testing** — `compose-ui-testing-patterns/`, `android-community/android-unit-test-editor.md`
- **Coroutines/Flow** — `kotlin-coroutines-structured-concurrency/`, `kotlin-flow-state-event-modeling/`
- **Koin DI** — `android-community/koin-editor.md`
- **KMP** — `kotlin-multiplatform-expect-actual/`, `kotlin-types-value-class/`
- **Navigation** — `android-official/navigation-3.md`, `android-official/edge-to-edge.md`
- **Gradle** — `android-community/gradle-configuration.md`

See `skills/README.md` for sources and licenses.

## Development Rules

- **MUST** create all temporary files and directories inside the project directory (e.g., `.tmp/`). NEVER use `/tmp`, `$TMPDIR`, or any system temp directory. Clean up temp files when done.

## Reference Projects

- **Kai 9000** — source code is available at `tmp/Kai/`. When the user refers to "Kai", look at this directory for architecture, UI patterns, and implementation reference. It's a KMP project; main code is in `tmp/Kai/composeApp/src/commonMain/`.

## Android Device Testing

- **MUST** invoke the `android-cli` skill (via Skill tool) before ANY emulator or device interaction — deploying, starting/stopping emulators, capturing screenshots, inspecting layouts, or running apps. Never use `adb` directly for tasks the `android` CLI covers. If the skill is not loaded yet in this session, load it before proceeding.
- **MUST** use the `android` CLI (`android run`, `android emulator`, `android screen capture`, `android layout`) for all device interaction. Prefer `android layout` over screenshots for inspecting UI state.
- Use `uiautomator dump` to find elements by `resource-id` (from Compose `testTag`). Never hard-code pixel coordinates — always resolve element positions from the layout tree or resource-ids.
- All Compose screens **MUST** have `testTag` modifiers on interactive elements (fields, buttons, switches) using the convention `field_<name>`, `button_<name>`, `switch_<name>`. The root `AppNavigation` has `testTagsAsResourceId = true` so tags appear as `resource-id` in uiautomator.
- Use `adb logcat` to verify network requests and errors instead of relying on screenshots.

## Security Rules

- Never hardcode URLs or tokens
- Only use DataStore + Tink for credentials — never `EncryptedSharedPreferences` (deprecated), plain `SharedPreferences`, or SQLite
- Enforce HTTPS-only via network security config


# Ansible Jane — Architecture Service Contracts

This document defines the enforceable architecture contracts for the Ansible Jane codebase.
These contracts are checked during PR review (see `skills/pr-architecture-review/SKILL.md`).

Violations of **hard rules** must be fixed before merge. **Soft guidelines** are advisory
and flagged as recommendations.

---

## 1. Layer Architecture

The codebase follows a strict 6-layer architecture. Each layer may only depend on the
layers below it. No layer skipping is permitted.

```
┌─────────────────────────────────────────────────────┐
│ Layer 6: UI (Composables)                           │
│ Renders state, captures user events                 │
│ May depend on: Presentation, Model                  │
├─────────────────────────────────────────────────────┤
│ Layer 5: Presentation (ViewModels)                  │
│ Manages UI state via StateFlow, orchestrates        │
│ May depend on: Repository, Engine, Model            │
├─────────────────────────────────────────────────────┤
│ Layer 4: Engine (ChatEngine, ToolRouter)             │
│ Business rules, tool orchestration, agentic loop    │
│ May depend on: Repository, Tool definitions, Model  │
├─────────────────────────────────────────────────────┤
│ Layer 3: Repository (Data Access)                   │
│ Data abstraction, caching, source coordination      │
│ May depend on: Network, Platform, Model             │
├─────────────────────────────────────────────────────┤
│ Layer 2: Network (HTTP + MCP)                       │
│ HTTP transport, MCP discovery, API facades          │
│ May depend on: Model, Platform                      │
├─────────────────────────────────────────────────────┤
│ Layer 1: Platform & Models                          │
│ Domain models (accessible to all layers above)      │
│ Platform expect/actual (restricted to Repository+)  │
│ May depend on: Kotlin stdlib only                   │
└─────────────────────────────────────────────────────┘
```

### Hard Rules

- **No layer skipping.** UI must not import from Repository, Network, or Platform
  (domain models in `model/` are accessible to all layers). ViewModels must not import
  from Network. Repositories must not import from Presentation.
- **No upward dependencies.** Network never imports from Presentation or UI.
  Repository never imports from Presentation or UI.
- **Composables never call repository or network methods directly.** All data access
  goes through ViewModel state observation (`collectAsState`).
- **ViewModels never instantiate HTTP clients.** Network access goes through
  repositories, which use `IAapApiProvider`.

### Permitted Exception

`SettingsViewModel` may use `ModelFetcher` directly for dynamic LLM model discovery
from user-provided URLs. This is documented and intentional — creating a repository
for one-shot discovery of external endpoints would be over-engineering.

---

## 2. Interface Contracts

### Hard Rules

- **Every repository must have an interface.** The interface is named `IXxxRepository`
  and lives in the same package as the implementation. The implementation is named
  `XxxRepository`.

  ```kotlin
  // ✅ Correct
  interface IJobRepository { ... }
  class JobRepository(...) : IJobRepository { ... }

  // ❌ Wrong: no interface
  class JobRepository(...) { ... }
  ```

- **Koin binds to interfaces, never concrete types.** Use `bind IXxxRepository::class`.

  ```kotlin
  // ✅ Correct
  single { JobRepository(get()) } bind IJobRepository::class

  // ❌ Wrong: binds to concrete type
  single { JobRepository(get()) }
  ```

- **API service interfaces are grouped by AAP component.** Three interfaces exist:
  - `AapApiService` — Controller endpoints (`/api/v2/`)
  - `EdaApiService` — EDA endpoints (`/api/eda/v1/`)
  - `PlatformApiService` — Gateway endpoints (`/api/gateway/v1/`)

  New API endpoints must be added to the appropriate existing interface. Do not create
  new API service interfaces without an ADR.

### Tool Contracts

- Local tools implement the `LocalTool` interface (which extends `Tool`).
- MCP tools are instances of the `McpTool` concrete class, constructed from MCP server
  discovery data at runtime. You do not subclass `McpTool`.
- Both satisfy the `Tool` interface defined in `shared/.../tools/ToolSpec.kt`.
- New local tools must be registered in `AssistantDiModule` with `bind LocalTool::class`.

---

## 3. Module Boundaries

### Hard Rules

| Module | May depend on | Must not depend on |
|--------|---------------|--------------------|
| `app/` | `shared`, `composeApp` | — |
| `composeApp/` | `shared` | `app/` |
| `shared/` | — | `app/`, `composeApp/` |

- **`commonMain` must never import Android or JVM APIs.** No `android.*`, `java.*`,
  or `javax.*` imports in `commonMain` source sets.
- **`expect`/`actual` requires both platforms.** Do not create `expect` declarations
  unless you will implement `actual` in both `androidMain` and `jvmMain`. Android-only
  features go in `app/` without ceremony.
- **Shared logic must not live in `app/`.** If a tool, repository, or engine feature
  is in `app/`, desktop can never reach it. Use `shared/commonMain` for reusable logic.

### Source Set Rules

| Source set | Purpose | Allowed imports |
|------------|---------|-----------------|
| `commonMain` | Cross-platform logic | Kotlin stdlib, Ktor, kotlinx.*, Koin |
| `androidMain` | Android platform implementations | Android SDK, AndroidX |
| `jvmMain` | Desktop platform implementations | JVM stdlib, java.* |
| `desktopMain` | Desktop UI specifics | Compose Desktop APIs |

---

## 4. State Management Contracts

### Hard Rules

- **ViewModels expose `StateFlow<XxxUiState>`, never `MutableStateFlow`.** The mutable
  backing field is private.

  ```kotlin
  // ✅ Correct
  private val _uiState = MutableStateFlow<JobUiState>(JobUiState.Idle)
  val uiState: StateFlow<JobUiState> = _uiState.asStateFlow()

  // ❌ Wrong: exposes mutable state
  val uiState = MutableStateFlow<JobUiState>(JobUiState.Idle)
  ```

- **UiState uses sealed classes/interfaces** with the standard variants:
  - `Idle` — initial state before any action
  - `Loading` — operation in progress
  - `Success` — operation completed, holds result data
  - `Error` — operation failed, holds error message/type

  ```kotlin
  sealed interface JobUiState {
      data object Idle : JobUiState
      data object Loading : JobUiState
      data class Success(val jobs: List<Job>) : JobUiState
      data class Error(val message: String) : JobUiState
  }
  ```

- **One-time events are modeled separately from persistent UiState.** Use `Channel`
  or `SharedFlow` for navigation events, snackbars, toasts — not as states in the
  sealed class.

### Soft Guidelines

- Prefer `stateIn` with `SharingStarted.WhileSubscribed(5000)` for repository-backed
  flows to avoid unnecessary upstream work.
- Use `combine` for composing multiple state sources rather than nested `collect` blocks.

---

## 5. Dependency Injection Contracts

### Hard Rules

- **All DI uses Koin.** No Hilt, no Dagger, no manual service locators.
- **DI modules are organized by layer:**
  - `sharedNetworkModule` — HTTP, API discovery
  - `sharedDataModule` + `sharedRepositoryModule` — Data access, repositories
  - `sharedAssistantModule` — Engine, tools, MCP
  - `presentationModule` — ViewModels
- **ViewModels are injected via `koinViewModel()` in Composables** or `viewModelOf(::XxxViewModel)`
  in module definitions.
- **New repositories, tools, and services must be registered in the appropriate Koin module.**
  Do not instantiate collaborators manually in constructors when Koin can provide them.

### Soft Guidelines

- Prefer constructor injection over field injection.
- Use `single {}` for stateful services (repositories, managers).
- Use `factory {}` for stateless utilities.
- Use `named()` qualifier only when multiple instances of the same type are needed.

---

## 6. Naming Conventions

### Hard Rules

- **Interfaces:** `IXxxRepository`, `IXxxManager` (prefix `I`)
- **Backing fields:** `_prefix` single-underscore for private mutable backing fields
  (standard Kotlin convention, e.g., `private val _uiState`)
- **Files:** `snake_case` for Ansible content, `PascalCase` for Kotlin files
- **Packages:** `lowercase` following Kotlin conventions

### Soft Guidelines

- ViewModels: `XxxViewModel`
- UiState: `XxxUiState`
- Screens: `XxxScreen`
- Composable components: descriptive name matching function purpose
- Local tools: `XxxLocalTool` with tool name as `snake_case` (e.g., `list_job_templates`)

---

## 7. File Size and Complexity Guidelines

### Soft Guidelines

- **400 LOC warning threshold.** Files exceeding 400 lines should be examined for
  single-responsibility violations. Not all large files are problems — some domains
  are inherently complex.

- **Documented exceptions** (currently acceptable):
  - `ChatEngine.kt` (~509 LOC) — agentic loop orchestration is inherently complex
  - `ToolRouter.kt` (~485 LOC) — category matching + scoring + MCP/local merging
  - `AapApiClient.kt` (~413 LOC) — REST facade with 50+ endpoint wrappers
  - `TokenManager.kt` (~422 LOC) — token lifecycle (candidate for future extraction)
  - `McpServerManager.kt` (~357 LOC) — MCP connection lifecycle

- **Extraction signals:** A file likely needs splitting when it has:
  - More than 3 distinct responsibilities (e.g., networking + caching + validation + formatting)
  - Multiple groups of private helper methods that don't interact
  - Constructor with more than 6 dependencies

---

## 8. Error Handling Contract

### Hard Rules

- **Use `AppError` sealed class** for domain errors. Do not use raw strings or
  platform exceptions as the error model.
- **Transport errors are normalized at the repository boundary.** Ktor exceptions
  become `AppError` variants before reaching ViewModels.
- **User-facing error messages are derived in the Presentation layer.** Repositories
  return structured errors; ViewModels format them for display.

### Soft Guidelines

- Distinguish retryable vs non-retryable errors where relevant.
- Auth failures (401/403) should trigger token refresh or re-authentication flow,
  not generic error display.

---

## 9. Security Contracts

### Hard Rules

- **No hardcoded URLs, tokens, or credentials.** All secrets go through
  `TokenManager` → DataStore + cryptography-kotlin (AES-256-GCM).
- **HTTPS only.** Enforced via `network_security_config.xml`. Self-signed certs
  are supported via `TlsTrustManager` with explicit user opt-in per instance.
- **Never use `EncryptedSharedPreferences`.** It is deprecated in this project.
  Migration shims in `androidMain` are for legacy data only.
- **Sensitive data stays in minimum layers.** Tokens live in `TokenManager` only.
  ViewModels and UI never hold raw tokens.

---

## Versioning

This document follows the project's semver scheme. Updates require a PR with
rationale for the change. The PR review skill checks against the version in `main`.

| Version | Date | Change |
|---------|------|--------|
| 1.0.0 | 2026-06-18 | Initial contracts derived from codebase audit |

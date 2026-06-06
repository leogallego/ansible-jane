<!--
Sync Impact Report
==================
Version change: 1.3.0 → 2.0.0
Rationale: KMP migration (#230) requires replacing Retrofit with
  Ktor Client and Tink with cryptography-kotlin. Both were previously
  prohibited by the Technology Constraints table. This is a MAJOR
  version bump because core principles (IV, V, VI) are fundamentally
  redefined to support Kotlin Multiplatform targets (Android, Desktop,
  iOS).
Modified principles:
  - II. Compose-First UI: generalized to include Compose Multiplatform
    (JetBrains) for KMP targets. Android entry point remains single
    ComponentActivity; Desktop/iOS have platform entry points.
  - IV. Security-First: replaced "Tink encryption" with
    "cryptography-kotlin (platform-native backends)". Replaced
    "OkHttp interceptor" with "HTTP client auth plugin". Added
    one-time Tink migration requirement for existing users.
  - V. Lean Dependencies: replaced "Retrofit + Kotlin Serialization"
    with "Ktor Client + Kotlin Serialization". Replaced "Google Tink"
    with "cryptography-kotlin".
  - VI. API-Driven Design: replaced "Retrofit service interfaces"
    with "Ktor HttpClient-based API clients". Replaced OkHttpClient
    references with HttpClient.
Modified sections:
  - Technology Constraints table: Ktor Client moved from Prohibited
    to Required under Networking. Retrofit moved to Prohibited.
    Tink moved to Prohibited under Security. cryptography-kotlin
    added as Required. UI Framework generalized to include CMP.
    Added Platform Entry Points row.
Templates requiring updates:
  - CLAUDE.md: update Tech Stack section (Retrofit → Ktor, Tink →
    cryptography-kotlin references)
Follow-up TODOs:
  - Update CLAUDE.md after KMP migration implementation completes
-->

# AAP Remote Control Constitution

## Core Principles

### I. Kotlin-Only

All application code MUST be written in Kotlin. Java code MUST NOT
be introduced under any circumstance — not for utilities, not for
compatibility shims, not for generated code wrappers. The entire
codebase MUST remain 100% Kotlin to ensure consistency and to
maximize AI-assisted code generation quality.

### II. Compose-First UI

All UI MUST be built with Compose Multiplatform (JetBrains) using
Material 3 (Material You) components. On Android this includes
`androidx.compose.*` re-exported by CMP; on Desktop and iOS it
uses the JetBrains Compose runtime directly. The following are
strictly prohibited:

- XML layouts
- Fragments
- Multiple Activities (Android)

On Android, the app MUST use a single `ComponentActivity` as its
entry point. Desktop and iOS have their own platform entry points
(`main()` function and `ComposeUIViewController`, respectively).
No legacy Android UI patterns are permitted. This ensures a
declarative, composable UI layer with minimal boilerplate across
all targets.

### III. MVVM with Unidirectional Data Flow

The architecture MUST follow Model-View-ViewModel with
Unidirectional Data Flow (UDF):

- ViewModels MUST expose UI state via `StateFlow`
- UI state MUST follow the `Idle | Loading | Success | Error`
  sealed pattern
- Compose screens MUST react to ViewModel state — no imperative
  UI updates
- Business logic MUST NOT reside in Composables or Activities

This principle ensures predictable state management and testable
presentation logic.

### IV. Security-First

Credential and token storage MUST use DataStore KMP with
cryptography-kotlin AES-256-GCM encryption. Key material MUST be
protected by the platform-native key store (Android Keystore, Java
KeyStore, iOS Keychain). The following are non-negotiable:

- NEVER hardcode AAP URLs or tokens
- NEVER use plain `SharedPreferences`, `EncryptedSharedPreferences`
  (deprecated), or SQLite for credentials
- MUST enforce HTTPS-only via network security configuration
  (Android: `network_security_config.xml`; other platforms:
  platform-native TLS defaults)
- All authenticated API calls MUST include
  `Authorization: Bearer <TOKEN>` header via an HTTP client auth
  plugin (Ktor `HttpRequestInterceptor`)
- Existing Android users with Tink-encrypted credentials MUST be
  migrated transparently on first launch after the KMP migration

Security is foundational — no convenience shortcut may bypass
these rules.

### V. Lean Dependencies

The dependency footprint MUST remain minimal to reduce app size
and build complexity:

- **DI:** Koin only. Hilt/Dagger MUST NOT be used (too much
  boilerplate for AI-assisted development)
- **Networking:** Ktor Client + Kotlin Serialization + Coroutines.
  Per-platform engines: OkHttp (Android), CIO (Desktop), Darwin
  (iOS)
- **Security:** DataStore KMP + cryptography-kotlin (platform-native
  backends: JCA on Android/JVM, CryptoKit on iOS)

New dependencies MUST be justified by a clear need that cannot be
met by the existing stack. Prefer stdlib and existing libraries
over adding new ones.

### VI. API-Driven Design

The app is a thin client for the AAP REST API. All features MUST
be driven by API endpoints:

- `/api/v2/me/` — credential validation
- `/api/v2/job_templates/` — template listing
- `/api/v2/job_templates/{id}/launch/` — job execution
- `/api/v2/jobs/{id}/` — job status monitoring
- `/api/v2/tokens/` — token acquisition
- `/api/v2/workflow_job_templates/` — workflow template listing
- `/api/v2/workflow_job_templates/{id}/launch/` — workflow execution
- `/api/v2/workflow_jobs/{id}/` — workflow job status
- `/api/v2/workflow_jobs/{id}/workflow_nodes/` — workflow sub-job listing
- `/api/v2/schedules/` — schedule listing
- `/api/v2/schedules/{id}/` — schedule toggle (PATCH)
- `/api/eda/v1/audit-rules/` — EDA rule audit events (via Gateway)

The network layer MUST be defined as Ktor HttpClient-based API
clients (`AapApiClient` for Controller, `EdaApiClient` for EDA,
`PlatformApiClient` for Gateway) with a Koin-provided
`networkModule`. EDA endpoints use a separate base path
(`/api/eda/v1/`) and a separate client configuration, sharing
the same HttpClient factory and auth plugin. No business logic
should assume offline capability unless explicitly scoped.

## Technology Constraints

The following technology choices are strictly enforced and MUST NOT
be deviated from without a constitution amendment:

| Layer | Technology | Prohibited Alternatives |
|-------|-----------|------------------------|
| Language | Kotlin | Java |
| UI Framework | Compose Multiplatform (Material 3) | XML, Fragments |
| Architecture | MVVM + UDF | MVI, MVP |
| DI | Koin | Hilt, Dagger, Manual |
| Networking | Ktor Client + KotlinX Serialization | Volley, Retrofit |
| Async | Coroutines + Flow | RxJava, Callbacks |
| Security | DataStore KMP + cryptography-kotlin | Tink, EncryptedSharedPreferences (deprecated), SharedPreferences, SQLite |
| Platform Entry | Android: Single ComponentActivity; Desktop: main(); iOS: ComposeUIViewController | Multiple Activities (Android) |

## Development Workflow

- **Architecture layers** MUST be respected: Network → Data →
  Presentation → UI. No layer may bypass an intermediate layer.
- **Koin modules** MUST be organized by layer: `networkModule`,
  `dataModule`, `presentationModule`.
- **State modeling** MUST use sealed classes/interfaces for UI
  state with explicit `Idle`, `Loading`, `Success`, and `Error`
  variants.
- **API validation** MUST use the `/api/v2/me/` endpoint to
  verify credentials before granting access to the app.
- **Code organization** MUST follow feature-based packaging
  within each layer.

## Governance

This constitution is the supreme authority for all development
decisions in the AAP Remote Control project. It supersedes any
conflicting guidance except explicit user overrides.

**Amendment procedure:**

1. Propose the change with rationale
2. Document the change with a version bump
3. Update all dependent artifacts (templates, CLAUDE.md)
4. Commit with message referencing the new version

**Versioning policy:** Semantic versioning (MAJOR.MINOR.PATCH):

- MAJOR: Principle removed or fundamentally redefined
- MINOR: New principle or section added, material expansion
- PATCH: Clarifications, wording fixes, non-semantic refinements

**Compliance review:** All code changes MUST be verified against
these principles. Any deviation MUST be flagged and justified
before merge.

**Version**: 2.0.0 | **Ratified**: 2026-04-02 | **Last Amended**: 2026-06-06

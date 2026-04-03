<!--
Sync Impact Report
==================
Version change: 1.2.0 → 1.3.0
Modified principles:
  - VI. API-Driven Design: added schedule and EDA audit endpoints
    (/api/v2/schedules/, /api/eda/v1/audit-rules/)
  - VI. API-Driven Design: generalized Retrofit interface statement
    to acknowledge multiple service interfaces (AapApiService, EdaApiService)
Modified sections:
  - None beyond Principle VI endpoint list and wording
Templates requiring updates:
  - None — endpoint list is informational, not template-referenced
Follow-up TODOs:
  - None
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

All UI MUST be built with Jetpack Compose using Material 3
(Material You) components. The following are strictly prohibited:

- XML layouts
- Fragments
- Multiple Activities

The app MUST use a single `ComponentActivity` as its entry point.
No legacy Android UI patterns are permitted. This ensures a
declarative, composable UI layer with minimal boilerplate.

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

Credential and token storage MUST use Jetpack DataStore with
Tink encryption (backed by Android Keystore). The following are
non-negotiable:

- NEVER hardcode AAP URLs or tokens
- NEVER use plain `SharedPreferences`, `EncryptedSharedPreferences`
  (deprecated), or SQLite for credentials
- MUST enforce HTTPS-only via `network_security_config.xml`
- All authenticated API calls MUST include
  `Authorization: Bearer <TOKEN>` header via an OkHttp interceptor

Security is foundational — no convenience shortcut may bypass
these rules.

### V. Lean Dependencies

The dependency footprint MUST remain minimal to reduce APK size
and build complexity:

- **DI:** Koin only. Hilt/Dagger MUST NOT be used (too much
  boilerplate for AI-assisted development)
- **Networking:** Retrofit + Kotlin Serialization + Coroutines
- **Security:** Jetpack DataStore + Google Tink (Android
  Keystore-backed encryption)

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

The network layer MUST be defined as Retrofit service interfaces
(`AapApiService` for Controller, `EdaApiService` for EDA) with a
Koin-provided `networkModule`. EDA endpoints use a separate base
path (`/api/eda/v1/`) and a separate Retrofit interface, sharing
the same OkHttpClient and auth interceptor. No business logic
should assume offline capability unless explicitly scoped.

## Technology Constraints

The following technology choices are strictly enforced and MUST NOT
be deviated from without a constitution amendment:

| Layer | Technology | Prohibited Alternatives |
|-------|-----------|------------------------|
| Language | Kotlin | Java |
| UI Framework | Jetpack Compose (Material 3) | XML, Fragments |
| Architecture | MVVM + UDF | MVI, MVP |
| DI | Koin | Hilt, Dagger, Manual |
| Networking | Retrofit + KotlinX Serialization | Volley, Ktor Client |
| Async | Coroutines + Flow | RxJava, Callbacks |
| Security | DataStore + Tink/Keystore | EncryptedSharedPreferences (deprecated), SharedPreferences, SQLite |
| Activity | Single ComponentActivity | Multiple Activities |

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

**Version**: 1.3.0 | **Ratified**: 2026-04-02 | **Last Amended**: 2026-04-03

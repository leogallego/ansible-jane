# Feature Specification: KMP Migration

**Feature Branch**: `197-kmp-migration`
**Created**: 2026-06-06
**Status**: Draft
**Input**: GitHub issue #230 — KMP Migration: Android + Desktop + iOS with Compose Multiplatform

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Project Restructure + Kotlin Upgrade (Priority: P1)

Restructure the single-module Android project into a multi-module KMP layout with a `shared/` module for business logic and a `composeApp/` module for UI. Upgrade Kotlin to 2.4.0. Move all portable code (models, tools, engine, LLM providers — 138+ files with zero Android imports) into `shared/commonMain/`. The Android app continues to build and run from `composeApp/androidMain/` depending on `shared/`.

**Why this priority**: Foundation for everything else. No multiplatform work is possible until the project structure exists. Kotlin upgrade is done here because build files are being rewritten anyway — doing it later means a second round of build churn.

**Independent Test**: Android app builds, runs, and passes all existing tests from the new multi-module structure. All features work identically to the single-module version.

**Acceptance Scenarios**:

1. **Given** the current single-module Android project, **When** the restructure is complete, **Then** `./gradlew :composeApp:assembleDebug` produces a working APK with identical behavior.
2. **Given** Kotlin 2.3.21, **When** upgraded to 2.4.0, **Then** the project compiles with no K2 compiler errors and all existing tests pass.
3. **Given** 138+ portable files (models, tools, engine, LLM providers), **When** moved to `shared/commonMain/`, **Then** they compile as a KMP library and the Android app depends on them.

---

### User Story 2 — Platform Abstractions (Priority: P2)

Create 7 `expect/actual` contracts that abstract Android-specific APIs behind platform-neutral interfaces, plus 1 lightweight `expect fun` for app initialization. Android `actual` implementations wrap existing code. Desktop/iOS stubs are created but not fully implemented yet.

The 7 contracts + 1 expect function:
- **SecureKeyStorage** — key store/load/delete (Android Keystore / Java KeyStore / Keychain)
- **DataStoreStorageFactory** — DataStore file creation per platform
- **TlsTrustManager** — SSL/TLS trust for self-signed certs
- **ConnectivityObserver** — network connectivity monitoring
- **BackgroundWorker** — background task scheduling
- **PlatformUtils** — URL opening, toast/snackbar, app version
- **NotificationManager** — approval notifications (Android-only initially, Desktop/iOS no-op)
- **`expect fun initializeApp()`** — cross-platform Koin startup (Android needs `androidContext()`, others don't)

**Why this priority**: These abstractions are prerequisites for moving data layer and repositories into `shared/`. Without them, the remaining business logic stays Android-only.

**Independent Test**: Android app builds with the new abstractions and existing behavior is preserved. Calling the `expect` interfaces from `commonMain` code compiles and routes to Android `actual` at runtime.

**Acceptance Scenarios**:

1. **Given** Android-specific `TokenManager` using Tink + Android Keystore, **When** refactored to use `expect SecureKeyStorage`, **Then** encryption/decryption works identically on Android.
2. **Given** 4 DataStore instances using Android `Context`, **When** refactored to use `expect DataStoreStorageFactory`, **Then** DataStore reads/writes work identically.
3. **Given** `ConnectivityObserver` using `ConnectivityManager`, **When** wrapped as `expect/actual`, **Then** connectivity changes are still detected on Android.

---

### User Story 3 — Network Migration: Retrofit → Ktor (Priority: P3)

Replace all 14 files using Retrofit/OkHttp with Ktor Client equivalents. This includes 8 network-layer files (API service interfaces, provider, interceptor, discovery, DI module) and 6 files outside the network layer (ModelFetcher, AuthRepository, AssistantModule, SettingsViewModel, AppError, EdaAuditViewModel).

**Why this priority**: Retrofit is JVM-only and blocks the shared module from compiling on non-JVM targets. Ktor is KMP-native and already a dependency (used for MCP).

**Independent Test**: All AAP API calls (templates, jobs, inventories, hosts, EDA, platform) work identically via Ktor on Android. MCP SSE connections work. Self-signed cert support works.

**Acceptance Scenarios**:

1. **Given** 3 Retrofit API service interfaces (`AapApiService`, `EdaApiService`, `PlatformApiService`), **When** migrated to Ktor extension functions, **Then** all API endpoints return correct data.
2. **Given** `AuthInterceptor` as OkHttp interceptor, **When** migrated to Ktor `HttpRequestInterceptor` plugin, **Then** all requests include the Bearer token.
3. **Given** `ModelFetcher` using OkHttp directly, **When** migrated to Ktor `client.get()`, **Then** LLM model discovery works for all providers.
4. **Given** `AppError.kt` mapping `retrofit2.HttpException`, **When** migrated to Ktor `ResponseException`, **Then** error mapping produces the same user-facing messages.

---

### User Story 4 — Encryption Migration: Tink → cryptography-kotlin (Priority: P4)

Replace Google Tink (Android-only) with cryptography-kotlin for AES-256-GCM encryption. Implement `SecureKeyStorage` actual for Android (Keystore). Write one-time transparent migration for existing Tink-encrypted credentials. Migrate `BackupManager` crypto to cryptography-kotlin for cross-platform consistency.

**Why this priority**: Tink is Android-only. Encryption must be in `commonMain` for credentials to be accessible on all platforms. Depends on US2 (`SecureKeyStorage` contract).

**Independent Test**: Existing Android users upgrade and their saved credentials (AAP tokens, LLM API keys) are seamlessly decrypted from Tink format and re-encrypted with cryptography-kotlin. No data loss. New credential saves work. Backup export/import works.

**Acceptance Scenarios**:

1. **Given** credentials encrypted with Tink AES-256-GCM, **When** app upgrades, **Then** one-time migration decrypts with Tink and re-encrypts with cryptography-kotlin transparently.
2. **Given** `BackupManager` using `javax.crypto` PBKDF2 + AES-GCM, **When** migrated to cryptography-kotlin, **Then** backup export/import works with the same password-based encryption.
3. **Given** new credential storage after migration, **When** a token is stored and retrieved, **Then** encryption uses cryptography-kotlin AES-256-GCM with key stored in Android Keystore.

---

### User Story 5 — Compose Multiplatform UI (Priority: P5)

Migrate 60 Compose screen files from `androidx.compose.*` to Compose Multiplatform (JetBrains). Replace Jetpack Navigation with Navigation 3. Replace 56 `collectAsStateWithLifecycle` usages with `collectAsState`. Migrate 33 ViewModels to CMP lifecycle. Move all UI and presentation code to `composeApp/commonMain/`.

**Why this priority**: Largest phase by file count but mostly mechanical (import changes, lifecycle API swaps). Depends on US1-US4 being complete so all business logic is already in `shared/`.

**Independent Test**: Android app builds from `composeApp/commonMain/` Compose screens, all screens render correctly, navigation works, state collection works. No visual regressions.

**Acceptance Scenarios**:

1. **Given** 60 screen files using `androidx.compose.*`, **When** migrated to Compose Multiplatform imports, **Then** all screens render identically on Android.
2. **Given** Jetpack Navigation with `NavHost` and `NavController`, **When** replaced with Navigation 3, **Then** all navigation flows work (Dashboard, Templates, Activity, Infrastructure, EDA, Chat, Settings, job details, workflow status, approval detail).
3. **Given** 56 usages of `collectAsStateWithLifecycle`, **When** replaced with `collectAsState`, **Then** ViewModel state is collected correctly (with platform-specific handling for expensive flows if needed).

---

### User Story 6 — Desktop Target (Priority: P6)

Add a Desktop (JVM) entry point with Compose Multiplatform window. Implement Desktop `actual` classes for all 6 contracts (Java KeyStore, CIO Ktor engine, file-based DataStore, `javax.net.ssl` TLS, polling connectivity, `ScheduledExecutor` background work). Package as runnable JAR or native distribution.

**Why this priority**: First non-Android target. JVM is easiest — portable from Linux, no special hardware or OS required. Validates the entire KMP architecture.

**Independent Test**: Desktop app launches, connects to an AAP instance, browses templates, launches jobs, monitors status, and interacts with Jane AI assistant. All features except Android-specific ones (push notifications, WorkManager) work.

**Acceptance Scenarios**:

1. **Given** the shared module + composeApp, **When** `./gradlew :composeApp:run` is executed, **Then** a desktop window opens with the Ansible Jane UI.
2. **Given** a Desktop build, **When** connecting to AAP with a PAT, **Then** authentication, template browsing, job launching, and monitoring work.
3. **Given** Desktop `SecureKeyStorage` using Java KeyStore, **When** credentials are stored, **Then** they persist across app restarts.

---

### User Story 7 — iOS Target (Priority: P7)

Add iOS target with Kotlin/Native. Implement iOS `actual` classes (Keychain, Darwin Ktor engine, NWPathMonitor, BGTaskScheduler). Create Xcode project wrapper. Minimum iOS 15.

**Why this priority**: Requires macOS + Xcode — cannot be developed or tested from Linux. Deferred until after Desktop validates the KMP architecture.

**Independent Test**: iOS app builds on macOS, launches on simulator, connects to AAP, and all shared features work.

**Acceptance Scenarios**:

1. **Given** the shared module + composeApp + iosApp, **When** built on macOS with Xcode, **Then** the app runs on iOS simulator.
2. **Given** iOS `SecureKeyStorage` using Keychain, **When** credentials are stored, **Then** they persist securely.
3. **Given** iOS `ConnectivityObserver` using NWPathMonitor, **When** network changes, **Then** the app detects connectivity.

---

### Edge Cases

- What happens when existing Android users upgrade and have Tink-encrypted credentials? → One-time transparent migration (US4)
- What happens if Tink migration fails (corrupted keyset)? → Log error, prompt user to re-enter credentials, don't crash
- What happens with `collectAsStateWithLifecycle` → `collectAsState` for flows that poll network? → May continue collecting when backgrounded; add platform-specific suspend for expensive flows
- What happens when `koog-google` (Gemini provider, v1.0.0-beta-preview7) is used on non-JVM targets? → Needs verification; fallback: Gemini calls via platform-specific `actual` if needed
- What happens to existing Android push notifications on Desktop/iOS? → Android notifications stay Android-only; desktop/iOS notifications are out of scope for this migration
- What happens with the 13 Android-dependent files that can't move to `commonMain`? → Stay in `androidMain/` source set, accessed via `expect/actual` contracts

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Project MUST restructure into `shared/` (KMP library) and `composeApp/` (CMP application) modules
- **FR-002**: All 138+ portable files (models, tools, engine, LLM providers) MUST compile in `commonMain` without Android imports
- **FR-003**: The 7 `expect/actual` contracts MUST abstract all platform-specific APIs (SecureKeyStorage, DataStoreStorageFactory, TlsTrustManager, ConnectivityObserver, BackgroundWorker, PlatformUtils, NotificationManager) plus `expect fun initializeApp()` for cross-platform Koin startup
- **FR-004**: All 14 Retrofit/OkHttp files MUST be replaced with Ktor Client equivalents
- **FR-005**: Tink encryption MUST be replaced with cryptography-kotlin AES-256-GCM
- **FR-006**: Existing Tink-encrypted credentials MUST be migrated transparently on first launch
- **FR-007**: All 60 Compose screen files MUST use Compose Multiplatform imports
- **FR-008**: Jetpack Navigation MUST be replaced with Navigation 3
- **FR-009**: All 56 `collectAsStateWithLifecycle` usages MUST be replaced with `collectAsState`
- **FR-010**: Desktop target MUST produce a runnable JVM application
- **FR-011**: iOS target MUST produce a buildable Xcode project (requires macOS)
- **FR-012**: Android app MUST preserve all existing functionality after each migration phase
- **FR-013**: AGP 9.x KMP plugin (`com.android.kotlin.multiplatform.library`) MUST be used for the shared module
- **FR-014**: Kotlin MUST be upgraded to 2.4.0 in Phase 1

### Key Entities

- **shared module**: KMP library containing all business logic, models, tools, network, data, and DI — compiled for Android, JVM Desktop, and iOS
- **composeApp module**: Compose Multiplatform application with all UI screens, ViewModels, navigation — with platform entry points in androidMain, jvmMain, iosMain
- **expect/actual contracts**: 6 platform abstraction interfaces bridging commonMain to platform-specific implementations
- **Ktor HttpClient**: Replaces Retrofit + OkHttp as the HTTP client, with per-platform engines (OkHttp/Android, CIO/Desktop, Darwin/iOS)
- **cryptography-kotlin**: Replaces Tink for AES-256-GCM encryption with platform-native backends

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Android app builds and runs with identical behavior after each migration phase — zero feature regressions
- **SC-002**: Shared module compiles for Android, JVM Desktop, and (on macOS) iOS targets
- **SC-003**: Desktop app connects to AAP and performs core workflows (auth, browse templates, launch jobs, monitor, chat with Jane)
- **SC-004**: 100% of existing unit tests pass after migration
- **SC-005**: Tink credential migration succeeds on first launch for existing Android users with no data loss
- **SC-006**: All 61 local tools work identically when called from shared module
- **SC-007**: Network calls via Ktor return identical responses to current Retrofit calls
- **SC-008**: Build time increase is less than 30% compared to current single-module build

## Assumptions

- Kotlin 2.4.0 is released and stable (confirmed: released June 3, 2026)
- Compose Multiplatform 1.11.1 is stable for Android and Desktop (confirmed)
- cryptography-kotlin 0.6.0 provides reliable AES-256-GCM on all targets (mitigated: uses platform-native backends)
- Koog 1.0.0 core is KMP-ready; koog-google (Gemini) may need non-JVM verification
- DataStore Preferences v1.2.1 supports KMP (confirmed: since v1.1.0)
- iOS development requires macOS + Xcode — Phase 7 is deferred
- Existing `scripts/test-all.sh` can be adapted for multi-module testing
- No breaking API changes in AAP 2.5+/2.6 during migration period

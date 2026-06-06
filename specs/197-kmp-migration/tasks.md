# Tasks: KMP Migration

**Input**: Design documents from `/specs/197-kmp-migration/`
**Prerequisites**: plan.md (required), spec.md (required for user stories)
**Issue**: GitHub #230

**Tests**: Not included unless explicitly requested. Validation checkpoints at the end of each phase.

**Organization**: Tasks follow the 7 migration phases from the plan. Each phase builds on the previous. US1 (Project Restructure) is the foundational work; US2-US7 are sequential phases.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story (US1-US7) — Setup and Polish have no story label
- Include exact file paths in descriptions

## Path Conventions

- **shared module**: `shared/src/{commonMain,androidMain,jvmMain,iosMain}/kotlin/`
- **composeApp module**: `composeApp/src/{commonMain,androidMain,jvmMain,iosMain}/kotlin/`
- **existing source**: `app/src/main/kotlin/io/github/leogallego/ansiblejane/`
- **package**: `io.github.leogallego.ansiblejane`

---

## Phase 0: Baseline Capture

**Purpose**: Record test results and build time before any changes. Required for SC-001 (zero regressions) and SC-008 (build time <30% increase).

- [X] T000 Capture baseline metrics: run `./gradlew assembleDebug` and record wall-clock build time. Run `./gradlew testDebugUnitTest` and record pass/fail counts. Save results to `specs/197-kmp-migration/baseline.txt` for comparison after each phase

---

## Phase 1: Setup (Gradle Restructure)

**Purpose**: Create the multi-module KMP project structure. No code moves yet — just the build system skeleton.

**Skills to load before starting this phase** (read each file into context):
```
Read skills/kotlin-build-kmp-gradle-governance/SKILL.md
Read skills/kotlin-project-modularization/SKILL.md
Read skills/kotlin-kmp-abstraction-decision/SKILL.md
Read skills/kotlin-platform-kmp-bridges/SKILL.md
```

- [X] T001 Update `gradle/libs.versions.toml` — add Compose Multiplatform 1.11.1 (`compose-multiplatform`), cryptography-kotlin 0.6.0 (`cryptography-kotlin`), Ktor engine entries (`ktor-client-okhttp`, `ktor-client-cio`, `ktor-client-darwin`), Navigation 3. Keep existing entries. Upgrade kotlin from `2.3.21` to `2.4.0`
- [X] T002 Create `shared/build.gradle.kts` — KMP library module using `com.android.kotlin.multiplatform.library` plugin (AGP 9.x). Configure targets: `androidLibrary {}`, `jvm()`, `iosArm64()`, `iosSimulatorArm64()`. Add commonMain dependencies: `kotlinx-coroutines-core`, `kotlinx-serialization-json`, `ktor-client-core`, `datastore-preferences`, `koin-core`, `koog`, `collections-immutable`. Add androidMain/jvmMain/iosMain engine dependencies. Set android namespace, minSdk 31, compileSdk 36
- [X] T003 Create `composeApp/build.gradle.kts` — Compose Multiplatform application module. Apply `org.jetbrains.compose`, `org.jetbrains.kotlin.multiplatform`, `com.android.application`. Configure targets: `androidTarget {}`, `jvm()`. Add dependency on `:shared`. Add CMP dependencies for commonMain. Keep android config from current `app/build.gradle.kts` (applicationId, versionCode, versionName, signing)
- [X] T004 Update `settings.gradle.kts` — add `include(":shared")`, `include(":composeApp")`. Keep `:app` for now (removed after full migration). Add Compose Multiplatform plugin to pluginManagement
- [X] T005 Update root `build.gradle.kts` — add KMP and Compose Multiplatform plugin declarations (apply false). Remove AGP `com.android.application` from root if only in submodules
- [X] T006 [P] Create directory structure: `shared/src/commonMain/kotlin/`, `shared/src/androidMain/kotlin/`, `shared/src/jvmMain/kotlin/`, `shared/src/iosMain/kotlin/`, `composeApp/src/commonMain/kotlin/`, `composeApp/src/androidMain/kotlin/`, `composeApp/src/jvmMain/kotlin/`
- [X] T007 Run `./gradlew :shared:build` and `./gradlew :composeApp:assembleDebug` to verify empty modules compile. Fix any Gradle configuration errors

**Checkpoint**: Multi-module skeleton compiles. No code moved yet. `:app` module still builds independently.

---

## Phase 2: Foundational — Move Portable Code (US1)

**Purpose**: Move 138+ immediately portable files to `shared/commonMain/`. Fix the few files that need minor changes. Verify Android builds from the new structure.

**⚠️ CRITICAL**: No further phases can begin until this phase is complete and Android app runs from the new multi-module structure.

**Skills to load before starting this phase** (read each file into context):
```
Read skills/kotlin-kmp-abstraction-decision/SKILL.md
Read skills/kotlin-platform-kmp-bridges/SKILL.md
Read skills/kotlin-project-modularization/SKILL.md
```

### Portable Code Moves (67 + 45 + 4 + 5 = ~121 files)

- [X] T008 [P] [US1] Move `tools/` (67 files) from `app/src/main/kotlin/.../assistant/tools/` to `shared/src/commonMain/kotlin/.../assistant/tools/` — zero changes needed, pure Kotlin
- [X] T009 [P] [US1] Move `model/` (44 files, excluding `AppError.kt`) from `app/src/main/kotlin/.../model/` to `shared/src/commonMain/kotlin/.../model/` — leave `AppError.kt` in `:app` until Phase 4/US3 when Ktor replaces Retrofit (it depends on both `retrofit2.HttpException` and Compose Icons)
- [X] T010 [P] [US1] Move `llm/` (4 files) from `app/src/main/kotlin/.../assistant/llm/` to `shared/src/commonMain/kotlin/.../assistant/llm/` — already KMP-native via Koog 1.0.0
- [X] T011 [US1] Move `engine/` (5 files) from `app/src/main/kotlin/.../assistant/engine/` to `shared/src/commonMain/kotlin/.../assistant/engine/` — fix `DebugLog.kt`: replace `android.util.Log` wrapper with `expect/actual` or `println` multiplatform logger. Update all 7 consumer files: `engine/ChatEngine.kt`, `engine/ToolExecutor.kt`, `engine/ToolRouter.kt`, `network/InstanceDiscovery.kt`, `network/mcp/McpServerManager.kt`, `data/ToolManifestRepository.kt`, `assistant/presentation/AssistantViewModel.kt`

### DI and Supporting Code

- [X] T012 [US1] Move portable DI modules to `shared/src/commonMain/kotlin/.../di/` — `AssistantModule.kt` and `ToolModule.kt` (if they don't reference android/OkHttp). Keep `DataModule.kt` and `NetworkModule.kt` in `:app` for now (they have Android/Retrofit deps)
- [X] T013 [US1] Move `assistant/data/` interfaces and models to `shared/src/commonMain/kotlin/.../assistant/data/` — `AssistantRepository.kt` implementation stays in `:app` temporarily (it uses Android `Context` for file I/O). Extract repository interface to commonMain; full implementation moves in Phase 3/US2 (T038) after DataStoreStorageFactory is ready
- [X] T014 [US1] Update `shared/build.gradle.kts` — ensure all moved code compiles. Add any missing commonMain dependencies discovered during moves

### Wire Up Android App

- [X] T015 [US1] Update `:app` (or `:composeApp`) `build.gradle.kts` to depend on `:shared`. Update import paths in remaining `:app` code to reference classes from `:shared`
- [X] T016 [US1] Run `./gradlew :composeApp:assembleDebug` (or `:app:assembleDebug` if still using app module). Fix compilation errors from moved code
- [X] T017 [US1] Run existing test suite (`scripts/test-all.sh` or `./gradlew testDebugUnitTest`). Fix any test failures from restructure

**Checkpoint**: Android app builds and runs from multi-module structure. All features work identically. 138+ files now in `shared/commonMain/`.

---

## Phase 3: User Story 2 — Platform Abstractions (Priority: P2)

**Goal**: Create 7 `expect/actual` contracts + 1 `expect fun`. Android `actual` implementations wrap existing code. Desktop stubs created. Move DataStore, repositories, and DI to `shared/`.

**Independent Test**: Android app builds with new abstractions. `expect` interfaces called from `commonMain` compile and route to Android `actual` at runtime. All credential storage, connectivity, and background work function identically.

**Skills to load before starting this phase** (read each file into context):
```
Read skills/kotlin-multiplatform-expect-actual/SKILL.md
Read skills/kotlin-platform-kmp-bridges/SKILL.md
Read skills/kotlin-kmp-abstraction-decision/SKILL.md
Read skills/kotlin-data-kmp-data-layer/SKILL.md
```

### expect declarations (commonMain)

- [X] T018 [P] [US2] Create `expect class SecureKeyStorage` in `shared/src/commonMain/kotlin/.../platform/SecureKeyStorage.kt` — methods: `suspend fun storeKey(alias: String, keyBytes: ByteArray)`, `suspend fun loadKey(alias: String): ByteArray?`, `suspend fun deleteKey(alias: String)`
- [X] T019 [P] [US2] Create `expect class DataStoreStorageFactory` in `shared/src/commonMain/kotlin/.../platform/DataStoreStorageFactory.kt` — method: `fun createStorage(name: String): Storage<Preferences>`
- [X] T020 [P] [US2] Create `expect class TlsTrustManager` in `shared/src/commonMain/kotlin/.../platform/TlsTrustManager.kt` — method: `fun createTrustManager(fingerprint: String?): Any` (returns platform-specific trust manager)
- [X] T021 [P] [US2] Create `expect class ConnectivityObserver` in `shared/src/commonMain/kotlin/.../platform/ConnectivityObserver.kt` — method: `fun observe(): Flow<Boolean>`
- [X] T022 [P] [US2] Create `expect class BackgroundWorker` in `shared/src/commonMain/kotlin/.../platform/BackgroundWorker.kt` — methods: `fun schedulePolling(intervalMinutes: Long)`, `fun cancelPolling()`
- [X] T023 [P] [US2] Create `expect class PlatformUtils` in `shared/src/commonMain/kotlin/.../platform/PlatformUtils.kt` — methods: `fun openUrl(url: String)`, `fun showToast(message: String)`, `fun getAppVersion(): String`

### actual implementations — Android (androidMain)

- [X] T024 [P] [US2] Create `actual class SecureKeyStorage` in `shared/src/androidMain/kotlin/.../platform/SecureKeyStorage.kt` — wrap Android Keystore (extract from current `TokenManager.kt` Tink keyset management). Uses `AndroidKeysetManager` for now, replaced in Phase 5/US4
- [X] T025 [P] [US2] Create `actual class DataStoreStorageFactory` in `shared/src/androidMain/kotlin/.../platform/DataStoreStorageFactory.kt` — wrap current `DataStoreProvider.kt` logic using Android `Context`
- [X] T026 [P] [US2] Create `actual class TlsTrustManager` in `shared/src/androidMain/kotlin/.../platform/TlsTrustManager.kt` — wrap current `CertTrustManager.kt` using `javax.net.ssl.X509TrustManager`
- [X] T027 [P] [US2] Create `actual class ConnectivityObserver` in `shared/src/androidMain/kotlin/.../platform/ConnectivityObserver.kt` — wrap current `ConnectivityObserver.kt` using `ConnectivityManager`
- [X] T028 [P] [US2] Create `actual class BackgroundWorker` in `shared/src/androidMain/kotlin/.../platform/BackgroundWorker.kt` — wrap current `ApprovalPollingWorker.kt` using WorkManager
- [X] T029 [P] [US2] Create `actual class PlatformUtils` in `shared/src/androidMain/kotlin/.../platform/PlatformUtils.kt` — wrap Intent/Toast/BuildConfig from current `AuthScreen.kt`, `GeneralTab.kt`, `BackupRestoreSection.kt`

### actual stubs — Desktop (jvmMain)

- [X] T030 [P] [US2] Create stub `actual class SecureKeyStorage` in `shared/src/jvmMain/kotlin/.../platform/SecureKeyStorage.kt` — throw `NotImplementedError("Desktop SecureKeyStorage: Phase 7/US6")` for now
- [X] T031 [P] [US2] Create stub `actual class DataStoreStorageFactory` in `shared/src/jvmMain/kotlin/.../platform/DataStoreStorageFactory.kt` — basic `FileStorage` using `java.io.File` in user home dir
- [X] T032 [P] [US2] Create stub `actual class TlsTrustManager` in `shared/src/jvmMain/kotlin/.../platform/TlsTrustManager.kt` — reuse `javax.net.ssl` (portable from Android)
- [X] T033 [P] [US2] Create stub `actual class ConnectivityObserver` in `shared/src/jvmMain/kotlin/.../platform/ConnectivityObserver.kt` — return `flowOf(true)` for now
- [X] T034 [P] [US2] Create stub `actual class BackgroundWorker` in `shared/src/jvmMain/kotlin/.../platform/BackgroundWorker.kt` — no-op stubs
- [X] T035 [P] [US2] Create stub `actual class PlatformUtils` in `shared/src/jvmMain/kotlin/.../platform/PlatformUtils.kt` — `println` for toast, no-op for others

### Migrate remaining Android-dependent files

- [X] T036 [US2] Move `AnsibleJaneApp.kt` to `composeApp/src/androidMain/kotlin/.../AnsibleJaneApp.kt` — stays Android-only (Application subclass, WorkManager init, Koin `startKoin { androidContext() }`). Create `expect fun initializeApp()` in `shared/src/commonMain/kotlin/.../platform/AppInitializer.kt` for cross-platform Koin initialization; Android `actual` calls `startKoin` with `androidContext()`, Desktop `actual` calls `startKoin` without it
- [X] T036a [P] [US2] Create `expect/actual` notification interface: `expect class NotificationManager` in `shared/src/commonMain/kotlin/.../platform/NotificationManager.kt` — methods: `fun showApprovalNotification(approval: WorkflowApproval)`, `fun createChannel()`. Android `actual` wraps current `ApprovalNotificationManager.kt` (31 android.* imports). Desktop stub: no-op. iOS stub: no-op (platform notifications out of scope for this migration)
- [X] T036b [US2] Refactor `ApprovalPollingWorker.kt` to use `BackgroundWorker` contract — current file extends `CoroutineWorker` (WorkManager). Extract polling logic to `shared/src/commonMain/kotlin/.../notification/ApprovalPollingService.kt` (shared coroutine-based poller). Android `actual BackgroundWorker` delegates to WorkManager which calls the shared poller. Desktop/iOS `actual` calls the shared poller directly via `ScheduledExecutorService`/`BGTaskScheduler`

### Move repositories to commonMain

- [X] T036c [US2] Move `TokenManager.kt` to `shared/src/commonMain/kotlin/.../data/TokenManager.kt` — refactor to use `SecureKeyStorage` and `DataStoreStorageFactory` instead of direct Android APIs
- [X] T037 [P] [US2] Move `UserPreferencesRepository.kt` to `shared/src/commonMain/kotlin/.../data/UserPreferencesRepository.kt` — refactor to use `DataStoreStorageFactory`
- [X] T038 [P] [US2] Move `AssistantRepository.kt` to `shared/src/commonMain/kotlin/.../assistant/data/AssistantRepository.kt` — replace `Context` file I/O with DataStore KMP
- [X] T039 [P] [US2] Move `ApprovalTracker.kt` to `shared/src/commonMain/kotlin/.../notification/ApprovalTracker.kt` — refactor to use `DataStoreStorageFactory`
- [X] T040 [US2] Move `DataModule.kt` to `shared/src/commonMain/kotlin/.../di/DataModule.kt` — replace `androidContext()` with platform-injected dependencies via `expect/actual`
- [X] T041 [US2] Verify Android build: `./gradlew :composeApp:assembleDebug`. Fix compilation errors. Run unit tests

**Checkpoint**: All 7 `expect/actual` contracts + `initializeApp()` created. Android `actual` wraps existing code. Desktop stubs compile. Repositories and DataStore moved to `shared/commonMain/`. Android app works identically.

---

## Phase 4: User Story 3 — Retrofit → Ktor Migration (Priority: P3)

**Goal**: Replace all 14 files using Retrofit/OkHttp with Ktor Client. Move network layer to `shared/commonMain/`.

**Independent Test**: All AAP API calls (templates, jobs, inventories, hosts, EDA, platform) work via Ktor. MCP SSE connections work. Self-signed cert support works.

**Skills to load before starting this phase** (read each file into context):
```
Read skills/kotlin-data-kmp-data-layer/SKILL.md
Read skills/kotlin-project-feature-implementation/SKILL.md
Read skills/android-community/retrofit-networking.md
```
Also fetch up-to-date Ktor docs:
```
Use Context7 MCP tool: resolve-library-id for "Ktor", then query-docs for "HttpClient setup, request interceptors, content negotiation"
```

### Core HTTP Client Setup

- [ ] T042 [US3] Create Ktor `HttpClient` factory in `shared/src/commonMain/kotlin/.../network/HttpClientFactory.kt` — replaces `AapApiProvider.kt`. Configure: content negotiation (kotlinx.serialization JSON), logging, default request (base URL from config), timeout (30s). Use `expect/actual` for engine selection
- [ ] T043 [US3] Create Ktor auth plugin in `shared/src/commonMain/kotlin/.../network/AuthPlugin.kt` — replaces `AuthInterceptor.kt`. Add `Bearer` token header to every request. Get token from `TokenManager`
- [ ] T044 [P] [US3] Create `expect fun createHttpEngine(): HttpClientEngine` in `shared/src/commonMain/kotlin/.../network/HttpEngine.kt`. Android actual: `OkHttp()`. JVM actual: `CIO()`. iOS actual: `Darwin()`

### API Service Migration (8 network-layer files)

- [ ] T045 [US3] Migrate `AapApiService.kt` to `shared/src/commonMain/kotlin/.../network/AapApiClient.kt` — replace Retrofit `@GET/@POST/@PATCH` annotations with Ktor extension functions (`suspend fun getJobTemplates()`, `suspend fun launchJob()`, etc.). Preserve all endpoint paths from plan.md API table
- [ ] T046 [P] [US3] Migrate `EdaApiService.kt` to `shared/src/commonMain/kotlin/.../network/EdaApiClient.kt` — Ktor extension functions for EDA endpoints (`/api/eda/v1/`)
- [ ] T047 [P] [US3] Migrate `PlatformApiService.kt` to `shared/src/commonMain/kotlin/.../network/PlatformApiClient.kt` — Ktor extension functions for Gateway endpoints (`/api/gateway/v1/`)
- [ ] T048 [US3] Migrate `ApiVersionDetector.kt` to `shared/src/commonMain/kotlin/.../network/ApiVersionDetector.kt` — replace `OkHttp.Request` with `client.get()`
- [ ] T049 [US3] Migrate `InstanceDiscovery.kt` to `shared/src/commonMain/kotlin/.../network/InstanceDiscovery.kt` — replace `OkHttp.Request` with `client.get()`
- [ ] T050 [US3] Migrate `NetworkModule.kt` to `shared/src/commonMain/kotlin/.../di/NetworkModule.kt` — replace OkHttpClient/Retrofit Koin bindings with Ktor HttpClient. Use `createHttpEngine()` for platform engine selection

### Outside Network Layer (6 files)

- [ ] T051 [US3] Migrate `ModelFetcher.kt` in `shared/src/commonMain/kotlin/.../assistant/llm/ModelFetcher.kt` — replace OkHttp `Call`/`Callback`/`Request`/`Response` with Ktor `client.get()`. Keep async callback pattern or convert to suspend
- [ ] T052 [US3] Migrate `AuthRepository.kt` to `shared/src/commonMain/kotlin/.../data/AuthRepository.kt` — replace private `OkHttpClient` + `Retrofit.Builder` with Ktor `HttpClient`
- [ ] T053 [P] [US3] Update `AssistantModule.kt` in `shared/src/commonMain/kotlin/.../di/AssistantModule.kt` — replace `OkHttpClient`/`HttpLoggingInterceptor` Koin bindings with Ktor `HttpClient`
- [ ] T054 [P] [US3] Update `SettingsViewModel.kt` — replace `OkHttpClient` injection with Ktor `HttpClient` injection
- [ ] T055 [US3] Move and migrate `AppError.kt` from `app/src/main/kotlin/.../model/AppError.kt` to `shared/src/commonMain/kotlin/.../model/AppError.kt` — this file was deferred from Phase 2/T009 because it depends on `retrofit2.HttpException` (now replaced by Ktor `ResponseException`/`ClientRequestException`) and Compose Icons (move icon mapping to a UI-layer extension in `composeApp/commonMain/`)
- [ ] T056 [US3] Migrate `EdaAuditViewModel.kt` — replace `retrofit2.HttpException` error handling with Ktor exception types

### Cleanup

- [ ] T057 [US3] Remove Retrofit and OkHttp dependencies from `gradle/libs.versions.toml` and all `build.gradle.kts` files. Remove old network files from `:app` module
- [ ] T058 [US3] Verify all API calls on Android: authentication, template listing, job launch, job status, workflow approvals, EDA, infrastructure, model fetching, MCP SSE. Run `./gradlew testDebugUnitTest`

**Checkpoint**: All 14 Retrofit/OkHttp files replaced with Ktor. Network layer fully in `shared/commonMain/`. Android app works identically.

---

## Phase 5: User Story 4 — Tink → cryptography-kotlin (Priority: P4)

**Goal**: Replace Tink encryption with cryptography-kotlin AES-256-GCM. Implement secure key storage for Android. Write transparent migration for existing encrypted credentials.

**Independent Test**: New credential saves use cryptography-kotlin. Existing Tink-encrypted credentials are migrated on first launch. Backup export/import works.

**Skills to load before starting this phase** (read each file into context):
```
Read skills/kotlin-platform-kmp-bridges/SKILL.md
Read skills/kotlin-kmp-refactor-safety/SKILL.md
```
Also fetch cryptography-kotlin docs:
```
Use Context7 MCP tool: resolve-library-id for "cryptography-kotlin", then query-docs for "AES-GCM encryption decryption"
```

- [ ] T059 [US4] Add `cryptography-kotlin` dependencies to `gradle/libs.versions.toml` and `shared/build.gradle.kts` — `cryptography-core` in commonMain, `cryptography-provider-jdk` in androidMain/jvmMain, `cryptography-provider-apple` in iosMain (stub for now)
- [ ] T060 [US4] Create `CryptoManager.kt` in `shared/src/commonMain/kotlin/.../data/crypto/CryptoManager.kt` — AES-256-GCM encrypt/decrypt using cryptography-kotlin. Methods: `suspend fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray`, `suspend fun decrypt(ciphertext: ByteArray, key: ByteArray): ByteArray`
- [ ] T061 [US4] Update `actual class SecureKeyStorage` in `shared/src/androidMain/kotlin/.../platform/SecureKeyStorage.kt` — replace Tink `AndroidKeysetManager` with cryptography-kotlin key generation + Android Keystore for key protection
- [ ] T062 [US4] Refactor `TokenManager.kt` in `shared/src/commonMain/kotlin/.../data/TokenManager.kt` — use `CryptoManager` + `SecureKeyStorage` instead of Tink AEAD. Same encrypt → Base64 → DataStore flow
- [ ] T063 [US4] Create `TinkMigration.kt` in `shared/src/androidMain/kotlin/.../platform/TinkMigration.kt` — one-time migration: detect Tink keyset in SharedPreferences → decrypt with Tink AEAD → re-encrypt with cryptography-kotlin → store new key → delete old Tink keyset. Must handle: missing keyset (fresh install), corrupted keyset (log error, prompt re-entry), successful migration
- [ ] T064 [US4] Migrate `BackupManager.kt` crypto in `shared/src/commonMain/kotlin/.../data/BackupManager.kt` — replace `javax.crypto` PBKDF2 + AES-GCM with cryptography-kotlin equivalents for cross-platform consistency
- [ ] T065 [US4] Wire `TinkMigration` into app startup in `composeApp/src/androidMain/kotlin/.../AnsibleJaneApp.kt` or `MainActivity.kt` — run migration before first DataStore access
- [ ] T066 [US4] Remove Tink dependencies from `gradle/libs.versions.toml` and `build.gradle.kts`. Note: keep Tink temporarily in androidMain for `TinkMigration.kt` only (can remove after one release cycle)
- [ ] T067 [US4] Verify on Android: save new credentials → close → reopen → credentials decrypted. Simulate upgrade: pre-populate Tink-encrypted data → run migration → verify credentials accessible. Test backup export/import

**Checkpoint**: Encryption fully migrated to cryptography-kotlin. Tink migration works. Android app handles both fresh install and upgrade scenarios.

---

## Phase 6: User Story 5 — Compose Multiplatform UI (Priority: P5)

**Goal**: Migrate 60 Compose screen files to CMP, replace Jetpack Navigation with Navigation 3, replace 56 `collectAsStateWithLifecycle`, migrate 33 ViewModels to CMP lifecycle. Move all UI to `composeApp/commonMain/`.

**Independent Test**: All screens render correctly on Android from `composeApp/commonMain/`. Navigation works. State collection works. No visual regressions.

**Skills to load before starting this phase** (read each file into context):
```
Read skills/compose-expert/SKILL.md
Read skills/kotlin-ui-compose-multiplatform/SKILL.md
Read skills/kotlin-project-state-management/SKILL.md
Read skills/android-official/navigation-3.md
Read skills/compose-expert/references/navigation-migration.md
Read skills/android-official/adaptive/SKILL.md
Read skills/kotlin-ui-adaptive-resources/SKILL.md
```

### Import Migration

- [ ] T068 [P] [US5] Batch-migrate Compose imports in UI files (60 files): replace `androidx.compose.*` with Compose Multiplatform equivalents. Move files from `app/src/main/kotlin/.../ui/` to `composeApp/src/commonMain/kotlin/.../ui/`. Most imports are identical (`org.jetbrains.compose.*` or same `androidx.compose.*` re-exported by CMP)
- [ ] T069 [P] [US5] Batch-migrate ViewModel imports in presentation files (33 files): replace `androidx.lifecycle.ViewModel` with CMP `ViewModel` (from `org.jetbrains.lifecycle`). Move from `app/src/main/kotlin/.../presentation/` to `composeApp/src/commonMain/kotlin/.../presentation/`

### collectAsStateWithLifecycle → collectAsState

- [ ] T070 [US5] Replace all 56 `collectAsStateWithLifecycle()` calls with `collectAsState()` across 20 files. Files affected (check each): `DashboardScreen.kt`, `TemplatesScreen.kt`, `ActivityScreen.kt`, `InfrastructureScreen.kt`, `EdaScreen.kt`, `ChatScreen.kt`, `SettingsScreen.kt`, `GeneralTab.kt`, `InstancesTab.kt`, `AgentTab.kt`, `ToolsTab.kt`, `AuthScreen.kt`, `ApprovalDetailScreen.kt`, `WorkflowJobStatusScreen.kt`, `JobStatusScreen.kt`, `HostsScreen.kt`, `SchedulesScreen.kt`, `EdaAuditScreen.kt`, `BackupRestoreSection.kt`, `McpSettingsSection.kt`. Note: `collectAsState()` doesn't stop when backgrounded — add TODO comments for expensive flows (polling) that may need platform-specific lifecycle handling later

### Navigation 3 Migration

- [ ] T071 [US5] Rewrite `AppNavigation.kt` in `composeApp/src/commonMain/kotlin/.../navigation/AppNavigation.kt` — replace `NavHost`/`NavController`/`composable()` with Navigation 3 `NavDisplay`/`BackStack` pattern. Define all destinations as data classes. Keep `testTagsAsResourceId = true` for UI automation
- [ ] T072 [US5] Update all screen composables to receive Navigation 3 typed route parameters instead of NavController arguments. Update navigation calls from `navController.navigate()` to back stack manipulation
- [ ] T073 [US5] Verify all navigation flows: Dashboard ↔ Templates ↔ Activity ↔ Infrastructure ↔ EDA ↔ Chat. Settings flow (tabbed: General/Instances/Agent/Tools). Job launch → job status. Workflow → workflow status. Template → approval detail

### Android-Specific UI Fixes

- [ ] T074 [US5] Fix `AuthScreen.kt` in `composeApp/src/commonMain/kotlin/.../ui/AuthScreen.kt` — replace `android.widget.Toast` with Compose `Snackbar` or call `PlatformUtils.showToast()`
- [ ] T075 [US5] Fix `BackupRestoreSection.kt` in `composeApp/src/commonMain/kotlin/.../ui/settings/BackupRestoreSection.kt` — replace `Context`/`Uri`/`Toast` with `expect/actual` file picker and `PlatformUtils`
- [ ] T076 [US5] Fix `GeneralTab.kt` in `composeApp/src/commonMain/kotlin/.../ui/settings/GeneralTab.kt` — replace `Intent`/`Uri` (open browser) with `PlatformUtils.openUrl()`
- [ ] T077 [US5] Keep `MainActivity.kt` in `composeApp/src/androidMain/kotlin/.../MainActivity.kt` — Android entry point with `enableEdgeToEdge()`, Intent handling, deep links. Calls into shared `App()` composable

### DI Updates

- [ ] T078 [US5] Update Koin DI modules for CMP ViewModel injection — replace `viewModel {}` (koin-android) with CMP-compatible ViewModel factory. Update `composeApp/build.gradle.kts` with correct koin-compose dependencies

### Dependency Cleanup (old Android-only libraries)

- [ ] T078a [US5] Remove Compose BOM (`2026.05.01`) from `gradle/libs.versions.toml` and `build.gradle.kts` — replaced by Compose Multiplatform 1.11.1. Old BOM may conflict with CMP-provided Compose artifacts
- [ ] T078b [P] [US5] Remove `androidx.navigation:navigation-compose` (2.9.8) from version catalog and build files — replaced by Navigation 3 (CMP-native)
- [ ] T078c [P] [US5] Remove `androidx.lifecycle:lifecycle-*` Android-only artifacts from version catalog — replaced by CMP lifecycle equivalents. Keep only if still needed in `composeApp/androidMain/` for Activity-specific lifecycle
- [ ] T078d [P] [US5] Replace `koin-android`/`koin-androidx-compose` with `koin-core`/`koin-compose` in `shared/` and `composeApp/`. Keep `koin-android` only in `composeApp/androidMain/` for `androidContext()` bootstrap
- [ ] T078e [P] [US5] Replace `kotlinx-coroutines-android` with `kotlinx-coroutines-core` in `shared/`. Keep `coroutines-android` only in `composeApp/androidMain/` for `Dispatchers.Main` (Android requires the Android dispatcher artifact)

**Reference**: Consult `tmp/Kai/` (Kai reference project) for CMP module structure, Koin setup, and Navigation 3 patterns in a production KMP app.

### Validation

- [ ] T079 [US5] Build and run Android app: `./gradlew :composeApp:assembleDebug`. Verify all screens render, navigation works, state collection works. Test on emulator
- [ ] T080 [US5] Run unit tests: `./gradlew testDebugUnitTest`. Fix any failures from import/lifecycle changes

**Checkpoint**: All 60 screens in `composeApp/commonMain/`. Navigation 3 working. All ViewModels use CMP lifecycle. Android app works identically.

---

## Phase 7: User Story 6 — Desktop Target (Priority: P6)

**Goal**: Add Desktop (JVM) entry point. Implement all Desktop `actual` classes. Package as runnable application.

**Independent Test**: Desktop app launches, connects to AAP, browses templates, launches jobs, monitors status, chats with Jane.

**Skills to load before starting this phase** (read each file into context):
```
Read skills/kotlin-platform-kmp-bridges/SKILL.md
Read skills/compose-expert/references/platform-specifics.md
Read skills/kotlin-testing-kmp/SKILL.md
```

### Desktop Entry Point

- [ ] T081 [US6] Create `Main.kt` in `composeApp/src/jvmMain/kotlin/.../Main.kt` — desktop entry point with `application { Window(title = "Ansible Jane") { App() } }`. Configure window size, title, icon

### Desktop `actual` Implementations (fill stubs from Phase 3)

- [ ] T082 [US6] Implement `actual class SecureKeyStorage` in `shared/src/jvmMain/kotlin/.../platform/SecureKeyStorage.kt` — Java KeyStore (`java.security.KeyStore`, PKCS12 type). Store AES key wrapped in JKS file in user home dir (`~/.ansiblejane/keystore.p12`)
- [ ] T083 [P] [US6] Implement `actual class DataStoreStorageFactory` in `shared/src/jvmMain/kotlin/.../platform/DataStoreStorageFactory.kt` — `FileStorage` using `java.io.File` in `~/.ansiblejane/datastore/`
- [ ] T084 [P] [US6] Implement `actual class ConnectivityObserver` in `shared/src/jvmMain/kotlin/.../platform/ConnectivityObserver.kt` — polling with `Socket` connect to DNS (8.8.8.8:53) every 30s, emit via `StateFlow`
- [ ] T085 [P] [US6] Implement `actual class BackgroundWorker` in `shared/src/jvmMain/kotlin/.../platform/BackgroundWorker.kt` — `ScheduledExecutorService` with configurable interval for approval polling
- [ ] T086 [P] [US6] Implement `actual class PlatformUtils` in `shared/src/jvmMain/kotlin/.../platform/PlatformUtils.kt` — `Desktop.getDesktop().browse()` for URLs, `println`/logging for toast (or CMP Snackbar), manifest version for app version

### Desktop Networking

- [ ] T087 [US6] Verify Ktor CIO engine works for Desktop — test with AAP instance. Verify self-signed cert support via `TlsTrustManager` (already using `javax.net.ssl`, portable from Android)

### Verify koog-google on Desktop

- [ ] T087a [US6] Verify `koog-google` (Gemini provider, v1.0.0-beta-preview7) works on JVM Desktop target — test Gemini API calls from Desktop app. If it fails on non-Android JVM, create a platform-specific `actual` wrapper or document as known limitation

### Packaging

- [ ] T088 [US6] Configure `composeApp/build.gradle.kts` for Desktop distribution — add `compose.desktop { application { mainClass = "...MainKt"; nativeDistributions { targetFormats(TargetFormat.Dmg, TargetFormat.Deb, TargetFormat.Rpm) } } }`
- [ ] T089 [US6] Run desktop app: `./gradlew :composeApp:run`. Verify: login screen → connect to AAP → dashboard → browse templates → launch job → monitor → chat with Jane

**Checkpoint**: Desktop app runs as standalone JVM application. Core features work. Packaging configured for distribution.

---

## Phase 8: User Story 7 — iOS Target (Priority: P7)

**Goal**: Add iOS target with Kotlin/Native. Implement iOS `actual` classes. Create Xcode project wrapper. Min iOS 15.

**Independent Test**: iOS app builds on macOS, runs on simulator, connects to AAP.

**⚠️ Requires macOS + Xcode**: This phase cannot be developed or tested from Linux.

**Skills to load before starting this phase** (read each file into context):
```
Read skills/kotlin-platform-kmp-bridges/SKILL.md
Read skills/compose-expert/references/platform-specifics.md
Read skills/kotlin-testing-kmp/SKILL.md
```

### iOS Source Set

- [ ] T090 [US7] Create `shared/src/iosMain/kotlin/` source set — configure in `shared/build.gradle.kts` with `iosArm64()` and `iosSimulatorArm64()` targets. Add `cryptography-provider-apple` dependency

### iOS `actual` Implementations

- [ ] T091 [US7] Implement `actual class SecureKeyStorage` in `shared/src/iosMain/kotlin/.../platform/SecureKeyStorage.kt` — iOS Keychain Services via `Security` framework
- [ ] T092 [P] [US7] Implement `actual class DataStoreStorageFactory` in `shared/src/iosMain/kotlin/.../platform/DataStoreStorageFactory.kt` — `OkioStorage` with NSDocumentDirectory path
- [ ] T093 [P] [US7] Implement `actual class TlsTrustManager` in `shared/src/iosMain/kotlin/.../platform/TlsTrustManager.kt` — Ktor Darwin engine SSL configuration
- [ ] T094 [P] [US7] Implement `actual class ConnectivityObserver` in `shared/src/iosMain/kotlin/.../platform/ConnectivityObserver.kt` — `NWPathMonitor` from Network framework
- [ ] T095 [P] [US7] Implement `actual class BackgroundWorker` in `shared/src/iosMain/kotlin/.../platform/BackgroundWorker.kt` — `BGTaskScheduler` for background refresh
- [ ] T096 [P] [US7] Implement `actual class PlatformUtils` in `shared/src/iosMain/kotlin/.../platform/PlatformUtils.kt` — `UIApplication.shared.open()` for URLs, `UIApplication` info dict for version

### iOS Entry Point

- [ ] T097 [US7] Create `MainViewController.kt` in `composeApp/src/iosMain/kotlin/.../MainViewController.kt` — CMP `ComposeUIViewController { App() }`
- [ ] T098 [US7] Configure Ktor Darwin engine for iOS in `shared/src/iosMain/kotlin/.../network/HttpEngine.kt`

### Xcode Project

- [ ] T099 [US7] Create `iosApp/` Xcode project wrapper — Swift entry point calling `MainViewController`. Configure: bundle ID, min iOS 15, app icons, launch screen
- [ ] T100 [US7] Build and run on iOS simulator: `./gradlew :shared:linkDebugFrameworkIosSimulatorArm64` → open Xcode → run on simulator. Verify: login → connect → dashboard → templates → chat

**Checkpoint**: iOS app builds on macOS. Runs on simulator with core features working.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Improvements affecting all targets.

**Skills to load before starting this phase** (read each file into context):
```
Read skills/kotlin-project-architecture-review/SKILL.md
Read skills/kotlin-kmp-code-review/SKILL.md
Read skills/kotlin-testing-kmp/SKILL.md
```

- [ ] T101 Update `scripts/test-all.sh` for multi-module — test `:shared` (commonTest, androidUnitTest, jvmTest) and `:composeApp` (androidTest, jvmTest)
- [ ] T102 [P] Update `README.md` — add multiplatform info, Desktop installation instructions, updated tech stack table
- [ ] T103 [P] Update `CLAUDE.md` — update project structure, architecture layers, file paths for new multi-module layout
- [ ] T104 [P] Update `.github/workflows/` CI/CD — add Desktop build job, update Android build for multi-module
- [ ] T105 Remove old `:app` module (if fully migrated to `:composeApp`) — delete `app/` directory, remove from `settings.gradle.kts`
- [ ] T106 Final dependency audit — verify no stale Android-only deps remain in `shared/` or `composeApp/commonMain/`. Check: no `koin-android` in shared, no `coroutines-android` in shared, no Compose BOM, no Retrofit/OkHttp, no Tink (except androidMain for migration), no `androidx.navigation:navigation-compose`, no `androidx.lifecycle` Android-only artifacts in commonMain
- [ ] T107 Performance profiling — compare build time against baseline from T000. Verify increase < 30%. Optimize Gradle config if needed (parallel builds, configuration cache, build cache)
- [ ] T108 Run full test suite on all available targets. Fix any remaining failures

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup) ──► Phase 2 (US1: Move Code) ──► Phase 3 (US2: Abstractions)
                                                         │
                                                         ▼
                                               Phase 4 (US3: Ktor) ──► Phase 5 (US4: Crypto)
                                                                              │
                                                                              ▼
                                                                     Phase 6 (US5: CMP UI)
                                                                              │
                                                                       ┌──────┴──────┐
                                                                       ▼             ▼
                                                              Phase 7 (US6)   Phase 8 (US7)
                                                              Desktop         iOS (macOS only)
                                                                       └──────┬──────┘
                                                                              ▼
                                                                     Phase 9 (Polish)
```

### Key Constraints

- **Phase 1 → Phase 2**: Must have module skeleton before moving code
- **Phase 2 → Phase 3**: Must have portable code in shared/ before creating abstractions
- **Phase 3 → Phase 4**: Must have abstractions before migrating network (Ktor needs engine expect/actual)
- **Phase 4 → Phase 5**: Must finish Ktor before crypto (TokenManager uses network layer)
- **Phase 5 → Phase 6**: Must finish crypto before UI migration (SecureKeyStorage fully implemented)
- **Phase 6 → Phase 7/8**: Must finish UI migration before platform targets (all UI in commonMain)
- **Phase 7 and Phase 8 are independent**: Desktop and iOS can proceed in parallel

### Parallel Opportunities Within Phases

**Phase 2**: T008, T009, T010 can run in parallel (moving different directories)
**Phase 3**: T018-T023 (expect declarations) can run in parallel. T024-T029 (Android actuals) can run in parallel. T030-T035 (Desktop stubs) can run in parallel
**Phase 4**: T046, T047 can run in parallel (EdaApiClient, PlatformApiClient). T053, T054 can run in parallel
**Phase 6**: T068, T069 can run in parallel (UI files vs ViewModel files). T074, T075, T076 can run in parallel
**Phase 7**: T082-T086 (Desktop actuals) can run in parallel after T081
**Phase 8**: T091-T096 (iOS actuals) can run in parallel after T090

---

## Implementation Strategy

### MVP First (Phases 1-4, ~4-6 weeks)

1. Complete Phase 1: Setup → Module skeleton compiles
2. Complete Phase 2: US1 → Portable code in `shared/commonMain/`
3. Complete Phase 3: US2 → Platform abstractions, repositories in shared
4. Complete Phase 4: US3 → Network in shared (Ktor)
5. **STOP and VALIDATE**: Android app works identically. `shared/` module compiles for JVM target. All business logic is now multiplatform.

### Quick Win: Desktop (Phase 7, ~1 extra week after Phase 6)

After completing Phases 1-6 (UI migration), Phase 7 adds Desktop in ~1 week. This is the first visible multiplatform result.

### Full Migration (all 9 phases, ~10-14 weeks)

Each phase is a complete, independently verifiable increment. Commit after each task or logical group. The Android app must pass all tests after every phase.

---

## Notes

- [P] tasks = different files, no dependencies on incomplete tasks in same phase
- [USn] label maps task to specific user story / migration phase
- All skill loads use `Read` tool on the skill file path (local SKILL.md files in `skills/` directory)
- Context7 MCP lookups (`resolve-library-id` + `query-docs`) are for fetching up-to-date library documentation
- Commit after each completed task or logical group
- Run Android build verification (`./gradlew :composeApp:assembleDebug`) at every phase checkpoint
- The old `:app` module is kept alongside `:composeApp` until Phase 9 to enable incremental migration

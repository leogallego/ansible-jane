# Implementation Plan: KMP Migration

**Branch**: `197-kmp-migration` | **Date**: 2026-06-06 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/197-kmp-migration/spec.md`, GitHub issue #230

## Summary

Restructure the Android-only Ansible Jane app into a Kotlin Multiplatform (KMP) project with Compose Multiplatform UI. The migration creates a `shared/` module for business logic and a `composeApp/` module for shared UI, enabling Android, Desktop (JVM), and iOS targets. 7 phases, ~10-14 weeks total.

## Technical Context

**Language/Version**: Kotlin 2.3.21 → **2.4.0** (upgraded in Phase 1)
**Primary Dependencies**: Compose Multiplatform 1.11.1, Ktor Client 3.5.0, cryptography-kotlin 0.6.0, Koin 4.2.1, Koog 1.0.0, Navigation 3, DataStore KMP 1.2.1
**Storage**: DataStore Preferences (KMP-native since v1.1.0), encrypted via cryptography-kotlin AES-256-GCM
**Testing**: `scripts/test-all.sh` (unit + instrumented), Gradle test tasks
**Target Platforms**: Android (API 31+), Desktop (JVM), iOS (15+, Phase 7)
**Project Type**: Mobile/Desktop multiplatform app
**Constraints**: iOS development requires macOS + Xcode; AGP 9.x mandates new KMP plugin

## Codebase Snapshot (June 2026)

| Metric | Value |
|--------|-------|
| Total Kotlin source files | 278 |
| Files with `android.*` imports | 13 (4.7%) |
| Files with `androidx.*` imports | 93 (33.5%) |
| Immediately portable (zero changes) | 138+ (49.6%) |

### Files by Layer

| Layer | Files | Portable? | Notes |
|-------|-------|-----------|-------|
| Models (`model/`) | 45 | 98% | `AppError.kt` has Compose icons + `retrofit2.HttpException` |
| Tools (`tools/`) | 67 | 100% | Pure Kotlin, zero Android imports |
| LLM Providers (`llm/`) | 4 | 100% | KMP-native Koog 1.0.0 |
| Engine (`engine/`) | 5 | 80% | `DebugLog.kt` wraps `android.util.Log` (7 consumers) |
| Network (`network/`) | 12 | 0% | Retrofit/OkHttp → Ktor (8 files) |
| Data (`data/`) | 43 | ~70% | 5 files have `android.*` imports |
| Notification (`notification/`) | 3 | 0% | Android-specific |
| Presentation (`presentation/`) | 33 | ~90% | ViewModels use `androidx.lifecycle` |
| UI (`ui/`) | 60 | 0% | All `androidx.compose.*` → CMP |
| DI modules | 5 | ~60% | `DataModule` has `androidContext()` |

### The 13 Android-Dependent Files

| File | Android APIs | Migration Path |
|------|-------------|----------------|
| `AnsibleJaneApp.kt` | `Application`, WorkManager, Koin bootstrap | `expect/actual` app init |
| `MainActivity.kt` | `ComponentActivity`, Intent, deep links | Android entry point (stays in composeApp/androidMain/) |
| `TokenManager.kt` | `Context`, DataStore, Tink | cryptography-kotlin + DataStore KMP |
| `DataStoreProvider.kt` | `Context` + DataStore factory | `expect/actual` Storage factory |
| `UserPreferencesRepository.kt` | `Context` + DataStore | DataStore KMP (commonMain) |
| `AssistantRepository.kt` | `Context` for file I/O | DataStore KMP (commonMain) |
| `ApprovalNotificationManager.kt` | 31 android.* imports | `expect/actual` notification interface |
| `ApprovalPollingWorker.kt` | `CoroutineWorker` | `expect/actual` background work |
| `ApprovalTracker.kt` | `Context` dependency | DataStore KMP (commonMain) |
| `ConnectivityObserver.kt` | `ConnectivityManager` | `expect/actual` |
| `AuthScreen.kt` | `android.widget.Toast` | CMP snackbar |
| `BackupRestoreSection.kt` | `Context`, `Uri`, `Toast` | `expect/actual` file picker |
| `GeneralTab.kt` | `Intent`, `Uri` | `expect/actual` URL opener |

## Dependency Migration Map

| Current | Version | KMP Status | Migration |
|---------|---------|------------|-----------|
| Kotlin | 2.3.21 | KMP-ready | → **2.4.0** in Phase 1 |
| AGP | 9.2.1 | New plugin needed | → `com.android.kotlin.multiplatform.library` for shared/ |
| Compose BOM | 2026.05.01 | Must replace | → Compose Multiplatform 1.11.1 |
| Retrofit | 3.0.0 | JVM-only | → Ktor Client 3.5.0 |
| OkHttp | 5.3.2 | JVM-only | → Ktor engines: OkHttp (Android), CIO (Desktop), Darwin (iOS) |
| Tink Android | 1.21.0 | Android-only | → cryptography-kotlin 0.6.0 |
| DataStore Prefs | 1.2.1 | KMP-ready | Same library, KMP mode |
| Koin | 4.2.1 | KMP-ready | Replace `koin-android` → `koin-core` in shared |
| Koog | 1.0.0 | KMP-ready | Same. `koog-google` (v1.0.0-beta-preview7) needs non-JVM verification |
| kotlinx.serialization | 1.11.0 | KMP-ready | Same |
| kotlinx.coroutines | 1.11.0 | KMP-ready | `coroutines-android` → `coroutines-core` in shared |
| Ktor Client | 3.5.0 | KMP-ready | Already used for MCP; becomes primary HTTP client |
| Navigation Compose | 2.9.8 | Must replace | → Navigation 3 |
| Lifecycle | 2.10.0 | Must replace | → CMP lifecycle |
| WorkManager | 2.11.2 | Android-only | → `expect/actual` BackgroundWorker |
| collections-immutable | 0.4.0 | KMP-ready | Same |

## Project Structure

### Source Code

```text
ansible-jane/
├── shared/                              # KMP shared module
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── model/                   # 45 data models (as-is)
│       │   ├── network/                 # Ktor client, API services
│       │   ├── data/                    # Repositories, DataStore KMP, TokenManager
│       │   ├── assistant/
│       │   │   ├── engine/              # ChatEngine, ToolRouter, ToolExecutor, DebugLog
│       │   │   ├── tools/              # 67 tool implementations (as-is)
│       │   │   ├── llm/                # LLM providers (as-is)
│       │   │   └── data/              # AssistantRepository
│       │   ├── notification/           # Approval tracking (DataStore, no platform notifications)
│       │   └── di/                     # Common Koin modules
│       ├── commonMain/kotlin/
│       │   └── platform/
│       │       └── AppInitializer.kt   # expect fun initializeApp()
│       ├── androidMain/kotlin/
│       │   ├── SecureKeyStorage.kt
│       │   ├── DataStoreStorageFactory.kt
│       │   ├── TlsTrustManager.kt
│       │   ├── ConnectivityObserver.kt
│       │   ├── BackgroundWorker.kt
│       │   ├── PlatformUtils.kt
│       │   ├── NotificationManager.kt  # Android notifications (31 imports)
│       │   └── TinkMigration.kt        # One-time Tink → cryptography-kotlin
│       ├── jvmMain/kotlin/             # Desktop
│       │   ├── SecureKeyStorage.kt
│       │   ├── DataStoreStorageFactory.kt
│       │   ├── TlsTrustManager.kt
│       │   ├── ConnectivityObserver.kt
│       │   ├── BackgroundWorker.kt
│       │   ├── PlatformUtils.kt
│       │   └── NotificationManager.kt  # No-op
│       └── iosMain/kotlin/             # iOS (Phase 7)
│           ├── SecureKeyStorage.kt
│           ├── DataStoreStorageFactory.kt
│           ├── TlsTrustManager.kt
│           ├── ConnectivityObserver.kt
│           ├── BackgroundWorker.kt
│           ├── PlatformUtils.kt
│           └── NotificationManager.kt  # No-op
├── composeApp/                          # Compose Multiplatform UI
│   └── src/
│       ├── commonMain/kotlin/
│       │   ├── ui/                     # 60 screen files (migrated to CMP)
│       │   ├── presentation/           # 33 ViewModels (CMP lifecycle)
│       │   └── navigation/            # Navigation 3 graph
│       ├── androidMain/kotlin/
│       │   └── MainActivity.kt
│       ├── jvmMain/kotlin/
│       │   └── Main.kt                # Desktop entry point
│       └── iosMain/kotlin/
│           └── MainViewController.kt
├── iosApp/                              # Xcode project wrapper (Phase 7)
├── gradle/libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts                 # :shared + :composeApp
```

## `expect/actual` Contracts

7 `expect class` contracts + 1 lightweight `expect fun`:

| Contract | Methods | Android | Desktop (JVM) | iOS |
|----------|---------|---------|---------------|-----|
| `SecureKeyStorage` | `storeKey`, `loadKey`, `deleteKey` | Android Keystore | Java KeyStore | Keychain |
| `DataStoreStorageFactory` | `createStorage(name)` | `FileStorage` (Context) | `FileStorage` (java.io) | `OkioStorage` |
| `TlsTrustManager` | `createTrustManager(fingerprint?)` | `javax.net.ssl` | Same (JCA) | Ktor Darwin SSL |
| `ConnectivityObserver` | `observe(): Flow<Boolean>` | `ConnectivityManager` | Socket polling | `NWPathMonitor` |
| `BackgroundWorker` | `schedulePolling`, `cancelPolling` | WorkManager | `ScheduledExecutorService` | BGTaskScheduler |
| `PlatformUtils` | `openUrl`, `showToast`, `getAppVersion` | Intent/Toast/BuildConfig | Desktop.browse/Snackbar | UIApplication |
| `NotificationManager` | `showApprovalNotification`, `createChannel` | Android NotificationManager (31 imports) | No-op | No-op |

Additionally, `expect fun initializeApp()` via `AppInitializer` handles cross-platform Koin startup (Android needs `androidContext()`, others don't).

Notes:
- `TlsTrustManager` uses `javax.net.ssl.*` — portable between Android and JVM Desktop. Only needs iOS-specific `actual`.
- `NotificationManager` is Android-only for now — desktop/iOS notifications can be added later.
- `enableEdgeToEdge()` stays in `composeApp/androidMain/` (Android-only, not an `expect/actual`).

## Encryption Migration: Tink → cryptography-kotlin

### Current flow (Tink)
1. `AeadConfig.register()` + `AndroidKeysetManager` → AES-256-GCM keyset
2. Keyset stored in SharedPreferences, protected by Android Keystore
3. `aead.encrypt/decrypt` → Base64 → DataStore

### New flow (cryptography-kotlin)
1. `cryptography-kotlin` generates AES-256-GCM key material → `commonMain`
2. Key stored via `expect/actual SecureKeyStorage` → platform-specific
3. Encrypt/decrypt via `cryptography-kotlin` AES-GCM → `commonMain`
4. Values stored in DataStore KMP → `commonMain`

### Data migration for existing Android users
1. On first launch after update: detect Tink keyset in SharedPreferences
2. Decrypt all credentials with Tink
3. Re-encrypt with cryptography-kotlin, store new key in Android Keystore
4. Delete old Tink keyset
5. One-time, transparent to user

## Network Migration: Retrofit → Ktor

### 14 files requiring migration

**Network layer (8 files):**
- `AapApiService.kt` → Ktor extension functions
- `EdaApiService.kt` → Ktor extension functions
- `PlatformApiService.kt` → Ktor extension functions
- `AapApiProvider.kt` → `HttpClient` factory with Ktor engine
- `AuthInterceptor.kt` → Ktor `HttpRequestInterceptor` plugin
- `ApiVersionDetector.kt` → Ktor `client.get()`
- `InstanceDiscovery.kt` → Ktor `client.get()`
- `NetworkModule.kt` → `HttpClient` for Koin DI

**Outside network layer (6 files):**
- `ModelFetcher.kt` → Ktor `client.get()`
- `AuthRepository.kt` → Ktor `HttpClient`
- `AssistantModule.kt` → Ktor DI
- `SettingsViewModel.kt` → Ktor injection
- `AppError.kt` → Ktor `ResponseException`
- `EdaAuditViewModel.kt` → Ktor exception type

### Ktor engine strategy

| Platform | Engine | Why |
|----------|--------|-----|
| Android | OkHttp | HTTP/2, system integration, connection pooling |
| Desktop (JVM) | CIO | Pure Kotlin, no native deps |
| iOS | Darwin | Apple system networking, App Transport Security |

## DataStore Inventory

| DataStore | Name | Content | Encrypted? |
|-----------|------|---------|-----------|
| `credentialsDataStore` | `"credentials"` | AAP URLs + tokens, LLM keys, tool manifests | Yes — Tink → cryptography-kotlin |
| `userPreferencesDataStore` | `"user_preferences"` | Theme, timezone, time format | No |
| `assistantDataStore` | `"assistant_config"` | LLM configs, active provider, disabled tools | No |
| `approvalTrackerDataStore` | `"approval_tracker"` | Seen approval IDs | No |

## Migration Phases

### Phase 1 — Kotlin Upgrade + Project Structure (1-2 weeks)
**Step 1:** Upgrade Kotlin 2.3.21 → 2.4.0. Fix any breaking changes (K1 removed — already on K2, low risk).
**Step 2:** Create KMP Gradle project with `shared/` module (using `com.android.kotlin.multiplatform.library` plugin) and `composeApp/` module. Move 138+ portable files to `shared/commonMain/`. Android app builds from `composeApp/androidMain/`.

### Phase 2 — Platform Abstractions (1 week)
Create 7 `expect/actual` contracts + 1 `expect fun`. Android `actual` wraps existing code. Stub Desktop/iOS. Move DataStore, repositories, DI to `shared/`.

### Phase 3 — Retrofit → Ktor (1-2 weeks)
Replace 14 files. Network layer + outside network. Verify all API calls on Android.

### Phase 4 — Tink → cryptography-kotlin (3-5 days)
Replace Tink in `TokenManager`. Implement `SecureKeyStorage` for Android. Write Tink migration. Migrate BackupManager crypto.

### Phase 5 — Compose Multiplatform UI (3-4 weeks)
Migrate 60 screens from `androidx.compose` → CMP. Replace Navigation → Navigation 3. Replace 56 `collectAsStateWithLifecycle`. Migrate 33 ViewModels. Move UI to `composeApp/commonMain/`.

### Phase 6 — Desktop Target (1 week)
Add `jvmMain` entry point. Implement Desktop `actual` classes. Package as JAR/native.

### Phase 7 — iOS Target (2-3 weeks, requires macOS)
Add `iosMain` with Kotlin/Native. Implement iOS `actual` classes. Xcode project wrapper. Min iOS 15.

## Effort Estimate

| Phase | Duration | From Linux? |
|-------|----------|-------------|
| 1. Kotlin Upgrade + Project Structure | 1-2 weeks | Yes |
| 2. Platform Abstractions | 1 week | Yes |
| 3. Retrofit → Ktor | 1-2 weeks | Yes |
| 4. Tink → cryptography-kotlin | 3-5 days | Yes |
| 5. Compose Multiplatform UI | 3-4 weeks | Yes |
| 6. Desktop Target | 1 week | Yes |
| 7. iOS Target | 2-3 weeks | No (macOS) |
| **Total** | **~10-14 weeks** | **Phases 1-6: ~8-11 weeks** |

## Skills per Phase

All skills are local SKILL.md files in `skills/`. Read the relevant skill files into context before starting each phase.

### Phase 1 — Kotlin Upgrade + Project Structure
```
Read skills/kotlin-build-kmp-gradle-governance/SKILL.md
Read skills/kotlin-project-modularization/SKILL.md
Read skills/kotlin-kmp-abstraction-decision/SKILL.md
Read skills/kotlin-platform-kmp-bridges/SKILL.md
```

### Phase 2 — Platform Abstractions
```
Read skills/kotlin-multiplatform-expect-actual/SKILL.md
Read skills/kotlin-platform-kmp-bridges/SKILL.md
Read skills/kotlin-kmp-abstraction-decision/SKILL.md
Read skills/kotlin-data-kmp-data-layer/SKILL.md
```

### Phase 3 — Retrofit → Ktor
```
Read skills/kotlin-data-kmp-data-layer/SKILL.md
Read skills/kotlin-project-feature-implementation/SKILL.md
Read skills/android-community/retrofit-networking.md
```

### Phase 4 — Tink → cryptography-kotlin
```
Read skills/kotlin-platform-kmp-bridges/SKILL.md
Read skills/kotlin-kmp-refactor-safety/SKILL.md
```

### Phase 5 — Compose Multiplatform UI
```
Read skills/compose-expert/SKILL.md
Read skills/kotlin-ui-compose-multiplatform/SKILL.md
Read skills/kotlin-project-state-management/SKILL.md
Read skills/android-official/navigation-3.md
Read skills/compose-expert/references/navigation-migration.md
Read skills/android-official/adaptive/SKILL.md
Read skills/kotlin-ui-adaptive-resources/SKILL.md
```

### Phase 6 — Desktop Target
```
Read skills/kotlin-platform-kmp-bridges/SKILL.md
Read skills/compose-expert/references/platform-specifics.md
Read skills/kotlin-testing-kmp/SKILL.md
```

### Phase 7 — iOS Target
```
Read skills/kotlin-platform-kmp-bridges/SKILL.md
Read skills/compose-expert/references/platform-specifics.md
Read skills/kotlin-testing-kmp/SKILL.md
```

### Cross-phase (load for any review or bugfix)
```
Read skills/kotlin-project-architecture-review/SKILL.md
Read skills/kotlin-kmp-code-review/SKILL.md
Read skills/kotlin-project-bugfix/SKILL.md
```

## Risks

1. **AGP 9.x plugin migration** — Must use `com.android.kotlin.multiplatform.library`. Legacy removed in AGP 10 (late 2026).
2. **Tink data migration** — Existing users have Tink-encrypted credentials. Must be lossless.
3. **Compose Multiplatform maturity** — CMP 1.11.1 stable for Android/Desktop/iOS. Minor: iOS accessibility not at SwiftUI parity.
4. **Desktop TLS trust** — `javax.net.ssl` portable between Android and JVM Desktop. iOS needs Ktor Darwin SSL config.
5. **iOS requires macOS** — Phase 7 deferred.
6. **cryptography-kotlin maturity** — v0.6.0 with 642 stars. Uses platform-native backends (JCA, CryptoKit, OpenSSL).
7. **`collectAsStateWithLifecycle` behavioral change** — 56 usages. `collectAsState` doesn't stop when backgrounded.
8. **`koog-google` KMP support** — v1.0.0-beta-preview7 may need non-JVM verification.
9. **Kotlin 2.4.0 breaking changes** — K1 removal (on K2, no impact). Apple targets raised to iOS 15.

## Impact on Open Issues

### Would need rework if done BEFORE KMP (9 issues)
- #217, #173, #214, #96, #95, #2, #34, #35, #17

### Transparent — survive KMP untouched (10 issues)
- #171, #159, #104, #141, #139, #136, #120, #134, #30

### Partially impacted (2 issues)
- #191, #116

**After Phase 2, all new logic/tool features go into `shared/` and are automatically multiplatform.**

## References

- [AGP 9 KMP migration](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html)
- [KMP compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- [AGP KMP plugin setup](https://developer.android.com/kotlin/multiplatform/plugin)
- [cryptography-kotlin](https://github.com/whyoleg/cryptography-kotlin)
- [DataStore KMP](https://developer.android.com/kotlin/multiplatform/datastore)
- [Compose Multiplatform 1.11.0](https://blog.jetbrains.com/kotlin/2026/05/compose-multiplatform-1-11-0/)
- [Navigation 3 in CMP](https://kotlinlang.org/docs/multiplatform/compose-navigation-3.html)
- [Ktor Client engines](https://ktor.io/docs/client-engines.html)
- [Koog 1.0.0](https://blog.jetbrains.com/ai/2026/05/koog-1-0-is-out-stable-core-better-interop-and-multiplatform-observability/)
- [Koin KMP](https://insert-koin.io/docs/reference/koin-mp/kmp/)
- [Kotlin 2.4.0](https://blog.jetbrains.com/kotlin/2026/06/kotlin-2-4-0-released/)
- Kai reference project: `tmp/Kai/`
- Previous analysis: #37, #18, #120

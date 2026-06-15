# Spec: Port AI Assistant to KMP for Desktop (#243)

**Issue:** [#243](https://github.com/leogallego/ansible-jane/issues/243)
**Related:** #230 (KMP parent), #233 (KMP PR), #120 (ToolRouter KMP), #30 (Local LLM), #296 (ToolRouter cleanup)
**Status:** Draft spec — ready for review

---

## 1. Goal

Move the AI assistant engine, tools, LLM providers, and UI from `app/` (Android-only) to `shared/commonMain` + `composeApp/commonMain` so the Desktop build gets a working AI assistant. Android continues to work exactly as before.

## 2. Current State

### File Inventory (80 files in `app/.../assistant/`)

| Layer | Files | LOC | JVM/Android deps | Target |
|-------|-------|-----|-------------------|--------|
| engine/ | ChatEngine, ToolExecutor, ToolRouter | ~1,200 | IOException, System.currentTimeMillis, AtomicBoolean, @Synchronized, synchronized(), @VisibleForTesting | `shared/commonMain` |
| llm/ | LlmProvider, GeminiLlmProvider, KoogLlmProvider | ~450 | java.io.Closeable, java.net.URI, java.net.SocketTimeoutException, System.currentTimeMillis | `shared/commonMain` |
| tools/ | ToolSpec, LocalTool, McpTool, CachedMcpTool, AapLocalTool, ToolDescriptorMapping | ~350 | IOException, SocketTimeoutException (McpTool only) | `shared/commonMain` |
| tools/local/ | 60 local tools | ~3,000 | **None** (all pure Kotlin + Ktor) | `shared/commonMain` |
| DI | AssistantModule, LocalToolsModule | ~130 | Koin `viewModel {}` scope | `shared/commonMain` (split) |
| presentation/ | AssistantViewModel, AssistantUiState | ~480 | ViewModel, viewModelScope, @Immutable | `composeApp/commonMain` |
| ui/ | AssistantScreen, ChatBubble, ConfirmationCard | ~880 | collectAsStateWithLifecycle, rememberSaveable, WindowInsets.isImeVisible, PreviewLightDark, koinViewModel | `composeApp/commonMain` |

**Key finding:** 75 of 80 files have zero Android imports. Only 5 files need adaptation.

### Dependencies Already KMP-Compatible

| Library | In shared/build.gradle.kts? | KMP? |
|---------|----------------------------|------|
| Koog (prompt-executor-openai-client) | Yes | Yes — publishes jvm, ios, iossimulator variants |
| Koog (prompt-executor-google-client) | Yes | Yes |
| Koog (http-client-ktor) | Yes | Yes |
| MCP SDK (io.modelcontextprotocol.kotlin.sdk) | Yes | Yes |
| Ktor Client | Yes | Yes |
| kotlinx.serialization | Yes | Yes |
| kotlinx.coroutines | Yes | Yes |
| kotlinx.collections.immutable | Yes | Yes |
| multiplatform-markdown-renderer | composeApp | Yes — KMP library by mikepenz |
| CMP Lifecycle ViewModel | composeApp | Yes — `libs.cmp-lifecycle-viewmodel` |

### What's NOT KMP-Compatible (must replace)

| JVM/Android API | Files | Replacement |
|-----------------|-------|-------------|
| `java.util.concurrent.atomic.AtomicBoolean` | ToolRouter | `kotlinx.atomicfu.atomic(false)` |
| `@Synchronized` (10 methods) | ToolRouter | `kotlinx.coroutines.sync.Mutex` with `withLock {}` |
| `synchronized(this) {}` block | ToolRouter.toggleToolEnabled | `Mutex.withLock {}` |
| `@VisibleForTesting` | ToolRouter.setToolEnabled | Remove — use `internal` visibility |
| `java.io.Closeable` | LlmProvider interface | `AutoCloseable` (Kotlin stdlib since 1.8) — compile-verified |
| `java.io.IOException` | ChatEngine, McpTool | `kotlinx.io.IOException` (KMP, transitive via Ktor) — compile-verified |
| `java.net.SocketTimeoutException` | GeminiLlmProvider, KoogLlmProvider, McpTool | `io.ktor.client.network.sockets.SocketTimeoutException` (Ktor, already a dep) — compile-verified |
| `java.net.URI` | KoogLlmProvider | `io.ktor.http.Url()` (Ktor, already a dep) — compile-verified |
| `System.currentTimeMillis()` | ToolExecutor, GeminiLlmProvider, KoogLlmProvider, McpServerManager | `kotlin.time.Clock.System.now().toEpochMilliseconds()` (Kotlin 2.4.0 stdlib) — compile-verified |
| `java.util.concurrent.ConcurrentHashMap` | McpServerManager | `mutableMapOf()` guarded by `SynchronizedObject` (same lock as `synchronized()` replacement) — compile-verified |
| `androidx.lifecycle.ViewModel` | AssistantViewModel | Already available via `libs.cmp-lifecycle-viewmodel` in composeApp |
| `viewModelScope` | AssistantViewModel | Same CMP lifecycle library |
| `@Immutable` | AssistantUiState | `@Immutable` from `androidx.compose.runtime` — already in CMP |
| `koinViewModel` | AssistantScreen | `koinViewModel()` from `org.koin.compose.viewmodel` — already in composeApp deps |

| `WindowInsets.isImeVisible` | AssistantScreen | **Does NOT compile on desktop** (verified by compile test). Move the `isImeVisible` + `LaunchedEffect(imeVisible)` block (4 lines) behind an `expect/actual` platform check. Keep the behavior on Android (auto-scroll when soft keyboard opens). On desktop, default to `false` for now — but note that virtual/accessibility keyboards exist on desktop too, so this may need revisiting. |

**APIs that work in CMP commonMain without changes** (verified in Kai reference project, existing composeApp code, and compile test):
- `collectAsStateWithLifecycle()` — CMP lifecycle-runtime-compose already in deps (`libs.cmp.lifecycle.runtime.compose`)
- `rememberSaveable` — already used in `composeApp/commonMain` (MainScreen.kt)
- `imePadding()` — Compose Foundation is CMP; no-op on desktop (confirmed: exists in desktop JAR)
- `@PreviewLightDark` — CMP tooling-preview already in deps (`libs.compose.components.uiToolingPreview`), used in existing commonMain components
- `koinViewModel()` — Koin Compose already in deps
| Koin `viewModel {}` scope | AssistantModule | Use `viewModelOf()` like existing PresentationModule |
| `Dispatchers.IO` | AssistantViewModel.onCleared | `Dispatchers.IO` — available in coroutines JVM, but for KMP use `Dispatchers.Default` |
| `SupervisorJob()` | AssistantViewModel.onCleared | Available in KMP coroutines, no change needed |

---

## 3. Architecture After Migration

```
shared/src/commonMain/kotlin/.../assistant/
├── engine/
│   ├── ChatEngine.kt          ← from app/
│   ├── ChatMessage.kt         ← already here
│   ├── DebugLog.kt            ← already here (expect/actual)
│   ├── ToolExecutor.kt        ← from app/
│   └── ToolRouter.kt          ← from app/ (with JVM→KMP replacements)
├── llm/
│   ├── LlmProvider.kt         ← from app/ (Closeable → kotlin.io.Closeable)
│   ├── LlmTypes.kt            ← already here
│   ├── GeminiLlmProvider.kt   ← from app/ (URI, SocketTimeout fixes)
│   └── KoogLlmProvider.kt     ← from app/ (URI, SocketTimeout, Closeable fixes)
├── tools/
│   ├── ToolSpec.kt             ← from app/ (already pure Kotlin)
│   ├── ToolSource.kt           ← already here
│   ├── LocalTool.kt            ← from app/
│   ├── McpTool.kt              ← from app/ (IOException fix)
│   ├── CachedMcpTool.kt        ← from app/
│   ├── AapLocalTool.kt         ← from app/
│   ├── ToolDescriptorMapping.kt ← from app/
│   └── local/                  ← all 60 local tools from app/ (zero changes)
├── data/                       ← already here (AssistantRepository etc.)
└── di/
    └── AssistantDiModule.kt    ← NEW: localToolsModule + shared assistantModule parts

composeApp/src/commonMain/kotlin/.../assistant/
├── presentation/
│   ├── AssistantViewModel.kt   ← from app/ (ViewModel → CMP ViewModel)
│   └── AssistantUiState.kt     ← from app/ (@Immutable already in CMP)
└── ui/
    ├── AssistantScreen.kt      ← from app/ (5 API replacements)
    ├── ChatBubble.kt           ← from app/ (already CMP-compatible)
    └── ConfirmationCard.kt     ← from app/ (already CMP-compatible)
```

### What Stays in `app/` (Android-Only)

- `AnsibleJaneApp.kt` — Android Application class
- `TinkMigration.kt` — Android credential migration
- Settings UI screens (General, Instances, Agent, Tools tabs) — these are Android-only for now
- `SettingsViewModel.kt` — stays in app/ (has SettingsScreen coupling)

### DI Module Split

**Before (all in `app/`):**
- `assistantModule` — McpServerManager, ToolRouter, AssistantViewModel, SettingsViewModel
- `localToolsModule` — 60 local tool singletons

**After:**
- `shared/.../di/AssistantDiModule.kt`:
  - `sharedAssistantModule` — McpServerManager, ToolRouter, localToolsModule entries
  - Registration: `single { McpServerManager(...) }`, `single { ToolRouter(...) }`, all `single { XxxLocalTool(get()) } bind LocalTool::class`
- `composeApp/.../presentation/PresentationModule.kt`:
  - Add `viewModelOf(::AssistantViewModel)` to existing module
- `app/.../assistant/AssistantModule.kt`:
  - Becomes thin: only `viewModel { SettingsViewModel(...) }` and the `llm` named HttpClient

### Desktop Entry Point Change

```kotlin
// Main.kt — before:
startKoin {
    modules(desktopPlatformModule, sharedDataModule, sharedRepositoryModule, sharedNetworkModule, presentationModule)
}
App()

// Main.kt — after:
startKoin {
    modules(desktopPlatformModule, sharedDataModule, sharedRepositoryModule,
            sharedNetworkModule, sharedAssistantModule, presentationModule)
}
App(
    assistantContent = { AssistantScreen() }
)
```

---

## 4. Implementation Plan

### Step 1: Move engine + tools to `shared/commonMain` (largest step)

**Files to move (73 files, ~5,000 LOC):**
1. `engine/ChatEngine.kt` — change `import java.io.IOException` → `import kotlinx.io.IOException` (one line)
2. `engine/ToolExecutor.kt` — replace `System.currentTimeMillis()` with `kotlin.time.Clock.System.now().toEpochMilliseconds()` (4 call sites)
3. `engine/ToolRouter.kt` — this is the **critical file**:
   - Remove `import java.util.concurrent.atomic.AtomicBoolean` → add `kotlinx.atomicfu` dependency, use `val initialized = atomic(false)` and `initialized.compareAndSet(false, true)`
   - Remove all `@Synchronized` annotations (10 methods) → wrap each method body in `mutex.withLock { }`
   - Replace `synchronized(this) { }` block in `toggleToolEnabled` → `mutex.withLock { }`
   - Since `@Synchronized` methods become suspend (Mutex.withLock is suspend), OR use a non-suspend lock:
     - **Option A: kotlinx.atomicfu Lock** — `val lock = SynchronizedObject()` with `synchronized(lock) { }` — this IS available in KMP via `kotlinx.atomicfu.locks`
     - **Option B: ReentrantLock** — JVM-only, not KMP
     - **Option C: Mutex** — suspend-only, changes public API signatures
     - **Recommended: Option A** — `kotlinx.atomicfu.locks.SynchronizedObject()` provides `synchronized()` blocks that work across KMP targets. No API signature changes. Drop-in replacement.
   - Remove `@VisibleForTesting` → make `setToolEnabled()` `internal`
4. `llm/LlmProvider.kt` — change `java.io.Closeable` → `AutoCloseable` (Kotlin stdlib, one import)
5. `llm/GeminiLlmProvider.kt`:
   - `java.net.SocketTimeoutException` → `io.ktor.client.network.sockets.SocketTimeoutException`
   - `System.currentTimeMillis()` → `kotlin.time.Clock.System.now().toEpochMilliseconds()`
6. `llm/KoogLlmProvider.kt`:
   - `java.net.URI` → `io.ktor.http.Url()` (used at line 87 for URL path extraction)
   - `java.io.Closeable` → `AutoCloseable`
   - `java.net.SocketTimeoutException` → `io.ktor.client.network.sockets.SocketTimeoutException`
   - `System.currentTimeMillis()` → `kotlin.time.Clock.System.now().toEpochMilliseconds()`
7. `tools/McpTool.kt` — change `java.io.IOException` → `kotlinx.io.IOException`, `java.net.SocketTimeoutException` → `io.ktor.client.network.sockets.SocketTimeoutException`
8. All 60 local tools — move unchanged (zero JVM deps confirmed)
9. ToolSpec, LocalTool, CachedMcpTool, AapLocalTool, ToolDescriptorMapping — move unchanged

**McpServerManager also needs KMP fixes** (currently in `app/.../network/mcp/`, moves to shared alongside the engine):
- 6 `synchronized(mcpTools) { }` blocks → replace with `SynchronizedObject` + `synchronized()` from atomicfu (same pattern as ToolRouter)
- These are in `getAllTools()`, `getToolsForServer()`, `setCachedTools()`, `connectServer()`, `disconnectAll()`, `refreshConnections()`

**Package updates:** All moved files change package from `io.github.leogallego.ansiblejane.assistant.*` to same — no package rename needed since shared already has `assistant/` package.

**Build changes:**
- `shared/build.gradle.kts` — add `implementation(libs.kotlinx.atomicfu)` to commonMain deps (if not already present)
- `shared/build.gradle.kts` — Koog and MCP SDK already in shared deps (confirmed)
- `app/build.gradle.kts` — remove Koog, MCP SDK deps (now transitive from shared)

**DI changes:**
- Create `shared/.../di/AssistantDiModule.kt` with `sharedAssistantModule` containing:
  - All 60 local tool registrations (from localToolsModule)
  - `single { McpServerManager(...) }` (from assistantModule)
  - `single { ToolRouter(initialLocalTools = getAll(), repository = get()) }`
  - `single(named("llm")) { createPlatformHttpClient { expectSuccess = false } }`
- Remove `localToolsModule` from app/
- **Remove `single { McpServerManager(...) }`, `single { ToolRouter(...) }`, and `single(named("llm")) { ... }` from app/assistantModule** — these now live in sharedAssistantModule. Keeping both causes `KoinDuplicateDefinitionException` at startup.
- Slim down `assistantModule` in app/ to only SettingsViewModel

### Step 2: Move presentation to `composeApp/commonMain`

**Files to move (2 files, ~480 LOC):**
1. `presentation/AssistantViewModel.kt`:
   - `import androidx.lifecycle.ViewModel` — already works in CMP (cmp-lifecycle-viewmodel is in composeApp deps)
   - `import androidx.lifecycle.viewModelScope` — same, CMP compatible
   - `Dispatchers.IO` in `onCleared()` — replace with `Dispatchers.Default` (or add KMP-compatible IO dispatcher)
   - No other changes needed
2. `presentation/AssistantUiState.kt`:
   - `@Immutable` from `androidx.compose.runtime` — already available in CMP
   - No other changes needed

**DI changes:**
- Add `viewModelOf(::AssistantViewModel)` to `composeApp/.../PresentationModule.kt`
- **Remove `viewModel { AssistantViewModel(...) }` from app/assistantModule** — now registered in PresentationModule. Keeping both causes `KoinDuplicateDefinitionException`.

### Step 3: Move UI to `composeApp/commonMain`

**Files to move (3 files, ~880 LOC):**
1. `ui/AssistantScreen.kt` — **one change required**:
   - `WindowInsets.isImeVisible` — does NOT compile on desktop. Add `expect/actual` platform check: `expect val isImeVisible: Boolean` (true on Android via `WindowInsets.isImeVisible`, false on desktop). Wrap the `LaunchedEffect(imeVisible)` auto-scroll block with this check. ~10 lines across 3 files (expect + 2 actuals).
   - Everything else works as-is: `collectAsStateWithLifecycle`, `rememberSaveable`, `imePadding()`, `@PreviewLightDark`, `koinViewModel()` — all CMP-compatible (verified)
2. `ui/ChatBubble.kt` — **zero changes expected**:
   - All imports are `androidx.compose.*` which is CMP
   - `mikepenz.markdown.*` is KMP library (confirmed)
   - `snipme.highlights.*` — KMP, transitive dep of markdown-renderer-code (confirmed: all targets published)
3. `ui/ConfirmationCard.kt` — **zero changes expected**:
   - Pure Compose Material3 components

### Step 4: Wire up Desktop + DI

1. **`composeApp/src/desktopMain/.../Main.kt`:**
   ```kotlin
   import io.github.leogallego.ansiblejane.assistant.di.sharedAssistantModule
   import io.github.leogallego.ansiblejane.assistant.ui.AssistantScreen

   fun main() = application {
       startKoin {
           modules(
               desktopPlatformModule,
               sharedDataModule,
               sharedRepositoryModule,
               sharedNetworkModule,
               sharedAssistantModule,    // NEW
               presentationModule         // already includes AssistantViewModel
           )
       }
       Window(...) {
           App(
               assistantContent = { AssistantScreen() }  // NEW — was empty lambda
           )
       }
   }
   ```

2. **`app/.../AnsibleJaneApp.kt`** or Android's Koin setup:
   - Add `sharedAssistantModule` to the modules list (replacing `localToolsModule`)
   - Keep slim `assistantModule` for SettingsViewModel only

---

## 5. Dependency Changes

### shared/build.gradle.kts additions

```kotlin
commonMain.dependencies {
    // Already present — verify:
    // implementation(libs.koog.openai.client)    ✓
    // implementation(libs.koog.google.client)     ✓
    // implementation(libs.koog.http.ktor)          ✓
    // implementation(libs.mcp.sdk.client)          ✓

    // ADD:
    implementation(libs.kotlinx.atomicfu)  // For KMP-compatible AtomicBoolean + SynchronizedObject
    // kotlinx.datetime already present
    // kotlin.time.Clock.System is in Kotlin 2.4.0 stdlib (no dependency needed)
}
```

### app/build.gradle.kts removals

```kotlin
// REMOVE (now transitive from shared):
// implementation(libs.koog.openai.client)
// implementation(libs.koog.google.client)
// implementation(libs.koog.http.ktor)
// implementation(libs.mcp.sdk.client)
```

### libs.versions.toml additions (if not present)

```toml
[versions]
atomicfu = "0.33.0"

[libraries]
kotlinx-atomicfu = { group = "org.jetbrains.kotlinx", name = "atomicfu", version.ref = "atomicfu" }
```

---

## 6. Test Migration

### Tests That Move to `shared/commonTest` (Tier 1 — low effort)

| Test File | LOC | Changes Needed |
|-----------|-----|----------------|
| ToolRouterTest.kt | 1,283 | `org.junit.Test` → `kotlin.test.Test`, `@Before` → init or setUp |
| ChatEngineTest.kt | 177 | Same annotation swap |
| ChatEngineStreamingTest.kt | 225 | Same annotation swap |
| ToolExecutorTest.kt | 120 | Same + `System.currentTimeMillis` |
| TokenSavingModeTest.kt | 110 | Same annotation swap |
| LlmProviderDefinitionsTest.kt | 66 | Same |
| TokenUsageTest.kt | 51 | Same annotation swap |
| GeminiLlmProviderTest.kt | 171 | Same + Koog exception type replacements |
| KoogLlmProviderTest.kt | 197 | Same + Koog exception type replacements |
| CachedMcpToolTest.kt | 215 | Same annotation swap |
| ModelFetcherTest.kt | 133 | Same annotation swap |

**Total: ~2,748 LOC of tests move to KMP commonTest**

### Fakes That Move to `shared/commonTest`

- FakeAssistantRepository.kt (102 LOC) — pure StateFlow/SharedFlow, zero Android deps
- TestData.kt (218 LOC) — pure data builders

### Tests That Stay in `app/test` (Android-only)

- AssistantScreenTest.kt — Compose UI testing with Robolectric
- SettingsViewModelTest.kt — depends on SettingsViewModel which stays in app/
- SettingsScreenTest.kt — Compose UI testing

### Test Infrastructure Changes

| Current (JUnit4) | KMP (kotlin.test) |
|-------------------|--------------------|
| `import org.junit.Test` | `import kotlin.test.Test` |
| `import org.junit.Before` | `import kotlin.test.BeforeTest` |
| `import org.junit.After` | `import kotlin.test.AfterTest` |
| `import org.junit.Assert.assertEquals` | `import kotlin.test.assertEquals` |
| `import org.junit.Assert.assertTrue` | `import kotlin.test.assertTrue` |
| `import org.junit.Assert.assertFalse` | `import kotlin.test.assertFalse` |
| `@get:Rule val rule = MainDispatcherRule()` | Remove — `runTest` handles dispatchers |

**Turbine and coroutines-test** are already KMP-compatible and in shared/commonTest deps.

---

## 7. Risk Analysis

### Low Risk
- Moving 60 local tools: zero code changes, just file moves
- Moving ToolSpec, LocalTool, CachedMcpTool, AapLocalTool, ToolDescriptorMapping: zero code changes
- Moving ChatBubble, ConfirmationCard: zero code changes (CMP-compatible Compose)
- Moving AssistantUiState: one import change (@Immutable)

### Medium Risk
- ToolRouter JVM→KMP: AtomicBoolean + @Synchronized replacement is mechanical but touches 10+ methods. Well covered by 1,283 LOC of tests.
- AssistantScreen Android→CMP: all APIs (collectAsStateWithLifecycle, rememberSaveable, imePadding, PreviewLightDark) already CMP-compatible — verified in Kai and existing composeApp code. Pure file move.
- LlM providers: 4 java.* imports to replace, each has a clear KMP equivalent

### High Risk (Needs Verification)
- **MCP SSE transport on Desktop**: McpServerManager uses Ktor SSE plugin. Verify Ktor SSE works with CIO engine on Desktop (should work — Ktor SSE is KMP).
- **Koog SDK on JVM Desktop**: Koog is fully KMP (common, jvm, androidJvm, native, js, wasm targets confirmed), but verify OpenAI and Gemini clients work from a non-Android JVM (no Android-specific TLS or HTTP stack needed — they use Ktor internally).

### Bonus: Pre-existing bug in shared/commonMain
- **`@Synchronized` in HttpClientFactory.kt**: 4 methods in `shared/src/commonMain/.../network/HttpClientFactory.kt` use `@Synchronized` (JVM-only annotation). This compiles today because both targets (Android + Desktop) are JVM-based, but it's a latent bug for any future non-JVM target. Fix alongside ToolRouter's `@Synchronized` migration in Step 1.

---

## 8. Commit Strategy

Based on user preference for commit-per-feature:

1. **Commit 1: "feat: move engine + tools to shared/commonMain (#243)"**
   - Move 73 files (engine/, llm/, tools/, tools/local/)
   - Replace JVM types in ToolRouter, LlmProvider, ChatEngine, ToolExecutor, McpTool
   - Add atomicfu dependency
   - Update DI modules (create sharedAssistantModule, slim assistantModule)
   - Move engine tests to commonTest
   - Verify: `./gradlew :shared:jvmTest :shared:compileKotlinJvm :app:assembleDebug`

2. **Commit 2: "feat: move assistant presentation to composeApp (#243)"**
   - Move AssistantViewModel, AssistantUiState to composeApp/commonMain
   - Add viewModelOf to PresentationModule
   - Verify: `./gradlew :composeApp:compileKotlinDesktop :app:assembleDebug`

3. **Commit 3: "feat: move assistant UI to composeApp (#243)"**
   - Move AssistantScreen, ChatBubble, ConfirmationCard
   - Add `expect/actual` for `isImeVisible` (Android keeps behavior, desktop defaults to false)
   - All other Compose APIs already CMP-compatible
   - Verify: `./gradlew :composeApp:compileKotlinDesktop :app:assembleDebug`

4. **Commit 4: "feat: wire Desktop assistant + DI (#243)"**
   - Update Desktop Main.kt with assistantContent
   - Register sharedAssistantModule in Desktop Koin
   - Verify: `./gradlew :composeApp:run` runs and shows assistant tab

5. **Commit 5: "test: verify full test suite passes (#243)"**
   - Run `./scripts/test-all.sh`
   - Fix any remaining issues
   - Manual verification on Desktop with Ollama (local LLM)

---

## 9. Verification Checklist

- [ ] `./gradlew :shared:compileKotlinJvm` — shared module compiles
- [ ] `./gradlew :shared:jvmTest` — shared tests pass (including migrated engine tests)
- [ ] `./gradlew :composeApp:compileKotlinDesktop` — Desktop compiles
- [ ] `./gradlew :app:assembleDebug` — Android still compiles
- [ ] `./scripts/test-all.sh` — full test suite passes
- [ ] Desktop: `./gradlew :composeApp:run` — assistant tab shows chat UI
- [ ] Desktop: configure Ollama provider, send message, get response
- [ ] Desktop: MCP tools appear if configured
- [ ] Android: assistant works exactly as before (regression)
- [ ] No `java.*` imports remain in `shared/commonMain` or `composeApp/commonMain`
- [ ] No Koin duplicate registrations: `grep -r "single.*McpServerManager\|single.*ToolRouter" app/src` returns zero
- [ ] No Koin duplicate registrations: `grep "AssistantViewModel" app/.../AssistantModule.kt` returns zero
- [ ] No `synchronized()` blocks remain in `shared/commonMain` (McpServerManager + HttpClientFactory migrated)

---

## 10. Out of Scope

- **Settings screens**: SettingsViewModel + Settings UI stay in `app/` — they have deep coupling with Android-specific settings patterns and are not needed for the desktop assistant MVP.
- **Desktop Settings UI**: Tracked separately, not part of this issue.
- **iOS support**: Koog and MCP SDK publish iOS artifacts, but iOS is not a current target.
- **Issue #296**: Dead `persistState()` method and unsynchronized `init {}` in ToolRouter — can be fixed as part of the ToolRouter KMP work in Step 1 or left for a separate cleanup.
- **snipme/highlights**: Confirmed KMP (all targets published). Transitive dep of markdown-renderer-code. No action needed.

---

## 11. ToolRouter Concurrency Detail (Critical Path)

The ToolRouter is the most complex file to migrate. Here's the exact transformation:

### Before (JVM)
```kotlin
import java.util.concurrent.atomic.AtomicBoolean

class ToolRouter(...) {
    private val initialized = AtomicBoolean(false)

    suspend fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        // ...
    }

    @Synchronized
    fun registerLocalTools(tools: List<LocalTool>) { ... }

    @Synchronized
    fun getToolsForQuery(query: String, ...): QueryResult { ... }

    suspend fun toggleToolEnabled(...) {
        val snapshot = synchronized(this) { ... }
        repository?.saveToolState(snapshot.first, snapshot.second)
    }
}
```

### After (KMP) — using kotlinx.atomicfu.locks
```kotlin
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

class ToolRouter(...) : SynchronizedObject() {
    private val initialized = atomic(false)

    suspend fun initialize() {
        if (!initialized.compareAndSet(false, true)) return
        // ...
    }

    fun registerLocalTools(tools: List<LocalTool>) = synchronized(this) { ... }

    fun getToolsForQuery(query: String, ...): QueryResult = synchronized(this) { ... }

    suspend fun toggleToolEnabled(...) {
        val snapshot = synchronized(this) { ... }
        repository?.saveToolState(snapshot.first, snapshot.second)
    }
}
```

**Key points:**
- `SynchronizedObject()` provides `synchronized()` that works on all KMP targets
- `atomic(false)` replaces `AtomicBoolean(false)` with identical CAS semantics
- No API signature changes — all public methods keep the same types
- `toggleToolEnabled` stays suspend — the `synchronized` block is non-suspend, persistence happens outside the lock (same as current)

---

## 12. SettingsViewModel Coupling Analysis

SettingsViewModel stays in `app/` because:
1. It manages Android Settings screens (General, Instances, Agent, Tools tabs)
2. It depends on ToolRouter, McpServerManager — these move to shared, so imports update but SettingsViewModel stays put
3. Its Koin registration stays in the slim `assistantModule` in app/

After migration, SettingsViewModel imports `ToolRouter` from shared instead of from app — this is transparent since the package name doesn't change.

---

## 13. File Move Checklist

Complete list of files to move (for implementation reference):

### To `shared/src/commonMain/kotlin/io/github/leogallego/ansiblejane/network/mcp/`

```
McpServerManager.kt             (modify: 6 synchronized() blocks → atomicfu SynchronizedObject)
PopularMcpServers.kt            (no change)
```

### To `shared/src/commonMain/kotlin/io/github/leogallego/ansiblejane/assistant/`

```
engine/ChatEngine.kt           (modify: java.io.IOException → kotlinx.io.IOException)
engine/ToolExecutor.kt          (modify: 4 System.currentTimeMillis → kotlin.time.Clock.System)
engine/ToolRouter.kt            (modify: AtomicBoolean, @Synchronized, @VisibleForTesting)
llm/LlmProvider.kt              (modify: 1 import)
llm/GeminiLlmProvider.kt        (modify: 3 java.* imports + System.currentTimeMillis)
llm/KoogLlmProvider.kt          (modify: 4 java.* imports + System.currentTimeMillis)
tools/ToolSpec.kt                (no change)
tools/LocalTool.kt               (no change)
tools/McpTool.kt                 (modify: 2 java.* imports)
tools/CachedMcpTool.kt           (no change)
tools/AapLocalTool.kt            (no change)
tools/ToolDescriptorMapping.kt   (no change)
tools/local/ApproveWorkflowLocalTool.kt          (no change)
tools/local/DenyWorkflowLocalTool.kt             (no change)
tools/local/GetConfigLocalTool.kt                (no change)
tools/local/GetCredentialLocalTool.kt            (no change)
tools/local/GetEdaActivationLocalTool.kt         (no change)
tools/local/GetHostFactsLocalTool.kt             (no change)
tools/local/GetHostJobSummariesLocalTool.kt      (no change)
tools/local/GetInstanceLocalTool.kt              (no change)
tools/local/GetJobLocalTool.kt                   (no change)
tools/local/GetJobStdoutLocalTool.kt             (no change)
tools/local/GetMeshTopologyLocalTool.kt          (no change)
tools/local/GetProjectLocalTool.kt               (no change)
tools/local/GetSettingsLocalTool.kt              (no change)
tools/local/GetSurveySpecLocalTool.kt            (no change)
tools/local/GetWorkflowJobLocalTool.kt           (no change)
tools/local/LaunchJobLocalTool.kt                (no change)
tools/local/LaunchWorkflowLocalTool.kt           (no change)
tools/local/ListApplicationsLocalTool.kt         (no change)
tools/local/ListAuthenticatorsLocalTool.kt       (no change)
tools/local/ListCredentialsLocalTool.kt          (no change)
tools/local/ListCredentialTypesLocalTool.kt      (no change)
tools/local/ListEdaActivationsLocalTool.kt       (no change)
tools/local/ListEdaAuditRulesLocalTool.kt        (no change)
tools/local/ListEdaCredentialsLocalTool.kt       (no change)
tools/local/ListEdaCredentialTypesLocalTool.kt   (no change)
tools/local/ListEdaDecisionEnvironmentsLocalTool.kt (no change)
tools/local/ListEdaEventStreamsLocalTool.kt       (no change)
tools/local/ListEdaProjectsLocalTool.kt          (no change)
tools/local/ListEdaRulebooksLocalTool.kt         (no change)
tools/local/ListEdaUsersLocalTool.kt             (no change)
tools/local/ListExecutionEnvironmentsLocalTool.kt (no change)
tools/local/ListGroupsLocalTool.kt               (no change)
tools/local/ListHostsLocalTool.kt                (no change)
tools/local/ListInstanceGroupsLocalTool.kt       (no change)
tools/local/ListInstancesLocalTool.kt            (no change)
tools/local/ListInventoriesLocalTool.kt          (no change)
tools/local/ListInventorySourcesLocalTool.kt     (no change)
tools/local/ListJobsLocalTool.kt                 (no change)
tools/local/ListJobTemplatesLocalTool.kt         (no change)
tools/local/ListLabelsLocalTool.kt               (no change)
tools/local/ListNotificationTemplatesLocalTool.kt (no change)
tools/local/ListOrganizationsLocalTool.kt        (no change)
tools/local/ListPendingApprovalsLocalTool.kt     (no change)
tools/local/ListPlatformOrganizationsLocalTool.kt (no change)
tools/local/ListPlatformRoleDefinitionsLocalTool.kt (no change)
tools/local/ListPlatformServicesLocalTool.kt     (no change)
tools/local/ListPlatformTeamsLocalTool.kt        (no change)
tools/local/ListPlatformUsersLocalTool.kt        (no change)
tools/local/ListProjectsLocalTool.kt             (no change)
tools/local/ListRoleDefinitionsLocalTool.kt      (no change)
tools/local/ListRolesLocalTool.kt                (no change)
tools/local/ListSchedulesLocalTool.kt            (no change)
tools/local/ListServiceClustersLocalTool.kt      (no change)
tools/local/ListTeamsLocalTool.kt                (no change)
tools/local/ListTokensLocalTool.kt               (no change)
tools/local/ListToolsLocalTool.kt                (no change)
tools/local/ListUsersLocalTool.kt                (no change)
tools/local/ListWorkflowNodesLocalTool.kt        (no change)
tools/local/ListWorkflowTemplatesLocalTool.kt    (no change)
tools/local/PingLocalTool.kt                     (no change)
tools/local/ToggleScheduleLocalTool.kt           (no change)
```

### To `composeApp/src/commonMain/kotlin/io/github/leogallego/ansiblejane/assistant/`

```
presentation/AssistantViewModel.kt    (modify: Dispatchers.IO)
presentation/AssistantUiState.kt      (no change)
ui/AssistantScreen.kt                 (modify: replace isImeVisible with expect/actual platform check)
ui/ChatBubble.kt                      (no change — snipme/highlights is KMP via markdown-renderer-code)
ui/ConfirmationCard.kt                (no change)
```

### New Files

```
shared/.../assistant/di/AssistantDiModule.kt     (NEW: combined DI module)
```

### Files Removed from `app/`

```
app/.../network/mcp/McpServerManager.kt          (DELETED — moved to shared)
app/.../network/mcp/PopularMcpServers.kt         (DELETED — moved to shared)
app/.../assistant/engine/ChatEngine.kt           (DELETED — moved to shared)
app/.../assistant/engine/ToolExecutor.kt          (DELETED — moved to shared)
app/.../assistant/engine/ToolRouter.kt            (DELETED — moved to shared)
app/.../assistant/llm/LlmProvider.kt              (DELETED — moved to shared)
app/.../assistant/llm/GeminiLlmProvider.kt        (DELETED — moved to shared)
app/.../assistant/llm/KoogLlmProvider.kt          (DELETED — moved to shared)
app/.../assistant/tools/*                          (DELETED — moved to shared)
app/.../assistant/LocalToolsModule.kt             (DELETED — merged into shared DI)
app/.../assistant/presentation/AssistantViewModel.kt  (DELETED — moved to composeApp)
app/.../assistant/presentation/AssistantUiState.kt    (DELETED — moved to composeApp)
app/.../assistant/ui/AssistantScreen.kt           (DELETED — moved to composeApp)
app/.../assistant/ui/ChatBubble.kt                (DELETED — moved to composeApp)
app/.../assistant/ui/ConfirmationCard.kt          (DELETED — moved to composeApp)
```

### Files Modified (not moved)

```
app/.../assistant/AssistantModule.kt              (MODIFIED — slim to SettingsVM only)
app/.../AnsibleJaneApp.kt                         (MODIFIED — add sharedAssistantModule)
composeApp/.../presentation/PresentationModule.kt (MODIFIED — add AssistantViewModel)
composeApp/.../desktopMain/Main.kt                (MODIFIED — add assistant modules + content)
shared/build.gradle.kts                            (MODIFIED — add atomicfu dep)
app/build.gradle.kts                               (MODIFIED — remove Koog/MCP deps)
```

---

## 14. Estimated Effort

| Step | Files Changed | Code Changes | Effort |
|------|---------------|-------------|--------|
| Step 1: Engine + Tools → shared | 73 moved, 3 new, 3 modified | ~30 lines of actual code changes (imports + JVM replacements) | Medium (bulk of the work is testing) |
| Step 2: Presentation → composeApp | 2 moved, 1 modified | ~5 lines (Dispatchers.IO → Default) | Low |
| Step 3: UI → composeApp | 3 moved, 3 new (expect/actual for isImeVisible) | ~10 lines (isImeVisible platform check) | Low |
| Step 4: Wire Desktop | 2 modified | ~10 lines | Low |
| Step 5: Tests | 6 test files migrated | ~50 lines (annotation swaps) | Medium (verify all pass) |

**Total actual code changes: ~85 lines across 80+ file moves.**

The migration is overwhelmingly file moves with minimal code changes. The UI layer requires zero code changes — all Compose APIs (collectAsStateWithLifecycle, rememberSaveable, imePadding, PreviewLightDark) are CMP-compatible, verified by Kai reference project and existing composeApp code. The codebase was already well-separated during the original KMP migration (#233).

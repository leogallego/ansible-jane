# Test Migration Follow-ups — 2026-06-21

Post-mortem from PR #391 (migrate ViewModel tests to commonTest, issue #372 Phase 1).
Three topics surfaced during migration that need tracking.

---

## 1. Robolectric UI test duplication — tracked in #372 Phase 2

**Status:** Already covered by existing issue #372 Phase 2.

**What happened:** After migrating 14 ViewModel tests and 13 fakes to
`composeApp/src/commonTest/`, the fakes had to be *duplicated* (not moved)
because 9 Robolectric screen tests in `app/src/test/ui/` still import them.
Gradle module boundaries prevent `app/src/test/` from accessing
`composeApp/src/commonTest/` classes.

**Issue landscape:**
- **#372 Phase 2 (open)** — covers creating desktop equivalents of the 9
  Robolectric screen tests using `runComposeUiTest` (Compose Multiplatform
  API) with Skiko offscreen rendering. Once desktop screen tests exist in
  `composeApp/commonTest/`, the Robolectric versions in `app/src/test/ui/`
  can be deleted, which eliminates the fake duplication.
- **#299 (closed)** — was the predecessor: migrated 11 assistant/engine
  tests to `shared/commonTest/`. Completed via PR #376/#383.
- **#378 (closed)** — removed unnecessary Robolectric from 5 ViewModel
  tests, unblocking their migration in PR #391.
- **#86-#89 (closed)** — the original 4-phase testing plan. Phases 1-3
  (infra, unit tests, UI behavior tests) are done. Phase 4 (screenshots,
  E2E) is partially done.

**What the duplication looks like today:**
- `app/src/test/.../fakes/` — 14 fakes (13 shared + FakeApprovalNotificationManager)
- `composeApp/src/commonTest/.../fakes/` — 13 fakes (same 13, minus the Android-only one)
- `app/src/test/.../TestData.kt` and `composeApp/src/commonTest/.../TestData.kt` — identical

**Clean-up path:** Completing #372 Phase 2 (desktop screen tests) would
allow deleting the 9 Robolectric screen tests from `app/src/test/ui/`,
which in turn allows deleting the duplicate fakes and TestData from
`app/src/test/`. The remaining `app/src/test/` files (2 platform tests,
2 MCP tests, ParseDeepLinkTest, ToolManifestRepositoryTest) either have
their own local fakes or don't use the shared fakes at all.

**No new issue needed.** #372 Phase 2 covers this.

---

## 2. Shared test helpers — `testFixtures` source set

**Status:** No existing issue covers this. Action needed: file a new issue
if the team decides to pursue this before completing #372 Phase 2.

**The problem:** When test helpers (fakes, TestData, test utilities) are
needed by tests in multiple Gradle modules (`app/src/test/`,
`composeApp/src/commonTest/`, potentially `shared/src/commonTest/`),
there's no clean sharing mechanism. Today we duplicate files.

**The solution:** Gradle's `testFixtures` feature lets a module publish
test helpers that other modules can depend on:

```kotlin
// shared/build.gradle.kts
kotlin {
    jvm {
        testFixtures { }  // or java-test-fixtures plugin for JVM
    }
}

// In fakes that implement shared interfaces:
// shared/src/testFixtures/kotlin/.../fakes/FakeTokenManager.kt

// Consumer modules:
// app/build.gradle.kts
testImplementation(testFixtures(projects.shared))
// composeApp/build.gradle.kts
commonTest.dependencies {
    implementation(testFixtures(projects.shared))
}
```

**Assessment:** This is a *nice-to-have* optimization, not urgent. The
duplication introduced in PR #391 is contained (13 simple interface stubs
with zero logic), and #372 Phase 2 will eliminate most of it naturally.
`testFixtures` becomes worth it if:
- More fakes accumulate across modules
- Fakes gain non-trivial behavior that could drift between copies
- A third module needs the same fakes

**Recommendation:** Don't file an issue yet. Revisit after #372 Phase 2
completes — if duplication persists, file it then with concrete scope.

---

## 3. Service contracts gap — test infrastructure rules

**Status:** No existing issue. No section in `docs/architecture/service-contracts.md`
covers test infrastructure placement, test API choices, or test helper ownership.

**What's missing:** The migration exposed that there are no documented rules
for:

1. **Test source set placement** — which tests go where:
   - `commonTest` (kotlin.test): shared business logic, ViewModels, data/parsing
   - `src/test` (JUnit4/Robolectric): Android-specific UI rendering, platform tests
   - `src/androidTest`: instrumented tests needing real Android runtime
   - `desktopTest`: desktop-specific smoke tests

2. **Test API boundaries** — what's allowed in each source set:
   - `commonTest` MUST use `kotlin.test` (not JUnit4 directly)
   - `commonTest` MUST NOT use `System.currentTimeMillis()`, `String(ByteArray, Charset)`,
     or other JVM-only APIs — use `kotlinx.datetime.Clock`, `ByteArray.decodeToString()`
   - `commonTest` MUST NOT use JUnit4 rules (`@Rule`, `TestWatcher`) — use
     `@BeforeTest`/`@AfterTest` functions
   - `assertTrue(message, value)` parameter order differs between JUnit4 and
     kotlin.test — kotlin.test uses `assertTrue(value, message)`

3. **Test helper ownership** — where fakes and utilities live:
   - Fakes implementing `shared/commonMain` interfaces → `composeApp/src/commonTest/fakes/`
     (or `shared/src/commonTest/fakes/` if consumed by shared tests)
   - Android-only fakes (using `Context`, etc.) → `app/src/test/fakes/`
   - Test data fixtures → alongside the test source set that uses them
   - If multiple modules need the same fakes → evaluate `testFixtures` source set

4. **MainDispatcher replacement pattern:**
   - `app/src/test/` uses `MainDispatcherRule` (JUnit4 `TestWatcher`)
   - `composeApp/src/commonTest/` uses `setupMainDispatcher()`/`tearDownMainDispatcher()`
     (plain functions called from `@BeforeTest`/`@AfterTest`)
   - Both patterns coexist — document that new tests should use the KMP-compatible one

**Why this matters:** Without these rules, future contributors will:
- Put KMP-compatible tests in `app/src/test/` out of habit (wrong placement)
- Use JUnit4 assertions in `commonTest` (breaks non-JVM compilation targets)
- Use JVM-only APIs in `commonTest` (same)
- Create new `MainDispatcherRule` usages instead of the KMP-compatible pattern

**Recommendation:** Add a "§10. Test Infrastructure" section to
`docs/architecture/service-contracts.md` in a follow-up PR. This is a
documentation-only change with no code impact. File an issue to track it.

**Proposed hard rules for the new section:**
- Tests mirror code placement: `commonTest` for code in `commonMain`,
  `src/test` for code in `androidMain`/`app`
- `commonTest` uses `kotlin.test` exclusively — no `org.junit.*`
- `commonTest` uses no JVM-only APIs (`System.*`, `java.*`)
- New ViewModel tests go in `composeApp/src/commonTest/`, not `app/src/test/`

**Proposed soft guidelines:**
- Prefer fakes over mocks for repository/manager dependencies
- Use `setupMainDispatcher()`/`tearDownMainDispatcher()` for new tests
  (not `MainDispatcherRule`)
- Test file naming: `XxxTest.kt` in the same package as the class under test

---

## Summary

| Topic | Existing issue | Action |
|-------|---------------|--------|
| Robolectric UI test migration + fake dedup | #372 Phase 2 (open) | None — already tracked |
| `testFixtures` shared helpers | None | Revisit after #372 Phase 2; file issue only if duplication persists |
| Service contracts test infra section | None | **File new issue**, add §10 to service-contracts.md |

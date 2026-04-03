# Implementation Plan: Navigation Foundation & UI Modernization

**Branch**: `002-nav-ui-modernize` | **Date**: 2026-04-02 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/002-nav-ui-modernize/spec.md`

## Summary

Replace the current toolbar-based single-screen navigation with a Material 3 bottom navigation bar (3 tabs: Templates, Infrastructure, Activity), each with segmented sub-sections. Enhance template cards, add skeleton loading states, pull-to-refresh, animated transitions, a notification bell placeholder, and a Settings screen for logout. No new dependencies required — all components are available in the existing Material 3 BOM.

## Technical Context

**Language/Version**: Kotlin (JVM 17), compileSdk 35, minSdk 31  
**Primary Dependencies**: Jetpack Compose (Material 3 BOM), Navigation Compose, Retrofit, Koin  
**Storage**: DataStore + Tink (unchanged)  
**Testing**: Manual testing (no test framework currently in project)  
**Target Platform**: Android phone (API 31+)  
**Project Type**: Mobile app  
**Performance Goals**: Tab switch <1s, segment switch <500ms, skeleton visible within 100ms  
**Constraints**: Single ComponentActivity, no new dependencies, no Fragments  
**Scale/Scope**: 3 tabs, 8 segments (2 implemented, 6 placeholders), ~7 new files, ~7 modified files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Pre-Design | Post-Design | Notes |
|-----------|-----------|-------------|-------|
| I. Kotlin-Only | PASS | PASS | All new code is Kotlin |
| II. Compose-First UI | PASS | PASS | M3 NavigationBar, SegmentedButton, PullToRefreshBox — all Compose |
| III. MVVM + UDF | PASS | PASS | Existing ViewModels reused; no logic in Composables |
| IV. Security-First | PASS | PASS | No credential changes |
| V. Lean Dependencies | PASS | PASS | Zero new dependencies — shimmer is custom, all M3 components in BOM |
| VI. API-Driven Design | PASS | PASS | No new API endpoints |

## Project Structure

### Documentation (this feature)

```text
specs/002-nav-ui-modernize/
├── plan.md              # This file
├── spec.md              # Feature specification
├── research.md          # Phase 0: research decisions
├── data-model.md        # Phase 1: navigation state model
├── quickstart.md        # Phase 1: build & test guide
├── contracts/
│   └── navigation-contract.md  # Navigation routes & transitions
├── checklists/
│   └── requirements.md  # Spec quality checklist
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/kotlin/com/example/aapremote/
├── navigation/
│   ├── AppNavigation.kt          # MODIFY: add Settings route, MainScreen as post-auth dest
│   └── MainNavigation.kt         # CREATE: tab-level NavHost with back stacks
├── ui/
│   ├── main/
│   │   ├── MainScreen.kt         # CREATE: root scaffold with bottom nav + top app bar
│   │   └── TabDefinitions.kt     # CREATE: tab/segment enums and data
│   ├── templates/
│   │   ├── TemplateListScreen.kt # MODIFY: remove TopAppBar, add pull-to-refresh, skeleton
│   │   └── TemplateListItem.kt   # MODIFY: enhanced card design
│   ├── jobs/
│   │   ├── RecentJobsScreen.kt   # MODIFY: remove TopAppBar, add pull-to-refresh, skeleton
│   │   └── JobStatusScreen.kt    # MODIFY: keep TopAppBar, slide transitions
│   ├── settings/
│   │   └── SettingsScreen.kt     # CREATE: server info + logout
│   └── components/
│       ├── PlaceholderScreen.kt   # CREATE: reusable "coming soon" screen
│       ├── SkeletonCard.kt        # CREATE: shimmer loading card
│       └── ShimmerModifier.kt     # CREATE: reusable shimmer animation modifier
├── presentation/
│   ├── templates/
│   │   └── TemplatesViewModel.kt  # MODIFY: add refresh() function
│   └── jobs/
│       └── RecentJobsViewModel.kt # MODIFY: add refresh() function
└── (all other files unchanged)
```

**Structure Decision**: Follows existing feature-based packaging. New `ui/main/` package groups the root scaffold and tab definitions. New `ui/settings/` package for the settings screen. Shared UI components go in the existing `ui/components/` package.

## Implementation Phases

### Phase A: Navigation Foundation (P1 — must be done first)

**Goal**: Bottom nav bar with 3 tabs, segmented buttons, independent back stacks. This is the structural foundation everything else depends on.

**Files to create**:
1. `ui/main/TabDefinitions.kt` — Define `TopLevelTab` sealed class/enum with route, label, icon, segments. Define `Segment` data class.
2. `ui/main/MainScreen.kt` — Root `Scaffold` with:
   - `TopAppBar` (app title, bell icon, gear icon)
   - `NavigationBar` with 3 `NavigationBarItem`s
   - Content area with segmented buttons + tab content
3. `navigation/MainNavigation.kt` — Tab-scoped navigation with `saveState`/`restoreState` for independent back stacks per tab.
4. `ui/components/PlaceholderScreen.kt` — Reusable "Coming Soon" composable for unimplemented segments.

**Files to modify**:
5. `navigation/AppNavigation.kt` — Replace `TEMPLATES` as post-auth destination with `MainScreen`. Add `SETTINGS` route. Update job status navigation to work within tab context.

**Validation**: All 3 tabs visible, segments switch, placeholders display, existing templates + jobs screens appear in correct tabs, back stacks preserved across tab switches, rotation preserves state.

### Phase B: Settings Screen (P1 — depends on Phase A)

**Goal**: Gear icon in top app bar opens Settings screen with server URL and logout.

**Files to create**:
1. `ui/settings/SettingsScreen.kt` — Display connected server URL, logout button, back navigation.

**Files to modify**:
2. `ui/templates/TemplateListScreen.kt` — Remove the logout icon button from TopAppBar (moved to Settings).
3. `navigation/AppNavigation.kt` — Wire Settings route with SettingsScreen composable.

**Validation**: Gear icon navigates to Settings, server URL displayed, logout works, back returns to previous tab.

### Phase C: UI Enhancements (P2 — independent of Phase B)

**Goal**: Enhanced template cards, skeleton loading, pull-to-refresh.

**Files to create**:
1. `ui/components/ShimmerModifier.kt` — `Modifier.shimmer()` extension using `InfiniteTransition` + `linearGradient`.
2. `ui/components/SkeletonCard.kt` — Placeholder card with shimmer matching `TemplateListItem` layout.

**Files to modify**:
3. `ui/templates/TemplateListItem.kt` — Enhanced card: `ElevatedCard` or `Card` with `tonalElevation`, improved typography (`titleMedium` for name, `bodySmall` for description), better spacing (16dp padding), label chips with `AssistChip` style.
4. `ui/templates/TemplateListScreen.kt` — Replace `CircularProgressIndicator` with `LazyColumn` of `SkeletonCard`s during loading. Wrap content in `PullToRefreshBox`.
5. `ui/jobs/RecentJobsScreen.kt` — Same skeleton + pull-to-refresh treatment.
6. `presentation/templates/TemplatesViewModel.kt` — Add `refresh()` method that resets pagination and reloads.
7. `presentation/jobs/RecentJobsViewModel.kt` — Add `refresh()` method.

**Validation**: Cards show improved design, skeleton loaders appear during loading, pull-to-refresh reloads data on all lists.

### Phase D: Animations & Polish (P3 — after Phase A)

**Goal**: Animated transitions and notification bell placeholder.

**Files to modify**:
1. `navigation/AppNavigation.kt` — Add `enterTransition`/`exitTransition` with `slideInHorizontally`/`slideOutHorizontally` for detail screens, `fadeIn`/`fadeOut` for tab switches.
2. `ui/main/MainScreen.kt` — Bell icon tap shows snackbar "Notifications coming soon". Ensure `AnimatedContent` or crossfade for segment switching within tabs.

**Validation**: Tab switches crossfade, detail screens slide, bell shows snackbar.

## Build Sequence

```
Phase A (Navigation Foundation)
    ├── Phase B (Settings Screen)
    ├── Phase C (UI Enhancements) ← can run in parallel with B
    └── Phase D (Animations & Polish) ← after A, can run in parallel with B/C
```

## Complexity Tracking

No constitution violations. No complexity justifications needed.

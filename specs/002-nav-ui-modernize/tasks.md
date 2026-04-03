# Tasks: Navigation Foundation & UI Modernization

**Input**: Design documents from `/specs/002-nav-ui-modernize/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/navigation-contract.md

**Tests**: No test tasks included (no test framework currently in project, manual testing per quickstart.md).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Path Conventions

- **Base path**: `app/src/main/kotlin/com/example/aapremote/`
- Paths below are relative to the base path unless prefixed with `app/`

## Phase Mapping (plan.md → tasks.md)

| plan.md | tasks.md | Content |
|---------|----------|---------|
| Phase A | Phase 3 | Navigation Foundation (US1+US2) |
| Phase B | Phase 4 | Settings Screen (US8) |
| Phase C | Phase 5+6 | UI Enhancements (US3, US4+US5) |
| Phase D | Phase 7 | Animations & Polish (US6+US7) |

---

## Phase 1: Setup

**Purpose**: Create package directories for new UI sections

- [x] T001 Create package directories: `ui/main/`, `ui/settings/`, `ui/components/` (if not existing) under `app/src/main/kotlin/com/example/aapremote/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared data definitions and reusable UI components that multiple user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

- [x] T002 Define `TopLevelTab` sealed class/enum and `Segment` data class in `app/src/main/kotlin/com/example/aapremote/ui/main/TabDefinitions.kt` — includes route, label, icon, selectedIcon, and segments list per the data model and navigation contract
- [x] T003 [P] Create reusable `PlaceholderScreen` composable in `app/src/main/kotlin/com/example/aapremote/ui/components/PlaceholderScreen.kt` — accepts title and optional description, displays centered icon + "Coming Soon" message (FR-008)
- [x] T004 [P] Create `Modifier.shimmer()` extension in `app/src/main/kotlin/com/example/aapremote/ui/components/ShimmerModifier.kt` — uses `InfiniteTransition` + `Brush.linearGradient` per R-003

**Checkpoint**: Foundation ready — shared components available for all user stories

---

## Phase 3: User Story 1 + User Story 2 — Tab-Based Navigation & Segmented Sections (Priority: P1) MVP

**Goal**: Bottom nav bar with 3 tabs (Templates, Infrastructure, Activity), segmented buttons within each tab, independent back stacks per tab, existing screens relocated to correct tabs

**Independent Test**: Login → see bottom nav with 3 tabs → tap each tab → segments visible → existing templates appear under Templates > Job Templates, recent jobs under Activity > Jobs → back stack preserved across tab switches → rotation preserves state

### Implementation

- [x] T005 [US1] Create `MainScreen` composable in `app/src/main/kotlin/com/example/aapremote/ui/main/MainScreen.kt` — root `Scaffold` with `TopAppBar` (app title, bell icon with no-op handler, gear icon with no-op handler) and `NavigationBar` with 3 `NavigationBarItem`s, segmented button row via `SingleChoiceSegmentedButtonRow`, content area showing selected segment's screen. Bell and gear click handlers wired in T019 and T011 respectively (FR-001, FR-002, FR-009)
- [x] T006 [US1] Create tab-level navigation in `app/src/main/kotlin/com/example/aapremote/navigation/MainNavigation.kt` — `NavHost` with `saveState`/`restoreState` for independent back stacks per tab, wire existing TemplateListScreen and RecentJobsScreen to their correct segments, PlaceholderScreen for unimplemented segments. State preservation across config changes handled by saveState/restoreState (FR-003, FR-004, FR-005, FR-006, FR-007, FR-008, FR-017, FR-019)
- [x] T007 [US1] Modify `app/src/main/kotlin/com/example/aapremote/navigation/AppNavigation.kt` — replace `TEMPLATES` as post-auth destination with `MainScreen`, add `SETTINGS` route, update job status navigation to work within tab context. Bottom nav must NOT be visible on auth screen (FR-016, FR-017)
- [x] T008 [US1] Modify `app/src/main/kotlin/com/example/aapremote/ui/templates/TemplateListScreen.kt` — remove `TopAppBar` (now provided by MainScreen), preserve existing list/search/filter functionality within the tab content area
- [x] T009 [US1] Modify `app/src/main/kotlin/com/example/aapremote/ui/jobs/RecentJobsScreen.kt` — remove `TopAppBar` (now provided by MainScreen), keep existing list content within the tab content area

**Checkpoint**: All 3 tabs visible with segments, existing screens work in their new locations, back stacks preserved, rotation preserves tab + segment selection. This is the MVP — stop and validate.

---

## Phase 4: User Story 8 — Settings Screen (Priority: P1, depends on Phase 3)

**Goal**: Gear icon in top app bar opens Settings screen with server URL and logout

**Independent Test**: Tap gear icon → Settings screen opens → server URL displayed → logout works → back returns to previous tab

### Implementation

- [x] T010 [US8] Create `SettingsScreen` composable in `app/src/main/kotlin/com/example/aapremote/ui/settings/SettingsScreen.kt` — display connected server URL, logout button, back navigation via callback lambdas. No ViewModel needed per R-006 — screen is stateless with callback lambdas (FR-018, FR-020)
- [x] T011 [US8] Wire Settings route in `app/src/main/kotlin/com/example/aapremote/navigation/AppNavigation.kt` — gear icon navigates to `settings` route, SettingsScreen composable receives serverUrl, onLogout, onNavigateBack
- [x] T012 [US8] Remove logout icon/button from `app/src/main/kotlin/com/example/aapremote/ui/templates/TemplateListScreen.kt` — logout action moved to Settings screen

**Checkpoint**: Settings accessible from gear icon, server URL visible, logout functional, back returns to previous tab

---

## Phase 5: User Story 3 — Enhanced Template Cards (Priority: P2)

**Goal**: Template cards with improved visual hierarchy: prominent name, secondary description, label chips, card elevation

**Independent Test**: View template list → cards show elevated surface, `titleMedium` for name, `bodySmall` for description, chips for labels, 16dp padding

### Implementation

- [x] T013 [US3] Modify `app/src/main/kotlin/com/example/aapremote/ui/templates/TemplateListItem.kt` — upgrade to `ElevatedCard` or `Card` with `tonalElevation`, use `titleMedium` for template name, `bodySmall` for description, 16dp padding, `AssistChip`-style label chips (FR-011)

**Checkpoint**: Template cards display with improved design, labels render as chips

---

## Phase 6: User Story 4 + User Story 5 — Skeleton Loading & Pull-to-Refresh (Priority: P2)

**Goal**: Skeleton loading placeholders replace spinners on all list screens, pull-to-refresh reloads data on all lists

**Independent Test**: Navigate to any list → skeleton cards with shimmer appear during loading (not spinner) → pull down → refresh indicator appears → data reloads → error shows message and preserves existing data

### Implementation

- [x] T014 [P] [US4] Create `SkeletonCard` composable in `app/src/main/kotlin/com/example/aapremote/ui/components/SkeletonCard.kt` — placeholder card with shimmer matching `TemplateListItem` layout dimensions (name placeholder, description placeholder, chip placeholders) using `Modifier.shimmer()` from T004 (FR-012)
- [x] T015 [P] [US5] Add `refresh()` method to `app/src/main/kotlin/com/example/aapremote/presentation/templates/TemplatesViewModel.kt` — resets pagination state and reloads templates from API
- [x] T016 [P] [US5] Add `refresh()` method to `app/src/main/kotlin/com/example/aapremote/presentation/jobs/RecentJobsViewModel.kt` — resets state and reloads recent jobs from API
- [x] T017 [US4] [US5] Modify `app/src/main/kotlin/com/example/aapremote/ui/templates/TemplateListScreen.kt` — replace `CircularProgressIndicator` with `LazyColumn` of `SkeletonCard`s during loading state, wrap content in `PullToRefreshBox` calling ViewModel's `refresh()` (FR-012, FR-013, R-004)
- [x] T018 [US4] [US5] Modify `app/src/main/kotlin/com/example/aapremote/ui/jobs/RecentJobsScreen.kt` — same skeleton loading + `PullToRefreshBox` treatment as T017 (FR-012, FR-013)

**Checkpoint**: All list screens show skeleton loaders instead of spinners, pull-to-refresh works on templates and jobs lists, error handling preserves existing data

---

## Phase 7: User Story 6 + User Story 7 — Notification Bell & Animated Transitions (Priority: P3)

**Goal**: Bell icon shows "coming soon" snackbar, tab switches crossfade, detail screens slide

**Independent Test**: Tap bell icon → "Notifications coming soon" snackbar → switch tabs → crossfade animation → open job detail → slide in from right → press back → slide out to right

### Implementation

- [x] T019 [P] [US6] Add bell icon tap handler in `app/src/main/kotlin/com/example/aapremote/ui/main/MainScreen.kt` — show snackbar "Notifications coming soon" using `SnackbarHostState` (FR-010)
- [x] T020 [P] [US7] Add transition animations in `app/src/main/kotlin/com/example/aapremote/navigation/AppNavigation.kt` — `enterTransition`/`exitTransition` with `slideInHorizontally`/`slideOutHorizontally` for detail screens (job status, settings), `fadeIn`/`fadeOut` for tab-level transitions (FR-014, FR-015, R-005)
- [x] T021 [US7] Add segment switch animation in `app/src/main/kotlin/com/example/aapremote/ui/main/MainScreen.kt` — `Crossfade` for segment content switching within tabs (FR-014)

**Checkpoint**: Bell snackbar works, tab switches crossfade, detail/settings screens slide in/out

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Edge cases, validation, and final cleanup

- [x] T022 Verify empty state handling on all list screens — display "No items" message instead of blank screen or perpetual skeleton (edge case from spec)
- [x] T023 Verify configuration change (rotation) preserves selected tab, selected segment, and scroll position (FR-017, edge case from spec)
- [x] T024 Run full manual test checklist from `specs/002-nav-ui-modernize/quickstart.md` — validate all 9 test scenarios
- [x] T025 Verify `app/src/main/kotlin/com/example/aapremote/ui/jobs/JobStatusScreen.kt` retains its own `TopAppBar` and works as detail overlay with slide transitions within the tab navigation context

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **US1+US2 (Phase 3)**: Depends on Foundational (T002, T003) — MVP target
- **US8/Settings (Phase 4)**: Depends on Phase 3 (MainScreen + AppNavigation exist)
- **US3 (Phase 5)**: Depends on Foundational only — can run in parallel with Phase 4
- **US4+US5 (Phase 6)**: Depends on Foundational (T004 shimmer) — can run in parallel with Phase 4 and Phase 5
- **US6+US7 (Phase 7)**: Depends on Phase 3 (MainScreen + navigation exist) — can run in parallel with Phase 5 and Phase 6
- **Polish (Phase 8)**: Depends on all previous phases

### User Story Dependencies

- **US1+US2 (P1)**: Start after Foundational — no dependencies on other stories
- **US8/Settings (P1)**: Start after US1+US2 — needs MainScreen and AppNavigation
- **US3 (P2)**: Start after Foundational — independent of US1/US2 (modifies only TemplateListItem)
- **US4+US5 (P2)**: Start after Foundational — independent of US3 (modifies TemplateListScreen/RecentJobsScreen loading behavior)
- **US6+US7 (P3)**: Start after US1+US2 — needs MainScreen for bell, AppNavigation for transitions
- **File conflict**: T012 (Settings) and T017 (Skeleton/Refresh) both modify `TemplateListScreen.kt` — T012 must complete first

### Build Sequence (from plan.md)

```
Phase 2 (Foundational)
    └── Phase 3 (US1+US2: Navigation Foundation) ← MVP
        ├── Phase 4 (Settings)
        ├── Phase 5 (US3: Enhanced Cards) ← parallel with 4, 6, 7
        ├── Phase 6 (US4+US5: Skeleton + Refresh) ← parallel with 4, 5, 7
        └── Phase 7 (US6+US7: Bell + Animations) ← parallel with 4, 5, 6
            └── Phase 8 (Polish)
```

### Parallel Opportunities

Within Phase 2 (Foundational):
```
T003 (PlaceholderScreen) || T004 (ShimmerModifier)  — different files, no dependencies
```

Within Phase 6 (US4+US5):
```
T014 (SkeletonCard) || T015 (TemplatesViewModel.refresh) || T016 (RecentJobsViewModel.refresh)
```

Within Phase 7 (US6+US7):
```
T019 (Bell handler) || T020 (Transition animations)
```

After Phase 3 completes (cross-story parallelism):
```
Phase 4 (Settings) || Phase 5 (US3) || Phase 6 (US4+US5) || Phase 7 (US6+US7)
```

---

## Parallel Example: Post-Navigation Foundation

```bash
# After Phase 3 (US1+US2) completes, launch 4 parallel streams:

# Stream 1: Settings (⚠️ T012 modifies TemplateListScreen.kt — must complete before Stream 3's T017)
Task: T010 "Create SettingsScreen in ui/settings/SettingsScreen.kt"
Task: T011 "Wire Settings route in navigation/AppNavigation.kt"
Task: T012 "Remove logout from TemplateListScreen.kt"

# Stream 2: Enhanced Cards
Task: T013 "Modify TemplateListItem.kt with improved card design"

# Stream 3: Skeleton + Refresh (⚠️ T017 modifies TemplateListScreen.kt — run after T012)
Task: T014 "Create SkeletonCard in ui/components/SkeletonCard.kt"     ← parallel
Task: T015 "Add refresh() to TemplatesViewModel.kt"                   ← parallel
Task: T016 "Add refresh() to RecentJobsViewModel.kt"                  ← parallel
Task: T017 "Modify TemplateListScreen.kt for skeleton + pull-to-refresh" ← after T012, T014, T015
Task: T018 "Modify RecentJobsScreen.kt for skeleton + pull-to-refresh"   ← after T014, T016

# Stream 4: Bell + Animations
Task: T019 "Add bell handler in MainScreen.kt"    ← parallel
Task: T020 "Add transitions in AppNavigation.kt"  ← parallel
Task: T021 "Add segment animation in MainScreen.kt" ← after T019 (same file)
```

---

## Implementation Strategy

### MVP First (User Stories 1+2 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: US1+US2 (Tab Navigation + Segments)
4. **STOP and VALIDATE**: Login → 3 tabs → segments → existing screens relocated → back stacks work → rotation preserves state
5. Build and test on device

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add US1+US2 → Test independently → **MVP!** (core navigation works)
3. Add Settings → Test → Logout accessible from gear icon
4. Add US3 → Test → Template cards look modern
5. Add US4+US5 → Test → Skeleton loading + pull-to-refresh on all lists
6. Add US6+US7 → Test → Bell placeholder + smooth animations
7. Polish → Full manual test → Ready for PR

### Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- File conflict between Stream 1 (T012) and Stream 3 (T017) on `TemplateListScreen.kt` — T012 must complete before T017
- Commit after each task or logical group
- Stop at any checkpoint to validate independently

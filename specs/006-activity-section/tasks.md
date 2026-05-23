# Tasks: Activity Section

**Input**: Design documents from `/specs/006-activity-section/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Not requested in the feature specification. Manual testing against live AAP 2.5+ instance.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Base path**: `app/src/main/kotlin/io/github/leogallego/ansiblejane/`

---

## Phase 1: Setup

**Purpose**: No project initialization needed — existing Android project. This phase covers shared data models used across multiple user stories.

- [X] T001 [P] Create Schedule data model with ScheduleSummaryFields and UnifiedJobTemplateRef in app/src/main/kotlin/io/github/leogallego/ansiblejane/model/Schedule.kt
- [X] T002 [P] Create EdaRuleAudit data model in app/src/main/kotlin/io/github/leogallego/ansiblejane/model/EdaRuleAudit.kt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Network layer and DI setup that MUST be complete before any user story can be implemented

**CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Create EdaApiService Retrofit interface for EDA /api/eda/v1/ endpoints in app/src/main/kotlin/io/github/leogallego/ansiblejane/network/EdaApiService.kt
- [X] T004 Add getEdaApiService() method to AapApiProvider in app/src/main/kotlin/io/github/leogallego/ansiblejane/network/AapApiProvider.kt
- [X] T005 Register EdaApiService in Koin networkModule in app/src/main/kotlin/io/github/leogallego/ansiblejane/network/NetworkModule.kt
- [X] T006 Add status filter query parameters (status, or__status) to getJobs() in app/src/main/kotlin/io/github/leogallego/ansiblejane/network/AapApiService.kt
- [X] T007 Add getSchedules() and toggleSchedule() endpoints to AapApiService in app/src/main/kotlin/io/github/leogallego/ansiblejane/network/AapApiService.kt

**Checkpoint**: Network layer ready — user story implementation can now begin

---

## Phase 3: User Story 1 — Browse and Filter Job History (Priority: P1) MVP

**Goal**: Enhance the existing Jobs list with status filter chips (multi-select, horizontal scrollable row) and relocate it under the Activity tab.

**Independent Test**: Navigate to Activity > Jobs, view job list, apply status filter chips, verify filtered results, clear filter, pull to refresh, tap job to see details.

### Implementation for User Story 1

- [X] T008 [US1] Add status filter parameter to getRecentJobs() in JobRepository in app/src/main/kotlin/io/github/leogallego/ansiblejane/data/JobRepository.kt
- [X] T009 [US1] Add filter state (activeFilters: Set<JobStatus>) and filter methods to RecentJobsViewModel in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/jobs/RecentJobsViewModel.kt
- [X] T010 [US1] Add activeFilters field to RecentJobsUiState in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/jobs/RecentJobsUiState.kt
- [X] T011 [US1] Create reusable StatusFilterChips composable in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/components/StatusFilterChips.kt
- [X] T012 [US1] Add filter chips row above job list in RecentJobsScreen in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/jobs/RecentJobsScreen.kt

**Checkpoint**: User Story 1 is fully functional — jobs list with multi-select status filtering works independently

---

## Phase 4: User Story 2 — View and Manage Schedules (Priority: P2)

**Goal**: Add a Schedules segment under Activity showing schedule list with name, template, next run time, enabled status, and toggle capability.

**Independent Test**: Navigate to Activity > Schedules, view schedule list with next run times, toggle a schedule on/off, verify toggle persists, test toggle failure revert, pull to refresh.

### Implementation for User Story 2

- [X] T013 [US2] Create ScheduleRepository with getSchedules() and toggleSchedule() in app/src/main/kotlin/io/github/leogallego/ansiblejane/data/ScheduleRepository.kt
- [X] T014 [US2] Register ScheduleRepository in Koin dataModule in app/src/main/kotlin/io/github/leogallego/ansiblejane/data/DataModule.kt
- [X] T015 [P] [US2] Create SchedulesUiState sealed interface in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/schedules/SchedulesUiState.kt
- [X] T016 [US2] Create SchedulesViewModel with pagination, pull-to-refresh, and toggle in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/schedules/SchedulesViewModel.kt
- [X] T017 [US2] Register SchedulesViewModel in Koin presentationModule in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/PresentationModule.kt
- [X] T018 [US2] Create SchedulesScreen composable with list, toggle switch, and pull-to-refresh in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/schedules/SchedulesScreen.kt
- [X] T019 [US2] Route Schedules segment to SchedulesScreen in MainNavigation in app/src/main/kotlin/io/github/leogallego/ansiblejane/navigation/MainNavigation.kt
- [X] T020 [US2] Mark Schedules segment as isImplemented = true in TabDefinitions in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/main/TabDefinitions.kt

**Checkpoint**: User Story 2 is fully functional — schedules list with toggle works independently

---

## Phase 5: User Story 3 — View EDA Rule Audit Events (Priority: P3)

**Goal**: Add an EDA Audit segment under Activity showing rule audit events with detail bottom sheet, handling EDA unavailability gracefully.

**Independent Test**: Navigate to Activity > EDA Audit, view rule audit events, tap event to see details in bottom sheet, test with EDA unavailable (shows empty state, not error), pull to refresh.

### Implementation for User Story 3

- [X] T021 [US3] Create EdaAuditRepository with getAuditRules() in app/src/main/kotlin/io/github/leogallego/ansiblejane/data/EdaAuditRepository.kt
- [X] T022 [US3] Register EdaAuditRepository in Koin dataModule in app/src/main/kotlin/io/github/leogallego/ansiblejane/data/DataModule.kt
- [X] T023 [P] [US3] Create EdaAuditUiState sealed interface in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/eda/EdaAuditUiState.kt
- [X] T024 [US3] Create EdaAuditViewModel with pagination and pull-to-refresh in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/eda/EdaAuditViewModel.kt
- [X] T025 [US3] Register EdaAuditViewModel in Koin presentationModule in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/PresentationModule.kt
- [X] T026 [P] [US3] Create EdaAuditDetailSheet bottom sheet composable in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/eda/EdaAuditDetailSheet.kt
- [X] T027 [US3] Create EdaAuditScreen composable with list, detail sheet, and pull-to-refresh in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/eda/EdaAuditScreen.kt
- [X] T028 [US3] Route EDA Audit segment to EdaAuditScreen in MainNavigation in app/src/main/kotlin/io/github/leogallego/ansiblejane/navigation/MainNavigation.kt
- [X] T029 [US3] Mark EDA Audit segment as isImplemented = true in TabDefinitions in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/main/TabDefinitions.kt

**Checkpoint**: User Story 3 is fully functional — EDA audit list with detail sheet works independently, graceful EDA unavailability handling

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T030 Verify empty states for all three segments (no jobs, no schedules, EDA not configured, no filter results) across all screens
- [X] T031 Verify EDA unavailability does not affect Jobs or Schedules segments (FR-011 graceful degradation)
- [X] T032 Run quickstart.md validation — manual walkthrough of all three segments end-to-end

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 models — BLOCKS all user stories
- **User Stories (Phases 3-5)**: All depend on Phase 2 completion
  - User stories can proceed in parallel (different files) or sequentially in priority order
- **Polish (Phase 6)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Phase 2 — no dependencies on other stories
- **User Story 2 (P2)**: Can start after Phase 2 — no dependencies on other stories
- **User Story 3 (P3)**: Can start after Phase 2 — no dependencies on other stories

### Within Each User Story

- Repository before ViewModel
- ViewModel before UI Screen
- UiState can be created in parallel with repository (different files)
- DI registration after component creation
- Navigation wiring after screen creation

### Parallel Opportunities

- T001 and T002 (models) can run in parallel
- T015 and T016 can partially overlap (UiState before ViewModel)
- T023 and T024 can partially overlap (UiState before ViewModel)
- T026 (detail sheet) can be built in parallel with T021-T024 (repository/ViewModel)
- Once Phase 2 completes, all three user stories can start in parallel

---

## Parallel Example: Phase 1

```
# Launch both model tasks together:
Task T001: "Create Schedule data model in model/Schedule.kt"
Task T002: "Create EdaRuleAudit data model in model/EdaRuleAudit.kt"
```

## Parallel Example: User Story 2 + User Story 3

```
# After Phase 2, both stories can start simultaneously:
Story 2: T013 → T014 → T015/T016 → T017 → T018 → T019/T020
Story 3: T021 → T022 → T023/T024 → T025 → T026/T027 → T028/T029
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (models)
2. Complete Phase 2: Foundational (network layer + DI)
3. Complete Phase 3: User Story 1 (job filtering)
4. **STOP and VALIDATE**: Test job list with status filter chips
5. Deploy/demo if ready

### Incremental Delivery

1. Phase 1 + Phase 2 → Foundation ready
2. Add User Story 1 → Test independently → MVP (filtered job history)
3. Add User Story 2 → Test independently → Schedule management
4. Add User Story 3 → Test independently → Full Activity section
5. Phase 6 → Polish and cross-cutting validation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- EDA model fields may need adjustment after live API verification (ignoreUnknownKeys = true handles this)

# Tasks: Workflow Templates Support

**Input**: Design documents from `/specs/004-workflow-templates/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: No automated tests — manual verification in Android Studio per project convention.

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)

## Phase Mapping

| Phase | User Story | Priority | Description |
|-------|-----------|----------|-------------|
| Phase 1 | — | — | Setup: shared models and API endpoints |
| Phase 2 | — | — | Foundational: repository, DI, navigation wiring |
| Phase 3 | US1 - Browse Workflow Templates | P1 | ViewModel, list screen, list item, segment routing |
| Phase 4 | US2 - Search and Filter | P1 | Search/filter in ViewModel (reuses existing UI components) |
| Phase 5 | US3 - Launch Workflow Template | P1 | Launch flow in ViewModel + navigation to status |
| Phase 6 | US4 - Monitor Workflow Job Status | P2 | Status ViewModel, status screen with sub-jobs |
| Phase 7 | US5 - Pull-to-Refresh | P2 | Refresh support in ViewModel + screen |
| Phase 8 | — | — | Polish: Activity tab integration, empty states, error handling |

---

## Phase 1: Setup (Shared Models & API)

**Purpose**: Create all data models and API endpoints needed across user stories

- [X] T001 [P] Create WorkflowJobTemplate model (with WorkflowTemplateSummaryFields) in app/src/main/kotlin/io/github/leogallego/ansiblejane/model/WorkflowJobTemplate.kt
- [X] T002 [P] Create WorkflowJob model (with WorkflowJobSummaryFields, WorkflowJobTemplateRef) in app/src/main/kotlin/io/github/leogallego/ansiblejane/model/WorkflowJob.kt
- [X] T003 [P] Create WorkflowNode model (with WorkflowNodeSummaryFields, WorkflowNodeJob) in app/src/main/kotlin/io/github/leogallego/ansiblejane/model/WorkflowNode.kt
- [X] T004 [P] Create WorkflowLaunchResponse model in app/src/main/kotlin/io/github/leogallego/ansiblejane/model/WorkflowLaunchResponse.kt
- [X] T005 Add workflow API endpoints (getWorkflowJobTemplates, launchWorkflowJob, getWorkflowJob, getWorkflowNodes) to app/src/main/kotlin/io/github/leogallego/ansiblejane/network/AapApiService.kt

**Note**: T005 depends on T001-T004 completing first (return types reference the new models). T001-T004 are parallel.

---

## Phase 2: Foundational (Repository, DI, Navigation)

**Purpose**: Core infrastructure that MUST complete before any user story work

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T006 Create WorkflowRepository with getWorkflowTemplates, launchWorkflow, getWorkflowJobStatus, pollWorkflowJobStatus, and getWorkflowNodes methods in app/src/main/kotlin/io/github/leogallego/ansiblejane/data/WorkflowRepository.kt
- [X] T007 Register WorkflowRepository in Koin dataModule in app/src/main/kotlin/io/github/leogallego/ansiblejane/data/DataModule.kt
- [X] T008 Add WORKFLOW_JOB_STATUS route (with workflowJobId argument) to app/src/main/kotlin/io/github/leogallego/ansiblejane/navigation/AppNavigation.kt

**Checkpoint**: Foundation ready — user story implementation can begin

---

## Phase 3: User Story 1 — Browse Workflow Templates (Priority: P1) 🎯 MVP

**Goal**: Users can switch to "Workflow Templates" segment and see a paginated list of workflow templates with name, description, and labels

**Independent Test**: Log in → Templates tab → tap "Workflow Templates" segment → verify list loads with skeleton placeholders, shows template cards with name/description/labels, and scrolls with pagination

### Implementation for User Story 1

- [X] T009 [P] [US1] Create WorkflowTemplatesUiState sealed interface (Idle, Loading, Success with templates/availableLabels/hasMore/isLoadingMore, Error) in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/workflows/WorkflowTemplatesUiState.kt
- [X] T010 [P] [US1] Create WorkflowTemplateListItem composable (ElevatedCard with name, description, label chips — mirrors TemplateListItem) in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/workflows/WorkflowTemplateListItem.kt
- [X] T011 [US1] Create WorkflowTemplatesViewModel with loadTemplates and loadMore methods, StateFlow<WorkflowTemplatesUiState> in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/workflows/WorkflowTemplatesViewModel.kt
- [X] T012 [US1] Register WorkflowTemplatesViewModel in Koin presentationModule in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/PresentationModule.kt
- [X] T013 [US1] Create WorkflowTemplateListScreen composable (LazyColumn with SkeletonCard loading, pagination via derivedStateOf, empty state) in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/workflows/WorkflowTemplateListScreen.kt
- [X] T014 [US1] Mark "Workflow Templates" segment as isImplemented=true in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/main/TabDefinitions.kt
- [X] T015 [US1] Route "Workflow Templates" segment to WorkflowTemplateListScreen in app/src/main/kotlin/io/github/leogallego/ansiblejane/navigation/MainNavigation.kt

**Checkpoint**: Workflow templates list is browsable with pagination. MVP complete.

---

## Phase 4: User Story 2 — Search and Filter (Priority: P1)

**Goal**: Users can search workflow templates by name and filter by label, matching the job templates experience

**Independent Test**: View workflow templates list → type in search bar → verify results filter → tap label chip → verify label filtering → clear filter → verify full list restores

**Depends on**: US1 (T011 WorkflowTemplatesViewModel, T013 WorkflowTemplateListScreen)

### Implementation for User Story 2

- [X] T016 [US2] Add search (with 300ms debounce) and filterByLabel methods to WorkflowTemplatesViewModel in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/workflows/WorkflowTemplatesViewModel.kt
- [X] T017 [US2] Add SearchBar and LabelChips components to WorkflowTemplateListScreen in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/workflows/WorkflowTemplateListScreen.kt

**Note**: T016 and T017 touch different files and can run in parallel, but T017 depends on T016's search/filterByLabel methods being available for the ViewModel call. Safer to run sequentially.

**Checkpoint**: Search and filter fully functional on workflow templates list.

---

## Phase 5: User Story 3 — Launch Workflow Template (Priority: P1)

**Goal**: Users can launch a workflow template with optional extra variables and navigate to the status screen

**Independent Test**: Find a workflow template with launch permission → tap play button → confirm dialog appears → confirm launch → verify navigation to workflow job status screen

**Depends on**: US1 (T011 WorkflowTemplatesViewModel, T013 WorkflowTemplateListScreen), Phase 2 (T008 WORKFLOW_JOB_STATUS route)

### Implementation for User Story 3

- [X] T018 [US3] Add LaunchState sealed interface and requestLaunch/confirmLaunch/cancelLaunch/resetLaunchState methods to WorkflowTemplatesViewModel in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/workflows/WorkflowTemplatesViewModel.kt
- [X] T019 [US3] Add launch button to WorkflowTemplateListItem (show only when userCapabilities.start is true) in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/workflows/WorkflowTemplateListItem.kt
- [X] T020 [US3] Add launch dialogs (LaunchConfirmDialog, ExtraVarsDialog), launch state handling, and onNavigateToWorkflowJobStatus callback to WorkflowTemplateListScreen in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/workflows/WorkflowTemplateListScreen.kt
- [X] T021 [US3] Wire WORKFLOW_JOB_STATUS route to WorkflowJobStatusScreen (placeholder initially) and update onNavigateToWorkflowJobStatus in navigation in app/src/main/kotlin/io/github/leogallego/ansiblejane/navigation/AppNavigation.kt
- [X] T031 [US3] Add error snackbar for workflow template launch failures in WorkflowTemplateListScreen in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/workflows/WorkflowTemplateListScreen.kt

**Checkpoint**: Full launch flow works — template → dialog → confirm → navigates to status route. Launch errors display snackbar.

---

## Phase 6: User Story 4 — Monitor Workflow Job Status (Priority: P2)

**Goal**: Users see workflow job status with auto-polling, sub-job list, and completion details

**Independent Test**: Launch a workflow template → verify status screen shows job name, status badge, template name, timestamps → verify auto-updates while running → verify sub-jobs list with names and statuses → verify polling stops on completion

**Depends on**: Phase 2 (T006 WorkflowRepository polling methods, T008 route)

### Implementation for User Story 4

- [X] T022 [P] [US4] Create WorkflowJobStatusUiState sealed interface (Idle, Loading, Active with workflowJob/nodes, Completed with workflowJob/nodes, Error) in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/workflows/WorkflowJobStatusUiState.kt
- [X] T023 [P] [US4] Create WorkflowNodeItem composable (Row with node name and JobStatusBadge) in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/components/WorkflowNodeItem.kt
- [X] T024 [US4] Create WorkflowJobStatusViewModel with pollWorkflowJob (5s interval, fetches job + nodes in parallel), retry, and stopPolling methods in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/workflows/WorkflowJobStatusViewModel.kt
- [X] T025 [US4] Register WorkflowJobStatusViewModel in Koin presentationModule in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/PresentationModule.kt
- [X] T026 [US4] Create WorkflowJobStatusScreen composable (job name, status badge, template name, start/finish/elapsed times, sub-jobs LazyColumn with WorkflowNodeItem, back navigation) in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/workflows/WorkflowJobStatusScreen.kt
- [X] T027 [US4] Replace placeholder in WORKFLOW_JOB_STATUS route with actual WorkflowJobStatusScreen in app/src/main/kotlin/io/github/leogallego/ansiblejane/navigation/AppNavigation.kt

**Checkpoint**: Full workflow job monitoring works with auto-polling and sub-job display.

---

## Phase 7: User Story 5 — Pull-to-Refresh (Priority: P2)

**Goal**: Users can pull down on the workflow templates list to refresh data

**Independent Test**: View workflow templates list → pull down → verify refresh indicator appears → verify list reloads → verify error handling on network failure

**Depends on**: US1 (T011 WorkflowTemplatesViewModel, T013 WorkflowTemplateListScreen)

### Implementation for User Story 5

- [X] T028 [US5] Add refresh method to WorkflowTemplatesViewModel in app/src/main/kotlin/io/github/leogallego/ansiblejane/presentation/workflows/WorkflowTemplatesViewModel.kt
- [X] T029 [US5] Wrap list content in PullToRefreshBox with isRefreshing state tracking in WorkflowTemplateListScreen in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/workflows/WorkflowTemplateListScreen.kt

**Checkpoint**: Pull-to-refresh works on workflow templates list.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final integration, error handling, and consistency checks

- [X] T030 Verify workflow jobs appear in Activity > Jobs list (AAP /api/v2/jobs/ should include them) — manual verification. If workflow jobs do not appear, check whether the jobs repository filters by type and update the API query to include workflow job types.
- [X] T032 Add retry option on WorkflowJobStatusScreen error state in app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/workflows/WorkflowJobStatusScreen.kt
- [X] T033 Run quickstart.md validation — verify all 5 integration scenarios work end-to-end

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (T001-T005) — BLOCKS all user stories
- **User Stories (Phase 3-7)**: All depend on Phase 2 completion
  - US1 (Browse) → US2 (Search/Filter) → US3 (Launch): Sequential dependency chain
  - US4 (Status): Depends on Phase 2 only — can run in parallel with US1-US3
  - US5 (Pull-to-Refresh): Depends on US1 (uses ViewModel/Screen)
- **Polish (Phase 8)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (Browse)**: After Phase 2 — no other story dependencies
- **US2 (Search/Filter)**: After US1 — extends ViewModel and Screen created in US1
- **US3 (Launch)**: After US1 — extends ViewModel and Screen, plus needs Phase 2 route
- **US4 (Status)**: After Phase 2 — independent of US1-US3 (different files)
- **US5 (Pull-to-Refresh)**: After US1 — extends ViewModel and Screen

### Within Each User Story

- UiState/models before ViewModel
- ViewModel before Screen
- Screen before navigation wiring

### File Conflict Warnings

- **WorkflowTemplatesViewModel.kt**: Modified by US1 (T011), US2 (T016), US3 (T018), US5 (T028) — MUST be sequential
- **WorkflowTemplateListScreen.kt**: Modified by US1 (T013), US2 (T017), US3 (T020), US5 (T029), Polish (T031) — MUST be sequential
- **WorkflowTemplateListItem.kt**: Modified by US1 (T010), US3 (T019) — MUST be sequential
- **PresentationModule.kt**: Modified by US1 (T012), US4 (T025) — MUST be sequential
- **AppNavigation.kt**: Modified by Phase 2 (T008), US3 (T021), US4 (T027) — MUST be sequential

### Parallel Opportunities

```text
Phase 1: T001 ‖ T002 ‖ T003 ‖ T004  (4 model files in parallel)
          └── T005 (API service, depends on all models)

Phase 3 ‖ Phase 6 (partially):
  T009 ‖ T010 ‖ T022 ‖ T023  (UiStates + UI components in parallel)

Phase 6: T022 ‖ T023  (UiState and NodeItem in parallel)
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Models + API endpoints
2. Complete Phase 2: Repository + DI + Route
3. Complete Phase 3: Browse workflow templates
4. **STOP and VALIDATE**: Template list loads, shows cards, paginates

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 (Browse) → Skeleton loading, template cards, pagination (MVP!)
3. US2 (Search/Filter) → Search bar and label chips work
4. US3 (Launch) → Launch dialog flow, navigates to status
5. US4 (Status) → Workflow job monitoring with sub-jobs
6. US5 (Pull-to-Refresh) → Standard refresh interaction
7. Polish → Activity tab verification, error handling

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- Shared UI components (SearchBar, LabelChips, SkeletonCard, LaunchConfirmDialog, ExtraVarsDialog, JobStatusBadge) are reused — no new shared component creation needed
- WorkflowNodeItem is the only new shared component (Phase 6)
- The existing LaunchRequest model is reused for workflow launches
- The existing JobStatus enum is reused for workflow job/node statuses
- Commit after each phase or logical group

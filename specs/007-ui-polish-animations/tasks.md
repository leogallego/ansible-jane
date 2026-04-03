# Tasks: UI Polish — Animations and Micro-Interactions

**Input**: Design documents from `/specs/007-ui-polish-animations/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: No test tasks — not requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the new shared components that multiple user stories depend on

- [x] T001 [P] Create AppError sealed class with from(Throwable) factory and ErrorDetail data class in `app/src/main/kotlin/com/example/aapremote/model/AppError.kt`
- [x] T002 [P] Create Modifier.pressScale() extension function with InteractionSource-based press detection and spring animation in `app/src/main/kotlin/com/example/aapremote/ui/components/PressScaleModifier.kt`
- [x] T003 [P] Create AapIcons object with Status, Error, Navigation, and Action icon groups in `app/src/main/kotlin/com/example/aapremote/ui/icons/AapIcons.kt`
- [x] T004 [P] Create StatusColors data class and LocalStatusColors CompositionLocal in `app/src/main/kotlin/com/example/aapremote/ui/theme/StatusColors.kt`
- [x] T005 [P] Create Flow.asResult() extension for wrapping repository suspend calls into Loading/Success/Error flow in `app/src/main/kotlin/com/example/aapremote/data/ResultExtensions.kt`
- [x] T006 Update AapRemoteTheme to provide StatusColors via CompositionLocalProvider and add AapRemoteTheme.statusColors accessor in `app/src/main/kotlin/com/example/aapremote/ui/theme/Theme.kt`
- [x] T007 Remove top-level Status* color constants from Color.kt and update any remaining direct references in `app/src/main/kotlin/com/example/aapremote/ui/theme/Color.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Update repositories and UiState definitions so all user stories can consume typed errors. Refactor ViewModels to use asResult() + AppError.from(). Migrate icon/color references to centralized registries.

**CRITICAL**: No user story work can begin until this phase is complete

### Batch 1: Repositories (propagate raw exceptions)

- [x] T008 [P] Update TemplateRepository to propagate raw exceptions instead of wrapping in generic Exception(message) in `app/src/main/kotlin/com/example/aapremote/data/TemplateRepository.kt`
- [x] T009 [P] Update WorkflowRepository to propagate raw exceptions in `app/src/main/kotlin/com/example/aapremote/data/WorkflowRepository.kt`
- [x] T010 [P] Update JobRepository to propagate raw exceptions in `app/src/main/kotlin/com/example/aapremote/data/JobRepository.kt`
- [x] T011 [P] Update ScheduleRepository to propagate raw exceptions in `app/src/main/kotlin/com/example/aapremote/data/ScheduleRepository.kt`
- [x] T012 [P] Update EdaAuditRepository to propagate raw exceptions in `app/src/main/kotlin/com/example/aapremote/data/EdaAuditRepository.kt`
- [x] T013 [P] Update AuthRepository to propagate raw exceptions in `app/src/main/kotlin/com/example/aapremote/data/AuthRepository.kt`

### Batch 2: UiState definitions (String → AppError)

- [x] T014 [P] Update TemplatesUiState.Error and LaunchState.LaunchError from String to AppError in `app/src/main/kotlin/com/example/aapremote/presentation/templates/TemplatesUiState.kt`
- [x] T015 [P] Update WorkflowTemplatesUiState.Error and WorkflowLaunchState.LaunchError from String to AppError in `app/src/main/kotlin/com/example/aapremote/presentation/workflows/WorkflowTemplatesUiState.kt`
- [x] T016 [P] Update WorkflowJobStatusUiState.Error from String to AppError in `app/src/main/kotlin/com/example/aapremote/presentation/workflows/WorkflowJobStatusUiState.kt`
- [x] T017 [P] Update JobStatusUiState.Error and RecentJobsUiState.Error from String to AppError in `app/src/main/kotlin/com/example/aapremote/presentation/jobs/JobUiState.kt`
- [x] T018 [P] Update SchedulesUiState.Error from String to AppError in `app/src/main/kotlin/com/example/aapremote/presentation/schedules/SchedulesUiState.kt`
- [x] T019 [P] Update EdaAuditUiState.Error from String to AppError in `app/src/main/kotlin/com/example/aapremote/presentation/eda/EdaAuditUiState.kt`
- [x] T020 [P] Update AuthUiState.Error from String to AppError in `app/src/main/kotlin/com/example/aapremote/presentation/auth/AuthUiState.kt`

### Batch 3: ViewModels (use asResult() + AppError.from())

- [x] T021 Update TemplatesViewModel to use asResult() and AppError.from(throwable) when mapping errors in `app/src/main/kotlin/com/example/aapremote/presentation/templates/TemplatesViewModel.kt`
- [x] T022 [P] Update WorkflowTemplatesViewModel to use asResult() and AppError.from(throwable) in `app/src/main/kotlin/com/example/aapremote/presentation/workflows/WorkflowTemplatesViewModel.kt`
- [x] T023 [P] Update JobStatusViewModel to use asResult() and AppError.from(throwable) in `app/src/main/kotlin/com/example/aapremote/presentation/jobs/JobStatusViewModel.kt`
- [x] T024 [P] Update RecentJobsViewModel to use asResult() and AppError.from(throwable) in `app/src/main/kotlin/com/example/aapremote/presentation/jobs/RecentJobsViewModel.kt`
- [x] T025 [P] Update SchedulesViewModel to use asResult() and AppError.from(throwable) in `app/src/main/kotlin/com/example/aapremote/presentation/schedules/SchedulesViewModel.kt`
- [x] T026 [P] Update EdaAuditViewModel to use asResult() and AppError.from(throwable) in `app/src/main/kotlin/com/example/aapremote/presentation/eda/EdaAuditViewModel.kt`
- [x] T027 [P] Update AuthViewModel to use asResult() and AppError.from(throwable) in `app/src/main/kotlin/com/example/aapremote/presentation/auth/AuthViewModel.kt`
- [x] T027b [P] Update WorkflowJobStatusViewModel to use asResult() and AppError.from(throwable) in `app/src/main/kotlin/com/example/aapremote/presentation/workflows/WorkflowJobStatusViewModel.kt`

### Batch 4: Migrate icon/color references

- [x] T028 Update JobStatusBadge to use AapIcons.Status.* and AapRemoteTheme.statusColors.* instead of inline icon/color references in `app/src/main/kotlin/com/example/aapremote/ui/components/JobStatusBadge.kt`

**Checkpoint**: All repositories propagate raw exceptions, all UiState Error variants use AppError, all ViewModels use asResult() + AppError.from(), icons and status colors use centralized registries. App will not compile yet — screens still reference old Error(message) pattern.

---

## Phase 3: User Story 1 — Tactile Card Press Feedback (Priority: P1) MVP

**Goal**: All clickable list cards show a press-scale animation on touch

**Independent Test**: Tap any card in any list screen and observe scale-down animation. Enable "Remove animations" accessibility and verify cards still work without animation.

### Implementation for User Story 1

- [x] T029 [P] [US1] Apply Modifier.pressScale() to ElevatedCard in `app/src/main/kotlin/com/example/aapremote/ui/templates/TemplateListItem.kt`
- [x] T030 [P] [US1] Apply Modifier.pressScale() to ElevatedCard in `app/src/main/kotlin/com/example/aapremote/ui/workflows/WorkflowTemplateListItem.kt`
- [x] T031 [P] [US1] Apply Modifier.pressScale() to Card (RecentJobItem) in `app/src/main/kotlin/com/example/aapremote/ui/jobs/RecentJobsScreen.kt`
- [x] T032 [P] [US1] Apply Modifier.pressScale() to Card (EdaAuditItem) in `app/src/main/kotlin/com/example/aapremote/ui/eda/EdaAuditScreen.kt`

**Checkpoint**: All clickable cards across all list screens respond to press with a visible scale animation

---

## Phase 4: User Story 2 — Informative Error Display (Priority: P1)

**Goal**: Error states show typed icons, titles, expandable details, and slide-in entrance animation instead of generic string messages

**Independent Test**: Toggle airplane mode → see Network Error with wifi-off icon sliding in. Use expired token → see Auth Error with lock icon. Tap "Show details" → see status code and URL.

### Implementation for User Story 2

- [x] T033 [US2] Refactor ErrorMessage composable to accept AppError with typed icon (from AapIcons.Error.*), title, message, expandable detail section, retry button, and AnimatedVisibility entrance (fadeIn + slideInVertically) in `app/src/main/kotlin/com/example/aapremote/ui/components/ErrorMessage.kt`
- [x] T034 [P] [US2] Update TemplateListScreen to pass AppError to ErrorMessage in `app/src/main/kotlin/com/example/aapremote/ui/templates/TemplateListScreen.kt`
- [x] T035 [P] [US2] Update WorkflowTemplateListScreen to pass AppError to ErrorMessage in `app/src/main/kotlin/com/example/aapremote/ui/workflows/WorkflowTemplateListScreen.kt`
- [x] T036 [P] [US2] Update WorkflowJobStatusScreen to pass AppError to ErrorMessage in `app/src/main/kotlin/com/example/aapremote/ui/workflows/WorkflowJobStatusScreen.kt`
- [x] T037 [P] [US2] Update RecentJobsScreen to pass AppError to ErrorMessage in `app/src/main/kotlin/com/example/aapremote/ui/jobs/RecentJobsScreen.kt`
- [x] T038 [P] [US2] Update JobStatusScreen to pass AppError to ErrorMessage in `app/src/main/kotlin/com/example/aapremote/ui/jobs/JobStatusScreen.kt`
- [x] T039 [P] [US2] Update SchedulesScreen to pass AppError to ErrorMessage in `app/src/main/kotlin/com/example/aapremote/ui/schedules/SchedulesScreen.kt`
- [x] T040 [P] [US2] Update EdaAuditScreen to pass AppError to ErrorMessage in `app/src/main/kotlin/com/example/aapremote/ui/eda/EdaAuditScreen.kt`
- [x] T041 [P] [US2] Update AuthScreen to pass AppError to ErrorMessage in `app/src/main/kotlin/com/example/aapremote/ui/auth/AuthScreen.kt`

**Checkpoint**: All error states across all screens show typed error display with icons, expandable details, and slide-in entrance. App should now compile and run.

---

## Phase 5: User Story 3 — Smooth Dialog Entrances (Priority: P2)

**Goal**: All dialogs and bottom sheets enter with a spring animation

**Independent Test**: Tap Launch on any template → confirmation dialog springs in. Tap an EDA audit rule → bottom sheet slides up with spring effect. Enable "Remove animations" → dialogs appear instantly.

### Implementation for User Story 3

- [x] T042 [P] [US3] Add spring entrance animation to LaunchConfirmDialog in `app/src/main/kotlin/com/example/aapremote/ui/components/LaunchConfirmDialog.kt`
- [x] T043 [P] [US3] Add spring entrance animation to ExtraVarsDialog in `app/src/main/kotlin/com/example/aapremote/ui/components/ExtraVarsInput.kt`
- [x] T044 [P] [US3] Add spring entrance animation to EdaAuditDetailSheet (ModalBottomSheet) in `app/src/main/kotlin/com/example/aapremote/ui/eda/EdaAuditDetailSheet.kt`

**Checkpoint**: All dialogs and bottom sheets enter with spring animation

---

## Phase 6: User Story 4 — Animated App Bar Title (Priority: P3)

**Goal**: App bar title crossfades when switching between tabs

**Independent Test**: Tap between Templates, Infrastructure, and Activity tabs → title crossfades smoothly. Enable "Remove animations" → title changes instantly.

### Implementation for User Story 4

- [x] T045 [US4] Replace static Text("AAPdroid") title with AnimatedContent keyed on selectedTab.label in `app/src/main/kotlin/com/example/aapremote/ui/main/MainScreen.kt`

**Checkpoint**: Tab title transitions are smooth

---

## Phase 7: User Story 5 — Breathing Pulse on Running Jobs (Priority: P3)

**Goal**: Running job status badge pulses with a subtle scale animation

**Independent Test**: Launch a job → view its status badge while running → observe pulse. When job completes → pulse stops. Enable "Remove animations" → no pulse, static badge.

### Implementation for User Story 5

- [x] T046 [US5] Add breathing pulse animation (rememberInfiniteTransition, scale 0.96-1.04) to JobStatusBadge when status is RUNNING, with reduce-motion check in `app/src/main/kotlin/com/example/aapremote/ui/components/JobStatusBadge.kt`

**Checkpoint**: Running job status badges pulse, stop on completion

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Final accessibility verification and cleanup

- [x] T047 Verify all animations respect reduce-motion accessibility setting across all modified components
- [x] T048 Run quickstart.md validation scenarios end-to-end (all error types, all card screens, all dialogs, tab switching, running job pulse)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — T001-T005 run in parallel, then T006-T007 sequentially (depend on T004)
- **Phase 2 (Foundational)**: Depends on T001 (AppError), T003 (AapIcons), T004/T006 (StatusColors), T005 (asResult). Batch 1 (repos T008-T013) in parallel. Batch 2 (UiStates T014-T020) in parallel. Batch 3 (ViewModels T021-T027) in parallel. Batch 4 (T028 icon/color migration) after Batch 3.
- **Phase 3 (US1 Cards)**: Depends on T002 (PressScaleModifier). All T029-T032 run in parallel.
- **Phase 4 (US2 Errors)**: Depends on Phase 2 completion (all ViewModels emit AppError). T033 first, then T034-T041 in parallel.
- **Phase 5 (US3 Dialogs)**: No dependencies on other stories. T042-T044 run in parallel.
- **Phase 6 (US4 Title)**: No dependencies on other stories.
- **Phase 7 (US5 Pulse)**: Depends on T028 (icon/color migration in JobStatusBadge).
- **Phase 8 (Polish)**: Depends on all stories being complete.

### User Story Dependencies

- **US1 (Cards)**: Independent — only needs T002 from Setup
- **US2 (Errors)**: Needs entire Phase 2 (Foundational) — the heaviest dependency chain
- **US3 (Dialogs)**: Independent — no dependencies on other stories or phases beyond Setup
- **US4 (Title)**: Independent — self-contained in MainScreen
- **US5 (Pulse)**: Needs T028 (JobStatusBadge icon/color migration)

### Parallel Opportunities

- **Phase 1**: T001-T005 all in parallel, then T006-T007
- **Phase 2**: Batch 1 (T008-T013) in parallel, then Batch 2 (T014-T020) in parallel, then Batch 3 (T021-T027) in parallel, then T028
- **Phase 3**: T029-T032 all in parallel
- **Phase 4**: T034-T041 all in parallel (after T033)
- **Phase 5**: T042-T044 all in parallel
- **US1 + US3 + US4**: Can all run in parallel since they touch different files

---

## Parallel Example: Phase 1 Setup

```bash
# All setup tasks in parallel:
Task: "T001 Create AppError sealed class"
Task: "T002 Create Modifier.pressScale()"
Task: "T003 Create AapIcons object"
Task: "T004 Create StatusColors + LocalStatusColors"
Task: "T005 Create Flow.asResult() extension"
# Then sequentially:
Task: "T006 Update AapRemoteTheme to provide StatusColors"
Task: "T007 Remove top-level Status* constants from Color.kt"
```

## Parallel Example: Phase 2 Foundational

```bash
# Batch 1: All repositories in parallel (T008-T013)
Task: "Update TemplateRepository to propagate raw exceptions"
Task: "Update WorkflowRepository to propagate raw exceptions"
# ... etc

# Batch 2: All UiState definitions in parallel (T014-T020)
Task: "Update TemplatesUiState.Error from String to AppError"
# ... etc

# Batch 3: All ViewModels in parallel (T021-T027)
Task: "Update TemplatesViewModel to use asResult() + AppError.from()"
# ... etc

# Batch 4: Icon/color migration
Task: "T028 Update JobStatusBadge to use AapIcons + StatusColors"
```

## Parallel Example: Independent Stories

```bash
# These stories can run simultaneously (different files):
Task: "T029-T032 [US1] Apply press-scale to all cards"
Task: "T042-T044 [US3] Add spring animation to all dialogs"
Task: "T045 [US4] Animate app bar title"
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2)

1. Complete Phase 1: Setup (T001-T007)
2. Complete Phase 2: Foundational (T008-T028) — biggest phase
3. Complete Phase 3: User Story 1 — Cards (T029-T032)
4. Complete Phase 4: User Story 2 — Errors (T033-T041)
5. **STOP and VALIDATE**: App compiles, all cards have press feedback, all errors are typed, icons/colors centralized

### Incremental Delivery

1. Setup + Foundational → Error pipeline + design system foundations ready
2. Add US1 (Cards) + US2 (Errors) → Core polish complete (MVP)
3. Add US3 (Dialogs) → Dialog animations
4. Add US4 (Title) + US5 (Pulse) → Final polish
5. Polish phase → Accessibility verification

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Phase 1 is now 7 tasks (was 2) — adds AapIcons, StatusColors, asResult()
- Phase 2 is the largest phase (21 tasks) but highly parallelizable in 4 batches
- US1, US3, US4 are all independent and can run in any order after Setup
- US2 requires Phase 2 completion (the only story with a foundational dependency)
- US5 depends on T028 (JobStatusBadge icon/color migration)
- Commit after each phase checkpoint

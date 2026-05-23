# Tasks: Multi-AAP Instance Support

**Input**: Design documents from `/specs/009-multi-instance-support/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, quickstart.md

**Tests**: Manual only (no automated test framework configured per plan.md).

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- All paths relative to `app/src/main/kotlin/io/github/leogallego/ansiblejane/`

---

## Phase 1: Setup

**Purpose**: Create the new model and state files needed across all user stories

- [x] T001 [P] Create `AapInstance` data class with all fields (id, baseUrl, token, alias, apiVersion, trustSelfSigned, certFingerprint) and display label logic in `model/AapInstance.kt`
- [x] T002 [P] Create `SettingsUiState` sealed interface (Idle, Loading, Success with instances list and selectedInstance, Error) in `presentation/settings/SettingsUiState.kt`

**Checkpoint**: New model and state files created, project compiles

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Refactor core data and network layers from single-instance to multi-instance architecture

**CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 Define serialization models in `data/TokenManager.kt` — add `SerializedInstance` and `InstancesState` data classes with `@Serializable` annotations. Define the new DataStore key `instances_json` to replace flat credential keys (`base_url`, `token`, `api_version`, `trust_self_signed`, `cert_fingerprint`)
- [x] T004 Implement `TokenManager` core methods in `data/TokenManager.kt` — add `saveInstance()`, `removeInstance()`, `setActiveInstance()`, `getInstanceById()`. Each method reads `instances_json`, deserializes, mutates, re-serializes, and writes back. Per-instance Tink AES-256-GCM encryption for URL and token fields (encrypt on save, decrypt on read)
- [x] T005 Expose reactive state from `TokenManager` in `data/TokenManager.kt` — add `instances: StateFlow<List<AapInstance>>` and `activeInstance: StateFlow<AapInstance?>`. Initialize from DataStore on construction, update after every save/remove/setActive operation
- [x] T006 Add legacy credential cleanup in `data/TokenManager.kt` — on first read of `instances_json` (when key is absent), detect and clear old flat DataStore keys (`base_url`, `token`, `api_version`, `trust_self_signed`, `cert_fingerprint`). No migration — user must re-authenticate (R-005)
- [x] T007 [P] Refactor `AapApiProvider` in `network/AapApiProvider.kt` — change from single cached service to `Map<String, Pair<AapApiService, EdaApiService>>` keyed by instance ID. `getApiService()` and `getEdaApiService()` resolve active instance from `TokenManager.activeInstance`. Evict cache entry on instance removal (R-002)
- [x] T008 [P] Refactor `AuthInterceptor` in `network/AuthInterceptor.kt` — change `unauthorizedEvent` from `SharedFlow<Unit>` to `SharedFlow<String>` (emitting instance ID). Read token from `TokenManager.activeInstance` instead of flat storage. Update existing 401 observer in `AppNavigation` to accept `String` and maintain current global-logout behavior as a backward-compatible stub until T023 wires per-instance re-auth (R-004)
- [x] T009 Update `AuthRepository` in `data/AuthRepository.kt` — add `alias` parameter to `validateCredentials()`. Add `reAuthenticate(instanceId, newToken)`. Replace global `logout()` with `logoutInstance(instanceId)`. Use `TokenManager.saveInstance()` instead of flat token save
- [x] T010 Update Koin DI modules — in `data/DataModule.kt`: update `TokenManager` singleton to pass Tink `Aead` and `DataStore` dependencies with new constructor signature. In `network/NetworkModule.kt`: update `AapApiProvider` singleton to inject `TokenManager` for active instance resolution. Add `SettingsViewModel` factory to `presentation` module

**Checkpoint**: Foundation ready — multi-instance storage, API caching, and auth all work at the data layer. Existing 401 handling preserved via backward-compat stub.

---

## Phase 3: User Story 1 — Add a New AAP Instance (Priority: P1) MVP

**Goal**: Users can connect to multiple AAP instances by entering URL, token, and optional alias. Each instance appears as a pill in Settings.

**Independent Test**: Add two instances and verify both appear as pills in Settings with correct labels (alias + URL or URL only).

### Implementation for User Story 1

- [x] T011 [US1] Update `AuthViewModel` in `presentation/auth/AuthViewModel.kt` — add `alias` parameter to `connect()` method. Pass alias through to `AuthRepository.validateCredentials()`. Support pre-fill mode (URL + alias provided via nav args) for re-auth flow
- [x] T012 [US1] Update `AuthUiState` in `presentation/auth/AuthUiState.kt` — add re-auth mode flag and pre-filled instance ID if needed
- [x] T013 [US1] Update `AuthScreen` in `ui/auth/AuthScreen.kt` — add optional alias `OutlinedTextField` below the URL field. Wire alias value to `AuthViewModel.connect()`. Support pre-filled URL and alias when navigating for re-auth
- [x] T014 [US1] Create `SettingsViewModel` in `presentation/settings/SettingsViewModel.kt` — observe `TokenManager.instances` and `TokenManager.activeInstance`. Expose `SettingsUiState` via `StateFlow`. Implement `addInstance()` navigation trigger
- [x] T015 [US1] Update `SettingsScreen` in `ui/settings/SettingsScreen.kt` — add Instances section with `FlowRow` (from `androidx.compose.foundation.layout`) of pills/chips. Each pill shows alias (primary, max ~20 chars with ellipsis) + URL hostname (secondary, truncated with ellipsis) or URL hostname only when no alias. Active pill has filled indicator dot, inactive has hollow dot. Add "+ Add Instance" button/chip that navigates to auth screen. Note: pill always displays URL per FR-008; top bar (T021) uses different logic (alias OR hostname)
- [x] T016 [US1] Update `AppNavigation` in `navigation/AppNavigation.kt` — add route parameters for add-instance flow (from Settings → Auth → back to Settings). Handle nav args for pre-filled URL/alias in re-auth mode
- [x] T017 [US1] Add duplicate URL prevention — when saving a new instance in `TokenManager.saveInstance()`, normalize URL (case-insensitive, strip trailing slash) and reject if duplicate exists (FR-010)

**Checkpoint**: User Story 1 fully functional — users can add multiple instances and see them in Settings

---

## Phase 4: User Story 2 — Switch Between Instances (Priority: P1)

**Goal**: Users can tap an inactive instance pill to switch the active instance, triggering a full data refresh across the app. Per-instance 401 handling ensures token expiry on one instance doesn't affect others.

**Independent Test**: Connect two instances, switch between them, verify that templates/jobs/infrastructure data changes to match the selected instance.

### Implementation for User Story 2

- [x] T018 [US2] Implement `switchInstance(instanceId)` in `SettingsViewModel` in `presentation/settings/SettingsViewModel.kt` — call `TokenManager.setActiveInstance()`, handle loading state during switch
- [x] T019 [US2] Update all data ViewModels to observe `TokenManager.activeInstance` and re-fetch data on change. Files to modify: `presentation/templates/TemplateListViewModel.kt`, `presentation/jobs/JobListViewModel.kt`, `presentation/workflows/WorkflowTemplateListViewModel.kt`, `presentation/schedules/ScheduleListViewModel.kt`, `presentation/infrastructure/InventoryListViewModel.kt`, `presentation/infrastructure/HostListViewModel.kt`
- [x] T020 [US2] Update `SettingsScreen` in `ui/settings/SettingsScreen.kt` — wire inactive pill tap to `SettingsViewModel.switchInstance()`. Active/inactive visual indicators are already rendered from T015; this task only adds the tap handler for inactive pills
- [x] T021 [US2] Update `MainScreen` in `ui/main/MainScreen.kt` — observe `TokenManager.activeInstance` and display active instance label as subtitle in the top bar (FR-013). Top bar label logic: show alias if set, otherwise show hostname extracted from URL. Note: this differs from pill label logic (T015) where both alias and URL are always shown
- [x] T022 [US2] Implement request cancellation on instance switch — in each data ViewModel (T019 files), cancel the active `viewModelScope` coroutine job before launching a new fetch when `activeInstance` changes. Coroutine cancellation propagates to Retrofit calls via cooperative cancellation (Retrofit suspending functions check `isActive`), so no explicit `Call.cancel()` is needed (FR-012)
- [x] T023 [US2] Wire per-instance 401 re-auth in `AppNavigation` — replace the backward-compat stub from T008. Observe `AuthInterceptor.unauthorizedEvent` (emitting instance ID), navigate to auth screen pre-filled with that instance's URL and alias for re-authentication without removing the instance. Only the affected instance prompts re-auth; other instances remain functional (FR-011)

**Checkpoint**: User Story 2 fully functional — switching instances refreshes all data, top bar updates, and 401 re-auth is scoped per instance

---

## Phase 5: User Story 3 — View Instance Details (Priority: P2)

**Goal**: Users can tap the active instance pill to see a bottom sheet with instance details (URL, alias, API version, certificate trust status).

**Independent Test**: Tap an active instance pill and verify the bottom sheet shows correct details.

### Implementation for User Story 3

- [x] T024 [US3] Add `selectedInstanceForDetails` field to `SettingsUiState` in `presentation/settings/SettingsUiState.kt` — nullable `AapInstance` for bottom sheet display
- [x] T025 [US3] Add `showInstanceDetails(instanceId)` and `dismissDetails()` methods to `SettingsViewModel` in `presentation/settings/SettingsViewModel.kt`
- [x] T026 [US3] Implement instance details `ModalBottomSheet` in `ui/settings/SettingsScreen.kt` — show URL, alias (if set), API version, self-signed certificate trust status. Dismiss on swipe down or tap outside. Wire active pill tap to show details instead of switch

**Checkpoint**: User Story 3 fully functional — tapping active pill shows details bottom sheet

---

## Phase 6: User Story 4 — Remove a Specific Instance (Priority: P2)

**Goal**: Users can remove any instance via the "x" button on its pill. Credentials are deleted and other instances remain unaffected.

**Independent Test**: Remove one instance and verify the other remains connected and functional.

### Implementation for User Story 4

- [x] T027 [US4] Implement `removeInstance(instanceId)` in `SettingsViewModel` in `presentation/settings/SettingsViewModel.kt` — call `TokenManager.removeInstance()`. Handle auto-promotion (if active instance removed, next instance becomes active). Handle last-instance removal (navigate to auth screen) (FR-004, FR-005)
- [x] T028 [US4] Add "x" (close/remove) icon button to each instance pill in `ui/settings/SettingsScreen.kt` — wire to `SettingsViewModel.removeInstance()`. Ensure `AapApiProvider` cache entry is evicted for the removed instance
- [x] T029 [US4] Update `AppNavigation` in `navigation/AppNavigation.kt` — handle navigation to auth screen when last instance is removed. Ensure back stack is cleared so user cannot navigate back to empty main screen

**Checkpoint**: User Story 4 fully functional — instances can be removed cleanly

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: UX refinements and final validation

- [x] T030 [P] Add confirmation dialog for instance removal in `ui/settings/SettingsScreen.kt` — prevent accidental removal of instances
- [x] T031 Run `quickstart.md` validation checklist — verify all 8 test scenarios pass manually

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (T001 needed for TokenManager refactor) — **BLOCKS all user stories**
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) completion
- **User Story 2 (Phase 4)**: Depends on Phase 2. Can run in parallel with US1 but logically benefits from US1 being complete (pills must exist to switch). Includes per-instance 401 re-auth (T023) to deliver FR-011 end-to-end
- **User Story 3 (Phase 5)**: Depends on Phase 2. Requires US1 pills UI (T015) to exist
- **User Story 4 (Phase 6)**: Depends on Phase 2. Requires US1 pills UI (T015) to exist
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (Add Instance)**: After Phase 2 — no cross-story dependency
- **US2 (Switch Instance)**: After Phase 2 — benefits from US1 UI but core logic is independent. Delivers FR-011 (re-auth) end-to-end
- **US3 (View Details)**: After Phase 2 + US1 pill UI (T015)
- **US4 (Remove Instance)**: After Phase 2 + US1 pill UI (T015)

### Within Each User Story

- ViewModel logic before UI wiring
- UI components before navigation changes
- Core functionality before edge cases

### Parallel Opportunities

- T001 and T002 can run in parallel (Phase 1)
- T007 and T008 can run in parallel (different network files, Phase 2)
- T011, T012, T013 can run in parallel with T014 (auth vs settings, Phase 3)
- T030 is independent (Phase 7)
- With multiple developers: US1 and US2 core logic can proceed in parallel after Phase 2

---

## Parallel Example: Phase 1

```bash
# Launch both setup tasks together:
Task: "Create AapInstance data class in model/AapInstance.kt"
Task: "Create SettingsUiState in presentation/settings/SettingsUiState.kt"
```

## Parallel Example: User Story 1

```bash
# Auth-side and Settings-side can proceed in parallel:
Task: "Update AuthViewModel with alias parameter"
Task: "Create SettingsViewModel with instance observation"
```

---

## Implementation Strategy

### MVP First (User Stories 1 + 2)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 2: Foundational (T003-T010) — **CRITICAL, blocks all stories**
3. Complete Phase 3: User Story 1 — Add Instances (T011-T017)
4. **STOP and VALIDATE**: Test adding multiple instances
5. Complete Phase 4: User Story 2 — Switch Instances (T018-T023)
6. **STOP and VALIDATE**: Test switching between instances and 401 re-auth

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US1 → Test independently → MVP for adding instances
3. Add US2 → Test independently → Core multi-instance experience complete (including per-instance re-auth)
4. Add US3 → Test independently → Instance details available
5. Add US4 → Test independently → Full instance lifecycle
6. Polish → Confirmation dialogs, final validation

---

## Notes

- No automated tests — all validation is manual per plan.md
- ~25 Kotlin source files affected per plan.md estimate
- Performance target: instance switch + data refresh < 3 seconds
- Support up to 10 instances; Settings screen must render 10 pills within a single frame (16ms)
- Legacy flat credential keys are cleared on first launch (no migration)
- Coroutine cancellation propagates to Retrofit via cooperative cancellation — no explicit Call.cancel() needed

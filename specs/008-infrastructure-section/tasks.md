# Tasks: Infrastructure Section (Inventories and Hosts)

**Input**: Design documents from `/specs/008-infrastructure-section/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, quickstart.md

**Tests**: Not requested in feature specification. No test tasks included.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

---

## Phase 1: Setup

**Purpose**: No project setup needed — the project already exists. This phase is intentionally empty.

---

## Phase 2: Foundational (Data Layer — Models, API, Repositories, DI)

**Purpose**: All data models, API endpoints, repositories, and DI wiring that MUST be complete before any UI work begins.

**CRITICAL**: No user story work can begin until this phase is complete.

- [X] T001 [P] Create Inventory data model with InventorySummaryFields, OrganizationSummary (include created, modified, variables fields for bottom sheet) in `app/src/main/kotlin/com/example/aapremote/model/Inventory.kt`
- [X] T002 [P] Create Host data model with HostSummaryFields, InventorySummary, GroupsSummary, GroupSummary, HostFacts, JobHostSummary, JobHostSummaryFields (include created, modified, last_job fields) in `app/src/main/kotlin/com/example/aapremote/model/Host.kt`
- [X] T003 Add inventory and host API endpoints to `app/src/main/kotlin/com/example/aapremote/network/AapApiService.kt`: getInventories(), getInventory(), getInventoryHosts(), getHosts(), getHostFacts(), getHostJobSummaries()
- [X] T004 [P] Create InventoryRepository with getInventories() and getInventory() in `app/src/main/kotlin/com/example/aapremote/data/InventoryRepository.kt`
- [X] T005 [P] Create HostRepository with getAllHosts(), getInventoryHosts(), searchHosts(), getHostFacts(), getHostJobSummaries() in `app/src/main/kotlin/com/example/aapremote/data/HostRepository.kt`
- [X] T006 Register InventoryRepository, HostRepository in `app/src/main/kotlin/com/example/aapremote/di/DataModule.kt`
- [X] T007 Create HostDetailSheet composable with ModalBottomSheet showing host name, last successful run, inventory, created, last modified, and variables; expand-to-full-screen loads and displays facts (ansible_facts), groups, and jobs run in `app/src/main/kotlin/com/example/aapremote/ui/hosts/HostDetailSheet.kt` (shared by US1 and US2)

**Checkpoint**: Data layer and shared components complete — user story work can begin.

---

## Phase 3: User Story 1 — Browse Inventories (Priority: P1) MVP

**Goal**: Users can navigate to Infrastructure > Inventories, see a paginated list of inventories, tap an inventory to see details in a bottom sheet, and expand to full screen to see the inventory's hosts with group badges.

**Independent Test**: Navigate to Infrastructure tab, select Inventories, verify list loads with pagination and pull-to-refresh. Tap an inventory to see bottom sheet with name, type, organization, total hosts, created, modified, variables. Expand to see hosts with group badges.

### Implementation for User Story 1

- [X] T008 [P] [US1] Create InventoriesUiState sealed interface (Loading, Success, Error, Empty) in `app/src/main/kotlin/com/example/aapremote/presentation/inventory/InventoriesUiState.kt`
- [X] T009 [P] [US1] Create InventoryHostsUiState sealed interface (Loading, Success, Error, Empty) for the expanded inventory hosts view in `app/src/main/kotlin/com/example/aapremote/presentation/hosts/InventoryHostsUiState.kt`
- [X] T010 [US1] Create InventoriesViewModel with inventory list, pagination, and refresh in `app/src/main/kotlin/com/example/aapremote/presentation/inventory/InventoriesViewModel.kt`
- [X] T011 [US1] Create InventoryHostsViewModel with hosts-within-inventory list, pagination, and search (debounced) for the expanded view in `app/src/main/kotlin/com/example/aapremote/presentation/hosts/InventoryHostsViewModel.kt`
- [X] T012 [US1] Register InventoriesViewModel and InventoryHostsViewModel in `app/src/main/kotlin/com/example/aapremote/di/PresentationModule.kt`
- [X] T013 [US1] Create InventoriesScreen composable with LazyColumn, pull-to-refresh, infinite scroll, and skeleton loading in `app/src/main/kotlin/com/example/aapremote/ui/inventory/InventoriesScreen.kt`
- [X] T014 [US1] Create InventoryDetailSheet composable with ModalBottomSheet showing inventory details (name, type, organization, total hosts, created, modified, variables) and expand-to-full-screen with hosts list showing group badges; tapping a host opens shared HostDetailSheet (T007) in `app/src/main/kotlin/com/example/aapremote/ui/inventory/InventoryDetailSheet.kt`
- [X] T015 [US1] Mark Inventories segment as isImplemented=true in `app/src/main/kotlin/com/example/aapremote/ui/main/TabDefinitions.kt`
- [X] T016 [US1] Wire InventoriesScreen into TabContent for Infrastructure > Inventories in `app/src/main/kotlin/com/example/aapremote/navigation/MainNavigation.kt`

**Checkpoint**: Inventories list is functional. Users can browse inventories, view details in bottom sheet, and expand to see hosts. MVP complete.

---

## Phase 4: User Story 2 — Browse and Search All Hosts (Priority: P1)

**Goal**: Users can navigate to Infrastructure > Hosts and see a standalone list of ALL hosts across all inventories, with hostname, description, and inventory label. Search filters the list. Tapping a host shows details in a bottom sheet.

**Independent Test**: Navigate to Infrastructure > Hosts, verify all hosts load with descriptions and inventory labels. Search for a host by name. Tap a host to see bottom sheet with details. Expand to full screen.

### Implementation for User Story 2

- [X] T017 [P] [US2] Create HostsUiState sealed interface (Loading, Success, Error, Empty) in `app/src/main/kotlin/com/example/aapremote/presentation/hosts/HostsUiState.kt`
- [X] T018 [US2] Create HostsViewModel with all-hosts list, pagination, search (debounced), and refresh in `app/src/main/kotlin/com/example/aapremote/presentation/hosts/HostsViewModel.kt`
- [X] T019 [US2] Register HostsViewModel in `app/src/main/kotlin/com/example/aapremote/di/PresentationModule.kt`
- [X] T020 [US2] Create HostsScreen composable with LazyColumn, search bar, host cards showing hostname/description/inventory label, pull-to-refresh, infinite scroll, and skeleton loading; tapping a host opens shared HostDetailSheet (T007) in `app/src/main/kotlin/com/example/aapremote/ui/hosts/HostsScreen.kt`
- [X] T021 [US2] Mark Hosts segment as isImplemented=true in `app/src/main/kotlin/com/example/aapremote/ui/main/TabDefinitions.kt`
- [X] T022 [US2] Wire HostsScreen into TabContent for Infrastructure > Hosts in `app/src/main/kotlin/com/example/aapremote/navigation/MainNavigation.kt`

**Checkpoint**: Standalone Hosts screen is functional. Users can browse all hosts, search, and view details.

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Final integration checks, cleanup, and descoping.

- [X] T023 Remove Projects segment from Infrastructure tab in `app/src/main/kotlin/com/example/aapremote/ui/main/TabDefinitions.kt` (descoped to separate future tab)
- [X] T024 Verify all Infrastructure placeholder screens are replaced — confirm no PlaceholderScreen remains for Inventories and Hosts segments in `app/src/main/kotlin/com/example/aapremote/navigation/MainNavigation.kt`
- [ ] T025 Run quickstart.md validation scenarios (requires live AAP instance — manual testing) end-to-end against a live AAP 2.5+ instance

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Empty — project already exists
- **Foundational (Phase 2)**: No dependencies — can start immediately. BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2)
- **User Story 2 (Phase 4)**: Depends on Foundational (Phase 2) — independent of US1, can run in parallel
- **Polish (Phase 5)**: Depends on all user stories being complete

### User Story Dependencies

- **US1 (Browse Inventories)**: Can start after Phase 2 — No story dependencies. Uses shared HostDetailSheet (T007).
- **US2 (Browse/Search Hosts)**: Can start after Phase 2 — Independent of US1, can run in parallel. Uses shared HostDetailSheet (T007).

### Within Each User Story

- UiState before ViewModel
- ViewModel before Screen composable
- DI registration before Screen can use ViewModel
- Screen before navigation wiring

### Parallel Opportunities

- T001, T002 (both data models) can run in parallel
- T004, T005 (both repositories) can run in parallel (after T003)
- T008, T009, T017 (all UiState files) can run in parallel
- US1 and US2 can be implemented in parallel (both depend only on Phase 2)

## Parallel Example: Foundational Phase

```bash
# Launch both data models together:
Task: "Create Inventory model in model/Inventory.kt"
Task: "Create Host model in model/Host.kt"

# Then launch both repositories together:
Task: "Create InventoryRepository in data/InventoryRepository.kt"
Task: "Create HostRepository in data/HostRepository.kt"
```

## Parallel Example: User Stories 1 + 2

```bash
# After Phase 2, launch US1 and US2 in parallel:
# Agent A: User Story 1 (Inventories)
Task: "Create InventoriesUiState in presentation/inventory/InventoriesUiState.kt"
Task: "Create InventoriesViewModel in presentation/inventory/InventoriesViewModel.kt"
Task: "Create InventoriesScreen in ui/inventory/InventoriesScreen.kt"
Task: "Create InventoryDetailSheet in ui/inventory/InventoryDetailSheet.kt"

# Agent B: User Story 2 (Hosts) — HostDetailSheet already built in Phase 2
Task: "Create HostsUiState in presentation/hosts/HostsUiState.kt"
Task: "Create HostsViewModel in presentation/hosts/HostsViewModel.kt"
Task: "Create HostsScreen in ui/hosts/HostsScreen.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 2: Foundational (data layer)
2. Complete Phase 3: User Story 1 (Inventories)
3. **STOP and VALIDATE**: Test inventory browsing and detail view independently
4. Demo if ready

### Incremental Delivery

1. Complete Foundational → Data layer ready
2. Add US1 (Inventories + detail + hosts drill-down) → Test → Demo (MVP!)
3. Add US2 (Standalone Hosts) → Test → Demo
4. Each story adds value without breaking previous stories

### Parallel Strategy

With multiple agents/developers:

1. Complete Foundational phase together
2. Once Foundational is done:
   - Agent A: US1 (Inventories + InventoryDetailSheet)
   - Agent B: US2 (Hosts — uses shared HostDetailSheet from Phase 2)
3. Stories integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story is independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Projects have been descoped — a separate GitHub issue should be created for the Projects tab (Controller + EDA projects)

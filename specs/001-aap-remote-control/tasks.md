# Tasks: AAP Remote Control MVP

**Input**: Design documents from `/specs/001-aap-remote-control/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/aap-api-contract.md, quickstart.md

**Tests**: Not explicitly requested in the specification. Test tasks are excluded.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

All source paths are under `app/src/main/kotlin/com/example/aapremote/`.
Abbreviated as `src/` below for readability.

---

## Phase 1: Setup (Project Initialization)

**Purpose**: Create project structure, configure dependencies, establish build configuration

- [X] T001 Create Android project with Compose Empty Activity template, Kotlin, API 26 minimum, package `com.example.aapremote`
- [X] T002 Configure `app/build.gradle.kts` with all dependencies: Compose BOM + Material 3, Compose Navigation, Retrofit + KotlinX Serialization converter, OkHttp + logging interceptor, Koin (core + compose), DataStore + Tink, Coroutines
- [X] T003 [P] Create `app/src/main/res/xml/network_security_config.xml` enforcing HTTPS-only (cleartextTrafficPermitted=false) and reference it in AndroidManifest.xml
- [X] T004 [P] Create directory structure matching plan.md: `src/{network,data,model,presentation/{auth,templates,jobs},ui/{theme,auth,templates,jobs,components},navigation}/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared models, network layer, data layer, theme, and app scaffolding that ALL user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

- [X] T005 [P] Create `src/model/PaginatedResponse.kt` — generic `@Serializable` wrapper with `count`, `next`, `previous`, `results` fields matching AAP pagination contract
- [X] T006 [P] Create `src/model/JobStatus.kt` — enum with 8 values (NEW, PENDING, WAITING, RUNNING, SUCCESSFUL, FAILED, ERROR, CANCELED) with `isActive` and `isTerminal` properties and KotlinX Serialization support
- [X] T007 [P] Create `src/model/User.kt` — `@Serializable` data class with `id`, `username`, `first_name`, `last_name`, `email`, `is_superuser` fields
- [X] T008 [P] Create `src/model/Label.kt` — `@Serializable` data class with `id`, `name` fields
- [X] T009 [P] Create `src/model/JobTemplate.kt` — `@Serializable` data class with `id`, `name`, `description`, `ask_variables_on_launch`, `status`, `last_job_run`, `summary_fields` (nested labels and user_capabilities)
- [X] T010 [P] Create `src/model/Job.kt` — `@Serializable` data class with `id`, `name`, `status` (JobStatus), `failed`, `started`, `finished`, `elapsed`, `launch_type`, `job_template_id` (from `unified_job_template`), `summary_fields` (nested job_template name)
- [X] T011 [P] Create `src/model/LaunchRequest.kt` — `@Serializable` data class with optional `extra_vars` String field
- [X] T012 Create `src/network/AapApiService.kt` — Retrofit interface with all endpoints: `getMe()`, `getJobTemplates(page, pageSize, search, labelsFilter, orderBy)`, `launchJob(id, request)`, `getJob(id)`, `getJobs(orderBy, pageSize, status)`. Use `@GET`/`@POST` annotations with relative paths (prefix handled by base URL)
- [X] T013 Create `src/network/AuthInterceptor.kt` — OkHttp interceptor that adds `Authorization: Bearer <token>` header to all requests, reading token from TokenManager
- [X] T014 Create `src/network/ApiVersionDetector.kt` — class that attempts `GET /api/controller/v2/me/` first, falls back to `/api/v2/me/` on 404. Returns detected `ApiVersion` enum (V2, CONTROLLER_V2) and stores with instance config
- [X] T015 Create `src/network/CertTrustManager.kt` — custom `X509TrustManager` that supports per-instance self-signed certificate acceptance. When trust_self_signed is true, load stored certificate into a custom KeyStore and build TrustManagerFactory from it
- [X] T016 Create `src/network/NetworkModule.kt` — Koin module providing: OkHttpClient (with AuthInterceptor, logging interceptor, dynamic SSL socket factory from CertTrustManager), Retrofit instance (with KotlinX Serialization converter), AapApiService, ApiVersionDetector
- [X] T017 Create `src/data/TokenManager.kt` — encrypted credential storage using DataStore + Tink (AeadSerializer). Store/retrieve: base_url, token, api_version, trust_self_signed, server_cert_fingerprint. All access via suspend functions
- [X] T018 Create `src/data/DataModule.kt` — Koin module providing TokenManager, DataStore instance, all repositories (AuthRepository, TemplateRepository, JobRepository)
- [X] T019 Create `src/presentation/PresentationModule.kt` — Koin module providing all ViewModels (AuthViewModel, TemplatesViewModel, JobStatusViewModel, RecentJobsViewModel) via `viewModelOf()`
- [X] T020 [P] Create `src/ui/theme/Color.kt` — Material 3 color scheme including status-specific colors (green for successful, red for failed/error, orange for running, gray for pending/waiting/new, blue for canceled)
- [X] T021 [P] Create `src/ui/theme/Type.kt` — Material 3 typography
- [X] T022 Create `src/ui/theme/Theme.kt` — Material 3 dynamic color theme with dark mode support, using Color.kt and Type.kt
- [X] T023 Create `src/AapRemoteApp.kt` — Application class that initializes Koin with networkModule, dataModule, presentationModule
- [X] T024 Create `src/MainActivity.kt` — single ComponentActivity with `setContent { AapRemoteTheme { AppNavigation() } }`
- [X] T025 Create `src/ui/components/ErrorMessage.kt` — reusable Composable for displaying error messages with retry action (used across all screens)

**Checkpoint**: Foundation ready — all models, network layer, data layer, DI, and theme in place. User story implementation can now begin.

---

## Phase 3: User Story 1 — Connect to AAP Instance (Priority: P1)

**Goal**: User enters AAP URL + PAT, validates credentials, stores them securely, auto-reconnects on relaunch

**Independent Test**: Enter valid/invalid credentials, verify connect/error behavior. Reopen app to verify auto-reconnect. Logout to verify credential clearing.

### Implementation for User Story 1

- [X] T026 Create `src/data/AuthRepository.kt` — repository with `validateCredentials(baseUrl, token, trustSelfSigned)` (calls ApiVersionDetector then getMe()), `logout()` (clears TokenManager), `getStoredCredentials()`, `isLoggedIn()`. Handle 401/403/network errors
- [X] T027 Create `src/presentation/auth/AuthUiState.kt` — sealed interface with `Idle`, `Loading`, `Success(username)`, `Error(message)` variants
- [X] T028 Create `src/presentation/auth/AuthViewModel.kt` — ViewModel exposing `StateFlow<AuthUiState>`. Functions: `connect(baseUrl, token, trustSelfSigned)`, `checkExistingCredentials()` (auto-login on launch), `logout()`. Use viewModelScope + coroutines
- [X] T029 Create `src/ui/auth/AuthScreen.kt` — Compose screen with: text field for AAP Base URL, secure text field for PAT, "Accept self-signed certificate" toggle (default off), "Connect" button. React to AuthUiState: show CircularProgressIndicator on Loading, navigate to dashboard on Success, show ErrorMessage on Error
- [X] T030 Create `src/navigation/AppNavigation.kt` — Compose NavHost with routes: `auth` (start), `templates`, `job_status/{jobId}`, `recent_jobs`. AuthScreen navigates to templates on success. Include logout action navigating back to auth

**Checkpoint**: User Story 1 fully functional. User can connect, disconnect, and auto-reconnect.

---

## Phase 4: User Story 2 — Browse and Launch Job Templates (Priority: P2)

**Goal**: Authenticated user browses templates with search/label filtering, launches a job with optional extra vars and confirmation dialog

**Independent Test**: Authenticate, view template list, search by name, filter by label, launch a job with and without extra vars, verify confirmation dialog

### Implementation for User Story 2

- [X] T031 Create `src/data/TemplateRepository.kt` — repository with `getTemplates(page, search, labelFilter)` returning paginated results, `launchJob(templateId, extraVars)` returning Job id. Handle pagination (load more via `next` URL)
- [X] T032 Create `src/presentation/templates/TemplatesUiState.kt` — sealed interface with `Idle`, `Loading`, `Success(templates, availableLabels, hasMore)`, `Error(message)` variants. Include `LaunchState` sealed class: `Idle`, `Confirming(template)`, `EnteringVars(template)`, `Launching`, `Launched(jobId)`, `LaunchError(message)`
- [X] T033 Create `src/presentation/templates/TemplatesViewModel.kt` — ViewModel exposing `StateFlow<TemplatesUiState>` and `StateFlow<LaunchState>`. Functions: `loadTemplates()`, `loadMore()`, `search(query)`, `filterByLabel(label)`, `clearFilters()`, `requestLaunch(template)`, `confirmLaunch(extraVars?)`, `cancelLaunch()`. Extract unique labels from loaded templates for filter chips
- [X] T034 [P] Create `src/ui/components/SearchBar.kt` — Compose search bar with text input and clear button, emitting search queries with debounce
- [X] T035 [P] Create `src/ui/components/LabelChips.kt` — Compose row of FilterChip components for available labels, supporting single selection toggle
- [X] T036 [P] Create `src/ui/components/LaunchConfirmDialog.kt` — AlertDialog showing "Launch [template name]?" with Confirm and Cancel buttons
- [X] T037 [P] Create `src/ui/components/ExtraVarsInput.kt` — Composable with multiline text field for JSON input, with client-side JSON validation (parse check + 64KB size limit), showing validation error inline
- [X] T038 Create `src/ui/templates/TemplateListItem.kt` — Compose list item showing template name, description, label chips, last job status indicator, and launch button (play icon). Show launch button only if `user_capabilities.start` is true
- [X] T039 Create `src/ui/templates/TemplateListScreen.kt` — Compose screen with: SearchBar at top, LabelChips row below, LazyColumn of TemplateListItem with load-more on scroll, empty state ("No templates available"), loading state. Wire LaunchConfirmDialog and ExtraVarsInput to LaunchState. On Launched, navigate to job status screen

**Checkpoint**: User Stories 1 AND 2 fully functional. User can connect, browse, search, filter, and launch jobs.

---

## Phase 5: User Story 3 — Monitor Job Status (Priority: P3)

**Goal**: User sees real-time job status via polling, browses recent jobs from server

**Independent Test**: Launch a job via US2, observe status updates polling every 5 seconds until terminal state. View recent jobs list fetched from server.

### Implementation for User Story 3

- [X] T040 Create `src/data/JobRepository.kt` — repository with `getJobStatus(jobId)` and `getRecentJobs(page, pageSize)`. Include `pollJobStatus(jobId)` returning a `Flow<Job>` that emits every 5 seconds while status is active, stops on terminal
- [X] T041 Create `src/presentation/jobs/JobUiState.kt` — sealed interfaces: `JobStatusUiState` with `Loading`, `Active(job)`, `Completed(job)`, `Error(message)` variants. `RecentJobsUiState` with `Loading`, `Success(jobs, hasMore)`, `Error(message)` variants
- [X] T042 Create `src/presentation/jobs/JobStatusViewModel.kt` — ViewModel taking jobId parameter, exposing `StateFlow<JobStatusUiState>`. On init, start collecting from `pollJobStatus(jobId)` flow. Map active statuses to `Active`, terminal to `Completed`
- [X] T043 Create `src/presentation/jobs/RecentJobsViewModel.kt` — ViewModel exposing `StateFlow<RecentJobsUiState>`. Functions: `loadRecentJobs()`, `loadMore()`. Fetch from server via JobRepository
- [X] T044 [P] Create `src/ui/components/JobStatusBadge.kt` — Compose component showing status text with color-coded background (green=successful, red=failed/error, orange=running, gray=new/pending/waiting, blue=canceled) and icon
- [X] T045 Create `src/ui/jobs/JobStatusScreen.kt` — Compose screen showing: job name, JobStatusBadge, elapsed time (updating while active), started/finished timestamps. Show loading spinner on initial load. Animate status transitions
- [X] T046 Create `src/ui/jobs/RecentJobsScreen.kt` — Compose screen with LazyColumn of job items showing: job name, template name, status badge, started/finished time. Load more on scroll. Empty state for no jobs

**Checkpoint**: All user stories independently functional. User can connect, browse templates, launch jobs, and monitor status.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Error handling, edge cases, and refinements affecting multiple user stories

- [X] T047 Add 401 interceptor in `src/network/AuthInterceptor.kt` — detect 401 responses globally, emit event to trigger re-auth flow (navigate to AuthScreen with message)
- [X] T048 [P] Add network connectivity observer in `src/data/` — monitor connectivity state, expose as StateFlow, disable launch actions when offline, show offline indicator across screens
- [X] T049 [P] Update `src/navigation/AppNavigation.kt` — add logout action to TemplateListScreen toolbar, wire Recent Jobs navigation from dashboard, ensure back navigation works correctly across all flows
- [X] T050 Run quickstart.md verification checklist against all screens and document any deviations

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Setup — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational
- **User Story 2 (Phase 4)**: Depends on Foundational (integrates with US1 auth flow but independently testable)
- **User Story 3 (Phase 5)**: Depends on Foundational (integrates with US2 launch but independently testable)
- **Polish (Phase 6)**: Depends on all user stories

### Within Each User Story

- Repository before ViewModel
- UiState before ViewModel
- ViewModel before Screen
- Shared components (marked [P]) can be built in parallel
- Screen composition last (depends on all above)

### Parallel Opportunities

Phase 2 has the most parallelism:
- T005-T011 (all models) can run in parallel
- T020-T021 (theme files) can run in parallel
- T012 depends on T005-T011 (models used in API interface)
- T013-T015 can run in parallel (independent interceptors)

Phase 4 components:
- T034, T035, T036, T037 (all UI components) can run in parallel

---

## Parallel Example: Phase 2 Models

```bash
# Launch all model files together:
Task: "Create PaginatedResponse.kt"
Task: "Create JobStatus.kt"
Task: "Create User.kt"
Task: "Create Label.kt"
Task: "Create JobTemplate.kt"
Task: "Create Job.kt"
Task: "Create LaunchRequest.kt"
```

## Parallel Example: Phase 4 Components

```bash
# Launch all reusable UI components together:
Task: "Create SearchBar.kt"
Task: "Create LabelChips.kt"
Task: "Create LaunchConfirmDialog.kt"
Task: "Create ExtraVarsInput.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL — blocks all stories)
3. Complete Phase 3: User Story 1 (Auth)
4. **STOP and VALIDATE**: Test auth flow end-to-end
5. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add User Story 1 → Test auth → Demo (MVP!)
3. Add User Story 2 → Test templates + launch → Demo
4. Add User Story 3 → Test job monitoring → Demo
5. Polish → Final validation → Release

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (Auth)
   - Developer B: User Story 2 UI components (T034-T037)
3. After US1 complete:
   - Developer A: User Story 3
   - Developer B: User Story 2 (ViewModel + Screen)
4. Polish phase together

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story
- Each user story is independently testable after its phase completes
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- AAP uses "labels" not "tags" — use correct terminology throughout
- `extra_vars` must be sent as JSON string, not raw object
- Support both AAP 2.4 and 2.5+ API paths via auto-detection

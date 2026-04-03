# Implementation Plan: Activity Section

**Branch**: `006-activity-section` | **Date**: 2026-04-03 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/006-activity-section/spec.md`

## Summary

Add a full Activity section with three segments: Jobs (enhanced with status filter chips), Schedules (view + toggle enabled/disabled), and EDA Audit (view rule audit events). Follows existing MVVM + Unidirectional Data Flow patterns with new Retrofit endpoints, repositories, ViewModels, and Compose screens. EDA endpoints use a separate Retrofit service interface (`EdaApiService`) with a different base path.

## Technical Context

**Language/Version**: Kotlin (JVM 17), compileSdk 35, minSdk 31
**Primary Dependencies**: Jetpack Compose (Material 3 BOM), Navigation Compose, Retrofit + Kotlin Serialization, Koin
**Storage**: DataStore + Tink (unchanged — no new storage needs)
**Testing**: Manual testing against live AAP 2.5+ instance
**Target Platform**: Android (minSdk 31)
**Project Type**: Mobile app (Android)
**Performance Goals**: Initial data load within 3 seconds (SC-003), schedule toggle under 3 seconds (SC-002)
**Constraints**: HTTPS-only, Bearer token auth, EDA may not be configured on all instances
**Scale/Scope**: 3 new screens, 2 new data models, 1 new Retrofit service interface

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Pre-Phase 0 | Post-Phase 1 | Notes |
|-----------|-------------|--------------|-------|
| I. Kotlin-Only | PASS | PASS | All new code is Kotlin |
| II. Compose-First UI | PASS | PASS | New screens use Compose + Material 3 |
| III. MVVM + UDF | PASS | PASS | New ViewModels with StateFlow + sealed UiState |
| IV. Security-First | PASS | PASS | No new credential storage; existing auth interceptor shared |
| V. Lean Dependencies | PASS | PASS | No new dependencies — reuses existing stack |
| VI. API-Driven Design | PASS | PASS | New endpoints: schedules, audit-rules. Constitution update needed (1.3.0) |

**Constitution amendment required**: Add schedule and EDA audit endpoints to Principle VI endpoint list (minor version bump to 1.3.0).

## Project Structure

### Documentation (this feature)

```text
specs/006-activity-section/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── controller-api.md
│   └── eda-api.md
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/src/main/kotlin/com/example/aapremote/
├── model/
│   ├── Schedule.kt                    # NEW: Schedule + ScheduleSummaryFields
│   └── EdaRuleAudit.kt               # NEW: EDA rule audit event
├── network/
│   ├── AapApiService.kt              # MODIFIED: add schedule endpoints + job status filter
│   ├── EdaApiService.kt              # NEW: Retrofit interface for EDA /api/eda/v1/
│   ├── AapApiProvider.kt             # MODIFIED: add getEdaApiService()
│   └── NetworkModule.kt              # MODIFIED: register EdaApiService
├── data/
│   ├── JobRepository.kt              # MODIFIED: add status filter to getRecentJobs()
│   ├── ScheduleRepository.kt         # NEW: schedule list + toggle
│   ├── EdaAuditRepository.kt         # NEW: EDA audit event list
│   └── DataModule.kt                 # MODIFIED: register new repositories
├── presentation/
│   ├── jobs/
│   │   ├── RecentJobsViewModel.kt    # MODIFIED: add filter state + methods
│   │   └── RecentJobsUiState.kt      # MODIFIED: add activeFilters field
│   ├── schedules/
│   │   ├── SchedulesViewModel.kt     # NEW
│   │   └── SchedulesUiState.kt       # NEW
│   ├── eda/
│   │   ├── EdaAuditViewModel.kt      # NEW
│   │   └── EdaAuditUiState.kt        # NEW
│   └── PresentationModule.kt         # MODIFIED: register new ViewModels
├── ui/
│   ├── jobs/
│   │   └── RecentJobsScreen.kt       # MODIFIED: add filter chips row
│   ├── schedules/
│   │   └── SchedulesScreen.kt        # NEW
│   ├── eda/
│   │   ├── EdaAuditScreen.kt         # NEW
│   │   └── EdaAuditDetailSheet.kt    # NEW: bottom sheet for event details
│   ├── components/
│   │   └── StatusFilterChips.kt      # NEW: reusable filter chips component
│   └── main/
│       └── TabDefinitions.kt         # MODIFIED: mark segments as implemented
└── navigation/
    └── MainNavigation.kt             # MODIFIED: route new segments to screens
```

**Structure Decision**: Follows existing feature-based vertical slicing within each architecture layer. No new top-level directories needed.

## Complexity Tracking

No constitution violations to justify. All patterns follow existing codebase conventions.

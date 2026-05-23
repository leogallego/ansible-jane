# Implementation Plan: UI Polish — Animations and Micro-Interactions

**Branch**: `007-ui-polish-animations` | **Date**: 2026-04-03 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/007-ui-polish-animations/spec.md`

## Summary

Add micro-interaction animations (press-scale on cards, spring dialog entrances, animated app bar title, breathing pulse on running status) and refactor error handling from plain strings to typed errors with distinct icons and expandable details. Includes architectural improvements: centralized icon registry (AapIcons), theme-level status colors via CompositionLocal, and Flow.asResult() extension to reduce ViewModel boilerplate. All changes enhance existing components — no new screens or navigation routes.

## Technical Context

**Language/Version**: Kotlin (JVM 17), compileSdk 35, minSdk 31
**Primary Dependencies**: Jetpack Compose (Material 3 BOM), Compose Animation, Retrofit, Koin
**Storage**: N/A (no new storage for this feature)
**Testing**: Manual visual testing (animations) + unit tests for AppError mapping
**Target Platform**: Android 12+ (minSdk 31)
**Project Type**: Mobile app (Android)
**Performance Goals**: 60 fps animations, no jank
**Constraints**: All animations must respect reduce-motion accessibility setting
**Scale/Scope**: ~30 files modified, 0 new screens

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Kotlin-Only | PASS | All code is Kotlin |
| II. Compose-First UI | PASS | All changes are Compose composables |
| III. MVVM + UDF | PASS | Error types flow via StateFlow; no business logic in composables |
| IV. Security-First | PASS | No credential changes |
| V. Lean Dependencies | PASS | No new dependencies — uses existing Compose Animation APIs |
| VI. API-Driven Design | PASS | Error classification reads from existing Retrofit/OkHttp exceptions |

**Gate result**: PASS — no violations.

## Project Structure

### Documentation (this feature)

```text
specs/007-ui-polish-animations/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/kotlin/io/github/leogallego/ansiblejane/
├── model/
│   └── AppError.kt                    # NEW: Typed error sealed class
├── data/
│   ├── ResultExtensions.kt            # NEW: Flow.asResult() extension
│   ├── TemplateRepository.kt          # MODIFY: Preserve exception types
│   ├── WorkflowRepository.kt          # MODIFY: Preserve exception types
│   ├── JobRepository.kt               # MODIFY: Preserve exception types
│   ├── ScheduleRepository.kt          # MODIFY: Preserve exception types
│   ├── EdaAuditRepository.kt          # MODIFY: Preserve exception types
│   └── AuthRepository.kt              # MODIFY: Preserve exception types
├── presentation/
│   ├── auth/AuthUiState.kt            # MODIFY: Error(AppError)
│   ├── templates/
│   │   ├── TemplatesUiState.kt        # MODIFY: Error(AppError)
│   │   └── TemplatesViewModel.kt      # MODIFY: Use asResult() + AppError.from()
│   ├── workflows/
│   │   ├── WorkflowTemplatesUiState.kt    # MODIFY: Error(AppError)
│   │   ├── WorkflowJobStatusUiState.kt    # MODIFY: Error(AppError)
│   │   ├── WorkflowTemplatesViewModel.kt  # MODIFY: Use asResult() + AppError.from()
│   │   └── WorkflowJobStatusViewModel.kt  # MODIFY: Use asResult() + AppError.from()
│   ├── jobs/
│   │   ├── JobUiState.kt              # MODIFY: Error(AppError)
│   │   ├── JobStatusViewModel.kt      # MODIFY: Use asResult() + AppError.from()
│   │   └── RecentJobsViewModel.kt     # MODIFY: Use asResult() + AppError.from()
│   ├── schedules/
│   │   ├── SchedulesUiState.kt        # MODIFY: Error(AppError)
│   │   └── SchedulesViewModel.kt      # MODIFY: Use asResult() + AppError.from()
│   └── eda/
│       ├── EdaAuditUiState.kt         # MODIFY: Error(AppError)
│       └── EdaAuditViewModel.kt       # MODIFY: Use asResult() + AppError.from()
├── ui/
│   ├── components/
│   │   ├── ErrorMessage.kt            # MODIFY: Accept AppError, show typed display + slide-in
│   │   ├── JobStatusBadge.kt          # MODIFY: Add breathing pulse + use AapIcons/StatusColors
│   │   ├── LaunchConfirmDialog.kt     # MODIFY: Add spring entrance animation
│   │   ├── ExtraVarsInput.kt          # MODIFY: Add spring entrance animation
│   │   └── PressScaleModifier.kt      # NEW: Reusable press-scale Modifier extension
│   ├── icons/
│   │   └── AapIcons.kt               # NEW: Centralized icon registry
│   ├── theme/
│   │   ├── Color.kt                   # MODIFY: Move status colors to StatusColors data class
│   │   ├── Theme.kt                   # MODIFY: Provide StatusColors via CompositionLocal
│   │   └── StatusColors.kt            # NEW: StatusColors data class + LocalStatusColors
│   ├── main/MainScreen.kt            # MODIFY: AnimatedContent on title
│   ├── templates/TemplateListItem.kt  # MODIFY: Apply press-scale modifier
│   ├── templates/TemplateListScreen.kt    # MODIFY: Pass AppError to ErrorMessage
│   ├── workflows/WorkflowTemplateListItem.kt  # MODIFY: Apply press-scale modifier
│   ├── workflows/WorkflowTemplateListScreen.kt    # MODIFY: Pass AppError
│   ├── workflows/WorkflowJobStatusScreen.kt       # MODIFY: Pass AppError
│   ├── jobs/RecentJobsScreen.kt       # MODIFY: Apply press-scale + pass AppError
│   ├── jobs/JobStatusScreen.kt        # MODIFY: Pass AppError
│   ├── schedules/SchedulesScreen.kt   # MODIFY: Pass AppError
│   ├── eda/EdaAuditScreen.kt         # MODIFY: Apply press-scale + pass AppError
│   └── eda/EdaAuditDetailSheet.kt    # MODIFY: Spring entrance on bottom sheet
```

**Structure Decision**: Feature-based packaging within existing layer hierarchy. Five new files: `AppError.kt` (model), `ResultExtensions.kt` (Flow utility), `PressScaleModifier.kt` (UI modifier), `AapIcons.kt` (icon registry), `StatusColors.kt` (theme extension). Everything else modifies existing files.

## Design Decisions

### D1: AppError as a sealed class in model/ layer

The `AppError` sealed class lives in `model/` (not `presentation/`) because all three layers reference it: repositories propagate raw exceptions, ViewModels classify them via `AppError.from()`, and UI renders the result.

Currently, repositories wrap exceptions in `Result.failure(Exception(message))`, losing the original type. We need to change this.

**Approach**: Repositories will stop wrapping exceptions — they'll let the original exception propagate in `Result.failure(e)`. A companion `AppError.from(throwable)` factory function will classify the exception type. ViewModels call this factory when mapping errors to UiState.

### D2: Press-scale as a reusable Modifier extension

Rather than duplicating animation code in each card composable, create `Modifier.pressScale()` as an extension function in `PressScaleModifier.kt`. This follows the existing pattern of `Modifier.shimmer()` in `ShimmerModifier.kt`.

### D3: Reduce motion via system accessibility

Android's `Settings.Global.ANIMATOR_DURATION_SCALE` controls animation duration. When set to 0 (reduce motion), Compose animations complete instantly. For infinite animations (pulse), we'll check this setting and skip the animation entirely.

### D4: Error detail expansion scope

Error details (HTTP status, URL) are only available for HTTP errors. Network errors and SSL errors won't have an HTTP status code. The expandable detail section will conditionally show whatever detail info is available in the AppError.

### D5: Flow.asResult() extension (NiA pattern)

Inspired by Now in Android's `Result<T>` + `asResult()` Flow extension. Creates a reusable `Flow<Result<T>>.asResult()` that wraps any flow into `Loading → Success → Error`, eliminating the repeated try/catch/fold pattern in every ViewModel.

**Location**: `data/ResultExtensions.kt` — lives in the data layer since it operates on `Result<T>` from repositories.

**Impact**: ViewModels that currently do manual `result.fold(onSuccess = { ... }, onFailure = { ... })` will simplify to collecting from a flow that already carries the state transitions. Combined with `AppError.from()`, error mapping happens in one place.

### D6: Centralized AapIcons registry (NiA pattern)

Inspired by NiA's `NiaIcons` object. Consolidates all icon references (status icons, error icons, navigation icons) into a single `AapIcons` object. Components reference `AapIcons.Running` instead of `Icons.Default.PlayCircle`.

**Benefits**: Single place to change icons, prevents inconsistent icon usage across screens, makes the icon vocabulary discoverable.

### D7: StatusColors via CompositionLocal (NiA pattern)

Inspired by NiA's custom `LocalGradientColors` / `LocalTintTheme` pattern. Wraps job status colors in a `StatusColors` data class provided via `LocalStatusColors` CompositionLocal in `AapRemoteTheme`.

**Current state**: Status colors are top-level constants in `Color.kt` (`StatusRunning`, `StatusFailed`, etc.), imported directly. This makes them static — same colors in light and dark mode.

**New state**: Components access via `AapRemoteTheme.statusColors.running`, which could provide different color values per theme if needed in the future.

### D8: Error state entrance animation

Error displays enter with `AnimatedVisibility(fadeIn + slideInVertically)` instead of appearing instantly. Follows NiA's pattern from `ForYouScreen` loading overlay. Provides visual continuity when errors appear, especially during polling when content is already visible.

## Complexity Tracking

> No constitution violations — no complexity justification needed.

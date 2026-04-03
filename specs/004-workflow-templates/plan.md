# Implementation Plan: Workflow Templates Support

**Branch**: `004-workflow-templates` | **Date**: 2026-04-03 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/004-workflow-templates/spec.md`

## Summary

Add workflow job template browsing, search/filter, launching, and job status monitoring to the existing "Workflow Templates" segment under the Templates tab. The implementation mirrors the existing job templates pattern (TemplateRepository → TemplatesViewModel → TemplateListScreen) with workflow-specific models, a new repository, ViewModel, and screens. A workflow job status screen displays sub-jobs (workflow nodes). Workflow jobs also appear in the Activity > Jobs list via the AAP unified jobs endpoint.

## Technical Context

**Language/Version**: Kotlin (latest stable, targeting JVM 17)
**Primary Dependencies**: Jetpack Compose (Material 3 BOM), Navigation Compose, Retrofit + Kotlin Serialization, Koin
**Storage**: DataStore + Tink (unchanged — no new storage needs)
**Testing**: Manual verification in Android Studio (no automated test framework in project)
**Target Platform**: Android (minSdk 31, compileSdk 35)
**Project Type**: Mobile app (Android)
**Performance Goals**: List loads within 2 seconds, search results update within 500ms
**Constraints**: HTTPS-only, single ComponentActivity, no Fragments/XML
**Scale/Scope**: ~10 new/modified files, follows existing patterns exactly

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Kotlin-Only | PASS | All new code in Kotlin |
| II. Compose-First UI | PASS | All UI in Jetpack Compose with Material 3 |
| III. MVVM + UDF | PASS | New ViewModel with StateFlow, sealed UiState |
| IV. Security-First | PASS | No new credential handling; reuses existing auth interceptor |
| V. Lean Dependencies | PASS | No new dependencies — reuses Retrofit, Koin, Compose |
| VI. API-Driven | PASS | New endpoints added to AapApiService Retrofit interface |

## Project Structure

### Documentation (this feature)

```text
specs/004-workflow-templates/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
app/src/main/kotlin/com/example/aapremote/
├── model/
│   ├── WorkflowJobTemplate.kt      # NEW — data class for workflow templates
│   ├── WorkflowJob.kt              # NEW — data class for workflow jobs
│   └── WorkflowNode.kt             # NEW — data class for workflow nodes (sub-jobs)
├── network/
│   └── AapApiService.kt            # MODIFIED — add workflow endpoints
├── data/
│   ├── WorkflowRepository.kt       # NEW — workflow template + job API calls
│   └── DataModule.kt               # MODIFIED — register WorkflowRepository
├── presentation/
│   ├── workflows/
│   │   ├── WorkflowTemplatesViewModel.kt   # NEW — list/search/filter/launch
│   │   ├── WorkflowTemplatesUiState.kt     # NEW — sealed UI state
│   │   ├── WorkflowJobStatusUiState.kt     # NEW — sealed UI state for status screen
│   │   └── WorkflowJobStatusViewModel.kt   # NEW — workflow job status polling + sub-job stdout
│   └── PresentationModule.kt       # MODIFIED — register new ViewModels
├── ui/
│   ├── workflows/
│   │   ├── WorkflowTemplateListScreen.kt   # NEW — list screen (mirrors TemplateListScreen)
│   │   ├── WorkflowTemplateListItem.kt     # NEW — card component
│   │   └── WorkflowJobStatusScreen.kt      # NEW — status with sub-jobs list
│   └── components/
│       └── WorkflowNodeItem.kt             # NEW — expandable sub-job row with inline stdout
├── navigation/
│   ├── MainNavigation.kt           # MODIFIED — route Workflow Templates segment
│   └── AppNavigation.kt            # MODIFIED — add workflow job status route
└── ui/main/
    └── TabDefinitions.kt           # MODIFIED — mark Workflow Templates as implemented
```

**Structure Decision**: Follows existing feature-based packaging within each layer. Workflow-specific files go in `workflows/` packages mirroring the `templates/` and `jobs/` pattern. Shared components (SearchBar, LabelChips, SkeletonCard, ExtraVarsDialog, LaunchConfirmDialog) are reused from `ui/components/`.

## Complexity Tracking

No constitution violations to justify. All patterns mirror existing implementations.

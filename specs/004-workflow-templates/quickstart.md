# Quickstart: Workflow Templates Support

**Feature**: 004-workflow-templates | **Date**: 2026-04-03

## Integration Scenarios

### Scenario 1: Browse Workflow Templates

1. User logs in and navigates to Templates tab
2. User taps "Workflow Templates" segment
3. Skeleton loading cards display while data loads
4. Workflow templates appear with name, description, and label chips
5. User scrolls to bottom — more templates load automatically

**Key files**: `WorkflowTemplateListScreen.kt`, `WorkflowTemplatesViewModel.kt`, `WorkflowRepository.kt`

### Scenario 2: Search and Filter

1. User types in search bar — list filters after 300ms debounce
2. User taps a label chip — list filters to that label
3. User clears filter — full list restores

**Key files**: `WorkflowTemplatesViewModel.kt` (search/filterByLabel methods)

### Scenario 3: Launch Workflow Template

1. User taps play button on a workflow template card
2. If `askVariablesOnLaunch` is true → ExtraVarsDialog appears first
3. LaunchConfirmDialog appears → user confirms
4. App calls `POST workflow_job_templates/{id}/launch/`
5. On success → navigates to WorkflowJobStatusScreen with returned workflow_job ID
6. On error → snackbar shows error message

**Key files**: `WorkflowTemplatesViewModel.kt`, `WorkflowRepository.kt`, `AppNavigation.kt`

### Scenario 4: Monitor Workflow Job Status

1. WorkflowJobStatusScreen loads with workflow job ID from navigation argument
2. Polls `GET workflow_jobs/{id}/` every 5 seconds while job is active
3. Also fetches `GET workflow_jobs/{id}/workflow_nodes/` to show sub-jobs
4. Displays: job name, status badge, template name, start/finish times, elapsed time
5. Sub-jobs section shows list of workflow nodes with name and status badge
6. Tap any sub-job to expand it inline and view its stdout output (fetched on demand, cached)
7. Tap the expanded sub-job again to collapse it
8. When job reaches terminal status, polling stops

**Key files**: `WorkflowJobStatusScreen.kt`, `WorkflowJobStatusViewModel.kt`, `WorkflowRepository.kt`

### Scenario 5: Workflow Jobs in Activity Tab

1. User navigates to Activity > Jobs
2. Existing RecentJobsScreen loads jobs from `GET /api/v2/jobs/`
3. Workflow jobs appear alongside regular jobs (AAP includes them in unified response)
4. No code changes needed — the existing endpoint already returns workflow jobs

**Key files**: No changes — existing `RecentJobsScreen` and `JobRepository` handle this automatically.

## Build Sequence

1. **Models** — WorkflowJobTemplate, WorkflowJob, WorkflowNode, WorkflowLaunchResponse
2. **API** — Add 4 endpoints to AapApiService
3. **Repository** — WorkflowRepository with list/launch/status/nodes methods
4. **DI** — Register WorkflowRepository and ViewModels in Koin modules
5. **ViewModels** — WorkflowTemplatesViewModel + WorkflowJobStatusViewModel
6. **UI** — WorkflowTemplateListItem, WorkflowTemplateListScreen, WorkflowNodeItem, WorkflowJobStatusScreen
7. **Navigation** — Wire segment routing + workflow job status route
8. **Tab** — Mark "Workflow Templates" segment as implemented

## Shared Components (reused, no changes)

- `SearchBar` — search input with debounce
- `LabelChips` — horizontal label filter chips
- `SkeletonCard` — shimmer loading placeholder
- `LaunchConfirmDialog` — launch confirmation dialog
- `ExtraVarsDialog` — extra variables input dialog
- `ErrorMessage` — error display with retry button
- `JobStatusBadge` — status indicator chip (reusable for workflow job statuses)

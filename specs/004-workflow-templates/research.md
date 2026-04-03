# Research: Workflow Templates Support

**Feature**: 004-workflow-templates | **Date**: 2026-04-03

## Research Tasks

### 1. Workflow Job Templates API Structure

**Decision**: The AAP `/api/v2/workflow_job_templates/` endpoint follows the same paginated response pattern as `/api/v2/job_templates/`. The response includes `count`, `next`, `previous`, and `results` fields. Each result has `id`, `name`, `description`, `ask_variables_on_launch`, and `summary_fields` (containing `labels` and `user_capabilities` with `start` boolean).

**Rationale**: The AAP Controller API is consistent across resource types. Workflow job templates use the same pagination, filtering (`search`, `labels__name__icontains`), and ordering (`order_by`) query parameters as job templates. This was confirmed by the spec assumptions and AAP API v2 documentation patterns.

**Alternatives considered**:
- Custom pagination implementation → rejected; reuse existing `PaginatedResponse<T>` generic
- Different query parameter names → not needed; AAP uses the same parameter names across template types

### 2. Workflow Job Templates Launch API

**Decision**: Launch endpoint is `POST /api/v2/workflow_job_templates/{id}/launch/`. It accepts the same `extra_vars` body parameter as job template launch. The response returns a `workflow_job` field (integer ID) instead of `job` field.

**Rationale**: AAP API uses a consistent launch pattern but returns the job reference under a type-specific key. The existing `LaunchRequest` model can be reused. A new `WorkflowLaunchResponse` model is needed for the different response key.

**Alternatives considered**:
- Reuse `LaunchResponse` with nullable fields → rejected; cleaner to have a dedicated response model for the different field name
- Unified launch response with `@JsonNames` → adds complexity; separate model is simpler

### 3. Workflow Job Status API

**Decision**: Workflow job status is at `GET /api/v2/workflow_jobs/{id}/`. Response fields mirror regular jobs: `id`, `name`, `status`, `failed`, `started`, `finished`, `elapsed`, `summary_fields`. The `status` field uses the same enum values as regular jobs (new, pending, waiting, running, successful, failed, error, canceled).

**Rationale**: The existing `JobStatus` enum is reusable. A new `WorkflowJob` model is needed because the `summary_fields` structure differs (references `workflow_job_template` instead of `job_template`).

**Alternatives considered**:
- Reuse `Job` model → rejected; different summary_fields structure and different API endpoint
- Abstract base class for Job/WorkflowJob → over-engineering; the models are small and simple

### 4. Workflow Nodes (Sub-Jobs) API

**Decision**: Workflow nodes are fetched via `GET /api/v2/workflow_jobs/{id}/workflow_nodes/`. Returns paginated list of nodes, each with `id`, `job` (reference to the actual sub-job ID), and `summary_fields` containing `job` with `id`, `name`, `status`, `type` (e.g., "job", "project_update"). The node also has `do_not_run` and `unified_job_template` fields.

**Rationale**: To display sub-job names and statuses as specified in FR-015, we need the workflow_nodes endpoint. The `summary_fields.job` contains the displayable name and status without requiring a separate API call per node.

**Alternatives considered**:
- Fetch each sub-job individually via `/api/v2/jobs/{id}/` → rejected; N+1 query problem, workflow_nodes endpoint provides all needed data in one call
- Use `workflow_jobs/{id}/` response's `related.workflow_nodes` URL → same endpoint, just a different way to discover it

### 5. Unified Jobs Endpoint for Activity Tab

**Decision**: The existing `GET /api/v2/jobs/` endpoint in AAP already returns both regular jobs and workflow jobs in a unified list. The `type` field distinguishes them (`job` vs `workflow_job`). No additional endpoint needed for FR-016.

**Rationale**: AAP's `/api/v2/jobs/` is actually the unified jobs endpoint that includes all job types. The current `RecentJobsScreen` will automatically show workflow jobs once they exist on the server. However, the `Job` model may need a `type` field to distinguish them in the UI, and the `summary_fields` for workflow jobs will reference `workflow_job_template` instead of `job_template`.

**Alternatives considered**:
- Separate API call to `/api/v2/workflow_jobs/` merged client-side → rejected; unnecessary complexity when `/api/v2/jobs/` already includes them
- `/api/v2/unified_jobs/` endpoint → this is the actual name in some AAP versions, but `/api/v2/jobs/` works equivalently for our needs

### 6. Workflow Job Status Screen - Stdout Handling

**Decision**: Workflow jobs do not produce stdout directly (they orchestrate sub-jobs). The workflow job status screen will not attempt to fetch stdout. Individual sub-job stdout could be viewed by navigating to the sub-job's regular job status screen (future enhancement).

**Rationale**: Per spec assumption: "Workflow job stdout output may not be available." The status screen focuses on showing overall workflow status and sub-job statuses.

**Alternatives considered**:
- Fetch stdout for each sub-job → scope creep; deferred to potential future enhancement
- Show "No output available" placeholder → acceptable fallback if stdout endpoint returns empty/error

### 7. Polling Strategy for Workflow Job Status

**Decision**: Reuse the same 5-second polling interval as `JobRepository.pollJobStatus()`. Poll both the workflow job status and workflow nodes in parallel during each poll cycle.

**Rationale**: Consistent with existing job status polling. Workflow jobs may run longer, but the polling interval is already reasonable. Fetching nodes on each poll ensures sub-job status updates are reflected.

**Alternatives considered**:
- WebSocket-based updates → AAP doesn't expose WebSocket for job status; polling is the standard approach
- Longer polling interval for workflows → unnecessary differentiation; 5 seconds is fine
- Poll nodes separately at different interval → adds complexity for minimal benefit

# Data Model: Workflow Templates Support

**Feature**: 004-workflow-templates | **Date**: 2026-04-03

## Entities

### WorkflowJobTemplate

Represents a workflow job template from the AAP instance. Mirrors `JobTemplate` structure.

| Field | Type | Source JSON | Default | Notes |
|-------|------|-------------|---------|-------|
| id | Int | `id` | required | Unique template identifier |
| name | String | `name` | required | Display name |
| description | String | `description` | `""` | Optional description text |
| askVariablesOnLaunch | Boolean | `ask_variables_on_launch` | `false` | Whether to prompt for extra vars |
| status | String? | `status` | `null` | Last run status (may be null) |
| lastJobRun | String? | `last_job_run` | `null` | ISO timestamp of last run |
| summaryFields | WorkflowTemplateSummaryFields | `summary_fields` | default | Nested labels + capabilities |

**Nested: WorkflowTemplateSummaryFields**

| Field | Type | Source JSON | Default |
|-------|------|-------------|---------|
| labels | LabelSummary | `labels` | `LabelSummary()` |
| userCapabilities | UserCapabilities | `user_capabilities` | `UserCapabilities()` |

Reuses existing `LabelSummary`, `Label`, and `UserCapabilities` model classes.

### WorkflowJob

Represents a running or completed workflow job instance.

| Field | Type | Source JSON | Default | Notes |
|-------|------|-------------|---------|-------|
| id | Int | `id` | required | Unique job identifier |
| name | String | `name` | `""` | Job name |
| status | JobStatus | `status` | required | Reuses existing JobStatus enum |
| failed | Boolean | `failed` | `false` | Whether the job failed |
| started | String? | `started` | `null` | ISO timestamp |
| finished | String? | `finished` | `null` | ISO timestamp |
| elapsed | Double? | `elapsed` | `null` | Duration in seconds |
| summaryFields | WorkflowJobSummaryFields | `summary_fields` | default | Nested template ref |

**Nested: WorkflowJobSummaryFields**

| Field | Type | Source JSON | Default |
|-------|------|-------------|---------|
| workflowJobTemplate | WorkflowJobTemplateRef? | `workflow_job_template` | `null` |

**Nested: WorkflowJobTemplateRef**

| Field | Type | Source JSON | Default |
|-------|------|-------------|---------|
| id | Int | `id` | required |
| name | String | `name` | required |

### WorkflowNode

Represents a sub-job (node) within a workflow job execution.

| Field | Type | Source JSON | Default | Notes |
|-------|------|-------------|---------|-------|
| id | Int | `id` | required | Node identifier |
| summaryFields | WorkflowNodeSummaryFields | `summary_fields` | default | Contains job details |
| doNotRun | Boolean | `do_not_run` | `false` | Whether node was skipped |

**Nested: WorkflowNodeSummaryFields**

| Field | Type | Source JSON | Default |
|-------|------|-------------|---------|
| job | WorkflowNodeJob? | `job` | `null` |

**Nested: WorkflowNodeJob**

| Field | Type | Source JSON | Default |
|-------|------|-------------|---------|
| id | Int | `id` | required |
| name | String | `name` | required |
| status | JobStatus | `status` | required |
| type | String | `type` | `""` |

### WorkflowLaunchResponse

Response from launching a workflow template.

| Field | Type | Source JSON | Default | Notes |
|-------|------|-------------|---------|-------|
| workflowJob | Int | `workflow_job` | required | The created workflow job ID |
| id | Int | `id` | required | Same as workflow_job |
| status | String | `status` | `""` | Initial status |

## Reused Entities (no changes needed)

- **PaginatedResponse<T>** — generic paginated response wrapper (already parameterized)
- **Label** — label chip data (id, name)
- **LabelSummary** — label count + results list
- **UserCapabilities** — user permission flags (start boolean)
- **LaunchRequest** — launch body with extra_vars (reused for workflow launches)
- **JobStatus** — status enum (same values for workflow jobs)

## State Transitions

### WorkflowJob Status Flow

```
NEW → PENDING → WAITING → RUNNING → SUCCESSFUL
                                   → FAILED
                                   → ERROR
                    (any active) → CANCELED
```

Same as regular Job — reuses `JobStatus` enum with `isActive` and `isTerminal` properties.

## Relationships

```
WorkflowJobTemplate  --launches-->  WorkflowJob
WorkflowJob          --contains-->  WorkflowNode (1:many)
WorkflowNode         --references--> Job (via summary_fields.job)
```

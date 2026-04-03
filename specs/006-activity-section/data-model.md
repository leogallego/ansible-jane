# Data Model: Activity Section

**Feature Branch**: `006-activity-section`
**Date**: 2026-04-03

## Entities

### Schedule (New)

Represents a scheduled job or workflow template run in the Controller.

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `id` | Int | `id` | Primary key |
| `name` | String | `name` | Schedule name |
| `description` | String | `description` | Optional description |
| `enabled` | Boolean | `enabled` | Whether schedule is active |
| `rrule` | String | `rrule` | iCal recurrence rule |
| `dtstart` | String? | `dtstart` | Schedule start datetime (ISO 8601) |
| `dtend` | String? | `dtend` | Schedule end datetime (ISO 8601) |
| `timezone` | String? | `timezone` | Timezone for the schedule |
| `nextRun` | String? | `next_run` | Next scheduled execution (ISO 8601, needs verification) |
| `unifiedJobTemplate` | Int | `unified_job_template` | ID of associated template |
| `summaryFields` | ScheduleSummaryFields | `summary_fields` | Nested summary object |

#### ScheduleSummaryFields

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `unifiedJobTemplate` | UnifiedJobTemplateRef | `unified_job_template` | Template name, id, type |

#### UnifiedJobTemplateRef

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `id` | Int | `id` | Template ID |
| `name` | String | `name` | Template name |
| `unifiedJobType` | String | `unified_job_type` | "job" or "workflow_job" |

### EdaRuleAudit (New)

Represents a triggered EDA rule audit event.

| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `id` | Int | `id` | Primary key |
| `name` | String | `name` | Rule name |
| `status` | String | `status` | Audit status (e.g., "successful", "failed") |
| `activationInstanceId` | Int? | `activation_instance_id` | Associated activation instance |
| `firedAt` | String | `fired_at` | When the rule fired (ISO 8601) |
| `ruleName` | String? | `rule_name` | Name of the specific rule |
| `ruleSetName` | String? | `rule_set_name` | Name of the rule set |
| `rulesetName` | String? | `ruleset_name` | Alternative field name (EDA API variation) |
| `activationName` | String? | `activation_name` | Name of the activation |
| `createdAt` | String | `created_at` | Creation timestamp |

**Note**: Exact field names need verification against live EDA API. Model uses `ignoreUnknownKeys = true` (already configured in `networkJson`).

### Job (Existing — Enhanced)

No model changes. New query parameters added to the API service for status filtering.

### JobStatus (Existing — No Changes)

Existing enum: `NEW`, `PENDING`, `WAITING`, `RUNNING`, `SUCCESSFUL`, `FAILED`, `ERROR`, `CANCELED`.

Used as filter chip values in the Jobs segment.

## Relationships

```
Schedule --> UnifiedJobTemplate (via summary_fields)
EdaRuleAudit --> Activation (via activation_name / activation_instance_id)
Job --> JobTemplate (existing, via summary_fields)
```

## State Transitions

### Schedule Toggle
```
Enabled ──PATCH {enabled: false}──> Disabled
Disabled ──PATCH {enabled: true}──> Enabled
```

On failure: revert UI toggle, show error snackbar.

## Validation Rules

- Schedule `enabled` toggle: optimistic UI update, revert on API failure
- EDA audit events: read-only, no user mutations
- Job status filters: client-side enum values, server-side filtering via query params
- Empty states: each segment independently handles empty/error states

# Research: Activity Section

**Feature Branch**: `006-activity-section`
**Date**: 2026-04-03

## Decision 1: Schedule API Endpoint and Fields

**Decision**: Use Controller `/api/v2/schedules/` endpoint with standard AWX pagination.

**Rationale**: The `ansible.controller.schedule` module and `infra.controller_configuration` roles confirm this is the standard endpoint. The module supports `enabled` as a writable boolean field, confirming PATCH toggle is supported.

**Key fields (from collection source analysis)**:
- Core: `id`, `name`, `description`, `enabled` (bool), `rrule` (iCal recurrence rule)
- Timing: `dtstart`, `dtend`, `timezone`, `next_run` (needs runtime verification)
- Relationship: `unified_job_template` (int ID), `summary_fields.unified_job_template.name`
- Pagination: standard `page`, `page_size`, `order_by` query params

**Toggle mechanism**: PATCH `/api/v2/schedules/{id}/` with `{"enabled": true/false}`

**Alternatives considered**: None — this is the only schedules API.

**Verification needed at implementation time**: Confirm `next_run` field name with a live API call.

## Decision 2: EDA Audit Rules Endpoint and Fields

**Decision**: Use EDA Controller `/api/eda/v1/audit-rules/` endpoint.

**Rationale**: Confirmed from the EDA API index (`api-eda.json`). No Ansible collection module exists for audit-rules (read-only resource), but the endpoint is listed and follows EDA API conventions seen in `rulebook_activation_info.py`.

**Expected fields (based on EDA API conventions)**:
- Core: `id`, `name`, `status`, `fired_at` (timestamp)
- Relationships: `activation_instance` (object with `id`, `name`), `rulebook` (name), `rule` (name)
- Metadata: `created_at`, `modified_at`
- Pagination: `page`, `page_size` (EDA uses same Django REST pagination)

**Alternatives considered**: 
- Using `activation-instances/` — rejected, too broad; audit-rules gives per-rule granularity.

**Verification needed at implementation time**: Confirm exact field names with a live API call. Build model with `@Serializable` + `ignoreUnknownKeys = true` to handle any unexpected fields.

## Decision 3: Jobs Status Filtering

**Decision**: Use `?status=<value>` query parameter for single status, `?or__status=<value1>&or__status=<value2>` for multi-select OR filtering.

**Rationale**: AAP API docs (PDF Ch. 6) confirm Django-style filtering. By default filters are AND-ed; `or__` prefix creates OR conditions. Field lookups like `__in` are also supported.

**Key details**:
- Single filter: `?status=failed`
- Multi-select OR: `?or__status=failed&or__status=error`
- Alternative: `?status__in=failed,error` (Django `__in` lookup — needs verification)

**Alternatives considered**: Client-side filtering — rejected for performance; server-side filtering reduces data transfer.

## Decision 4: EDA Unavailability Handling

**Decision**: Treat EDA API failures independently. Catch exceptions in the EDA repository and return a specific error state that does not affect Controller-based segments (Jobs, Schedules).

**Rationale**: Spec FR-011 requires graceful degradation. Since EDA goes through a different API base path (`/api/eda/v1/`), failures are naturally isolated at the network layer.

**Implementation approach**: Separate Retrofit service interface (or separate base URL) for EDA endpoints. Repository returns `Result.failure()` with a descriptive message; UI shows empty state with explanation.

## Decision 5: Navigation Architecture for Activity Tab

**Decision**: Reuse existing `RecentJobsScreen` for the Jobs segment (with added filter chips), add new screens for Schedules and EDA Audit.

**Rationale**: The Activity tab with three segments (Jobs, Schedules, EDA Audit) is already defined in `TabDefinitions.kt`. The Jobs segment already renders `RecentJobsScreen`. Adding filter chips to the existing screen is less disruptive than creating a new one.

**Alternatives considered**: Creating a brand new `ActivityJobsScreen` — rejected to avoid duplicating the existing job list implementation.

## Decision 6: EDA API Service Architecture

**Decision**: Create a separate Retrofit service interface (`EdaApiService`) for EDA endpoints, sharing the same OkHttpClient and auth interceptor.

**Rationale**: EDA uses a different base path (`/api/eda/v1/`) than Controller (`/api/v2/`). A separate interface keeps the API contracts clear. The `AapApiProvider` already manages base URL construction; it can provide both services using the same authenticated client.

**Alternatives considered**: Adding EDA endpoints to `AapApiService` with full paths — rejected for clarity; mixing two different API versioning schemes in one interface is confusing.

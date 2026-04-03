# Feature Specification: Activity Section

**Feature Branch**: `006-activity-section`
**Created**: 2026-04-03
**Status**: Draft
**Input**: GitHub issue #6: Add Activity section (Jobs, Schedules, EDA audit)

## User Scenarios & Testing

### User Story 1 - Browse and Filter Job History (Priority: P1)

As an AAP operator, I want to view my job history with status filtering so I can quickly find failed jobs or check recent activity from my phone.

**Why this priority**: Job history is the most frequently accessed activity view. The existing Recent Jobs screen already provides basic functionality — this story enhances it with filtering and moves it to the Activity tab.

**Independent Test**: Can be fully tested by navigating to Activity > Jobs, viewing the job list, applying status filters, and tapping a job to see details. Delivers immediate value as a standalone feature.

**Acceptance Scenarios**:

1. **Given** the user is authenticated, **When** they navigate to the Activity tab, **Then** the Jobs segment is selected by default and shows a paginated list of recent jobs ordered by creation date.
2. **Given** the user is viewing the Jobs list, **When** they tap one or more status filter chips (horizontal scrollable row supporting multi-select), **Then** only jobs matching the selected statuses are displayed.
3. **Given** the user is viewing the Jobs list, **When** they tap a job, **Then** they are navigated to the existing job detail/stdout screen.
4. **Given** the user is viewing a filtered job list, **When** they clear the filter, **Then** all jobs are shown again.
5. **Given** the user is viewing the Jobs list, **When** they pull to refresh, **Then** the list reloads with the latest data.

---

### User Story 2 - View and Manage Schedules (Priority: P2)

As an AAP operator, I want to view scheduled jobs and toggle them on/off so I can manage recurring automation from my phone without logging into the web UI.

**Why this priority**: Schedules are a key operational concern — knowing what's scheduled and being able to quickly disable a problematic schedule from a mobile device is a high-value remote control feature.

**Independent Test**: Can be fully tested by navigating to Activity > Schedules, viewing the schedule list with next run times, and toggling a schedule on/off.

**Acceptance Scenarios**:

1. **Given** the user is authenticated, **When** they navigate to Activity > Schedules, **Then** they see a list of schedules showing name, associated template, next run time, and enabled/disabled status.
2. **Given** the user is viewing a schedule, **When** they toggle the enabled switch, **Then** the schedule's enabled state is updated on the server and reflected in the UI.
3. **Given** a schedule toggle fails (e.g., network error), **When** the error occurs, **Then** the toggle reverts to its previous state and an error message is shown.
4. **Given** the user is viewing schedules, **When** they pull to refresh, **Then** the list reloads with the latest data including updated next run times.

---

### User Story 3 - View EDA Rule Audit Events (Priority: P3)

As an AAP operator, I want to view Event-Driven Ansible rule audit events so I can monitor which rules have been triggered and check activation status.

**Why this priority**: EDA audit is a newer AAP 2.5+ feature. While valuable for operators managing event-driven automation, it serves a more specialized audience compared to Jobs and Schedules.

**Independent Test**: Can be fully tested by navigating to Activity > EDA Audit, viewing the list of rule audit events, and checking activation details.

**Acceptance Scenarios**:

1. **Given** the user is authenticated, **When** they navigate to Activity > EDA Audit, **Then** they see a list of rule audit events showing rule name, status, and timestamp.
2. **Given** the user is viewing EDA audit events, **When** they tap an event, **Then** they see details including the activation name, rule name, status, and fired timestamp.
3. **Given** EDA is not configured on the AAP instance, **When** the user navigates to EDA Audit, **Then** they see an appropriate empty state message (not an error).
4. **Given** the user is viewing EDA audit events, **When** they pull to refresh, **Then** the list reloads with the latest events.

---

### Edge Cases

- What happens when the user has no jobs at all? (empty state with helpful message)
- What happens when there are no schedules configured? (empty state)
- What happens when the EDA controller is unreachable but Controller is fine? (graceful degradation — show error only in EDA tab, Jobs/Schedules unaffected)
- What happens when the user applies a filter that returns no results? (empty state with "No jobs matching filter" message)
- What happens when a schedule toggle request times out? (revert toggle, show error)
- What happens when pagination reaches the end? (stop loading indicator, no more "load more")

## Requirements

### Functional Requirements

- **FR-001**: System MUST display a paginated list of jobs under the Activity > Jobs segment, ordered by creation date (newest first).
- **FR-002**: System MUST provide status-based filtering for the Jobs list via horizontal scrollable filter chips supporting multi-select (successful, failed, error, canceled, running, pending).
- **FR-003**: System MUST allow navigation from a job list item to the existing job detail screen.
- **FR-004**: System MUST display a paginated list of schedules under the Activity > Schedules segment, showing name, associated template, next run time, and enabled status.
- **FR-005**: System MUST allow toggling a schedule's enabled/disabled state with immediate server-side update.
- **FR-006**: System MUST revert the toggle and show an error message if a schedule toggle operation fails.
- **FR-007**: System MUST display a paginated list of EDA rule audit events under the Activity > EDA Audit segment, showing rule name, status, and timestamp.
- **FR-008**: System MUST show event details when an EDA audit event is tapped.
- **FR-009**: System MUST support pull-to-refresh on all three segments.
- **FR-010**: System MUST handle empty states gracefully with appropriate messages for each segment.
- **FR-011**: System MUST handle EDA unavailability independently from Controller features — failures in EDA should not affect Jobs or Schedules.
- **FR-012**: System MUST support infinite scroll pagination consistent with existing template lists.

### Key Entities

- **Job**: Existing entity — represents a completed or in-progress automation job (already implemented).
- **Schedule**: Represents a scheduled job or workflow template run. Key attributes: name, associated template, next run time, enabled status, recurrence rule (rrule).
- **EDA Rule Audit Event**: Represents a triggered EDA rule. Key attributes: rule name, activation name, status, fired timestamp.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can find a specific failed job within 10 seconds using status filtering.
- **SC-002**: Users can toggle a schedule on/off in under 3 seconds from opening the Activity tab.
- **SC-003**: All three Activity segments (Jobs, Schedules, EDA Audit) load their initial data within 3 seconds on a typical connection.
- **SC-004**: Users can monitor EDA rule activations without switching to the AAP web UI.
- **SC-005**: The Activity tab maintains consistent UX patterns (pagination, pull-to-refresh, error handling) with the existing Templates tab.

## Clarifications

### Session 2026-04-03

- Q: Job status filter UI mechanism? → A: Filter chips — horizontal scrollable row, multi-select, tap to toggle.

## Assumptions

- The existing Recent Jobs screen and Job detail screen will be reused/relocated, not rewritten.
- The AAP instance has the Schedules API available at the standard Controller endpoint.
- EDA audit events are accessible via the EDA Controller API through the Gateway at `/api/eda/v1/`.
- EDA may not be configured on all AAP instances — the app must handle this gracefully.
- Job status values are consistent with the existing `JobStatus` enum already in the codebase.
- Schedule toggling requires only a PATCH request to update the `enabled` field.
- The user has sufficient AAP permissions to view schedules and EDA audit events (the app does not manage permissions).

# Feature Specification: Workflow Templates Support

**Feature Branch**: `004-workflow-templates`  
**Created**: 2026-04-03  
**Status**: Draft  
**Input**: GitHub issue #4 — Add Workflow Templates support

## Clarifications

### Session 2026-04-03

- Q: Should the workflow job status screen show sub-job details? → A: Show top-level status plus a simple list of sub-jobs with their individual statuses. Visual node graph deferred to issue #9.
- Q: Should workflow jobs appear in the Activity > Jobs list? → A: Yes, show workflow jobs alongside regular jobs in Activity > Jobs.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse Workflow Templates (Priority: P1)

As a user on the Templates tab, I can switch to the "Workflow Templates" segment and see a list of workflow job templates from my AAP instance. Each template shows its name, description, and labels. I can scroll through the list with pagination loading more templates as I reach the bottom.

**Why this priority**: Browsing is the foundational interaction — users need to see what workflow templates exist before they can do anything with them. This replaces the current placeholder screen with real content.

**Independent Test**: Can be tested by logging in, navigating to Templates > Workflow Templates, and verifying the list displays workflow templates from the AAP server with names, descriptions, and labels.

**Acceptance Scenarios**:

1. **Given** the user is on the Templates tab, **When** they tap the "Workflow Templates" segment, **Then** the content switches to show a list of workflow job templates from the connected AAP instance.
2. **Given** the user is viewing the workflow templates list, **When** templates are loading, **Then** skeleton loading placeholders are displayed instead of a spinner.
3. **Given** the user has scrolled near the bottom of the list, **When** more templates are available, **Then** additional templates load automatically (pagination).
4. **Given** there are no workflow templates on the server, **When** the list loads, **Then** an empty state message "No workflow templates available" is displayed.

---

### User Story 2 - Search and Filter Workflow Templates (Priority: P1)

As a user viewing the workflow templates list, I can search by name and filter by label to find specific workflow templates, just like I do with job templates.

**Why this priority**: With many workflow templates, users need search and filter to find what they need quickly. This matches the existing job templates experience and is essential for usability.

**Independent Test**: Can be tested by typing in the search bar and verifying results filter, and by tapping label chips to filter by label.

**Acceptance Scenarios**:

1. **Given** the user is on the workflow templates list, **When** they type in the search bar, **Then** the list filters to show only templates matching the search query.
2. **Given** workflow templates have labels, **When** the user taps a label chip, **Then** the list filters to show only templates with that label.
3. **Given** the user has an active search or filter, **When** they clear it, **Then** the full template list is restored.

---

### User Story 3 - Launch Workflow Template (Priority: P1)

As a user, I can launch a workflow template directly from the list. If the template requires extra variables, I am prompted to enter them before launch. After launching, I am navigated to a workflow job status screen to monitor progress.

**Why this priority**: Launching workflows is the primary action users want to take. Without it, the workflow templates list is view-only and limited in value.

**Independent Test**: Can be tested by tapping a workflow template's launch button, confirming the launch dialog, and verifying navigation to the workflow job status screen.

**Acceptance Scenarios**:

1. **Given** the user has permission to launch a workflow template, **When** they tap the play button on a template card, **Then** a confirmation dialog appears asking to confirm the launch.
2. **Given** a workflow template requires extra variables, **When** the user taps launch, **Then** an extra variables input dialog appears before the confirmation.
3. **Given** the user confirms the launch, **When** the workflow job is created successfully, **Then** the user is navigated to the workflow job status screen.
4. **Given** the user does not have permission to launch, **When** they view the template card, **Then** no launch button is displayed.

---

### User Story 4 - Monitor Workflow Job Status (Priority: P2)

As a user who has launched a workflow template, I can view the workflow job status screen showing the job's current state, progress, and details. The status updates automatically while the job is running.

**Why this priority**: Monitoring job status completes the launch-and-track loop. Without it, users launch workflows blindly with no feedback. It is P2 because users can still see workflow jobs in the Activity > Jobs list as a workaround.

**Independent Test**: Can be tested by launching a workflow template and verifying the status screen shows job name, status, elapsed time, and auto-updates while running.

**Acceptance Scenarios**:

1. **Given** the user has just launched a workflow template, **When** they are navigated to the status screen, **Then** the workflow job's name, status, and template name are displayed.
2. **Given** a workflow job is running, **When** the user views the status screen, **Then** the status updates automatically via polling.
3. **Given** a workflow job has completed (success or failure), **When** the user views the status screen, **Then** the final status, elapsed time, and start/finish timestamps are shown.
4. **Given** the user is on the workflow job status screen, **When** they view the sub-jobs section, **Then** a list of sub-jobs is displayed with each sub-job's name and current status.
5. **Given** the user is on the workflow job status screen, **When** they tap a sub-job, **Then** the sub-job expands inline to show its stdout output. Tapping again collapses it.
6. **Given** the user is on the workflow job status screen, **When** they press back, **Then** they return to the workflow templates list with their previous state preserved.

---

### User Story 5 - Pull-to-Refresh Workflow Templates (Priority: P2)

As a user viewing the workflow templates list, I can pull down to refresh the data and see updated templates from the server.

**Why this priority**: Pull-to-refresh is a standard mobile interaction that users expect, consistent with the job templates and recent jobs lists.

**Independent Test**: Can be tested by pulling down on the workflow templates list and verifying data reloads with a visible refresh indicator.

**Acceptance Scenarios**:

1. **Given** the user is on the workflow templates list, **When** they pull down, **Then** a refresh indicator appears and the list reloads from the server.
2. **Given** a refresh fails due to a network error, **When** the refresh completes, **Then** an error message is shown and the existing list data is preserved.

---

### Edge Cases

- What happens when the AAP instance does not support workflow templates (older version)? The API will return 404; the app MUST display an appropriate error message (e.g., "Workflow templates are not available on this AAP instance") and MUST NOT crash.
- What happens when a workflow template launch fails on the server side? An error snackbar should display the server's error message.
- What happens when the user rotates the device while on the workflow templates list? The list state, search query, selected label, and scroll position should be preserved. *(Handled by architecture: ViewModel survives configuration changes; Compose restores scroll position via LazyListState.)*
- What happens when a workflow job is launched but the status endpoint returns an error? The status screen should show an error with a retry option.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The "Workflow Templates" segment under the Templates tab MUST display a list of workflow job templates fetched from the AAP instance.
- **FR-002**: Each workflow template card MUST display the template name, description (if present), labels (as chips), and a launch button (if the user has launch permission).
- **FR-003**: The workflow templates list MUST support infinite scroll pagination, loading more templates as the user scrolls near the bottom.
- **FR-004**: The workflow templates list MUST show skeleton loading placeholders during initial data loading.
- **FR-005**: The workflow templates list MUST support search by template name with 300ms debounced input.
- **FR-006**: The workflow templates list MUST support filtering by label using label chips.
- **FR-007**: Users with launch permission MUST be able to launch a workflow template via a confirmation dialog.
- **FR-008**: If a workflow template requires extra variables on launch, the app MUST present an extra variables input dialog before confirmation.
- **FR-009**: After a successful launch, the app MUST navigate the user to a workflow job status screen.
- **FR-010**: The workflow job status screen MUST display the job's name, status, template name, start time, finish time, and elapsed time.
- **FR-011**: The workflow job status screen MUST poll for status updates while the job is active (running, pending, waiting).
- **FR-012**: The workflow templates list MUST support pull-to-refresh to reload data from the server.
- **FR-013**: The workflow template cards MUST use the same enhanced card design (elevated card, typography hierarchy, label chips) as job template cards.
- **FR-014**: The workflow templates list MUST display an empty state message when no templates are available.
- **FR-015**: The workflow job status screen MUST display a list of sub-jobs (workflow nodes) with each sub-job's name and current status.
- **FR-016**: Workflow jobs MUST appear alongside regular jobs in the Activity > Jobs list.
- **FR-017**: Users MUST be able to tap a sub-job in the workflow job status screen to expand it inline and view its stdout output. Only one sub-job can be expanded at a time. Stdout is fetched on first expansion and cached for the session.

### Key Entities

- **WorkflowJobTemplate**: A workflow template on the AAP instance with a name, description, labels, launch permissions, and an option to require extra variables on launch. Similar structure to a job template but fetched from a different endpoint.
- **WorkflowJob**: A running or completed instance of a workflow template with a status, timestamps, and associated template reference. Similar to a regular job but tracked at a different endpoint.
- **WorkflowNode**: A sub-job (workflow node) within a workflow job, representing one step in the workflow's execution. Has a name, status, and reference to the underlying job. In user-facing text, referred to as "sub-job"; in code and API, referred to as "workflow node".

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can browse workflow templates with the same speed and responsiveness as job templates (list loads within 2 seconds on typical connectivity).
- **SC-002**: Users can search and filter workflow templates and see results update within 500 milliseconds of input.
- **SC-003**: Users can launch a workflow template and reach the status screen in under 3 interactions (tap launch → confirm → view status).
- **SC-004**: The workflow job status screen updates automatically while the job runs, with no manual refresh needed.
- **SC-005**: All existing functionality (job templates, recent jobs, settings, logout) remains unaffected by this addition.

## Assumptions

- The AAP instance supports the workflow job templates API endpoints (`/api/v2/workflow_job_templates/`, `/api/v2/workflow_job_templates/{id}/launch/`, `/api/v2/workflow_jobs/{id}/`).
- The workflow job templates API response structure is similar to job templates (paginated, with summary_fields containing labels and user_capabilities).
- The workflow job status API response structure is similar to regular jobs (id, name, status, started, finished, elapsed, summary_fields).
- The existing authentication token has permissions to access workflow template endpoints — no additional authentication is needed.
- Workflow jobs do not produce stdout directly (they orchestrate sub-jobs). Individual sub-job stdout is available via the standard job stdout endpoint and is shown inline when a sub-job is expanded.
- The "Workflow Templates" segment already exists in the TabDefinitions and currently shows a PlaceholderScreen — this feature replaces that placeholder with real content.

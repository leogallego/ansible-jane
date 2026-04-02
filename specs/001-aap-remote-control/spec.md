# Feature Specification: AAP Remote Control MVP

**Feature Branch**: `001-aap-remote-control`
**Created**: 2026-04-02
**Status**: Draft
**Input**: User description: "AAP Remote Control — a lightweight Android app that serves as a remote control for Ansible Automation Platform (AAP). Users authenticate with their AAP instance, browse job templates, launch playbooks, and monitor job status from their phone."

## Clarifications

### Session 2026-04-02

- Q: Should the app allow self-signed certificate override in MVP? → A: Yes, explicit toggle per instance (default: reject untrusted certs)
- Q: Should launching a job require a confirmation step? → A: Yes, confirmation dialog showing template name before executing
- Q: Where does the "Recent Jobs" list come from? → A: Fetched from AAP server API (includes jobs from all clients, not just this app)
- Q: Should the template list support search/filtering? → A: Yes, search bar for name filtering plus label-based filtering using AAP's labels feature

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Connect to AAP Instance (Priority: P1)

A systems administrator opens the app for the first time and needs
to connect it to their organization's AAP instance. They enter the
base URL of their AAP server and their Personal Access Token (PAT).
The app validates the credentials by contacting the server and, on
success, securely stores them so the user does not need to
re-authenticate on subsequent launches.

**Why this priority**: Without authentication, no other feature
can function. This is the foundational gate for all app
functionality.

**Independent Test**: Can be fully tested by entering valid and
invalid credentials and verifying the app correctly accepts or
rejects them. Delivers value by confirming connectivity to the
AAP instance.

**Acceptance Scenarios**:

1. **Given** the app is freshly installed, **When** the user
   enters a valid AAP URL and PAT and taps "Connect", **Then**
   the app validates credentials against the server, stores them
   securely, and navigates to the dashboard.
2. **Given** the app is freshly installed, **When** the user
   enters an invalid URL or token and taps "Connect", **Then**
   the app displays a clear error message indicating what went
   wrong (unreachable server, invalid token, etc.).
3. **Given** the user has previously authenticated, **When** they
   reopen the app, **Then** the app automatically loads stored
   credentials and navigates to the dashboard without requiring
   re-entry.
4. **Given** the user is authenticated, **When** they choose to
   disconnect/logout, **Then** the app clears all stored
   credentials and returns to the connection screen.

---

### User Story 2 - Browse and Launch Job Templates (Priority: P2)

An authenticated administrator wants to trigger automation from
their phone. They browse a list of available job templates, select
one, and launch it. Optionally, they can provide extra variables
(as JSON) before launching.

**Why this priority**: This is the core "remote control" value
proposition — the ability to trigger automation remotely. It
depends on authentication (US1) but delivers the primary use case.

**Independent Test**: Can be tested by authenticating, viewing the
template list, and launching a job. Success is confirmed when the
server acknowledges the launch request.

**Acceptance Scenarios**:

1. **Given** the user is authenticated, **When** they navigate to
   the dashboard, **Then** the app displays a scrollable list of
   available job templates showing template name, description,
   and associated labels. A search bar allows filtering by name,
   and label chips allow filtering by label.
2. **Given** the template list is displayed, **When** the user
   taps the launch button on a template, **Then** the app shows
   a confirmation dialog with the template name. When the user
   confirms, the app sends a launch request and confirms the job
   has been queued.
3. **Given** a template requires extra variables, **When** the
   user taps launch, **Then** the app presents a text field for
   entering extra variables as JSON before confirming the launch.
4. **Given** the user submits invalid JSON as extra variables,
   **When** they tap confirm, **Then** the app displays a
   validation error and does not send the request.
5. **Given** the server is unreachable during launch, **When** the
   user taps launch, **Then** the app displays a connectivity
   error with a retry option.

---

### User Story 3 - Monitor Job Status (Priority: P3)

After launching a job (or to check on previously triggered jobs),
the administrator wants to see the current status of running and
recent jobs. The app polls for status updates and displays them
clearly.

**Why this priority**: Monitoring completes the remote control
experience but is not strictly required for triggering automation.
Users can check status via other means (web UI) if needed.

**Independent Test**: Can be tested by launching a job via US2 and
observing real-time status updates until the job completes or
fails.

**Acceptance Scenarios**:

1. **Given** the user has just launched a job, **When** the launch
   is confirmed, **Then** the app navigates to a job status view
   showing the current state (New, Pending, Waiting, Running,
   Successful, Failed, Error, Canceled).
2. **Given** a job is in progress, **When** the status changes on
   the server, **Then** the app updates the displayed status
   within 10 seconds.
3. **Given** the user is on the dashboard, **When** they navigate
   to a "Recent Jobs" view, **Then** the app fetches and displays
   a list of recently executed jobs from the server (including
   jobs launched from any client) with their final status.
4. **Given** a job has failed, **When** the user views its status,
   **Then** the app clearly indicates failure with a visual
   indicator (color, icon) distinguishable from success.

---

### Edge Cases

- What happens when the stored AAP token expires or is revoked
  mid-session? The app MUST detect the 401 response and prompt
  the user to re-authenticate.
- What happens when the AAP server uses a self-signed certificate?
  The app MUST reject the connection by default (HTTPS-only) but
  MUST offer an explicit toggle allowing the user to accept a
  self-signed certificate for a specific instance.
- What happens when the template list is empty? The app MUST
  display a clear "No templates available" message rather than a
  blank screen.
- What happens when the device loses network connectivity? The
  app MUST display an offline indicator and disable launch
  actions, re-enabling them when connectivity returns.
- What happens when extra_vars JSON is deeply nested or very
  large? The app MUST accept valid JSON up to a reasonable size
  (e.g., 64 KB) and reject oversized input with a clear message.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow users to enter an AAP instance
  base URL and Personal Access Token to establish a connection.
- **FR-002**: System MUST validate credentials by contacting the
  AAP server's user identity endpoint before granting access.
- **FR-003**: System MUST store credentials securely using
  encrypted local storage, never in plaintext.
- **FR-004**: System MUST persist credentials across app restarts
  so users do not need to re-authenticate each time.
- **FR-005**: System MUST provide a logout/disconnect action that
  clears all stored credentials.
- **FR-006**: System MUST fetch and display a list of available
  job templates from the connected AAP instance with a search bar
  for filtering by name and label-based filtering (AAP's
  organizational labels) to group and narrow templates.
- **FR-007**: System MUST allow users to launch a job template
  via a tap action followed by a confirmation dialog displaying
  the template name. The job is only sent to the server after
  the user confirms.
- **FR-008**: System MUST support optional extra variables input
  (JSON format) before launching a template.
- **FR-009**: System MUST validate extra variables JSON format
  before sending to the server.
- **FR-010**: System MUST display the current status of a
  launched job using all 8 AAP status values: New, Pending,
  Waiting, Running, Successful, Failed, Error, Canceled.
- **FR-011**: System MUST poll for job status updates at regular
  intervals while a job is in progress.
- **FR-012**: System MUST fetch and display a list of recently
  executed jobs from the AAP server, including jobs launched from
  any client (web UI, CLI, or this app), with their final status.
- **FR-013**: System MUST enforce HTTPS-only connections to the
  AAP server.
- **FR-016**: System MUST allow the user to explicitly accept a
  self-signed certificate for a given AAP instance via a toggle
  on the connection screen. The default MUST be to reject
  untrusted certificates.
- **FR-014**: System MUST handle expired or revoked tokens by
  prompting the user to re-authenticate.
- **FR-015**: System MUST display clear, user-friendly error
  messages for all failure scenarios (network errors, auth
  failures, server errors).

### Key Entities

- **AAP Instance**: Represents a connected AAP server. Attributes:
  base URL, authentication token, connection status.
- **Job Template**: A playbook configuration available for
  execution. Attributes: identifier, name, description, labels
  (AAP organizational labels for grouping/filtering), launch
  eligibility.
- **Job**: An executed instance of a job template. Attributes:
  identifier, associated template, status (New, Pending, Waiting,
  Running, Successful, Failed, Error, Canceled), start time,
  completion time.
- **Extra Variables**: Optional JSON key-value pairs provided by
  the user when launching a template. Passed to the server as
  part of the launch request.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can complete initial setup (enter URL + token
  and connect) in under 60 seconds.
- **SC-002**: Template list loads and displays within 3 seconds of
  authentication on a standard mobile connection.
- **SC-003**: A job can be launched (from template list to
  confirmation) in under 3 taps.
- **SC-004**: Job status updates are reflected in the app within
  10 seconds of the status changing on the server.
- **SC-005**: 95% of first-time users can successfully connect
  and launch a job without external documentation.
- **SC-006**: The app correctly handles and displays errors for
  100% of defined failure scenarios (network loss, invalid
  credentials, expired token, server errors).
- **SC-007**: Stored credentials survive app restart and device
  reboot without requiring re-entry.

## Assumptions

- Users have network connectivity to their AAP instance (the app
  is not designed for offline use).
- Users have a pre-generated Personal Access Token (PAT) from
  their AAP instance; the app does not handle the full OAuth2
  authorization code flow in MVP.
- The AAP instance is running a version that supports the v2 REST
  API.
- Users are technical (systems/infrastructure administrators) and
  familiar with concepts like job templates and extra variables.
- The app targets Android devices running Android 8.0 (API 26)
  or higher.
- Job status polling (rather than WebSockets) is acceptable for
  MVP; real-time push notifications are out of scope.
- The app does not need to support multiple AAP instances
  simultaneously in MVP; one connection at a time.

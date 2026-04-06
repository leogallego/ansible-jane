# Feature Specification: Multi-AAP Instance Support

**Feature Branch**: `009-multi-instance-support`  
**Created**: 2026-04-04  
**Status**: Draft  
**Input**: GitHub issue #10 — Multi-AAP instance support: simultaneous login and instance switching  

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add a New AAP Instance (Priority: P1)

A user managing multiple AAP environments (dev, staging, production) opens the app for the first time and connects to their production AAP instance by entering its URL, a personal access token, and an optional friendly name like "Production". The app validates the credentials and saves the connection. Later, the user goes to Settings and taps "+ Add Instance" to connect to their dev environment with a different alias.

**Why this priority**: Without the ability to store multiple instances, no other multi-instance feature is possible. This is the foundational capability.

**Independent Test**: Can be fully tested by adding two instances and verifying both appear in Settings.

**Acceptance Scenarios**:

1. **Given** no instances are configured, **When** the user enters a URL, token, and optional alias and taps Connect, **Then** the instance is saved and the user is taken to the main dashboard.
2. **Given** one instance is already connected, **When** the user taps "+ Add Instance" in Settings and enters new credentials, **Then** both instances appear in the Instances section of Settings.
3. **Given** the user is adding an instance with an alias, **When** the pill is displayed, **Then** it shows both the alias (primary label) and the URL (secondary label).
4. **Given** the user is adding an instance without an alias, **When** the pill is displayed, **Then** it shows the URL only.
5. **Given** the user enters invalid credentials, **When** they tap Connect, **Then** an error is shown and no instance is saved.

---

### User Story 2 - Switch Between Instances (Priority: P1)

A user with multiple connected instances wants to check job templates on their dev environment after reviewing production. They open Settings, see their instances displayed as pills/chips, and tap the "Dev" pill. The app switches to the dev instance, refreshes all data (templates, jobs, infrastructure), and the user sees dev-environment data throughout the app.

**Why this priority**: Switching is the core value proposition — without it, storing multiple instances has no benefit.

**Independent Test**: Can be tested by connecting two instances, switching between them, and verifying that displayed data (templates, jobs) changes to match the selected instance.

**Acceptance Scenarios**:

1. **Given** two instances are connected and "Production" is active, **When** the user taps the "Dev" instance pill in Settings, **Then** "Dev" becomes the active instance and all data refreshes to show dev-environment content.
2. **Given** the user switches instances, **When** they navigate to Templates or Activity, **Then** the data shown belongs to the newly active instance.
3. **Given** the user switches instances, **When** the switch completes, **Then** the previously active pill shows as inactive (hollow dot) and the new one shows as active (filled dot).

---

### User Story 3 - View Instance Details (Priority: P2)

A user wants to verify which URL and settings are associated with their active instance. They tap the active instance pill in Settings, and a bottom sheet appears showing the instance details: URL, alias, API version, and whether self-signed certificates are trusted.

**Why this priority**: Useful for verification and troubleshooting, but not required for core multi-instance functionality.

**Independent Test**: Can be tested by tapping an active instance pill and verifying the bottom sheet shows correct details.

**Acceptance Scenarios**:

1. **Given** an active instance exists, **When** the user taps its pill, **Then** a bottom sheet appears showing the instance URL, alias (if set), API version, and self-signed certificate trust status.
2. **Given** the bottom sheet is open, **When** the user swipes it down or taps outside, **Then** it dismisses.

---

### User Story 4 - Remove a Specific Instance (Priority: P2)

A user no longer needs access to a decommissioned staging environment. They tap the "x" button on that instance's pill in Settings. The instance is removed and its credentials are deleted. Other instances remain unaffected.

**Why this priority**: Essential for credential hygiene, but secondary to adding and switching.

**Independent Test**: Can be tested by removing one instance and verifying the other remains connected and functional.

**Acceptance Scenarios**:

1. **Given** two instances are connected, **When** the user taps the "x" on an inactive instance pill, **Then** that instance is removed and only the active instance remains.
2. **Given** two instances are connected and "Dev" is active, **When** the user taps the "x" on the active "Dev" instance, **Then** "Dev" is removed and the remaining instance becomes active automatically.
3. **Given** only one instance is connected, **When** the user taps its "x" button, **Then** all credentials are cleared and the app navigates to the authentication screen.

---

### Edge Cases

- What happens when the active instance's token expires or becomes invalid during use? The app shows an authentication error and prompts re-authentication for that specific instance, without affecting other instances.
- What happens when the user tries to add an instance with the same URL as an existing one? The app warns the user and prevents duplicate instances.
- What happens when the user switches instances while a job launch or data load is in progress? In-flight requests are cancelled and data refreshes for the new instance.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST store credentials (URL, token, alias, settings) for multiple AAP instances simultaneously, each encrypted independently.
- **FR-002**: System MUST display all connected instances as pills/chips in the Settings screen, with visual distinction between the active instance (filled indicator) and inactive instances (hollow indicator).
- **FR-003**: Users MUST be able to switch the active instance by tapping an inactive instance pill, which triggers a full data refresh across the app.
- **FR-004**: Users MUST be able to remove any single instance by tapping its "x" button, deleting only that instance's credentials.
- **FR-005**: System MUST navigate to the authentication screen when the last instance is removed.
- **FR-006**: Users MUST be able to add new instances via a "+ Add Instance" option in Settings, which opens the authentication screen.
- **FR-007**: The authentication screen MUST include an optional alias/name field for the instance.
- **FR-008**: System MUST always display the instance URL in the pill. When an alias is set, the pill shows both the alias (primary) and the URL (secondary). When no alias is set, the pill shows the URL only.
- **FR-009**: Users MUST be able to view details of the active instance via a bottom sheet (URL, alias, API version, certificate trust status) by tapping the active instance pill.
- **FR-010**: System MUST prevent adding duplicate instances with the same URL.
- **FR-011**: System MUST handle per-instance token expiry independently — an expired token on one instance must not affect other instances. On token expiry, the app navigates to the auth screen pre-filled with the instance's URL and alias, allowing the user to enter a new token without re-adding the instance.
- **FR-012**: System MUST cancel in-flight requests when switching instances and load fresh data for the newly active instance.
- **FR-013**: System MUST display the active instance label (alias or URL) in the dashboard top bar so users always know which environment they are working with.

### Key Entities

- **Instance**: Represents a single AAP connection. Key attributes: unique identifier, base URL, personal access token, optional alias, API version, self-signed certificate trust flag, optional certificate fingerprint.
- **Active Instance**: The currently selected instance whose data is displayed throughout the app. Exactly one instance is active at any time (when instances exist).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can add and connect to a new AAP instance in under 30 seconds.
- **SC-002**: Switching between instances completes (data fully refreshed) in under 3 seconds on a typical network connection.
- **SC-003**: Removing an instance takes effect immediately with no residual data from the removed instance visible anywhere in the app.
- **SC-004**: Users can manage up to 10 simultaneous instance connections. Settings screen renders 10 instance pills within a single frame (16ms). App startup with 10 stored instances completes in under 2 seconds.
- **SC-005**: No credentials from one instance are ever accessible or visible when another instance is active (data isolation).

## Clarifications

### Session 2026-04-04

- Q: Should the URL still be visible in the pill when an alias is set? → A: Yes, always show the URL. When alias is set, show alias as primary label and URL as secondary.
- Q: What happens to existing single-instance credentials on app update? → A: Clear existing credentials; user must re-authenticate. No migration.
- Q: Should the active instance be visible outside Settings? → A: Yes, show the active instance label in the dashboard top bar / app bar.
- Q: How should expired tokens be handled? → A: Navigate to auth screen pre-filled with instance URL/alias; user enters new token. Instance is preserved.

## Assumptions

- Users will typically manage 2-5 AAP instances; supporting up to 10 is sufficient.
- Each instance uses independent authentication (personal access tokens); there is no single sign-on across instances.
- The existing encryption approach (per-value encryption with hardware-backed key management) is sufficient for securing multiple sets of credentials.
- Instance data (templates, jobs, infrastructure) is not cached across switches — each switch triggers a fresh fetch.
- Per-instance notification preferences and cross-instance data syncing are out of scope (per GitHub issue #10).
- Automatic instance discovery is out of scope.

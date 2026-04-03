# Feature Specification: UI Polish — Animations and Micro-Interactions

**Feature Branch**: `007-ui-polish-animations`
**Created**: 2026-04-03
**Status**: Draft
**Input**: User description: "UI polish: animations, typed errors, and micro-interactions. Implement Priority 1 (press-scale on cards, spring-animated dialogs, typed error states) and Priority 3 (animated app bar title, breathing pulse on running status) from issue #14."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tactile Card Press Feedback (Priority: P1)

A user browsing job templates, recent jobs, or EDA audit rules taps on a card and sees it subtly scale down before the action fires. This gives immediate visual confirmation that the tap was registered, making the app feel responsive and polished.

**Why this priority**: Press feedback is the most frequently experienced interaction — every list tap benefits. Without it, the app feels "flat" on touch.

**Independent Test**: Can be tested by tapping any card in any list screen and observing the scale-down animation.

**Acceptance Scenarios**:

1. **Given** a user is on the Templates list, **When** they press down on a template card, **Then** the card scales to ~98% of its size smoothly.
2. **Given** a user is pressing a template card, **When** they release, **Then** the card returns to 100% scale smoothly.
3. **Given** a user has accessibility "reduce motion" enabled, **When** they tap a card, **Then** no scale animation plays; the tap still functions normally.
4. **Given** a user is on any list screen with clickable cards (Jobs, Workflows, EDA Audit — excluding Schedules which use Switch toggles), **When** they press a clickable card, **Then** the same press-scale behavior occurs consistently.

---

### User Story 2 - Informative Error Display (Priority: P1)

When an API call fails, the user sees a specific error type (network, authentication, server, SSL) with a relevant icon and a short explanation, rather than a generic error string. For advanced troubleshooting, they can expand details to see the HTTP status code and URL.

**Why this priority**: AAP users troubleshoot connectivity to their controller frequently. Knowing whether a failure is network, auth, SSL, or server-side directly affects what action they take next.

**Independent Test**: Can be tested by simulating different failure types (airplane mode for network, expired token for auth, bad URL for SSL) and verifying each shows a distinct error presentation.

**Acceptance Scenarios**:

1. **Given** the device has no network, **When** the app tries to load templates, **Then** a "Network Error" message appears with a network-off icon and a retry button.
2. **Given** the user's token has expired (HTTP 401/403), **When** the app tries to load data, **Then** an "Authentication Error" message appears with a lock icon suggesting re-login.
3. **Given** the AAP server returns HTTP 500, **When** the app tries to load data, **Then** a "Server Error" message appears with a server icon.
4. **Given** the AAP server has an untrusted certificate, **When** the app tries to connect, **Then** an "SSL Error" message appears with a security icon.
5. **Given** any error is displayed, **When** the user taps "Show details", **Then** the error expands to show the HTTP status code and URL attempted.
6. **Given** any error is displayed, **When** the user taps "Retry", **Then** the failed operation is retried.

---

### User Story 3 - Smooth Dialog Entrances (Priority: P2)

When a user taps "Launch" on a template and the confirmation dialog appears, it enters with a spring animation (scaling from small to full size) instead of appearing instantly. This applies to all dialogs and bottom sheets in the app.

**Why this priority**: Dialogs are high-attention moments (confirming a launch). A polished entrance reinforces the significance of the action without slowing the user down.

**Independent Test**: Can be tested by triggering any dialog (launch confirmation, extra vars input, EDA detail sheet) and observing the spring entrance animation.

**Acceptance Scenarios**:

1. **Given** a user taps "Launch" on a template, **When** the confirmation dialog appears, **Then** it scales in from ~80% to 100% with a spring animation.
2. **Given** a user confirms a launch and extra vars are needed, **When** the extra vars dialog appears, **Then** it also enters with a spring animation.
3. **Given** a user taps an EDA audit rule, **When** the detail bottom sheet appears, **Then** it slides up with a smooth spring effect.
4. **Given** reduce motion is enabled, **When** any dialog appears, **Then** it appears instantly without animation.

---

### User Story 4 - Animated App Bar Title (Priority: P3)

When the user switches between tabs (Templates, Infrastructure, Activity), the app bar title crossfades to reflect the current section, providing a smooth visual transition.

**Why this priority**: Small polish detail that reinforces which section the user is in. Lower priority because the bottom nav already indicates the active tab.

**Independent Test**: Can be tested by tapping between bottom navigation tabs and observing the title crossfade.

**Acceptance Scenarios**:

1. **Given** a user is on the Templates tab, **When** they tap the Activity tab, **Then** the app bar title crossfades from "Templates" to "Activity".
2. **Given** reduce motion is enabled, **When** tabs change, **Then** the title changes instantly without animation.

---

### User Story 5 - Breathing Pulse on Running Jobs (Priority: P3)

When viewing a job that is actively running, its status badge subtly pulses (scale oscillation) to indicate the job is in progress, providing a visual heartbeat.

**Why this priority**: Nice visual cue but not essential — the "Running" text and icon already convey the state.

**Independent Test**: Can be tested by launching a job and observing the status badge on the job detail screen while it runs.

**Acceptance Scenarios**:

1. **Given** a job has status "Running", **When** the user views its status badge, **Then** the badge pulses with a subtle scale animation (oscillating between ~96% and ~104%).
2. **Given** a job transitions from "Running" to "Successful", **When** the status changes, **Then** the pulse stops and the badge settles at normal scale.
3. **Given** reduce motion is enabled, **When** a running job is viewed, **Then** no pulse animation plays.

---

### Edge Cases

- What happens when a card is pressed but the user drags away before releasing? The scale should return to normal without triggering the action.
- What happens with rapid tab switching during title animation? Each new switch should interrupt and start a fresh crossfade.
- What happens when an error occurs during a polling cycle (job status check)? The typed error should appear without disrupting the existing job data already displayed.
- How does the breathing pulse interact with the status badge color animation? Both should compose naturally — the color animates on status change while the pulse runs independently during "Running".

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: All clickable list cards (templates, workflow templates, recent jobs, EDA audit items) MUST show a press-scale animation on touch.
- **FR-002**: All dialogs (launch confirmation, extra vars input) and bottom sheets (EDA audit detail) MUST enter with a spring-based scale animation.
- **FR-003**: Error states MUST be categorized into distinct types: Network, Authentication, Server, SSL, and Unknown.
- **FR-004**: Each error type MUST display a unique icon and descriptive title.
- **FR-005**: Error displays MUST include an expandable detail section showing HTTP status code and URL when available.
- **FR-006**: Error displays MUST include a retry button that re-triggers the failed operation.
- **FR-007**: Error displays MUST enter with a slide-in + fade animation for visual continuity.
- **FR-008**: The app bar title MUST crossfade when switching between bottom navigation tabs.
- **FR-009**: The status badge for "Running" jobs MUST display a breathing pulse animation.
- **FR-010**: All animations MUST respect the system's "reduce motion" accessibility setting.
- **FR-011**: Animations MUST NOT block user interaction or delay navigation.
- **FR-012**: All status and error icons MUST be referenced from a centralized icon registry rather than scattered inline references.
- **FR-013**: Job status colors MUST be provided via the app theme as a custom composable local, making them theme-aware and accessible via a single API.
- **FR-014**: ViewModel error mapping MUST use a reusable Flow extension to reduce boilerplate when converting repository results to UI state.

### Key Entities

- **AppError**: Represents a categorized error with type (Network, Auth, Server, SSL, Unknown), user-facing message, optional detail information (HTTP status code, URL), and associated icon.
- **AapIcons**: Centralized icon registry mapping semantic names (status icons, error icons, navigation icons) to Material icon references.
- **StatusColors**: Theme-level color set for job statuses, provided via CompositionLocal so components access them through the theme rather than top-level constants.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All clickable cards across all list screens respond to press with a visible scale animation.
- **SC-002**: Users can distinguish between network, authentication, server, and SSL errors by their icon and title without reading the detail text.
- **SC-003**: Users can expand error details to see technical information (status code, URL) for troubleshooting.
- **SC-004**: All dialog and bottom sheet entrances display a spring animation that completes smoothly.
- **SC-005**: The running job pulse animation is visible and smooth, with no jank or frame drops.
- **SC-006**: Tab title transitions are smooth and do not flash or show both titles simultaneously.
- **SC-007**: With reduce motion enabled, the app remains fully functional with no animations playing.
- **SC-008**: All status and error icon references use the centralized AapIcons registry — no scattered inline definitions for job status or error type icons.
- **SC-009**: Job status colors are accessed via theme CompositionLocal, not via top-level constant imports.
- **SC-010**: ViewModels use a shared Flow.asResult() extension to map repository results, eliminating repeated try/catch/fold boilerplate.

## Assumptions

- The app already uses Compose animation APIs (shimmer, crossfade exist in the codebase) — no new dependencies needed.
- The existing `ErrorMessage` composable and all `UiState.Error` sealed class variants currently carry plain `String` messages and will be refactored.
- Schedule cards have a Switch interaction but are not clickable for navigation — press-scale applies only to cards with onClick/clickable behavior.
- The "reduce motion" setting refers to Android's system accessibility setting.
- Error type classification will be derived from network exception types and HTTP response codes at the repository/ViewModel layer.
- This feature does not add any new screens or navigation routes — it enhances existing UI components only.
- The centralized icon registry (AapIcons) and status color CompositionLocal are lightweight design system improvements that don't require new dependencies.

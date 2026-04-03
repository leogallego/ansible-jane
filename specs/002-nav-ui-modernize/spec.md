# Feature Specification: Navigation Foundation & UI Modernization

**Feature Branch**: `002-nav-ui-modernize`  
**Created**: 2026-04-02  
**Status**: Draft  
**Input**: Phase 1 combining GitHub issues #3 (Bottom navigation bar with 3-tab layout) and #2 (UI Modernization quick wins)

## Clarifications

### Session 2026-04-02

- Q: Should each tab preserve its navigation state when switching away and back? → A: Yes, each tab preserves its own back stack (switching back restores prior state, including detail screens and scroll position).
- Q: Where should the logout action be placed in the new navigation structure? → A: Move logout to a Settings screen accessible via a settings/gear icon in the top app bar.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Tab-Based Navigation (Priority: P1)

As a user, after logging in I see a bottom navigation bar with three tabs: Templates, Infrastructure, and Activity. I can tap any tab to switch between the main sections of the app. The currently selected tab is visually highlighted. When I switch tabs, the content area updates smoothly with an animated transition.

**Why this priority**: The bottom navigation is the structural foundation for all current and future sections. Without it, no other feature in this phase can be accessed or organized properly.

**Independent Test**: Can be fully tested by logging in and tapping each tab — each tab shows its section header and the navigation bar persists across all tabs.

**Acceptance Scenarios**:

1. **Given** the user is authenticated and on the Templates tab, **When** they tap the Infrastructure tab, **Then** the content area transitions to the Infrastructure section and the Infrastructure tab icon is highlighted.
2. **Given** the user is on any tab, **When** they tap the same tab again, **Then** the view scrolls to top (if scrolled) but does not re-trigger a full reload.
3. **Given** the user is on a detail screen (e.g., Job Status) under the Activity tab, **When** they tap the Templates tab and then tap Activity again, **Then** they return to the Job Status detail screen they were previously viewing (back stack preserved).
4. **Given** the user is on the Templates tab scrolled halfway down, **When** they switch to another tab and back, **Then** their scroll position is restored.

---

### User Story 2 - Segmented Section Switching Within Tabs (Priority: P1)

As a user on a tab that has multiple sub-sections (e.g., Templates tab has "Job Templates" and "Workflow Templates"), I see a segmented button row at the top of the content area. Tapping a segment switches the visible list without leaving the tab.

**Why this priority**: Segmented buttons are the internal navigation mechanism within each tab. This is required for the tab structure to be complete and usable.

**Independent Test**: Can be tested by navigating to any tab with multiple segments and verifying each segment loads its corresponding content.

**Acceptance Scenarios**:

1. **Given** the user is on the Templates tab, **When** the screen loads, **Then** a segmented button with "Job Templates" and "Workflow Templates" is visible, with "Job Templates" selected by default.
2. **Given** the user is on the Activity tab with "Jobs" segment selected, **When** they tap "Schedules", **Then** the content switches to show the Schedules placeholder.
3. **Given** the user switches segments, **When** the transition occurs, **Then** the content crossfades smoothly to the new segment.

---

### User Story 3 - Enhanced Template Cards (Priority: P2)

As a user browsing Job Templates, I see visually improved cards with better typography, clear hierarchy (template name prominent, description secondary), elevation/shadow, and consistent spacing. The cards feel modern and are easy to scan.

**Why this priority**: Templates are the most-used screen. Improving card design directly impacts the day-to-day experience without requiring structural changes.

**Independent Test**: Can be tested by viewing the template list and verifying cards display with proper visual hierarchy, elevation, and spacing.

**Acceptance Scenarios**:

1. **Given** the user is on the Job Templates list, **When** templates load, **Then** each template displays as a card with elevated surface, prominent name, secondary description, and consistent spacing.
2. **Given** a template has labels, **When** the card renders, **Then** labels appear as small chips below the description.

---

### User Story 4 - Skeleton Loading States (Priority: P2)

As a user, when I navigate to a tab or segment that is loading data, I see animated placeholder shapes (skeleton loaders) that match the layout of the content being loaded, instead of a plain spinner.

**Why this priority**: Skeleton loaders reduce perceived wait time and make the app feel more polished. This is a high-impact visual improvement.

**Independent Test**: Can be tested by triggering any list load (templates, jobs) and verifying skeleton shapes appear during the loading state.

**Acceptance Scenarios**:

1. **Given** the user navigates to the Templates tab, **When** data is loading, **Then** skeleton card shapes animate in place of the list.
2. **Given** data finishes loading, **When** results are ready, **Then** skeletons smoothly transition to actual content.

---

### User Story 5 - Pull-to-Refresh (Priority: P2)

As a user viewing a list (templates or jobs), I can pull down to refresh the data. A refresh indicator appears during the reload.

**Why this priority**: Pull-to-refresh is a standard mobile interaction pattern that users expect. It improves usability for monitoring job status changes.

**Independent Test**: Can be tested by pulling down on any list screen and verifying data reloads with a visible refresh indicator.

**Acceptance Scenarios**:

1. **Given** the user is on the Job Templates list, **When** they pull down, **Then** a refresh indicator appears and the template list reloads from the server.
2. **Given** a pull-to-refresh is in progress, **When** it completes, **Then** the indicator dismisses and the list updates with fresh data.
3. **Given** the refresh fails (network error), **When** the refresh completes, **Then** an error message is shown and the existing list data is preserved.

---

### User Story 6 - Notification Bell Placeholder (Priority: P3)

As a user, I see a bell icon in the top app bar. Tapping it shows a brief message indicating that notifications are coming soon. This establishes the visual presence for the future notification feature.

**Why this priority**: The bell icon reserves the UI real estate and sets user expectations for the future notification feature (#7) without requiring any backend integration.

**Independent Test**: Can be tested by tapping the bell icon and verifying a "coming soon" message appears.

**Acceptance Scenarios**:

1. **Given** the user is on any tab, **When** they look at the top app bar, **Then** a bell icon is visible on the right side.
2. **Given** the user taps the bell icon, **When** the action triggers, **Then** a snackbar or tooltip displays "Notifications coming soon".

---

### User Story 7 - Animated Transitions (Priority: P3)

As a user navigating between screens and tabs, I see smooth animated transitions (crossfade for tab switches, slide for detail navigation) rather than instant hard cuts.

**Why this priority**: Animations improve perceived quality but are polish rather than core functionality.

**Independent Test**: Can be tested by switching tabs, opening detail screens, and verifying visible transition animations.

**Acceptance Scenarios**:

1. **Given** the user taps a different bottom tab, **When** the tab switches, **Then** the content area transitions with a crossfade animation.
2. **Given** the user taps a template to view job status, **When** the detail screen opens, **Then** it slides in from the right.
3. **Given** the user presses back from a detail screen, **When** returning, **Then** the screen slides out to the right.

---

### User Story 8 - Settings Screen (Priority: P1)

As a user, I can tap the gear icon in the top app bar to access a Settings screen that shows which AAP server I'm connected to and lets me log out. This replaces the previous logout button that was embedded in the template list toolbar.

**Why this priority**: Logout must remain accessible after the toolbar is replaced by the bottom navigation bar. Without this, users cannot disconnect from their AAP instance.

**Independent Test**: Can be tested by tapping the gear icon, verifying the server URL is displayed, and confirming logout returns to the auth screen.

**Acceptance Scenarios**:

1. **Given** the user is on any tab, **When** they tap the gear icon in the top app bar, **Then** a Settings screen opens showing the connected server URL and a logout button.
2. **Given** the user is on the Settings screen, **When** they tap logout, **Then** they are returned to the authentication screen and their session is cleared.
3. **Given** the user is on the Settings screen, **When** they tap back, **Then** they return to the tab they were previously viewing.

---

### Edge Cases

- What happens when the user rotates the device while on a specific tab and segment? The selected tab and segment must be preserved.
- ~~What happens when the user receives a deep link while on a non-default tab?~~ *Out of scope for this phase — deep linking not implemented.*
- What happens when a list has zero items? An empty state message should be displayed, not a blank screen or skeleton that never resolves.
- What happens when the user has very slow connectivity? Skeleton loaders should display for as long as needed; pull-to-refresh should time out gracefully.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: App MUST display a persistent bottom navigation bar with three tabs: Templates, Infrastructure, and Activity, after authentication.
- **FR-002**: Each tab MUST show a segmented button row for switching between its sub-sections.
- **FR-003**: Templates tab segments MUST be "Job Templates" (default) and "Workflow Templates".
- **FR-004**: Infrastructure tab segments MUST be "Inventories", "Hosts", and "Projects".
- **FR-005**: Activity tab segments MUST be "Jobs" (default), "Schedules", and "EDA Audit".
- **FR-006**: The existing Recent Jobs screen MUST be relocated under the Activity tab's "Jobs" segment.
- **FR-007**: The existing Job Templates list MUST be relocated under the Templates tab's "Job Templates" segment.
- **FR-008**: Non-implemented segments (Workflow Templates, Inventories, Hosts, Projects, Schedules, EDA Audit) MUST show placeholder screens indicating the feature is planned.
- **FR-009**: A notification bell icon and a settings/gear icon MUST be displayed in the top app bar on all tabs.
- **FR-010**: Tapping the notification bell MUST display a "coming soon" message.
- **FR-011**: Template cards MUST display with improved visual hierarchy: prominent name, secondary description, label chips, and card elevation.
- **FR-012**: All list screens MUST show skeleton loading placeholders instead of plain spinners during data loading.
- **FR-013**: All list screens MUST support pull-to-refresh to reload data.
- **FR-014**: Tab switching MUST animate with crossfade transitions.
- **FR-015**: Detail screen navigation (forward/back) MUST animate with slide transitions.
- **FR-016**: The bottom navigation bar MUST NOT be visible on the authentication screen.
- **FR-017**: The selected tab and segment MUST be preserved across configuration changes (rotation).
- **FR-019**: Each tab MUST maintain its own independent back stack, preserving navigation state (including detail screens and scroll position) when switching between tabs.
- **FR-018**: A settings/gear icon MUST be displayed in the top app bar, opening a Settings screen that contains the logout action and server connection info.
- **FR-020**: The Settings screen MUST display the connected AAP server URL and provide a logout button.

### Key Entities

- **Tab**: A top-level navigation destination (Templates, Infrastructure, Activity) with an icon, label, and associated segments.
- **Segment**: A sub-section within a tab, switched via segmented buttons. Each segment has a label and associated content screen.
- **Template Card**: Enhanced visual representation of a job template with name, description, labels, and launch action.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can switch between all three tabs within 1 second (measured from tap to content area displaying skeleton or cached content), with visible animated feedback.
- **SC-002**: Users can switch between segments within a tab within 500 milliseconds.
- **SC-003**: All existing functionality (browse templates, launch jobs, view job status, view recent jobs, logout) remains fully accessible after the navigation restructure.
- **SC-004**: Skeleton loading placeholders appear within 100 milliseconds of navigating to a loading screen, replacing all previous plain spinner states. Verified by manual observation — no instrumented measurement required.
- **SC-005**: Pull-to-refresh successfully reloads data on all list screens without requiring the user to navigate away and back.
- **SC-006**: 100% of tab and segment selections survive device rotation without resetting.

## Assumptions

- The existing authentication flow and auth screen remain unchanged; the bottom navigation appears only after successful login.
- Non-implemented tab segments (Workflow Templates, all Infrastructure segments, Schedules, EDA Audit) show placeholder screens; their full implementation is deferred to issues #4, #5, and #6.
- The notification bell is purely a visual placeholder; no notification API integration is included in this phase (deferred to #7).
- The existing job detail screen (Job Status) continues to work as a full-screen detail pushed on top of the tab navigation.
- The app targets phone form factor only; tablet/landscape optimizations are out of scope for this phase.
- The existing search and label filter functionality on the templates screen is preserved as-is within the Templates tab.

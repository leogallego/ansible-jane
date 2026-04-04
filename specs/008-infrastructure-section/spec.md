# Feature Specification: Infrastructure Section (Inventories and Hosts)

**Feature Branch**: `008-infrastructure-section`  
**Created**: 2026-04-03  
**Status**: Draft  
**Input**: User description: "Add Infrastructure section (Inventories, Hosts) - GitHub issue #5"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Browse Inventories (Priority: P1)

As an AAP operator, I want to browse my inventories and inspect their details so I can see what infrastructure is managed by my AAP instance.

**Why this priority**: Inventories are the foundational object in the Infrastructure section. They group hosts and are the starting point for understanding managed infrastructure.

**Independent Test**: Can be fully tested by navigating to the Infrastructure tab, selecting "Inventories", verifying the list loads with inventory names and host counts, tapping an inventory to see its details in a bottom sheet, and expanding to full screen to see its hosts.

**Acceptance Scenarios**:

1. **Given** I am authenticated and on the main screen, **When** I tap the Infrastructure tab and select "Inventories", **Then** I see a paginated list of inventories showing name, description, and host count.
2. **Given** I am viewing the inventories list, **When** I pull down to refresh, **Then** the list reloads with current data from AAP.
3. **Given** I am viewing the inventories list, **When** I scroll to the bottom, **Then** additional inventories load automatically (infinite scroll).
4. **Given** I am viewing the inventories list, **When** I tap on an inventory, **Then** a bottom sheet appears showing inventory details: name, type, organization, total hosts, created date, last modified date, and variables.
5. **Given** I am viewing an inventory bottom sheet, **When** I expand to full screen, **Then** I see the full inventory details plus a list of hosts belonging to that inventory, with group badges on each host.
6. **Given** I am viewing hosts in the expanded inventory detail, **When** I tap on a host, **Then** the host detail bottom sheet appears (same as the Hosts segment detail view).

---

### User Story 2 - Browse and Search All Hosts (Priority: P1)

As an AAP operator, I want to browse all hosts across my AAP instance so I can quickly find and inspect any managed host regardless of which inventory it belongs to.

**Why this priority**: A standalone hosts list provides the fastest path to finding a specific host. Operators frequently need to check host status without knowing which inventory it's in.

**Independent Test**: Can be tested by navigating to Infrastructure > Hosts, verifying the full host list loads with descriptions and inventory labels, using search to filter by hostname, and tapping a host to view details.

**Acceptance Scenarios**:

1. **Given** I am on the Infrastructure tab, **When** I select "Hosts", **Then** I see a paginated list of all hosts showing hostname, description, and an inventory label indicating which inventory each host belongs to.
2. **Given** I am viewing the hosts list, **When** I type in the search bar, **Then** the host list filters to show only hosts matching my search query (server-side search).
3. **Given** I am viewing the hosts list, **When** I tap on a host, **Then** a bottom sheet appears showing: name, last successful run, inventory, created date, last modified date, and variables section, with an option to expand to full screen.
4. **Given** I am viewing a host bottom sheet, **When** I expand to full screen, **Then** I see the full host details plus additional sections: facts, groups, and jobs run.
5. **Given** I am viewing the hosts list, **When** the list contains no hosts, **Then** I see an empty state message.
6. **Given** I am viewing the hosts list, **When** I pull down to refresh, **Then** the list reloads with current data.

---

### Edge Cases

- What happens when the AAP instance has no inventories? Display an appropriate empty state message.
- What happens when the user lacks permissions to view certain inventories or hosts? AAP API returns only permitted resources; no special handling needed beyond showing what the API returns.
- What happens when the user has a very large inventory (thousands of hosts)? Pagination and search handle this — host list uses infinite scroll with server-side search.
- What happens when network connectivity is lost while browsing? Show the standard error state with a retry option, consistent with existing app behavior.
- What happens when an inventory has no hosts? Show an empty state in the expanded inventory detail view.
- What happens when host variables are very large? The bottom sheet can be expanded to full screen for lengthy content.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display a paginated list of inventories from the AAP API, showing inventory name, description, and total host count.
- **FR-002**: System MUST support infinite scroll pagination for inventory and host lists, loading more items as the user scrolls near the bottom.
- **FR-003**: System MUST display inventory details in a bottom sheet when an inventory is tapped, showing: name, type, organization, total hosts, created date, last modified date, and variables.
- **FR-004**: System MUST allow the inventory detail bottom sheet to expand to full screen, showing the full inventory details plus a list of hosts within that inventory with group badges. Tapping a host in the expanded view MUST open the host detail bottom sheet.
- **FR-005**: System MUST display a standalone paginated list of all hosts across all inventories, showing hostname, description, and an inventory label for each host.
- **FR-006**: System MUST provide a search bar on the Hosts screen to filter hosts by name (server-side search).
- **FR-007**: System MUST present host details in a bottom sheet showing: name, last successful run, inventory, created date, last modified date, and variables.
- **FR-007a**: System MUST allow the host detail bottom sheet to expand to full screen, showing additional sections: facts (from `/api/v2/hosts/{id}/ansible_facts/`), groups, and jobs run on this host.
- **FR-008**: System MUST support pull-to-refresh on all list screens (inventories, hosts, inventory detail hosts).
- **FR-009**: System MUST show loading skeletons while data is being fetched, consistent with existing app screens.
- **FR-010**: System MUST show appropriate empty state messages when no items exist in a list.
- **FR-011**: System MUST show error states with retry options when API requests fail, consistent with existing app behavior.
- **FR-012**: System MUST replace the current placeholder screens in the Infrastructure tab for Inventories and Hosts segments with the implemented screens.

### Key Entities

- **Inventory**: Represents a collection of managed hosts. Key attributes: name, description, type, organization, total host count, total group count, created date, last modified date, variables.
- **Host**: Represents a managed node within an inventory. Key attributes: hostname, description, enabled status, variables, last successful run, inventory (parent), created date, last modified date. Extended detail: ansible facts, group memberships, jobs run.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can browse inventories and inspect inventory details within 5 seconds of opening the Infrastructure tab.
- **SC-002**: Users can locate a specific host using the Hosts screen search within 15 seconds.
- **SC-003**: All list screens load their first page of data within 3 seconds on a typical mobile connection.
- **SC-004**: The Infrastructure section follows the same visual patterns and interaction behaviors as the existing Templates and Activity sections, providing a consistent user experience.

## Clarifications

### Session 2026-04-03

- Q: Should the app show inventory groups as an intermediate navigation level? → A: Show flat host list but include a "Group" label/badge on each host item.
- Q: How should host detail view be presented? → A: Bottom sheet initially for quick inspection, with an option to expand to full screen when more detail is needed (e.g., lengthy variables).
- Q: What is the scope of host search? → A: Within-inventory search is available in the expanded inventory detail view. Global cross-inventory host search is on the standalone Hosts segment.
- Q: What should the Hosts segment show? → A: A standalone list of ALL hosts across all inventories, with hostname, description, and inventory label. This is a separate section from the inventory drill-down.
- Q: What does tapping an inventory show? → A: A bottom sheet with inventory details (name, type, organization, total hosts, created, last modified, variables). Expanding to full screen shows the inventory's hosts.
- Q: Should Projects be in the Infrastructure tab? → A: No. Projects (Controller + EDA) will be moved to a separate dedicated tab in a future feature.
- Q: What fields does the host detail bottom sheet show? → A: Name, last successful run, inventory, created, last modified, and variables section. Expanding to full screen adds: facts, groups, and jobs run.

## Assumptions

- The AAP instance is version 2.5+ with Gateway, and all API calls go through the Gateway as defined in the project requirements.
- The user is already authenticated with a valid Bearer token before accessing the Infrastructure section.
- The Infrastructure tab already exists in the bottom navigation with placeholder screens; this feature replaces the Inventories and Hosts placeholders.
- Inventory and host APIs follow the same pagination pattern as existing endpoints (page/page_size query parameters, paginated response with count/next/previous/results).
- Host variables are displayed as read-only text; editing host variables from the app is out of scope.
- Smart inventories and constructed inventories are displayed the same as regular inventories in the list view.
- The host detail view shows variables in a readable format but does not support editing or copying individual values.
- Projects have been descoped from this feature and will be implemented as a separate tab with both Controller and EDA projects.

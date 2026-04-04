# Research: Infrastructure Section

**Feature**: 008-infrastructure-section | **Date**: 2026-04-03

## AAP Controller API Endpoints

### Inventories

- **List inventories**: `GET /api/v2/inventories/`
  - Query params: `page`, `page_size`, `search`, `order_by` (default: `-modified`)
  - Response: `PaginatedResponse<Inventory>` with `count`, `next`, `previous`, `results`
  - Key fields: `id`, `name`, `description`, `kind` (empty string = regular, "smart", "constructed"), `total_hosts`, `total_groups`, `has_inventory_sources`, `variables`, `created`, `modified`, `summary_fields`

- **Get inventory detail**: `GET /api/v2/inventories/{id}/`
  - Returns full inventory object with variables and summary fields
  - Used for bottom sheet detail view (list response may have sufficient data)

- **List hosts in inventory**: `GET /api/v2/inventories/{id}/hosts/`
  - Query params: `page`, `page_size`, `search`, `order_by`
  - Response: `PaginatedResponse<Host>`
  - Key fields: `id`, `name`, `description`, `enabled`, `variables` (JSON string), `has_active_failures`, `summary_fields` (with `groups` containing group names)

### Hosts

- **List all hosts**: `GET /api/v2/hosts/`
  - Query params: `page`, `page_size`, `search`, `order_by`
  - Returns hosts across all inventories; `summary_fields.inventory` contains parent inventory info
  - Same response model as inventory hosts

- **Host detail**: `GET /api/v2/hosts/{id}/`
  - Returns full host object with variables and summary fields
  - Used for bottom sheet detail view if needed

- **Host facts**: `GET /api/v2/hosts/{id}/ansible_facts/`
  - Returns gathered Ansible facts as a JSON object
  - Fetched on demand when user expands host detail to full screen

- **Host job summaries**: `GET /api/v2/hosts/{id}/job_host_summaries/`
  - Query params: `page`, `page_size`, `order_by` (default: `-created`)
  - Returns paginated list of job runs on this host with task counts (ok, changed, failures, skipped)
  - Includes summary_fields.job with job name and status

## Design Decisions

### Decision: Separate InventoryRepository and HostRepository
- **Rationale**: The Hosts segment is a standalone screen browsing all hosts independently of inventories. A dedicated HostRepository handles both `getAllHosts()` (for the Hosts screen) and `getInventoryHosts()` (for the expanded inventory detail). InventoryRepository focuses on inventory list and detail.
- **Alternatives considered**: Single shared repository — rejected because hosts now have their own standalone section with independent concerns.

### Decision: No extra API call for host detail in standalone list
- **Rationale**: The host list endpoint returns sufficient data (name, description, enabled, variables, inventory via summary_fields) for the bottom sheet. Only fetch individual host if variables are truncated.
- **Alternatives considered**: Always fetch `/api/v2/hosts/{id}/` on tap — adds latency with no benefit for typical hosts.

### Decision: Inventory bottom sheet with expandable detail
- **Rationale**: Tapping an inventory shows a ModalBottomSheet with key fields (name, type, organization, total hosts, created, modified, variables). Expanding to full screen adds a host list for that inventory with group badges. This keeps the inventory list clean while providing drill-down.
- **Alternatives considered**: Navigate to a separate screen — rejected to stay consistent with the bottom sheet pattern used elsewhere in the app.

### Decision: Host group display as badge (inventory hosts view)
- **Rationale**: In the expanded inventory detail, host list includes `summary_fields.groups.results[]` with group names. Display the first group name as a chip/badge. If host belongs to multiple groups, show first group + "+N more" indicator.
- **Alternatives considered**: Show all groups inline — too cluttered for a mobile list.

### Decision: Host detail bottom sheet + expanded full screen
- **Rationale**: Bottom sheet shows quick-reference fields (name, last successful run, inventory, created, modified, variables). Expanding to full screen loads additional data on demand: ansible facts (from `/api/v2/hosts/{id}/ansible_facts/`), groups (already in summary_fields), and jobs run (from `/api/v2/hosts/{id}/job_host_summaries/`). Facts and job summaries are fetched lazily to avoid unnecessary API calls.
- **Alternatives considered**: Load everything in bottom sheet — rejected because facts can be very large and job history is paginated, both unsuitable for a compact bottom sheet.

### Decision: Inventory label on standalone hosts list
- **Rationale**: In the standalone Hosts screen, each host item shows `summary_fields.inventory.name` as a label/chip so the user knows which inventory the host belongs to.
- **Alternatives considered**: No inventory label — rejected because without it, the standalone host list lacks crucial context.

### Decision: Projects descoped to separate tab
- **Rationale**: Projects (Controller + EDA) will be implemented as a dedicated tab in a future feature, not as part of the Infrastructure tab. This simplifies the Infrastructure section and allows a more comprehensive Projects implementation covering both Controller and EDA projects.
- **Alternatives considered**: Include in Infrastructure tab — rejected per user direction to create a separate Projects tab.

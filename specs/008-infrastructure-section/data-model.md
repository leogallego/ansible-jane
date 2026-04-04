# Data Model: Infrastructure Section

**Feature**: 008-infrastructure-section | **Date**: 2026-04-03

## Entities

### Inventory

```
Inventory
├── id: Int                    # Unique identifier
├── name: String               # Inventory name
├── description: String        # Optional description (default: "")
├── kind: String               # "" (regular), "smart", "constructed"
├── total_hosts: Int           # Number of hosts in inventory
├── total_groups: Int          # Number of groups in inventory
├── has_inventory_sources: Boolean  # Whether inventory has external sources
├── variables: String          # JSON string of inventory variables (default: "")
├── created: String            # ISO timestamp of creation
├── modified: String           # ISO timestamp of last modification
└── summary_fields: InventorySummaryFields
    └── organization: OrganizationSummary?
        ├── id: Int
        └── name: String
```

**Serialization**: `@Serializable` with `@SerialName` for snake_case fields.

**Bottom sheet fields**: name, kind (type), organization (from summary_fields), total_hosts, created, modified, variables.

### Host

```
Host
├── id: Int                    # Unique identifier
├── name: String               # Hostname
├── description: String        # Optional description (default: "")
├── enabled: Boolean           # Whether host is enabled (default: true)
├── variables: String          # JSON string of host variables (default: "")
├── has_active_failures: Boolean  # Whether host has failing jobs
├── inventory: Int             # Parent inventory ID
├── created: String            # ISO timestamp of creation
├── modified: String           # ISO timestamp of last modification
├── last_job: Int?             # ID of last job run on this host
├── last_job_host_summary: LastJobHostSummary?
│   └── failed: Boolean        # Whether last job failed
└── summary_fields: HostSummaryFields
    ├── inventory: InventorySummary?
    │   ├── id: Int
    │   └── name: String
    └── groups: GroupsSummary?
        └── results: List<GroupSummary>
            ├── id: Int
            └── name: String
```

**Standalone hosts list**: Shows name, description, and inventory label (from summary_fields.inventory.name).
**Inventory hosts list**: Shows name, enabled status, and group badge (from summary_fields.groups.results).
**Bottom sheet fields**: name, last successful run (from summary_fields or last_job), inventory (from summary_fields.inventory.name), created, modified, variables.
**Expanded full screen**: facts (from ansible_facts endpoint), groups (from summary_fields.groups), jobs run (from job_host_summaries endpoint).

### HostFacts (fetched on demand for expanded view)

```
HostFacts
└── ansible_facts: Map<String, JsonElement>   # Arbitrary key-value facts gathered by Ansible
```

Fetched from: `GET /api/v2/hosts/{id}/ansible_facts/`

### JobHostSummary (fetched on demand for expanded view)

```
JobHostSummary
├── id: Int
├── job: Int                    # Job ID
├── host: Int                   # Host ID
├── failed: Boolean
├── ok: Int                     # Number of OK tasks
├── changed: Int                # Number of changed tasks
├── failures: Int               # Number of failed tasks
├── skipped: Int                # Number of skipped tasks
├── created: String             # ISO timestamp
└── summary_fields: JobHostSummaryFields
    └── job: JobSummaryRef
        ├── id: Int
        ├── name: String
        └── status: String
```

Fetched from: `GET /api/v2/hosts/{id}/job_host_summaries/`

## Result Wrappers (Repository Layer)

Following existing pattern (e.g., `TemplateListResult`, `RecentJobsResult`):

```
InventoryListResult
├── inventories: List<Inventory>
├── hasMore: Boolean
└── totalCount: Int

HostListResult
├── hosts: List<Host>
├── hasMore: Boolean
└── totalCount: Int
```

## Relationships

```
Inventory (1) ──── (N) Host
    │                    │
    │                    └── belongs to groups (via summary_fields)
    │
    └── belongs to Organization (via summary_fields)
```

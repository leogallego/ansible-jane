# Data Model: AAP Remote Control MVP

**Date**: 2026-04-02
**Feature**: 001-aap-remote-control

## Entities

### AapInstance

Represents a saved connection to an AAP server.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| base_url | String | Required, valid HTTPS URL | User-provided AAP server URL |
| token | String | Required, encrypted at rest | Personal Access Token (PAT) |
| api_version | Enum(V2, CONTROLLER_V2) | Required | Auto-detected on first connection |
| trust_self_signed | Boolean | Default: false | User toggle for self-signed certs |
| server_cert_fingerprint | String? | Nullable | SHA-256 fingerprint when self-signed accepted |

**Lifecycle**: Created on first successful connection. Updated on
reconnection or version re-detection. Deleted on logout.

**Storage**: Encrypted via DataStore + Tink. Only one instance
stored at a time (MVP constraint).

### JobTemplate

Represents an executable playbook configuration fetched from AAP.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Int | Required, unique | AAP server-assigned ID |
| name | String | Required | Template display name |
| description | String | Optional (empty string) | Template description |
| labels | List\<Label\> | May be empty | Organizational labels for filtering |
| ask_variables_on_launch | Boolean | Required | Whether extra_vars can be provided |
| last_job_status | String? | Nullable | Status of last job run |
| last_job_run | DateTime? | Nullable | When the last job was run |

**Lifecycle**: Fetched from server on dashboard load. Not persisted
locally — always reflects server state. Paginated (default 25 per
page, max 200).

### Label

Organizational label attached to job templates.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Int | Required, unique | AAP server-assigned ID |
| name | String | Required | Label display name |

**Note**: AAP calls these "labels", not "tags". `job_tags` is a
separate concept (Ansible playbook `--tags`).

### Job

Represents an executed instance of a job template.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| id | Int | Required, unique | AAP server-assigned ID |
| name | String | Required | Usually matches template name |
| status | JobStatus | Required | Current execution state |
| job_template_id | Int | Required | Reference to source template |
| job_template_name | String | Required | From summary_fields |
| started | DateTime? | Nullable | Null if not yet started |
| finished | DateTime? | Nullable | Null if still running |
| elapsed | Double? | Nullable | Duration in seconds |
| launch_type | String | Required | "manual", "scheduled", etc. |
| failed | Boolean | Required | Quick check for failure |

**Lifecycle**: Fetched from server. Not persisted locally.
Polled at regular intervals while in active state.

### JobStatus (Enum)

| Value | Category | Description |
|-------|----------|-------------|
| NEW | Active | Job created, not yet started |
| PENDING | Active | Waiting for task manager |
| WAITING | Active | Assigned to node, waiting to execute |
| RUNNING | Active | Currently executing |
| SUCCESSFUL | Terminal | Completed without failures |
| FAILED | Terminal | Completed with failures |
| ERROR | Terminal | Unable to run (system error) |
| CANCELED | Terminal | Manually canceled |

**Polling rule**: Continue polling while status is Active.
Stop polling when status reaches any Terminal value.

### ExtraVars

User-provided key-value pairs for job launch customization.

| Field | Type | Constraints | Notes |
|-------|------|-------------|-------|
| json_string | String | Valid JSON, max 64 KB | Sent as string in API request |

**Validation**: Must parse as valid JSON. Size limit enforced
client-side. Sent to AAP as a string value, not a raw object.

## Relationships

```text
AapInstance (1) ──fetches──> (many) JobTemplate
AapInstance (1) ──fetches──> (many) Job
JobTemplate (1) ──has──> (many) Label
JobTemplate (1) ──launches──> (many) Job
Job (1) ──launched from──> (1) JobTemplate
```

## State Transitions

### Authentication Flow

```text
Disconnected ──[enter URL + PAT]──> Validating
Validating ──[/me/ success]──> Connected
Validating ──[/me/ failure]──> Disconnected (show error)
Connected ──[401 response]──> Disconnected (prompt re-auth)
Connected ──[user logout]──> Disconnected (clear credentials)
```

### Job Lifecycle

```text
(launch) ──> new ──> pending ──> waiting ──> running
running ──> successful (terminal)
running ──> failed (terminal)
running ──> error (terminal)
running ──> canceled (terminal)
pending/waiting ──> canceled (terminal)
```

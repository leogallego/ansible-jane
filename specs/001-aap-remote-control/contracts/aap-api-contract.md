# AAP API Contract: AAP Remote Control MVP

**Date**: 2026-04-02
**Feature**: 001-aap-remote-control

## Base URL Resolution

The app MUST support two API prefix patterns:

- **AAP 2.4**: `{base_url}/api/v2/`
- **AAP 2.5+**: `{base_url}/api/controller/v2/`

Auto-detect by attempting `GET /api/controller/v2/me/` first.
If 404, fall back to `/api/v2/me/`.

## Authentication

All requests (except version detection) include:
```
Authorization: Bearer <PAT>
Content-Type: application/json
```

## Endpoints

### 1. Validate Credentials

```
GET {prefix}/me/
```

**Response** (200 OK):
```json
{
  "count": 1,
  "next": null,
  "previous": null,
  "results": [
    {
      "id": 1,
      "username": "admin",
      "first_name": "Admin",
      "last_name": "User",
      "email": "admin@example.com",
      "is_superuser": true
    }
  ]
}
```

**Used fields**: `results[0].username` (for display),
`results[0].id` (for identity).

**Error responses**: 401 (invalid token), 403 (forbidden).

### 2. List Job Templates

```
GET {prefix}/job_templates/?page={page}&page_size={size}
    &search={query}&labels__name__icontains={label}
    &order_by=-modified
```

**Query parameters**:

| Param | Purpose | Default |
|-------|---------|---------|
| `page` | Page number | 1 |
| `page_size` | Items per page | 25 (max 200) |
| `search` | Full-text search | none |
| `labels__name__icontains` | Filter by label | none |
| `order_by` | Sort field | `-modified` |

**Response** (200 OK):
```json
{
  "count": 42,
  "next": "{base_url}{prefix}/job_templates/?page=2",
  "previous": null,
  "results": [
    {
      "id": 7,
      "name": "Deploy App",
      "description": "Deploy application to production",
      "ask_variables_on_launch": true,
      "status": "successful",
      "last_job_run": "2024-03-01T10:35:45.000000Z",
      "last_job_failed": false,
      "summary_fields": {
        "labels": {
          "count": 2,
          "results": [
            { "id": 1, "name": "production" },
            { "id": 2, "name": "deploy" }
          ]
        },
        "user_capabilities": {
          "start": true
        }
      }
    }
  ]
}
```

**Used fields per template**: `id`, `name`, `description`,
`ask_variables_on_launch`, `status`, `last_job_run`,
`summary_fields.labels.results[*].{id, name}`,
`summary_fields.user_capabilities.start`.

### 3. Launch Job

```
POST {prefix}/job_templates/{id}/launch/
```

**Request body** (all optional):
```json
{
  "extra_vars": "{\"key\": \"value\"}"
}
```

**Important**: `extra_vars` MUST be a JSON **string**, not a raw
object.

**Response** (201 Created):
```json
{
  "job": 72,
  "id": 72,
  "type": "job",
  "url": "/api/v2/jobs/72/",
  "status": "pending"
}
```

**Used fields**: `job` (or `id`) for status polling.

**Error responses**: 400 (bad request / invalid extra_vars),
401 (unauthorized), 403 (no launch permission).

### 4. Get Job Status

```
GET {prefix}/jobs/{id}/
```

**Response** (200 OK):
```json
{
  "id": 72,
  "name": "Deploy App",
  "status": "running",
  "failed": false,
  "started": "2024-03-01T10:30:15.000000Z",
  "finished": null,
  "elapsed": 45.0,
  "summary_fields": {
    "job_template": {
      "id": 7,
      "name": "Deploy App"
    }
  }
}
```

**Used fields**: `id`, `name`, `status`, `failed`, `started`,
`finished`, `elapsed`, `summary_fields.job_template.name`.

**Status values**: `new`, `pending`, `waiting`, `running`,
`successful`, `failed`, `error`, `canceled`.

**Polling rule**: Poll every 5 seconds while status is `new`,
`pending`, `waiting`, or `running`. Stop on terminal status.

### 5. List Recent Jobs

```
GET {prefix}/jobs/?order_by=-created&page_size=20
```

**Query parameters**:

| Param | Purpose | Default |
|-------|---------|---------|
| `order_by` | Sort field | `-created` |
| `page_size` | Items per page | 20 |
| `status` | Filter by status | none |

**Response**: Same paginated wrapper as job templates. Each result
uses the same fields as Get Job Status.

## Pagination Contract

All list endpoints use the same wrapper:

```json
{
  "count": 42,
  "next": "URL or null",
  "previous": "URL or null",
  "results": [ ... ]
}
```

- `next`: Full URL for next page, or `null` if last page
- `previous`: Full URL for previous page, or `null` if first page
- Load more on scroll by following `next` URL

## Error Response Contract

All error responses follow:

```json
{
  "detail": "Human-readable error message"
}
```

HTTP status codes:
- 400: Bad request (invalid input)
- 401: Authentication required or token expired/revoked
- 403: Permission denied
- 404: Resource not found
- 500: Server error

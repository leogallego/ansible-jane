# API Contract: Controller Endpoints (Activity Section)

**Primary Base Path**: `/api/controller/v2/` (Gateway path, auto-detected)
**Fallback Base Path**: `/api/v2/` (used when Gateway path returns 404)

The app uses `ApiVersionDetector` to probe `/api/controller/v2/me/` at login. If it responds (non-404), all Controller endpoints use the `/api/controller/v2/` prefix. Otherwise, falls back to `/api/v2/`. All endpoints below are relative to whichever base path is active — the Retrofit service handles this transparently.

## GET jobs/ (Enhanced)

Existing endpoint — add status filtering support.

### New Query Parameters

| Param | Type | Description |
|-------|------|-------------|
| `status` | string | Filter by single status (e.g., `failed`) |
| `or__status` | string (repeated) | Filter by multiple statuses with OR logic |

### Example: Multi-status filter
```
GET {base}/jobs/?or__status=failed&or__status=error&order_by=-created&page=1&page_size=20
```

### Response
Unchanged — `PaginatedResponse<Job>` as already implemented.

---

## GET schedules/

List all schedules with pagination.

### Query Parameters

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | int | 1 | Page number |
| `page_size` | int | 20 | Results per page |
| `order_by` | string | `-next_run` | Sort field (prefix `-` for descending) |
| `enabled` | bool | — | Filter by enabled status |

### Response: `PaginatedResponse<Schedule>`

```json
{
  "count": 42,
  "next": "{base}/schedules/?page=2",
  "previous": null,
  "results": [
    {
      "id": 1,
      "name": "Daily Backup",
      "description": "Runs backup playbook daily",
      "enabled": true,
      "rrule": "DTSTART;TZID=UTC:20240101T000000 RRULE:FREQ=DAILY;INTERVAL=1",
      "dtstart": "2024-01-01T00:00:00Z",
      "dtend": null,
      "timezone": "UTC",
      "next_run": "2026-04-04T00:00:00Z",
      "unified_job_template": 5,
      "summary_fields": {
        "unified_job_template": {
          "id": 5,
          "name": "Backup Playbook",
          "unified_job_type": "job"
        }
      }
    }
  ]
}
```

---

## PATCH schedules/{id}/

Toggle schedule enabled/disabled state.

### Request Body

```json
{"enabled": false}
```

### Response

Returns the full updated schedule object (same shape as list item).

### Error Responses

| Status | Meaning |
|--------|---------|
| 200 | Success |
| 403 | Insufficient permissions |
| 404 | Schedule not found |

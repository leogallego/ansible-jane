# API Contract: EDA Controller Endpoints (Activity Section)

**Base Path**: `/api/eda/v1/` (via Gateway)

## GET /api/eda/v1/audit-rules/

List EDA rule audit events.

### Query Parameters

| Param | Type | Default | Description |
|-------|------|---------|-------------|
| `page` | int | 1 | Page number |
| `page_size` | int | 20 | Results per page |

### Response: `PaginatedResponse<EdaRuleAudit>`

```json
{
  "count": 15,
  "next": null,
  "previous": null,
  "results": [
    {
      "id": 1,
      "name": "Say Hello",
      "status": "successful",
      "activation_instance_id": 3,
      "fired_at": "2026-04-03T10:15:30Z",
      "rule_name": "Say Hello",
      "rule_set_name": "Say Hello Rules",
      "activation_name": "Demo Activation",
      "created_at": "2026-04-03T10:15:30Z"
    }
  ]
}
```

**Note**: Exact field names need verification against live EDA API at implementation time. The model is designed with nullable optional fields and `ignoreUnknownKeys = true` to handle variations.

### Error Responses

| Status | Meaning | App Handling |
|--------|---------|--------------|
| 200 | Success | Display list |
| 403 | Insufficient permissions | Show error in EDA tab only |
| 404 | EDA not configured | Show empty state: "EDA is not configured" |
| 502/503 | EDA controller unreachable | Show error in EDA tab only (FR-011) |

---

## GET /api/eda/v1/audit-rules/{id}/

Get details for a single rule audit event.

### Response: `EdaRuleAudit`

Same shape as list item, potentially with additional detail fields.

## Architecture Note: Separate Retrofit Interface

EDA endpoints use a different base path (`/api/eda/v1/`) than Controller (`/api/v2/`). Implementation uses a separate `EdaApiService` Retrofit interface with its own base URL, sharing the same OkHttpClient and auth interceptor.

```
AapApiProvider
├── getApiService()     → AapApiService  (base: /api/v2/)
└── getEdaApiService()  → EdaApiService  (base: /api/eda/v1/)
    └── shares OkHttpClient + auth interceptor
```

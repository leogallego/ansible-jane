# Research: AAP Remote Control MVP

**Date**: 2026-04-02
**Feature**: 001-aap-remote-control

## Decision 1: AAP API Endpoint Versioning

**Decision**: Support both AAP 2.4 (`/api/v2/`) and AAP 2.5+
(`/api/controller/v2/`) endpoint paths with auto-detection.

**Rationale**: AAP 2.5 introduced a platform gateway that changes
endpoint prefixes. Legacy `/api/v2/` paths still work but are
deprecated. Auto-detecting the version on first connection ensures
the app works with both current and future AAP installations.

**Endpoint mapping**:

| Function | AAP 2.4 | AAP 2.5+ |
|----------|---------|----------|
| Validate user | `GET /api/v2/me/` | `GET /api/controller/v2/me/` |
| List templates | `GET /api/v2/job_templates/` | `GET /api/controller/v2/job_templates/` |
| Launch job | `POST /api/v2/job_templates/{id}/launch/` | `POST /api/controller/v2/job_templates/{id}/launch/` |
| Job status | `GET /api/v2/jobs/{id}/` | `GET /api/controller/v2/jobs/{id}/` |
| List jobs | `GET /api/v2/jobs/` | `GET /api/controller/v2/jobs/` |
| Get token | `POST /api/v2/tokens/` | `POST /api/gateway/v1/tokens/` |

**Detection strategy**: On first connection, attempt
`GET /api/controller/v2/me/`. If 404, fall back to `/api/v2/me/`.
Store detected version with instance config.

**Alternatives considered**:
- Hardcode `/api/v2/` only — rejected: would break on newer AAP
- Require user to select version — rejected: unnecessary UX burden

## Decision 2: Template Filtering Terminology

**Decision**: Use "labels" (AAP's actual term) instead of "tags"
in the implementation. The UI can display them as "Labels" to
match AAP's nomenclature.

**Rationale**: AAP uses `labels` as the ManyToMany relationship on
JobTemplate for organizational grouping. `job_tags` is a separate
concept referring to Ansible playbook `--tags`. Confusing these
would cause filtering bugs.

**API query parameters for label filtering**:
- `?labels__name=production` (exact match)
- `?labels__name__icontains=prod` (partial match)
- `?search=deploy` (full-text search across fields)

**Alternatives considered**:
- Use `job_tags` field — rejected: different concept (playbook
  tags, not organizational labels)

## Decision 3: Job Status Values

**Decision**: Support all 8 AAP job status values, not just the 4
originally specified.

**All statuses**:
- Active (keep polling): `new`, `pending`, `waiting`, `running`
- Terminal (stop polling): `successful`, `failed`, `error`,
  `canceled`

**Rationale**: The spec listed only 4 statuses (Pending, Running,
Successful, Failed). The API actually returns 8 distinct values.
Missing `error`, `canceled`, `new`, and `waiting` would cause
undefined UI states.

**Alternatives considered**:
- Map to 4 simplified states — rejected: loses information; user
  may need to distinguish `error` from `failed`

## Decision 4: Encrypted Storage Architecture

**Decision**: Use Jetpack DataStore (Proto) + Google Tink +
Android Keystore for credential encryption.

**Dependencies**:
- `androidx.datastore:datastore` (Proto DataStore)
- `androidx.datastore:datastore-tink` (official Tink integration)
- `com.google.crypto.tink:tink-android` (crypto primitives)

**Rationale**: `EncryptedSharedPreferences` is deprecated.
Google shipped the official `datastore-tink` artifact specifically
as the replacement. It provides `AeadSerializer` for transparent
encryption of the entire DataStore file.

**Architecture**: DataStore handles async coroutine-based I/O.
Tink provides AEAD encryption. Android Keystore holds the master
key. All access is async (no main thread violations).

**Alternatives considered**:
- Raw Tink + file I/O — rejected: reinvents DataStore features
- SQLCipher — rejected: heavyweight for simple key-value storage

## Decision 5: Self-Signed Certificate Handling

**Decision**: Use a custom `X509TrustManager` per instance,
dynamically configured based on the user's toggle setting.

**Approach**: When the toggle is on, fetch the server's
certificate on first connection, present its fingerprint to the
user, and store it alongside instance config. Build a custom
`TrustManagerFactory` with that certificate loaded into a
`KeyStore`. When toggle is off, use the default system trust store.

**Rationale**: `network_security_config.xml` is compile-time and
app-wide — cannot support per-instance dynamic toggling. OkHttp's
`CertificatePinner` requires the TrustManager to already trust
the cert. Custom TrustManager is the correct approach.

**Alternatives considered**:
- Trust-all TrustManager — rejected: disables all certificate
  validation, security risk
- `network_security_config.xml` — rejected: compile-time only,
  not per-instance

## Decision 6: Extra Variables Format

**Decision**: The `extra_vars` field must be sent as a JSON
**string**, not a raw JSON object.

**Rationale**: The AAP API expects `extra_vars` as a string field
containing serialized JSON (or YAML). The app's JSON validation
(FR-009) must parse the user input to confirm validity, then send
it as a string value in the launch request body.

**Example request body**:
```json
{
  "extra_vars": "{\"target_env\": \"staging\"}"
}
```

## Decision 7: Pagination Strategy

**Decision**: Use AAP's built-in pagination (`?page=N&page_size=N`)
with client-side lazy loading.

**Rationale**: AAP defaults to 25 items per page, max 200.
Template lists and job lists both use the same paginated wrapper
(`count`, `next`, `previous`, `results`). Lazy loading (load more
on scroll) provides good UX without fetching all data upfront.

**Alternatives considered**:
- Fetch all pages at once — rejected: slow for large inventories
- Custom pagination — rejected: AAP already provides it

# Data Model: Multi-AAP Instance Support

**Date**: 2026-04-04  
**Feature**: 009-multi-instance-support

## Entities

### AapInstance

Represents a single AAP connection with all its configuration.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| id | String (UUID) | Yes | Unique identifier, generated on creation |
| baseUrl | String | Yes | AAP base URL (e.g., `https://aap.example.com`). Encrypted in storage |
| token | String | Yes | Personal Access Token. Encrypted in storage |
| alias | String? | No | User-defined friendly name (e.g., "Production") |
| apiVersion | String | Yes | API version prefix (default: `v2`) |
| trustSelfSigned | Boolean | Yes | Whether to accept self-signed certificates |
| certFingerprint | String? | No | Optional certificate fingerprint for pinning |

**Identity**: Unique by `id`. Duplicate detection uses normalized `baseUrl` (case-insensitive, trailing slash stripped).

**Display label**: `alias ?: URI(baseUrl).host` for short labels. Pills always show URL; if alias is set, alias is primary and URL is secondary.

### InstancesState (storage model)

The serialized form stored in DataStore.

| Field | Type | Description |
|-------|------|-------------|
| instances | List<SerializedInstance> | All saved instances |
| activeInstanceId | String? | ID of the currently active instance |

### SerializedInstance (storage model)

Per-instance record with encrypted fields.

| Field | Type | Description |
|-------|------|-------------|
| id | String | Instance UUID |
| encryptedUrl | String | Base64-encoded Tink ciphertext of baseUrl |
| encryptedToken | String | Base64-encoded Tink ciphertext of token |
| alias | String? | Plaintext alias (not sensitive) |
| apiVersion | String | Plaintext API version |
| trustSelfSigned | Boolean | Plaintext flag |
| certFingerprint | String? | Plaintext fingerprint |

## State Transitions

### Instance Lifecycle

```
[Not Exists] → Add → [Active] (if first instance)
[Not Exists] → Add → [Inactive] (if other instances exist)
[Inactive] → Tap pill → [Active] (previous active becomes Inactive)
[Active] → Remove → [Not Exists] (next instance auto-promoted to Active)
[Inactive] → Remove → [Not Exists]
[Active] → Token expires → [Active, Invalid] → Re-auth → [Active, Valid]
```

### App-Level State

```
No instances → Auth screen (start destination)
1+ instances → Main dashboard (active instance drives all data)
Last instance removed → Navigate to Auth screen
```

## Relationships

- `TokenManager` owns the `InstancesState` (source of truth)
- `AapApiProvider` reads active instance to resolve API service
- `AuthInterceptor` reads active instance's token for Bearer header
- All data repositories (Template, Job, Workflow, etc.) use API service for active instance implicitly via `AapApiProvider`
- `SettingsViewModel` observes `TokenManager.instances` and `TokenManager.activeInstance`
- `MainScreen` observes `TokenManager.activeInstance` for top bar label

## Validation Rules

- `baseUrl` must start with `https://` (enforced by HTTPS-only policy)
- `baseUrl` must be unique across all instances (case-insensitive, trailing slash normalized)
- `token` must not be empty
- `id` is auto-generated UUID, never user-editable

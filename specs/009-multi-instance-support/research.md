# Research: Multi-AAP Instance Support

**Date**: 2026-04-04  
**Feature**: 009-multi-instance-support

## R-001: Multi-Instance Credential Storage in DataStore

**Decision**: Store all instances as a single JSON-serialized string in DataStore, with each instance's URL and token encrypted individually via Tink before serialization.

**Rationale**: The current TokenManager already uses DataStore + Tink with Base64-encoded ciphertexts. Extending this to a JSON array of `{id, encryptedUrl, encryptedToken, alias, apiVersion, trustSelfSigned, certFingerprint}` objects keeps the same security model. Kotlin Serialization (already in the project) handles JSON encoding. A single DataStore key (`instances_json`) replaces the existing flat keys.

**Alternatives considered**:
- Per-instance DataStore files: Over-engineered for 2-10 instances, complicates enumeration and cleanup.
- Room database: Adds heavy dependency for what's essentially a small list. Tink column-level encryption is more complex.
- Proto DataStore: Would require adding protobuf dependency. Not justified.

## R-002: Per-Instance API Service Caching

**Decision**: Change `AapApiProvider` from caching a single `AapApiService`/`EdaApiService` to caching a `Map<String, Pair<AapApiService, EdaApiService>>` keyed by instance ID. `getApiService()` and `getEdaApiService()` resolve the active instance from `TokenManager.activeInstance`.

**Rationale**: Each instance may have a different base URL and SSL trust setting, requiring separate Retrofit instances. The map approach avoids rebuilding services on every switch — only the first access per instance builds. Evict entries when an instance is removed.

**Alternatives considered**:
- Single cached service (rebuild on switch): Simpler but adds latency on every switch. With 2-5 instances, caching all is cheap.
- Shared OkHttpClient with per-request base URL: Retrofit doesn't support dynamic base URLs cleanly without custom interceptors. Fragile.

## R-003: Active Instance Propagation to ViewModels

**Decision**: `TokenManager` exposes `activeInstance: StateFlow<AapInstance?>`. ViewModels that load data (Templates, Jobs, Schedules, etc.) collect `activeInstance` in their `init` block and call their `load*()` method when it changes. This leverages the existing pattern where ViewModels already call `loadTemplates()` etc. in `init`.

**Rationale**: Observing a StateFlow is the established pattern in this codebase (all ViewModels use `StateFlow` + `viewModelScope.launch`). No new reactive framework needed. The switch triggers a fresh fetch, matching the spec requirement that data is not cached across instances.

**Alternatives considered**:
- Event bus / SharedFlow for switch events: Adds coupling. StateFlow observation is simpler and already used.
- Re-creating ViewModels on switch: Would require navigation workarounds and lose tab state. Too disruptive.

## R-004: 401 Handling Per Instance

**Decision**: When `AuthInterceptor` detects a 401, include the instance ID in the unauthorized event so navigation can route to the auth screen pre-filled with that instance's URL/alias. Change `unauthorizedEvent` from `SharedFlow<Unit>` to `SharedFlow<String>` (instance ID).

**Rationale**: The spec requires per-instance re-authentication without affecting other instances. The current 401 handler logs out globally — this must be scoped to the failing instance.

**Alternatives considered**:
- Per-instance interceptors: Would require separate OkHttpClients per instance. Overkill since instance ID can be inferred from TokenManager state.

## R-005: Legacy Credential Cleanup

**Decision**: On first launch with the new version, detect the old flat DataStore keys (`base_url`, `token`, `api_version`, `trust_self_signed`, `cert_fingerprint`). If present, clear them. User must re-authenticate.

**Rationale**: Per clarification, only one legacy user exists. Migration code is not worth maintaining. Simply clearing old keys ensures no orphaned data.

**Alternatives considered**:
- Auto-migration: Minimal effort but adds code to maintain for one user. Rejected per user decision.

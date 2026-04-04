# Quickstart: Multi-AAP Instance Support

**Date**: 2026-04-04  
**Feature**: 009-multi-instance-support

## What This Feature Does

Adds support for connecting to multiple AAP instances simultaneously. Users can add, switch between, and remove instances from Settings. The active instance's label is visible in the dashboard top bar.

## Key Changes at a Glance

### New Files
- `model/AapInstance.kt` — Instance data class
- `presentation/settings/SettingsViewModel.kt` — Instance management logic
- `presentation/settings/SettingsUiState.kt` — Settings state model

### Modified Files (by layer)

**Data Layer:**
- `TokenManager.kt` — Replace flat credential storage with `Map<id, AapInstance>` + `activeInstanceId`. New methods: `saveInstance()`, `removeInstance()`, `setActiveInstance()`. Expose `instances: StateFlow<List<AapInstance>>` and `activeInstance: StateFlow<AapInstance?>`.
- `AuthRepository.kt` — Accept alias parameter in `validateCredentials()`. Add `reAuthenticate(instanceId, newToken)`. Remove global `logout()`, add `logoutInstance(instanceId)`.

**Network Layer:**
- `AapApiProvider.kt` — Cache `Map<instanceId, services>` instead of single service. Resolve active instance on `getApiService()`/`getEdaApiService()`.
- `AuthInterceptor.kt` — Emit instance ID with 401 events instead of `Unit`.

**Presentation Layer:**
- `AuthViewModel.kt` — Add alias parameter to `connect()`. Support pre-fill mode for re-authentication.
- All data ViewModels — Observe `TokenManager.activeInstance` and re-fetch on change.

**UI Layer:**
- `AuthScreen.kt` — Add optional alias text field. Support pre-filled URL/alias for re-auth mode.
- `SettingsScreen.kt` — Instance pills section with active/inactive indicators, remove buttons, details bottom sheet, and "+ Add Instance" button.
- `MainScreen.kt` — Show active instance label (alias or hostname) as subtitle in top bar.

**Navigation:**
- `AppNavigation.kt` — Handle add-instance and re-auth navigation flows. Scope 401 handling to specific instance.

## Build & Run

No new dependencies. No build configuration changes. Standard `./gradlew assembleDebug` builds the feature.

## Testing Checklist

1. Fresh install → auth screen → add instance with alias → verify pill shows alias + URL
2. Settings → "+ Add Instance" → add second instance → verify both pills visible
3. Tap inactive pill → verify data refreshes (templates/jobs change)
4. Tap active pill → verify details bottom sheet appears
5. Remove inactive instance → verify it disappears, active unaffected
6. Remove active instance → verify remaining instance auto-activates
7. Remove last instance → verify redirect to auth screen
8. Verify top bar shows active instance label on dashboard

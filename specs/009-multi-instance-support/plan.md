# Implementation Plan: Multi-AAP Instance Support

**Branch**: `009-multi-instance-support` | **Date**: 2026-04-04 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/009-multi-instance-support/spec.md`

## Summary

Refactor the app from single-instance to multi-instance AAP support. Replace the flat credential storage in `TokenManager` with a keyed `Map<instanceId, AapInstance>` collection (still DataStore + Tink encrypted). Update `AapApiProvider` to cache per-instance API services. Add instance management UI in Settings (pills/chips with switch/remove/details), optional alias field in auth screen, and active instance indicator in the dashboard top bar. All ViewModels observe the active instance and re-fetch data on switch.

## Technical Context

**Language/Version**: Kotlin (JVM 17), compileSdk 35, minSdk 31  
**Primary Dependencies**: Jetpack Compose (Material 3 BOM), Navigation Compose, Retrofit + Kotlin Serialization, Koin, Google Tink  
**Storage**: Jetpack DataStore (Preferences) + Tink AES-256-GCM encryption  
**Testing**: Manual (no automated test framework configured)  
**Target Platform**: Android 12+ (API 31+)  
**Project Type**: Mobile app (Android)  
**Performance Goals**: Instance switch + data refresh < 3 seconds  
**Constraints**: Single ComponentActivity, no Fragments, no XML layouts  
**Scale/Scope**: 2-10 instances per user, ~25 Kotlin source files affected

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Kotlin-Only | PASS | No Java introduced |
| II. Compose-First UI | PASS | New UI is all Compose + Material 3. No XML, Fragments, or extra Activities |
| III. MVVM with UDF | PASS | New SettingsViewModel exposes StateFlow. All ViewModels re-observe active instance |
| IV. Security-First | PASS | Still DataStore + Tink. Per-value encryption for each instance's credentials |
| V. Lean Dependencies | PASS | No new dependencies. Kotlin Serialization (already in stack) for JSON serialization of instance list |
| VI. API-Driven Design | PASS | No new API endpoints. Same Retrofit interfaces, just per-instance service caching |

All gates pass. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/009-multi-instance-support/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/kotlin/io/github/leogallego/ansiblejane/
├── model/
│   └── AapInstance.kt              # NEW — data class for instance
├── data/
│   ├── TokenManager.kt            # MODIFY — keyed instance storage
│   ├── AuthRepository.kt          # MODIFY — per-instance auth
│   └── DataModule.kt              # MODIFY — if DI changes needed
├── network/
│   ├── AapApiProvider.kt          # MODIFY — per-instance service cache
│   ├── AuthInterceptor.kt         # MODIFY — per-instance 401 handling
│   └── NetworkModule.kt           # MODIFY — if DI changes needed
├── presentation/
│   ├── auth/
│   │   ├── AuthViewModel.kt       # MODIFY — alias param, re-auth flow
│   │   └── AuthUiState.kt         # MODIFY — if re-auth mode state needed
│   └── settings/
│       ├── SettingsViewModel.kt   # NEW — instance list, switch, remove
│       └── SettingsUiState.kt     # NEW — settings state
├── ui/
│   ├── auth/
│   │   └── AuthScreen.kt          # MODIFY — add alias field, pre-fill for re-auth
│   ├── settings/
│   │   └── SettingsScreen.kt      # MODIFY — instance pills, bottom sheet, add button
│   └── main/
│       └── MainScreen.kt          # MODIFY — active instance label in top bar
└── navigation/
    └── AppNavigation.kt           # MODIFY — re-auth route params, add-instance flow
```

**Structure Decision**: Feature-based packaging within existing layers. One new model file (`AapInstance.kt`), one new ViewModel + state (`SettingsViewModel`/`SettingsUiState`), and modifications to ~10 existing files.

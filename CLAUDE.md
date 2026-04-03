# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

AAP Remote Control — a lightweight Android app that serves as a remote control for Ansible Automation Platform (AAP). Users authenticate with their AAP instance, browse job templates, launch playbooks, and monitor job status from their phone.

Full specification is in `idea.md`.

## Tech Stack (Strictly Enforced)

- **Language:** Kotlin only. No Java.
- **UI:** Jetpack Compose with Material 3 (Material You). No XML layouts, no Fragments. Single `ComponentActivity`.
- **Architecture:** MVVM with Unidirectional Data Flow. ViewModels expose UI state via `StateFlow`.
- **Networking:** Retrofit + Kotlin Serialization + Coroutines.
- **DI:** Koin (not Hilt).
- **Security:** Jetpack DataStore + Tink (Android Keystore-backed) for token/URL storage. HTTPS only via `network_security_config.xml`. `EncryptedSharedPreferences` is deprecated — do not use.

## AAP API Endpoints

All authenticated requests use header: `Authorization: Bearer <TOKEN>`

| Feature | Method | Endpoint |
|---------|--------|----------|
| Validate credentials | GET | `/api/v2/me/` |
| Get templates | GET | `/api/v2/job_templates/` |
| Launch job | POST | `/api/v2/job_templates/{id}/launch/` |
| Job status | GET | `/api/v2/jobs/{id}/` |
| Get token | POST | `/api/v2/tokens/` |

## Architecture Layers

- **Network Layer:** Retrofit interface `AapApiService`, OkHttpClient with auth interceptor, Koin `networkModule`
- **Data Layer:** `TokenManager` (DataStore + Tink encryption), repositories
- **Presentation:** ViewModels with `StateFlow<UiState>` (Idle, Loading, Success, Error pattern)
- **UI:** Compose screens reacting to ViewModel state

## Development Rules

- Create temporary files in the project directory (e.g., `.tmp/`), not in the system `/tmp`. Only use `/tmp` if absolutely necessary. Clean up temp files when done.

## Security Rules

- Never hardcode URLs or tokens
- Only use DataStore + Tink for credentials — never `EncryptedSharedPreferences` (deprecated), plain `SharedPreferences`, or SQLite
- Enforce HTTPS-only via network security config

## Active Technologies
- Kotlin (latest stable, targeting JVM 17) + Jetpack Compose (Material 3), Retrofit, (001-aap-remote-control)
- Jetpack DataStore + Tink (encrypted, local only) (001-aap-remote-control)
- Kotlin (JVM 17), compileSdk 35, minSdk 31 + Jetpack Compose (Material 3 BOM), Navigation Compose, Retrofit, Koin (002-nav-ui-modernize)
- DataStore + Tink (unchanged) (002-nav-ui-modernize)
- Kotlin (latest stable, targeting JVM 17) + Jetpack Compose (Material 3 BOM), Navigation Compose, Retrofit + Kotlin Serialization, Koin (004-workflow-templates)
- DataStore + Tink (unchanged — no new storage needs) (004-workflow-templates)

## Recent Changes
- 001-aap-remote-control: Added Kotlin (latest stable, targeting JVM 17) + Jetpack Compose (Material 3), Retrofit,

# Quickstart: AAP Remote Control MVP

**Date**: 2026-04-02
**Feature**: 001-aap-remote-control

## Prerequisites

- Android Studio (latest stable)
- Android SDK API 26+ (Android 8.0 Oreo minimum)
- An AAP/AWX instance with:
  - HTTPS access (or self-signed cert you can accept)
  - A Personal Access Token (PAT) with job template launch
    permissions
  - At least one job template configured

## Project Setup

1. Create a new Android project in Android Studio:
   - Template: Empty Activity (Compose)
   - Language: Kotlin
   - Minimum SDK: API 26
   - Build configuration: Kotlin DSL (build.gradle.kts)

2. Add dependencies to `app/build.gradle.kts`:
   - Jetpack Compose BOM + Material 3
   - Compose Navigation
   - Retrofit + KotlinX Serialization converter
   - OkHttp + logging interceptor
   - Koin (core + compose)
   - Jetpack DataStore + Tink (encrypted storage)
   - Coroutines

3. Configure `network_security_config.xml` for HTTPS-only.

4. Set up the single `ComponentActivity` with Compose content.

## Architecture Overview

```text
┌─────────────────────────────────────────────────┐
│                    UI Layer                      │
│  AuthScreen  TemplateListScreen  JobStatusScreen │
│         ↕ (collectAsState)                       │
├─────────────────────────────────────────────────┤
│               Presentation Layer                 │
│    AuthViewModel  TemplatesViewModel             │
│    JobViewModel                                  │
│         ↕ (StateFlow<UiState>)                   │
├─────────────────────────────────────────────────┤
│                  Data Layer                      │
│  AuthRepository  TemplateRepository              │
│  JobRepository  TokenManager                     │
│         ↕ (suspend functions)                    │
├─────────────────────────────────────────────────┤
│                Network Layer                     │
│  AapApiService (Retrofit)  OkHttpClient          │
│  AuthInterceptor  CertTrustManager               │
└─────────────────────────────────────────────────┘
```

## Koin Module Organization

```text
networkModule   → Retrofit, OkHttpClient, AapApiService
dataModule      → TokenManager, DataStore, Repositories
presentationModule → ViewModels
```

## Screens and Navigation

```text
AuthScreen (start)
  ├── [Connect success] → TemplateListScreen
  └── [Connect fail] → AuthScreen (show error)

TemplateListScreen (dashboard)
  ├── [Launch + confirm] → JobStatusScreen
  ├── [Recent Jobs] → RecentJobsScreen
  └── [Logout] → AuthScreen

JobStatusScreen
  └── [Back] → TemplateListScreen

RecentJobsScreen
  └── [Back] → TemplateListScreen
```

## Verification Checklist

After each user story implementation, verify:

### US1 (Auth)
- [ ] Can enter URL + PAT and connect
- [ ] Invalid credentials show error
- [ ] Credentials survive app restart
- [ ] Logout clears credentials
- [ ] Self-signed cert toggle works

### US2 (Templates + Launch)
- [ ] Template list loads with names, descriptions, labels
- [ ] Search bar filters by name
- [ ] Label chips filter by label
- [ ] Launch shows confirmation dialog
- [ ] Extra vars input appears when template supports it
- [ ] Invalid JSON is rejected
- [ ] Successful launch shows job ID

### US3 (Job Monitoring)
- [ ] Job status view shows current state
- [ ] Status auto-updates (polling)
- [ ] All 8 status values display correctly
- [ ] Recent jobs list loads from server
- [ ] Failed jobs are visually distinct

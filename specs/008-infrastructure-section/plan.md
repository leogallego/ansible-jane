# Implementation Plan: Infrastructure Section (Inventories and Hosts)

**Branch**: `008-infrastructure-section` | **Date**: 2026-04-03 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/008-infrastructure-section/spec.md`

## Summary

Add two functional screens plus detail bottom sheets to the existing Infrastructure tab (which currently shows placeholders): Inventories and Hosts. This follows the same MVVM + Compose patterns used by Templates and Activity sections. The feature adds new API endpoints to `AapApiService`, two data models, two repositories, three ViewModels, two screen composables, and two bottom sheet composables.

Projects have been descoped to a separate dedicated tab (Controller + EDA projects).

## Technical Context

**Language/Version**: Kotlin (JVM 17), compileSdk 35, minSdk 31
**Primary Dependencies**: Jetpack Compose (Material 3 BOM), Navigation Compose, Retrofit + Kotlin Serialization, Koin
**Storage**: N/A (no new local storage — all data fetched from API)
**Testing**: Manual testing against AAP 2.5+ instance
**Target Platform**: Android (minSdk 31)
**Project Type**: Mobile app (Android)
**Performance Goals**: First page load < 3 seconds on typical mobile connection
**Constraints**: All API calls through AAP Gateway, HTTPS only, Bearer token auth
**Scale/Scope**: 2 new screens, 2 bottom sheets, ~14 new files

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Kotlin-Only | PASS | All new code is Kotlin |
| II. Compose-First UI | PASS | All new screens use Jetpack Compose + Material 3 |
| III. MVVM + UDF | PASS | ViewModels with StateFlow, sealed UiState interfaces |
| IV. Security-First | PASS | No new credential handling; uses existing auth interceptor |
| V. Lean Dependencies | PASS | No new dependencies required |
| VI. API-Driven Design | PASS | New endpoints added to existing AapApiService |

All gates pass. No violations to justify.

## Project Structure

### Documentation (this feature)

```text
specs/008-infrastructure-section/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (N/A — internal app, no external contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks)
```

### Source Code (repository root)

```text
app/src/main/kotlin/com/example/aapremote/
├── model/
│   ├── Inventory.kt              # NEW — API response model
│   ├── Host.kt                   # NEW — API response model (includes HostFacts, JobHostSummary)
├── network/
│   └── AapApiService.kt          # MODIFY — add inventory and host endpoints (including ansible_facts, job_host_summaries)
├── data/
│   ├── InventoryRepository.kt    # NEW — inventory data access
│   └── HostRepository.kt         # NEW — host data access (all hosts + inventory hosts + facts + job summaries)
├── presentation/
│   ├── inventory/
│   │   ├── InventoriesViewModel.kt    # NEW — inventory list
│   │   └── InventoriesUiState.kt      # NEW — sealed state interface
│   └── hosts/
│       ├── HostsViewModel.kt          # NEW — all hosts list + search
│       ├── HostsUiState.kt            # NEW — sealed state interface
│       ├── InventoryHostsViewModel.kt # NEW — hosts within inventory (for expanded view)
│       └── InventoryHostsUiState.kt   # NEW — sealed state interface
├── ui/
│   ├── inventory/
│   │   ├── InventoriesScreen.kt       # NEW — inventory list
│   │   └── InventoryDetailSheet.kt    # NEW — bottom sheet with inventory details + expand to hosts
│   └── hosts/
│       ├── HostsScreen.kt            # NEW — standalone all-hosts list with search
│       └── HostDetailSheet.kt         # NEW — bottom sheet with host details + expand
├── ui/main/
│   └── TabDefinitions.kt             # MODIFY — mark Inventories and Hosts as isImplemented = true, remove Projects segment
├── navigation/
│   └── MainNavigation.kt             # MODIFY — route Infrastructure segments to screens
└── di/
    ├── DataModule.kt                  # MODIFY — register new repositories
    └── PresentationModule.kt          # MODIFY — register new ViewModels
```

**Structure Decision**: Feature-based packaging within existing architecture layers. Inventory and Host each get their own repository. HostRepository handles both the standalone all-hosts list and hosts-within-inventory queries. InventoryHostsViewModel is used for the expanded inventory detail view showing that inventory's hosts.

## Complexity Tracking

No constitution violations. Table not needed.

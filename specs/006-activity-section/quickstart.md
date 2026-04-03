# Quickstart: Activity Section

**Feature Branch**: `006-activity-section`

## What This Feature Does

Adds a full Activity section to the AAP Remote Control app with three segments:
1. **Jobs** — Enhanced job list with status filter chips (multi-select)
2. **Schedules** — View scheduled jobs and toggle enabled/disabled
3. **EDA Audit** — View Event-Driven Ansible rule audit events

## Architecture Overview

```
┌─────────────────────────────────────────────────┐
│                  Activity Tab                    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │   Jobs   │  │Schedules │  │EDA Audit │      │
│  │(enhanced)│  │  (new)   │  │  (new)   │      │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘      │
│       │              │              │            │
│  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐      │
│  │RecentJobs│  │Schedules │  │EdaAudit  │      │
│  │ViewModel │  │ViewModel │  │ViewModel │      │
│  └────┬─────┘  └────┬─────┘  └────┴─────┘      │
│       │              │              │            │
│  ┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐      │
│  │   Job    │  │Schedule  │  │EdaAudit  │      │
│  │Repository│  │Repository│  │Repository│      │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘      │
│       │              │              │            │
│  ┌────┴──────────────┴─────┐  ┌────┴─────┐      │
│  │     AapApiService       │  │EdaApi    │      │
│  │   (Controller /api/v2/) │  │Service   │      │
│  └─────────────────────────┘  └──────────┘      │
└─────────────────────────────────────────────────┘
```

## Key Files to Create/Modify

### New Files
| File | Purpose |
|------|---------|
| `model/Schedule.kt` | Schedule data class + summary fields |
| `model/EdaRuleAudit.kt` | EDA rule audit event data class |
| `network/EdaApiService.kt` | Retrofit interface for EDA endpoints |
| `data/ScheduleRepository.kt` | Schedule data access |
| `data/EdaAuditRepository.kt` | EDA audit data access |
| `presentation/schedules/SchedulesViewModel.kt` | Schedules state management |
| `presentation/schedules/SchedulesUiState.kt` | Sealed UI state |
| `presentation/eda/EdaAuditViewModel.kt` | EDA audit state management |
| `presentation/eda/EdaAuditUiState.kt` | Sealed UI state |
| `ui/schedules/SchedulesScreen.kt` | Schedules list UI |
| `ui/eda/EdaAuditScreen.kt` | EDA audit list UI |
| `ui/eda/EdaAuditDetailSheet.kt` | Bottom sheet for event details |
| `ui/components/StatusFilterChips.kt` | Reusable filter chips component |

### Modified Files
| File | Change |
|------|--------|
| `network/AapApiService.kt` | Add status filter params to `getJobs()`, add `getSchedules()`, `toggleSchedule()` |
| `network/AapApiProvider.kt` | Add `getEdaApiService()` method |
| `network/NetworkModule.kt` | Register `EdaApiService` in Koin |
| `data/DataModule.kt` | Register `ScheduleRepository`, `EdaAuditRepository` |
| `data/JobRepository.kt` | Add status filter parameter to `getRecentJobs()` |
| `presentation/PresentationModule.kt` | Register new ViewModels |
| `presentation/jobs/RecentJobsViewModel.kt` | Add filter state and filter methods |
| `presentation/jobs/RecentJobsUiState.kt` | Add filter state fields |
| `ui/jobs/RecentJobsScreen.kt` | Add filter chips row above job list |
| `ui/main/TabDefinitions.kt` | Mark Schedules and EDA Audit as `isImplemented = true` |
| `navigation/MainNavigation.kt` | Route Schedules and EDA Audit segments to new screens |

## Build Sequence

1. **Models + API** — Schedule, EdaRuleAudit data classes; API service updates
2. **Repositories** — ScheduleRepository, EdaAuditRepository; JobRepository filter support
3. **ViewModels** — SchedulesViewModel, EdaAuditViewModel; RecentJobsViewModel filter enhancement
4. **UI Screens** — SchedulesScreen, EdaAuditScreen, filter chips; RecentJobsScreen enhancement
5. **Navigation** — Wire segments to screens, mark as implemented
6. **Polish** — Empty states, error handling, EDA unavailability

## Patterns to Follow

- **Pagination**: Manual infinite scroll (not Paging library) — see `RecentJobsViewModel`
- **Pull-to-refresh**: `PullToRefreshBox` — see `RecentJobsScreen`
- **UiState**: Sealed interface with `Loading`, `Success(data, hasMore, isLoadingMore)`, `Error(message)`
- **Repository**: Returns `Result<T>`, wraps API calls in try/catch
- **Koin DI**: `viewModelOf(::ClassName)` in presentationModule

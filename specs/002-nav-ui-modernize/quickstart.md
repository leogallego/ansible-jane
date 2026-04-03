# Quickstart: Navigation Foundation & UI Modernization

**Branch**: `002-nav-ui-modernize` | **Date**: 2026-04-02

## What This Feature Does

Replaces the current single-screen navigation (toolbar with icons) with a Material 3 bottom navigation bar containing three tabs (Templates, Infrastructure, Activity), each with segmented sub-sections. Also improves the visual quality of template cards, adds skeleton loading states, pull-to-refresh, animated transitions, a notification bell placeholder, and a Settings screen for logout.

## Key Architecture Decisions

1. **Multi-back-stack navigation** using `NavHost` with `saveState`/`restoreState` — each tab preserves its own navigation state.
2. **No new dependencies** — all components (NavigationBar, SegmentedButton, PullToRefreshBox, shimmer) are available in the existing Material 3 BOM or built custom.
3. **Single reusable PlaceholderScreen** for all 6 unimplemented segments.
4. **No new ViewModels** except consideration for the main navigation state — existing ViewModels are reused within their new tab locations.
5. **Settings screen** is minimal (server URL + logout) with no dedicated ViewModel.

## Files to Create

| File | Purpose |
|------|---------|
| `ui/main/MainScreen.kt` | Root scaffold with bottom nav bar + top app bar |
| `ui/main/TabDefinitions.kt` | Tab and segment enum/data definitions |
| `ui/components/PlaceholderScreen.kt` | Reusable "Coming Soon" screen |
| `ui/components/SkeletonCard.kt` | Shimmer loading card composable |
| `ui/components/ShimmerModifier.kt` | Reusable shimmer animation modifier |
| `ui/settings/SettingsScreen.kt` | Settings with server info + logout |
| `navigation/MainNavigation.kt` | Tab-level navigation with back stacks |

## Files to Modify

| File | Changes |
|------|---------|
| `navigation/AppNavigation.kt` | Add Settings route, restructure post-auth destination to MainScreen |
| `ui/templates/TemplateListScreen.kt` | Remove TopAppBar (moved to MainScreen), add pull-to-refresh, replace spinner with skeleton |
| `ui/templates/TemplateListItem.kt` | Enhanced card design with elevation, typography, spacing |
| `ui/jobs/RecentJobsScreen.kt` | Remove TopAppBar, add pull-to-refresh, replace spinner with skeleton |
| `ui/jobs/JobStatusScreen.kt` | Keep own TopAppBar (detail screen), add slide transition |
| `presentation/templates/TemplatesViewModel.kt` | Add refresh function (reset page + reload) |
| `presentation/jobs/RecentJobsViewModel.kt` | Add refresh function |

## Build & Test

```bash
# Build
./gradlew assembleDebug

# Run on connected device
./gradlew installDebug
```

Manual test checklist:
1. Login → see bottom nav with 3 tabs
2. Tap each tab → correct segments visible
3. Switch segments → placeholder screens for unimplemented
4. Templates tab → enhanced cards, skeleton loading, pull-to-refresh
5. Activity > Jobs → recent jobs with skeleton loading, pull-to-refresh
6. Navigate to job detail → slide animation, back preserves tab state
7. Gear icon → Settings screen with server URL + logout
8. Bell icon → "coming soon" snackbar
9. Rotate device → tab + segment preserved

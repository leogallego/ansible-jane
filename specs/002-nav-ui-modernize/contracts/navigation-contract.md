# Navigation Contract

**Branch**: `002-nav-ui-modernize` | **Date**: 2026-04-02

## Bottom Navigation Tabs

| Tab | Route | Icon | Segments |
|-----|-------|------|----------|
| Templates | `main/templates` | `Description` | Job Templates (default), Workflow Templates |
| Infrastructure | `main/infrastructure` | `Dns` | Inventories (default), Hosts, Projects |
| Activity | `main/activity` | `History` | Jobs (default), Schedules, EDA Audit |

## Top App Bar Actions

| Position | Icon | Action |
|----------|------|--------|
| Right | `Notifications` (bell) | Show "Notifications coming soon" snackbar |
| Right | `Settings` (gear) | Navigate to Settings screen |

## Screen Routes

| Route | Parent | Back Stack |
|-------|--------|------------|
| `auth` | None | Start destination |
| `main` | None | Post-auth destination, contains bottom nav |
| `main/templates` | Bottom nav | Independent back stack |
| `main/infrastructure` | Bottom nav | Independent back stack |
| `main/activity` | Bottom nav | Independent back stack |
| `settings` | Top app bar | Returns to previous tab |
| `job_status/{jobId}` | Detail overlay | Returns to originating tab |

## Transition Animations

| Navigation Type | Enter | Exit | Pop Enter | Pop Exit |
|-----------------|-------|------|-----------|----------|
| Tab switch | fadeIn | fadeOut | fadeIn | fadeOut |
| Detail push | slideInFromRight | slideOutToLeft | slideInFromLeft | slideOutToRight |
| Settings push | slideInFromRight | slideOutToLeft | slideInFromLeft | slideOutToRight |

## Composable Contracts

### MainScreen
```
Inputs: navController (for detail/settings navigation), onLogout callback
Contains: Scaffold with TopAppBar + NavigationBar + content area
```

### PlaceholderScreen
```
Inputs: title (String), description (String, optional)
Outputs: Centered icon + text display
```

### SkeletonCard
```
Inputs: None (self-contained)
Outputs: Animated shimmer card matching TemplateListItem dimensions
```

### SettingsScreen
```
Inputs: serverUrl (String), onLogout callback, onNavigateBack callback
Outputs: Server info display + logout button
```

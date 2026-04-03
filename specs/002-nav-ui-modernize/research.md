# Research: Navigation Foundation & UI Modernization

**Branch**: `002-nav-ui-modernize` | **Date**: 2026-04-02

## R-001: Multi-back-stack Navigation in Jetpack Compose

**Decision**: Use `NavHost` with `saveState` and `restoreState` parameters on `NavigationBarItem` clicks, combined with `popUpTo(startDestination) { saveState = true }`.

**Rationale**: This is the officially recommended approach for bottom navigation with independent back stacks per tab. Each tab's navigation state (including nested detail screens and scroll position) is saved when switching away and restored when returning. This matches the clarified requirement (FR-019).

**Alternatives considered**:
- Multiple `NavHost` instances per tab: Overly complex, not officially supported, causes lifecycle issues.
- Manual back stack management with custom `SavedStateHandle`: Reinvents the wheel; the built-in `saveState`/`restoreState` handles this.
- Accompanist Navigation Animation: Deprecated — animation is now built into `androidx.navigation.compose` directly.

## R-002: Material 3 SegmentedButton in Compose

**Decision**: Use `SingleChoiceSegmentedButtonRow` with `SegmentedButton` from Material 3 (`androidx.compose.material3`).

**Rationale**: This component is part of the Material 3 Compose library already included via the BOM. It provides native single-choice segmented selection with proper Material 3 theming. No additional dependency needed.

**Alternatives considered**:
- Custom `TabRow`: Tabs have a different UX semantic (horizontal scrolling, full-width); segmented buttons are the correct M3 component for switching between related content views within a section.
- Custom composable with `Row` + `OutlinedButton`: Would require manual styling to match M3 spec; unnecessary when the official component exists.

## R-003: Skeleton Loading (Shimmer Effect)

**Decision**: Implement a custom shimmer modifier using `Brush.linearGradient` with `InfiniteTransition`. Create reusable `SkeletonCard` composable matching the template card layout.

**Rationale**: No additional library needed. A shimmer effect with `Modifier.background(shimmerBrush)` on placeholder shapes is lightweight and matches the M3 surface colors. The skeleton cards should mirror the real card dimensions (name placeholder, description placeholder, chip placeholders) for a smooth transition to real content.

**Alternatives considered**:
- Accompanist Placeholder: Deprecated and archived.
- Third-party shimmer libraries (e.g., `com.valentinilk.shimmer`): Adds an external dependency for something achievable in ~30 lines of custom code. Violates constitution principle V (Lean Dependencies).

## R-004: Pull-to-Refresh in Compose

**Decision**: Use `pullToRefresh` modifier with `PullToRefreshBox` from Material 3 Compose (`androidx.compose.material3`).

**Rationale**: The `PullToRefreshBox` component is available in the Material 3 Compose library (added in recent versions included in the BOM). It provides the standard Material 3 pull-to-refresh indicator. This replaces the deprecated Accompanist swipe-refresh.

**Alternatives considered**:
- Accompanist SwipeRefresh: Deprecated and archived.
- Custom pull-to-refresh: Unnecessary when M3 provides a built-in component.

## R-005: Navigation Animation Transitions

**Decision**: Use `AnimatedNavHost` (or `NavHost` with `enterTransition`/`exitTransition` parameters) from `androidx.navigation.compose` for slide and crossfade animations.

**Rationale**: Navigation Compose supports `enterTransition`, `exitTransition`, `popEnterTransition`, and `popExitTransition` natively on `composable()` destinations. Crossfade for tab switches can be achieved with `fadeIn`/`fadeOut`, and slide for detail screens with `slideInHorizontally`/`slideOutHorizontally`.

**Alternatives considered**:
- Accompanist Navigation Animation: Deprecated; functionality merged into core navigation-compose.
- Manual `AnimatedContent`: Would require managing navigation state ourselves; the built-in transitions handle back stack integration.

## R-006: Settings Screen Architecture

**Decision**: Create a minimal `SettingsScreen` composable with its own route, displaying the connected server URL (from `TokenManager`) and a logout button. No ViewModel needed initially — it can read server URL directly and delegate logout to `AuthViewModel`.

**Rationale**: The Settings screen is minimal in this phase (just server info + logout). A dedicated ViewModel would be overengineering. The screen can accept callback lambdas for logout and back navigation, consistent with the existing pattern.

**Alternatives considered**:
- Dropdown menu from a profile icon: User chose Settings screen (clarification Q2).
- Full Settings ViewModel with DataStore preferences: Premature; can be added when Settings grows in scope.

## R-007: Placeholder Screens for Unimplemented Segments

**Decision**: Create a single reusable `PlaceholderScreen` composable that accepts a title and optional description. Use it for all 6 unimplemented segments (Workflow Templates, Inventories, Hosts, Projects, Schedules, EDA Audit).

**Rationale**: DRY approach — one composable parameterized by segment name, showing a centered icon + "Coming Soon" message. When future issues (#4, #5, #6) are implemented, each placeholder gets replaced with the real screen.

**Alternatives considered**:
- Individual placeholder composables per segment: Duplication with no benefit.
- Empty screen with just text: Too minimal; a simple icon + message provides better UX.

# Quickstart: UI Polish — Animations and Micro-Interactions

**Branch**: `007-ui-polish-animations`

## Build Sequence

This feature has no new dependencies. Build in this order to minimize broken intermediate states:

### Step 1: Foundation (no UI changes yet)
1. Create `AppError` sealed class with `from()` factory in `model/AppError.kt`
2. Create `Modifier.pressScale()` extension in `ui/components/PressScaleModifier.kt`
3. Create `AapIcons` centralized icon registry in `ui/icons/AapIcons.kt`
4. Create `StatusColors` data class + `LocalStatusColors` in `ui/theme/StatusColors.kt`
5. Create `Flow.asResult()` extension in `data/ResultExtensions.kt`
6. Update `AapRemoteTheme` to provide `StatusColors` via `CompositionLocalProvider`
7. Remove top-level `Status*` constants from `Color.kt`

### Step 2: Error pipeline (data → presentation)
8. Update repositories to stop wrapping exceptions — let raw exceptions propagate
9. Update all UiState `Error` variants from `String` to `AppError`
10. Update all ViewModels to use `asResult()` + `AppError.from(throwable)` when mapping errors
11. Update `JobStatusBadge` to use `AapIcons.Status.*` and `AapRemoteTheme.statusColors.*`

### Step 3: UI changes
12. Refactor `ErrorMessage` composable to accept `AppError` with typed display, expandable details, and slide-in entrance animation
13. Update all screens to pass `AppError` to `ErrorMessage`
14. Apply `Modifier.pressScale()` to all clickable card composables
15. Add spring entrance to dialogs (LaunchConfirmDialog, ExtraVarsInput, EdaAuditDetailSheet)
16. Add breathing pulse to `JobStatusBadge` for Running status
17. Add `AnimatedContent` to app bar title in MainScreen

### Step 4: Accessibility
18. Add reduce-motion checks to all animation entry points

## Verification

### Animations
- Press any card → see scale-down animation
- Launch a template → see spring-animated confirmation dialog
- Switch tabs → see title crossfade
- View a running job → see pulsing status badge
- Error appears → slides in with fade (not instant)

### Typed Errors
- Toggle airplane mode → see Network Error with wifi-off icon
- Use expired token → see Auth Error with lock icon
- Point to invalid URL → see SSL or Network Error
- Tap "Show details" on any error → see status code and URL

### Design System
- Verify `JobStatusBadge` uses `AapRemoteTheme.statusColors` (not direct Color imports)
- Verify all status/error icons come from `AapIcons` (not inline `Icons.Default.*`)
- Verify ViewModels use `asResult()` (no manual try/catch/fold boilerplate)

### Accessibility
- Enable "Remove animations" in Android accessibility → all animations disabled, app still functional

# Research: UI Polish — Animations and Micro-Interactions

**Date**: 2026-04-03
**Branch**: `007-ui-polish-animations`

## R1: Press-Scale Animation Pattern in Compose

**Decision**: Use `Modifier.pointerInput` with `detectTapGestures` (for press/release detection) combined with `animateFloatAsState` for smooth scale transitions.

**Rationale**: This is the standard Compose approach for press feedback. Using `MutableInteractionSource.collectIsPressedAsState()` is simpler but only works with components that accept an `interactionSource` parameter. Since `ElevatedCard` and `Card` both support `interactionSource`, we can use the simpler approach.

**Alternatives considered**:
- `Modifier.pointerInput` + `detectTapGestures`: More control but more boilerplate. Better for custom touch handling.
- `InteractionSource.collectIsPressedAsState()`: Simpler, works with Material components that accept `interactionSource`. Preferred.
- `Modifier.indication()`: For visual ripple only, doesn't provide scale.

**Final approach**: Create `Modifier.pressScale()` that internally uses `MutableInteractionSource` + `collectIsPressedAsState()` + `graphicsLayer { scaleX; scaleY }` with `animateFloatAsState(spring())`.

## R2: Spring Animation Specifications

**Decision**: Use `spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)` for dialog entrances.

**Rationale**: Damping ratio of 0.7 provides a subtle bounce without feeling excessive. Medium stiffness ensures the animation completes quickly (~300ms). These values match the reference app (AI Hub) and are within Material Design motion guidelines.

**Alternatives considered**:
- `tween(300ms)`: Linear, no spring feel. Too mechanical.
- `spring(dampingRatio = 0.5f)`: Too bouncy for dialogs.
- `spring(dampingRatio = 1.0f)`: Critically damped, no bounce. Too plain.

## R3: Typed Error Classification from Retrofit/OkHttp Exceptions

**Decision**: Map exception types to AppError variants using a factory function.

**Mapping rules**:
| Exception Type | AppError Variant |
|---------------|-----------------|
| `java.net.UnknownHostException` | Network |
| `java.net.ConnectException` | Network |
| `java.net.SocketTimeoutException` | Network |
| `java.io.IOException` (generic) | Network |
| `javax.net.ssl.SSLException` | SSL |
| `javax.net.ssl.SSLHandshakeException` | SSL |
| `retrofit2.HttpException` with 401/403 | Auth |
| `retrofit2.HttpException` with 5xx | Server |
| `retrofit2.HttpException` with 4xx (other) | Server |
| Everything else | Unknown |

**Rationale**: These exception types are what Retrofit/OkHttp throw. The mapping covers all common failure modes an AAP user would encounter. SSL errors are checked before generic IOException since `SSLException` extends `IOException`.

**Detail extraction**:
- `HttpException`: status code via `e.code()`, URL via `e.response()?.raw()?.request?.url?.toString()`
- `IOException`: no HTTP details, but include exception message
- All types: include the original exception message as detail text

## R4: Reduce Motion Accessibility

**Decision**: Check `LocalReduceMotion` (available in Compose since 1.4) for spring/crossfade animations. For infinite animations (pulse), use `isSystemAnimationsEnabled()` check.

**Rationale**: `LocalReduceMotion` is the Compose-native way to respect the system's "Remove animations" accessibility setting. It's automatically provided by `MaterialTheme`. For `rememberInfiniteTransition`, the animation doesn't auto-disable, so we need to conditionally skip it.

**Implementation**:
- Press-scale: When reduce motion is on, set target scale to 1f (no animation). The card still responds to taps.
- Dialog spring: Use `snap()` instead of `spring()` spec when reduce motion is on.
- Title crossfade: Compose `AnimatedContent` respects system animation scale by default.
- Pulse: Skip `rememberInfiniteTransition` entirely; render static badge.

## R5: Repository Exception Preservation

**Decision**: Change repositories to stop wrapping exceptions in generic `Exception(message)`. Instead, let the original exception propagate via `Result.failure(e)`.

**Rationale**: Currently, repositories do `Result.failure(Exception("Launch failed: ${e.message}"))`, which loses the exception type needed for classification. The ViewModel layer will call `AppError.from(throwable)` to classify.

**Exception**: The `launchJob` function in `TemplateRepository` currently maps HTTP status codes to user-friendly messages. This mapping will move to `AppError.from()` — the factory will handle both classification and message generation.

**Alternatives considered**:
- Map at repository layer: Would require repositories to know about AppError, coupling data and model layers. Rejected.
- Create middleware/interceptor: Overengineered for this use case. Rejected.
- Map at ViewModel layer from raw exceptions: Chosen — keeps repositories simple, centralizes error mapping.

## R6: AnimatedContent vs Crossfade for App Bar Title

**Decision**: Use `AnimatedContent` with `fadeIn + fadeOut` transition, keyed on `selectedTab`.

**Rationale**: The MainScreen already uses `Crossfade` for content. `AnimatedContent` is more flexible — it supports size change animation and provides `targetState` in the content lambda. Since the title text changes width (e.g., "Templates" vs "Activity"), `AnimatedContent` handles this more gracefully.

**Implementation**: Replace `Text("AAPdroid")` with `AnimatedContent(targetState = selectedTab.label)` wrapping `Text(targetState)`.

## R7: Flow.asResult() Extension (from Now in Android)

**Decision**: Create a `Flow<Result<T>>.asResult()` extension that wraps repository Result flows into a Loading → Success → Error state flow, combined with `AppError.from()` for error classification.

**Rationale**: Every ViewModel currently repeats the same pattern: call repository suspend function, fold Result, emit UiState. NiA's approach wraps this in a reusable extension, reducing boilerplate. Our variant combines this with `AppError.from()` so error classification is automatic.

**NiA implementation**:
```kotlin
fun <T> Flow<T>.asResult(): Flow<Result<T>> = map { Result.Success(it) }
    .onStart { emit(Result.Loading) }
    .catch { emit(Result.Error(it)) }
```

**Our adaptation**: Since our repositories return `Result<T>` from suspend functions (not Flows), the extension operates at the ViewModel level to convert suspend call results into state flows. The key benefit is centralizing the `AppError.from()` call alongside the Result → UiState mapping.

**Alternatives considered**:
- Keep manual fold in each ViewModel: Works but duplicates error mapping logic everywhere. Rejected.
- OkHttp interceptor for error classification: Would lose the ability to add context-specific messages. Rejected.

## R8: Centralized Icon Registry (from Now in Android)

**Decision**: Create an `AapIcons` object mapping semantic names to Material icon references.

**Rationale**: NiA's `NiaIcons` centralizes all icons and enforces usage via custom lint rules. We won't add lint rules (overkill for our project size), but the centralized object provides discoverability and consistency. Currently, icons are referenced inline across ~10 files with no single source of truth.

**Scope**: Status icons (from JobStatusBadge), error type icons (from ErrorMessage), navigation icons (from MainScreen, SettingsScreen).

## R9: StatusColors CompositionLocal (from Now in Android)

**Decision**: Create a `StatusColors` data class and provide it via `LocalStatusColors` CompositionLocal in `AapRemoteTheme`.

**Rationale**: NiA extends MaterialTheme with custom CompositionLocals (`LocalGradientColors`, `LocalTintTheme`). Our status colors are currently top-level constants — same values for light and dark mode. Wrapping them in a CompositionLocal makes them theme-aware and accessible via `AapRemoteTheme.statusColors`.

**Implementation**:
- `StatusColors.kt`: Data class with color properties + `LocalStatusColors` CompositionLocal with default values
- `Theme.kt`: Provide `StatusColors()` via `CompositionLocalProvider` inside `AapRemoteTheme`
- `Color.kt`: Remove top-level `StatusSuccessful`, `StatusFailed`, etc. constants
- `JobStatusBadge.kt`: Replace direct color references with `AapRemoteTheme.statusColors.running`, etc.

**Alternatives considered**:
- Keep top-level constants: Simple but not theme-aware. Fine for now but limits future dark-mode customization.
- Add to MaterialTheme's ExtendedColors: No standard M3 API for this. CompositionLocal is the recommended approach.

## R10: Error State Entrance Animation (from Now in Android)

**Decision**: Wrap ErrorMessage in `AnimatedVisibility(fadeIn + slideInVertically)` for smooth entrance.

**Rationale**: NiA uses this pattern for loading overlays on ForYouScreen. Errors currently appear instantly, which can be jarring — especially during polling when content is already visible. A subtle slide-in provides visual continuity.

**Implementation**: Add `AnimatedVisibility` with `fadeIn() + slideInVertically()` for enter, `fadeOut() + slideOutVertically()` for exit, directly in the ErrorMessage composable. The animation is self-contained — no changes needed in calling screens.

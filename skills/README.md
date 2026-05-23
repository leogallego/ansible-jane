# AI Agent Skills

SKILL.md files that ground AI assistants with Android/Kotlin best practices.
These follow the [agentskills.io](https://agentskills.io) open standard.

## Sources

### `android-official/` - Google Android Skills
From [android/skills](https://github.com/android/skills) (Apache 2.0)
- **edge-to-edge** - Edge-to-edge display, insets, IME handling for Compose
- **navigation-3** - Jetpack Navigation 3 migration and patterns (incl. Koin recipes)

### `android-community/` - Community Android Skills
From [javiercamarenatriguero/android-skills](https://github.com/javiercamarenatriguero/android-skills) (Apache 2.0)
- **compose-editor** - Jetpack Compose patterns and conventions
- **compose-performance-auditor** - Compose recomposition and performance
- **kotlin-coroutines** - Coroutines, Flow, structured concurrency
- **koin-editor** - Koin DI patterns and best practices
- **kotlin-convention** - Kotlin coding conventions
- **android-unit-test-editor** - Unit testing patterns
- **gradle-configuration** - Gradle build configuration

### `compose-skill/` - Comprehensive Compose Reference
From [Meet-Miyani/compose-skill](https://github.com/Meet-Miyani/compose-skill) (MIT)
- **compose-skill** - Full Compose lifecycle: architecture, state, UI, networking, persistence, performance, accessibility
- **references/** - Detailed guides for architecture, MVI, Koin, coroutines, testing, anti-patterns, etc.

### chrisbanes skills - Focused Compose & Kotlin Skills
From [chrisbanes/skills](https://github.com/chrisbanes/skills) (Apache 2.0)

**Compose:**
- **compose-animations** - AnimatedVisibility, animate*AsState, rememberTransition, AnimatedContent, Crossfade
- **compose-focus-navigation** - TV/keyboard/desktop focus, D-pad, FocusRequester, key events
- **compose-modifier-and-layout-style** - Modifier chains, layout APIs, modifier parameters
- **compose-recomposition-performance** - Recomposition counts, skippable/restartable, compiler reports
- **compose-side-effects** - LaunchedEffect, DisposableEffect, SideEffect, rememberCoroutineScope
- **compose-slot-api-pattern** - Slot API design for reusable components
- **compose-stability-diagnostics** - Parameter stability, compiler reports, strong skipping (Kotlin 2.0+)
- **compose-state-authoring** - mutableStateOf, remember, mutableStateListOf patterns
- **compose-state-deferred-reads** - Deferring state reads to layout/draw phase
- **compose-state-hoisting** - Where to put state: local, hoisted, state holder, or ViewModel
- **compose-state-holder-ui-split** - Splitting screen composables into state wrapper + pure UI
- **compose-ui-testing-patterns** - UI tests, screenshot tests, semantics assertions

**Kotlin:**
- **kotlin-coroutines-structured-concurrency** - CoroutineScope, launching patterns, exception handling
- **kotlin-flow-state-event-modeling** - StateFlow vs SharedFlow vs Channel, one-shot events
- **kotlin-multiplatform-expect-actual** - KMP expect/actual, interface boundaries, source sets
- **kotlin-types-value-class** - @JvmInline value class vs data class, Compose stability

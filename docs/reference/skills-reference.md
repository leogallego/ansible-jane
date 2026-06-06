# Skills Reference

Skills that must be loaded for Android/Kotlin development sessions. Tell Claude to load the relevant ones before starting work.

Note: Project skills (in `skills/`) are reference files read directly -- they are NOT invocable via the Skill tool. Only global skills (`~/.claude/skills/`) can be invoked with the Skill tool.

## Mandatory (every session)

| Skill | Invoke with | Purpose |
|-------|-------------|---------|
| `android-cli` | `Skill: android-cli` | Android CLI tool for deploy, emulator, screenshots, layout inspection, SDK management. **Always load at session start.** |

## Global skills (~/.claude/skills/)

Installed globally, available in all projects. Invoke via the Skill tool.

| Skill | Invoke with | Purpose |
|-------|-------------|---------|
| `android-cli` | `Skill: android-cli` | Deploy apps, manage emulators, capture screenshots, inspect layouts, manage SDKs |
| `ai-gitignore` | `Skill: ai-gitignore` | Create/maintain .gitignore for AI-generated content |
| `appfunctions` | `Skill: appfunctions` | Analyze apps to identify key user workflows for AppFunctions |
| `r8-analyzer` | `Skill: r8-analyzer` | Analyze build files and R8 keep rules for redundancies |
| `styles` | `Skill: styles` | Integrate Jetpack Compose Styles API |
| `testing-setup` | `Skill: testing-setup` | Analyze and create testing strategy for native Android apps |
| `verified-email` | `Skill: verified-email` | Implement verified email retrieval workflow |

## Project skills (skills/)

Bundled with this repo. Read directly as reference files, not invocable via Skill tool.

### Compose & UI (android-community)

| Skill | File | When to use |
|-------|------|-------------|
| `compose-skill` | `compose-skill/compose-skill.md` | Comprehensive Compose guide (has sub-references for performance, coroutines, Koin, testing, anti-patterns, DataStore, navigation, Material Design) |
| `compose-editor` | `android-community/compose-editor.md` | Creating new screens/components, reviewing or refactoring Compose code |
| `compose-performance-auditor` | `android-community/compose-performance-auditor.md` | Diagnosing slow rendering, janky scrolling, excessive recompositions |

### Compose (chrisbanes)

Source: [chrisbanes/skills](https://github.com/chrisbanes/skills) (Apache-2.0)

| Skill | Directory | When to use |
|-------|-----------|-------------|
| `compose-animations` | `compose-animations/` | AnimatedVisibility, animate*AsState, rememberTransition, AnimatedContent, Crossfade, enter/exit transitions |
| `compose-focus-navigation` | `compose-focus-navigation/` | TV/keyboard/desktop focus, D-pad navigation, FocusRequester, focusProperties, key events |
| `compose-modifier-and-layout-style` | `compose-modifier-and-layout-style/` | Modifier chains, layout APIs, modifier parameters, root layout decisions |
| `compose-recomposition-performance` | `compose-recomposition-performance/` | Recomposition counts, skippable/restartable composables, compiler reports, Layout Inspector |
| `compose-side-effects` | `compose-side-effects/` | LaunchedEffect, DisposableEffect, SideEffect, rememberCoroutineScope, rememberUpdatedState, snapshotFlow |
| `compose-slot-api-pattern` | `compose-slot-api-pattern/` | Designing reusable components with slot APIs instead of boolean flags |
| `compose-stability-diagnostics` | `compose-stability-diagnostics/` | Parameter stability, compiler reports, skippability, unstable classes, strong skipping (Kotlin 2.0+) |
| `compose-state-authoring` | `compose-state-authoring/` | mutableStateOf, remember, mutableStateListOf/mutableStateMapOf, local state patterns |
| `compose-state-deferred-reads` | `compose-state-deferred-reads/` | Deferring scroll/animation/gesture state reads to layout/draw phase, avoiding composition reads |
| `compose-state-hoisting` | `compose-state-hoisting/` | Where to put state: local remember, hoisted params, state holder class, or ViewModel |
| `compose-state-holder-ui-split` | `compose-state-holder-ui-split/` | Splitting screen composables into state-collecting wrapper + pure UI composable |
| `compose-ui-testing-patterns` | `compose-ui-testing-patterns/` | UI tests, screenshot tests, semantics assertions, keyboard input, focus assertions |

### Kotlin (android-community)

| Skill | File | When to use |
|-------|------|-------------|
| `kotlin-convention` | `android-community/kotlin-convention.md` | Reviewing Kotlin code style, applying language best practices |
| `kotlin-coroutines` | `android-community/kotlin-coroutines.md` | Async operations, Flow/StateFlow/SharedFlow, coroutine correctness |

### Kotlin (chrisbanes)

| Skill | Directory | When to use |
|-------|-----------|-------------|
| `kotlin-coroutines-structured-concurrency` | `kotlin-coroutines-structured-concurrency/` | CoroutineScope storage, launching from init/non-suspending APIs, runBlocking, exception handling |
| `kotlin-flow-state-event-modeling` | `kotlin-flow-state-event-modeling/` | StateFlow vs SharedFlow vs Channel, MutableStateFlow.update, stateIn, SharingStarted, one-shot events |
| `kotlin-multiplatform-expect-actual` | `kotlin-multiplatform-expect-actual/` | KMP expect/actual, interface boundaries, source sets, platform interop |
| `kotlin-types-value-class` | `kotlin-types-value-class/` | @JvmInline value class vs data class, Compose stability implications |

### Architecture & DI

| Skill | File | When to use |
|-------|------|-------------|
| `koin-editor` | `android-community/koin-editor.md` | Koin DI setup, modules, scopes, testing, troubleshooting |

### Build

| Skill | File | When to use |
|-------|------|-------------|
| `gradle-configuration` | `android-community/gradle-configuration.md` | Adding dependencies, version catalogs, build optimization |

### Testing

| Skill | File | When to use |
|-------|------|-------------|
| `android-unit-test-editor` | `android-community/android-unit-test-editor.md` | Unit tests for ViewModels, repositories, utilities (MockK, Coroutines Test) |

### Platform (android-official)

| Skill | File | When to use |
|-------|------|-------------|
| `edge-to-edge` | `android-official/edge-to-edge.md` | Migrate to adaptive edge-to-edge display |
| `navigation-3` | `android-official/navigation-3.md` | Install and migrate to Jetpack Navigation 3 |

## Compose-skill sub-references

The `compose-skill` has detailed reference docs at `skills/compose-skill/references/`:

- `performance.md` - Compose performance optimization
- `coroutines-flow.md` - Coroutines and Flow patterns
- `koin.md` - Koin DI with Compose
- `testing.md` - Compose testing
- `anti-patterns.md` - Common Compose mistakes to avoid
- `architecture.md` - App architecture patterns
- `compose-essentials.md` - Core Compose concepts
- `datastore.md` - DataStore usage
- `lists-grids.md` - LazyColumn/LazyGrid patterns
- `material-design.md` - Material 3 components
- `mvi.md` - MVI pattern
- `navigation-2.md` - Navigation Compose (v2)

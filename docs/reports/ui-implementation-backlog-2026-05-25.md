# UI Implementation Backlog - 2026-05-25

Planning-only backlog for UI/UX optimization, standardization, and modernization in the Jetpack Compose layer.

No code changes are included in this document.

## Scope

- `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/**`
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/assistant/ui/**`
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/ui/theme/**`
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/navigation/**`
- `app/src/main/kotlin/io/github/leogallego/ansiblejane/MainActivity.kt`

## Priority Model

- `P0` - immediate reliability/accessibility/performance risk.
- `P1` - high-value standardization and architecture consolidation.
- `P2` - polish and long-term maintainability.

## Sequencing Rules

- Complete all `P0` items first.
- Start component extraction only after core behavior fixes are stable.
- Migrate screens incrementally (1-2 screens per PR) to avoid broad regressions.

## Backlog

### P0 - Immediate

#### UIB-001 - Stabilize pagination trigger behavior
- **Priority:** `P0`
- **Area:** list loading correctness
- **Primary targets:** `ui/components/PaginationEffect.kt`
- **Problem:** load-more can stall if bottom proximity remains true.
- **Deliverables:**
  - robust trigger keyed to both visibility and item growth,
  - protection against duplicate in-flight triggers.
- **Done when:**
  - load-more continues across multiple pages without scroll bounce hacks,
  - no repeated redundant calls while already loading.
- **Dependencies:** none.

#### UIB-002 - Reduce assistant message recomposition overhead
- **Priority:** `P0`
- **Area:** chat performance
- **Primary targets:** `assistant/ui/ChatBubble.kt`, `assistant/ui/AssistantScreen.kt`
- **Problem:** markdown config and callbacks are rebuilt too often in hot composition paths.
- **Deliverables:**
  - memoized markdown configuration objects,
  - stable callback/lambda wiring for message rows.
- **Done when:**
  - chat typing no longer causes heavy markdown work in unchanged bubbles,
  - recomposition scope is reduced to necessary nodes.
- **Dependencies:** none.

#### UIB-003 - Make chat actions visible and discoverable
- **Priority:** `P0`
- **Area:** assistant UX/accessibility
- **Primary targets:** `assistant/ui/ChatBubble.kt`
- **Problem:** copy/regenerate are long-press only.
- **Deliverables:**
  - visible affordance for copy/regenerate (inline or overflow),
  - long-press retained as optional shortcut.
- **Done when:**
  - user can discover actions without gesture knowledge,
  - actions are reachable with accessibility services.
- **Dependencies:** none.

#### UIB-004 - Align edge-to-edge with safe insets
- **Priority:** `P0`
- **Area:** layout safety
- **Primary targets:** `ui/main/MainScreen.kt`, `MainActivity.kt`
- **Problem:** edge-to-edge is enabled while scaffold content insets are zeroed.
- **Deliverables:**
  - consistent safe-area behavior for top/bottom bars and content.
- **Done when:**
  - content no longer risks clipping under system bars,
  - behavior is consistent across gesture and 3-button nav.
- **Dependencies:** none.

#### UIB-005 - Add semantic alternatives for dashboard chart data
- **Priority:** `P0`
- **Area:** accessibility
- **Primary targets:** `ui/dashboard/DashboardScreen.kt`
- **Problem:** canvas chart lacks semantic representation.
- **Deliverables:**
  - semantic summary for chart values,
  - heading semantics for dashboard section headers.
- **Done when:**
  - screen readers can access trend information and section structure.
- **Dependencies:** none.

### P1 - Standardization and Reuse

#### UIB-006 - Introduce spacing and shape design tokens
- **Priority:** `P1`
- **Area:** design system
- **Primary targets:** `ui/theme/**`, high-traffic screens/components
- **Problem:** repeated hardcoded spacing/corners cause drift.
- **Deliverables:**
  - shared spacing token set,
  - normalized shape tokens wired through theme.
- **Done when:**
  - most repeated raw spacing/radius literals are replaced with tokens.
- **Dependencies:** none.

#### UIB-007 - Complete Material 3 typography mapping
- **Priority:** `P1`
- **Area:** typography consistency
- **Primary targets:** `ui/theme/Type.kt` and major text-heavy screens
- **Problem:** only partial typography scale is defined.
- **Deliverables:**
  - full M3 typography definition or explicitly scoped override strategy,
  - consistent usage patterns by text role.
- **Done when:**
  - app headings/body/labels follow one coherent type hierarchy.
- **Dependencies:** UIB-006 recommended.

#### UIB-008 - Centralize status color semantics
- **Priority:** `P1`
- **Area:** theme consistency
- **Primary targets:** `ui/theme/StatusColors.kt`, dashboard/workflow/agent components
- **Problem:** status colors are duplicated and partially hardcoded.
- **Deliverables:**
  - single source of truth for status colors,
  - replacement of duplicated local hex usage.
- **Done when:**
  - status visuals are uniform across all status-bearing components.
- **Dependencies:** UIB-006 recommended.

#### UIB-009 - Build shared paginated screen scaffold
- **Priority:** `P1`
- **Area:** architecture/maintainability
- **Primary targets:** templates, workflows, jobs, hosts, inventories, schedules, EDA screens
- **Problem:** repeated search + refresh + pagination + list shell logic.
- **Deliverables:**
  - reusable paginated screen shell with slots,
  - migration plan for existing list screens.
- **Done when:**
  - duplicated list shell code is materially reduced,
  - migrated screens preserve current behavior.
- **Dependencies:** UIB-001.

#### UIB-010 - Build shared detail scaffold
- **Priority:** `P1`
- **Area:** architecture/consistency
- **Primary targets:** job status, workflow status, workflow template detail, approval detail, settings
- **Problem:** repeated detail screen scaffolds and back-navigation setup.
- **Deliverables:**
  - reusable detail scaffold with common top bar/back handling/insets.
- **Done when:**
  - duplicated scaffold boilerplate is reduced,
  - navigation semantics and layout are consistent.
- **Dependencies:** UIB-004.

#### UIB-011 - Normalize tab selector semantics in settings
- **Priority:** `P1`
- **Area:** accessibility/navigation
- **Primary targets:** `ui/settings/SettingsScreen.kt`
- **Problem:** custom tab chips have weaker native semantics than standard tab components.
- **Deliverables:**
  - semantic parity with tab behavior (role, selected state) or migration to native tab row.
- **Done when:**
  - selected tab state is properly announced and structured.
- **Dependencies:** none.

#### UIB-012 - Improve provider switch touch target and semantics
- **Priority:** `P1`
- **Area:** top-bar usability/accessibility
- **Primary targets:** `ui/components/ProviderSwitchChip.kt`
- **Problem:** compact custom control risks undersized interaction area and ambiguous semantics.
- **Deliverables:**
  - minimum touch target compliance,
  - explicit semantic labels and role cues.
- **Done when:**
  - interaction target meets minimum guidelines,
  - accessibility services announce intent clearly.
- **Dependencies:** none.

#### UIB-013 - Standardize empty/error/disconnected recovery surfaces
- **Priority:** `P1`
- **Area:** UX consistency
- **Primary targets:** `ui/components/EmptyState.kt`, `ui/components/ErrorMessage.kt`, `assistant/ui/AssistantScreen.kt`, `ui/notifications/NotificationsSheet.kt`
- **Problem:** recovery affordances vary by screen; some dead-end states have no action.
- **Deliverables:**
  - shared status panel model (title/body/actions),
  - consistent recovery actions across equivalent states.
- **Done when:**
  - each empty/error/disconnected state has a predictable interaction pattern.
- **Dependencies:** UIB-010 recommended.

### P2 - Polish and Long-Term Maintainability

#### UIB-014 - Add Compose preview coverage
- **Priority:** `P2`
- **Area:** developer velocity/quality
- **Primary targets:** shared components + core screen states
- **Problem:** no preview catalog for key UI states.
- **Deliverables:**
  - previews for loading/empty/error/content variants in critical components.
- **Done when:**
  - major reusable components and top-level screens have practical preview coverage.
- **Dependencies:** UIB-006 and UIB-007 recommended.

#### UIB-015 - Migrate inline UI strings to resources
- **Priority:** `P2`
- **Area:** maintainability/localization readiness
- **Primary targets:** all user-facing text in `ui/**` and `assistant/ui/**`
- **Problem:** user strings are hardcoded in composables.
- **Deliverables:**
  - string resources for shared/common UI copy first,
  - migration checklist for remaining screens.
- **Done when:**
  - high-frequency strings are resource-backed,
  - direct inline string usage is significantly reduced.
- **Dependencies:** none.

## Suggested Delivery Waves

### Wave A (P0, immediate)
- UIB-001, UIB-002, UIB-003, UIB-004, UIB-005

### Wave B (foundation)
- UIB-006, UIB-007, UIB-008

### Wave C (shared architecture)
- UIB-009, UIB-010, UIB-013

### Wave D (polish)
- UIB-011, UIB-012, UIB-014, UIB-015

## Definition of Backlog Health

- Every item has: clear scope, acceptance criteria, and dependency notes.
- Work is split into reviewable units (prefer small PRs).
- Shared primitives are built before broad migrations.
- Accessibility and performance checks are included in each relevant item's done criteria.

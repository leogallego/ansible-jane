# Tools Settings UI Redesign — Design Spec

**Issue:** #196
**Date:** 2026-05-26
**Status:** Draft

## Problem

The current Tools tab in Settings is a flat list of MCP servers with no per-tool visibility, no expandable sections, limited status feedback, and a stubbed "coming soon" local tools placeholder. Users cannot see or control which individual tools (local or MCP) are available to the AI assistant.

## Solution

Redesign the Tools tab following the Kai 9000 pattern: expandable MCP server cards with per-tool toggles, a category-grouped local tools section with per-tool toggles, and a modal bottom sheet for adding new MCP servers. Persist per-tool enable/disable state in DataStore so it survives app restarts.

## Design Decisions

### ToolRouter lifecycle: unchanged
ToolRouter stays per-message in `AssistantViewModel.sendMessage()`. The singleton refactor is tracked in #120 and has its own scope (META category, middleware pipeline, KMP extraction). This PR adds DataStore persistence for disabled tools; `AssistantViewModel` reads the persisted set and applies it to each per-message ToolRouter instance. When #120 lands, the persistence layer migrates cleanly into the singleton. See #120 comment for details.

### Disabled tools persistence key format
Namespaced as `"SOURCE:tool_name"` to avoid collisions between local and MCP tools:
- `"LOCAL:list_hosts"`
- `"MCP:controller.hosts_list"`

Maps directly to `toolRouter.setToolEnabled(name, ToolSource.LOCAL, false)`. The actual `ToolSpec.name` seen by the LLM is unchanged.

### Category mapping: ToolRouter as source of truth
ToolRouter's `Category` enum already maps tool names to categories. Rather than duplicating this mapping, we expose a `getCategoryForTool()` companion function on ToolRouter. SettingsViewModel uses this to group local tools for display.

### Add-server dialog: local composable state
The bottom sheet visibility (`showAddServerDialog`) is transient UI state, kept as local `remember` state in the composable — not in SettingsUiState.

---

## UI Structure

### MCP Servers Section (top)

- Section header: "MCP Servers" with subtitle about auto-detection
- Global MCP toggle (`switch_mcp_enabled`, existing)
- Per-server expandable card:
  - **Collapsed:** status dot (10dp circle) + server label (`titleSmall`) + URL (`bodySmall`, single line, `maxLines = 1`) + toolset badge (if set, `labelSmall` in `tertiaryContainer`) + enable switch + chevron icon (`KeyboardArrowUp`/`KeyboardArrowDown`)
  - **Expanded:** connection status text + error details (if error) + per-tool list with name/description/toggle + read-only switch + Refresh/Remove buttons
- "Add MCP Server" button opens `ModalBottomSheet` with name, URL, toolset fields

**Status dot colors** (via `StatusColors` theme tokens, not hardcoded):
- Connected: `statusColors.successful` (green, already exists)
- Connecting: `statusColors.running` (orange, already exists)
- Error: `statusColors.error` (red, **new** — add to `StatusColors`)
- Disconnected: `statusColors.disconnected` (gray, **new** — add to `StatusColors`)

### Local Tools Section (bottom)

- Section header: "Local Tools" (`titleMedium`) with subtitle showing total count (`bodySmall`, `onSurfaceVariant`)
- 8 collapsible category sections matching ToolRouter categories: Jobs, Inventory, Monitoring, Users, Security, Configuration, EDA, Platform
- Each category header: clickable `Row` (not a Card — lighter weight, no elevation) with category name (`titleSmall`) + tool count badge (`labelSmall`) + chevron icon
- Expanded category: single-column list of tool rows with name (`bodyMedium`) + description (`bodySmall`, `onSurfaceVariant`) + toggle switch
- All categories collapsed by default

---

## Interaction & UX Patterns

### Animation

Follow our established AgentTab pattern (not Kai's — Kai doesn't animate expand/collapse):

- **MCP card expand/collapse:** `AnimatedVisibility` with `expandVertically()` / `shrinkVertically()`. Applied to the expanded content below the header row.
- **Category section expand/collapse:** Same `AnimatedVisibility` pattern for consistency.
- **Tool list population:** `Modifier.animateContentSize()` on the MCP card Column to smooth height changes when tools load after connection.

### Click target isolation

The enable Switch on MCP server cards must be isolated from the card's expand/collapse click:

- Card header Row gets `.clickable { onToggleExpand() }`
- Switch sits inside the Row but `Switch(onCheckedChange = ...)` consumes its own click events — tapping the Switch toggles enable, not expand
- This matches both Kai's `Card(onClick=...)` + inner Switch pattern and our AgentTab's existing behavior
- Same isolation for per-tool toggles in expanded content: Switch consumes click, Row area does nothing extra

### Touch targets

- All interactive elements (Switches, IconButtons, clickable Rows) must meet 48dp minimum touch target
- Remove button: use standard `IconButton` (48dp default), not a sized-down 32dp variant as in current ToolsTab
- Category headers: full-width clickable Row with 48dp minimum height

### Empty and loading states

**MCP card expanded content by connection state:**
- `Connecting` → "Connecting..." text in `onSurfaceVariant`
- `Connected` with tools → tool list with toggles
- `Connected` with 0 tools → "No tools discovered" text
- `Error` → error message in `colorScheme.error` + "Tap Refresh to retry" hint
- `Disconnected` → "Not connected" text

### Accessibility

- Status dot must have a textual equivalent: wrap in `Box` with `Modifier.semantics { contentDescription = "Status: Connected" }`
- Chevron icon: `contentDescription = if (expanded) "Collapse" else "Expand"` (matches AgentTab)
- Card: `Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" }`
- Tool toggles: Switch `contentDescription` not needed (Material 3 Switch announces its state automatically), but the Row should be described: `"Toggle tool list_hosts"`
- Remove button: `contentDescription = "Remove ${server.label}"` (existing pattern)

### Spacing and padding

Follow existing Settings tab patterns:
- Root Column: 16dp padding, `spacedBy(12.dp)`
- Card internal padding: 16dp
- Status dot: 10dp size, 12dp right spacer (matches AgentTab)
- Section headers: `titleMedium`, 4dp gap to subtitle, 12dp gap to first card
- Tool rows inside expanded card: `padding(vertical = 4.dp)` per row
- Divider between MCP and Local sections: `HorizontalDivider` with 8dp vertical margin

---

## Data Model

### New: `LocalToolUiState`

```kotlin
// presentation/settings/LocalToolUiState.kt
data class LocalToolUiState(
    val name: String,        // ToolSpec.name, e.g. "list_hosts"
    val description: String, // ToolSpec.description
    val category: String,    // ToolRouter category name, e.g. "INVENTORY"
    val isEnabled: Boolean
)
```

### Modified: `SettingsUiState.Ready`

New fields added to the existing `Ready` data class:

```kotlin
// Tools (MCP) — existing
val mcpEnabled: Boolean = false,
val mcpServers: List<McpServerConfig> = emptyList(),
val connections: Map<String, McpConnectionState> = emptyMap(),

// Tools — new
val localTools: List<LocalToolUiState> = emptyList(),
val mcpServerTools: Map<String, List<McpToolUiState>> = emptyMap(),
val expandedMcpServers: Set<String> = emptySet(),
val expandedCategories: Set<String> = emptySet(),
val disabledTools: Set<String> = emptySet()  // "SOURCE:name" format
```

Note: `McpToolUiState` is a simple display model:

```kotlin
data class McpToolUiState(
    val name: String,
    val description: String,
    val isEnabled: Boolean
)
```

---

## State Flow

### Initialization (SettingsViewModel)

1. SettingsViewModel receives `getAll<LocalTool>()` via Koin injection
2. On init, loads `disabledTools` set from `AssistantRepository.getDisabledTools()`
3. Maps each `LocalTool` to `LocalToolUiState` using `ToolRouter.getCategoryForTool(name)` for grouping
4. Sets `isEnabled = "LOCAL:$name" !in disabledTools`

### Tool toggle (user action)

1. User toggles a tool in the UI
2. SettingsViewModel updates `disabledTools` set (add or remove `"SOURCE:name"`)
3. Persists via `AssistantRepository.saveDisabledTools(set)`
4. Updates UI state (re-derives `localTools` or `mcpServerTools` with new enabled flags)

### Message send (AssistantViewModel)

1. `sendMessage()` creates per-message `ToolRouter` as before
2. After `registerLocalTools()` / `registerMcpTools()`, reads `repository.getDisabledTools()`
3. For each entry in the set, parses `"SOURCE:name"` and calls `toolRouter.setToolEnabled(name, source, false)`
4. Proceeds with `getToolsForQuery()` as before — disabled tools are filtered out

### MCP server tools (reactive)

`mcpServerTools` is re-derived whenever `mcpServerManager.connections` emits (same flow SettingsViewModel already observes). For each connected server, calls `McpServerManager.getToolsForServer(label)` which filters the already-loaded in-memory tool list (no network call — tools are populated during `connectAll()`). Tool enabled state is derived from the `disabledTools` set.

### MCP server expand (user action)

1. User taps MCP server card
2. SettingsViewModel toggles `expandedMcpServers` set (UI-only, tools already loaded)

### MCP server refresh (user action)

1. User taps "Refresh" in expanded MCP card
2. SettingsViewModel calls `McpServerManager.reconnectServer(label)` (new method — disconnects and reconnects a single server)
3. Connection state flow updates → `mcpServerTools` re-derived automatically

---

## Files to Create

| File | Purpose |
|------|---------|
| `ui/settings/McpServerCard.kt` | Expandable MCP server card composable with status dot, tool list, action buttons |
| `ui/settings/AddMcpServerSheet.kt` | ModalBottomSheet dialog for adding MCP servers (name, URL, toolset fields) |
| `ui/settings/LocalToolsSection.kt` | Category-grouped collapsible sections with per-tool toggle cards |
| `presentation/settings/LocalToolUiState.kt` | `LocalToolUiState` and `McpToolUiState` data classes |

## Files to Modify

| File | Change |
|------|--------|
| `ui/settings/ToolsTab.kt` | Rewrite: compose `McpServersSection` + `LocalToolsSection`, remove inline server rows and stub |
| `presentation/settings/SettingsUiState.kt` | Add `localTools`, `mcpServerTools`, `expandedMcpServers`, `expandedCategories`, `disabledTools` to `Ready` |
| `presentation/settings/SettingsViewModel.kt` | Add `localTools: List<LocalTool>` constructor param, tool toggle/expand methods, load disabled set on init |
| `ui/settings/SettingsScreen.kt` | Pass new callbacks to `ToolsTab` |
| `assistant/data/AssistantRepository.kt` | Add `DISABLED_TOOLS_KEY`, `saveDisabledTools()`, `getDisabledTools()` |
| `assistant/data/IAssistantRepository.kt` | Add disabled tools interface methods |
| `assistant/presentation/AssistantViewModel.kt` | Read disabled set from repository, apply to per-message ToolRouter after registration |
| `assistant/engine/ToolRouter.kt` | Add `getCategoryForTool(name): String?` companion function |
| `assistant/AssistantModule.kt` | Add `localTools = getAll<LocalTool>()` to SettingsViewModel construction |
| `network/mcp/McpServerManager.kt` | Add `getToolsForServer(label): List<McpTool>` and `reconnectServer(label)` |
| `ui/theme/StatusColors.kt` | Add `error` and `disconnected` color tokens |

## Files Not Touched

- `ToolRouter` internals — categories, routing logic, per-message lifecycle unchanged
- `McpTool`, `LocalTool`, `ToolSpec` — existing tool interfaces unchanged
- `LocalToolsModule.kt` — Koin registrations unchanged
- `McpClient.kt`, `McpTransport.kt`, `McpSession.kt` — MCP protocol layer unchanged

---

## String Resources

New strings needed (following existing pattern in `strings.xml`):

- `tools_mcp_servers` — "MCP Servers"
- `tools_mcp_description` — "Auto-detect at {instance}/{toolset}/mcp"
- `tools_mcp_status_connected` — "Connected"
- `tools_mcp_status_connecting` — "Connecting..."
- `tools_mcp_status_error` — "Connection error"
- `tools_mcp_status_disconnected` — "Not connected"
- `tools_mcp_no_tools` — "No tools available"
- `tools_mcp_add_server` — "Add MCP Server"
- `tools_mcp_server_name` — "Server name"
- `tools_mcp_server_url` — "Server URL"
- `tools_mcp_server_toolset` — "Toolset (optional)"
- `tools_mcp_refresh` — "Refresh"
- `tools_mcp_remove` — "Remove"
- `tools_mcp_read_only` — "Read Only"
- `tools_local_title` — "Local Tools"
- `tools_local_description` — "%d tools across %d categories"
- `tools_category_jobs` — "Jobs"
- `tools_category_inventory` — "Inventory"
- `tools_category_monitoring` — "Monitoring"
- `tools_category_users` — "Users"
- `tools_category_security` — "Security"
- `tools_category_configuration` — "Configuration"
- `tools_category_eda` — "EDA"
- `tools_category_platform` — "Platform"

---

## Test Tags

Following the existing convention (`field_<name>`, `button_<name>`, `switch_<name>`):

- `switch_mcp_enabled` — existing, unchanged
- `card_mcp_server_{label}` — server card (tap to expand)
- `switch_mcp_server_{label}` — per-server enable toggle
- `switch_mcp_tool_{name}` — per-MCP-tool toggle
- `switch_local_tool_{name}` — per-local-tool toggle
- `button_mcp_refresh_{label}` — refresh button
- `button_mcp_remove_{label}` — remove button
- `button_add_mcp_server` — add server button
- `section_category_{name}` — category header (tap to expand)

---

## Out of Scope

- ToolRouter singleton refactor (#120)
- ToolRouter keyword/scoring improvements (#120 Tier 1-2)
- MCP client replacement with Koog/Kotlin SDK (#74)
- Popular MCP servers list (Kai has this; we could add later but AAP-specific servers are auto-detected)
- HTTP headers on MCP servers (Kai supports this; our servers use instance auth via interceptor)
- Responsive grid for local tools (phone screen is single-column; grid can be added for tablets later)

---

## References

- Kai 9000 reference: `tmp/Kai/composeApp/src/commonMain/kotlin/com/inspiredandroid/kai/ui/settings/McpSection.kt`
- Kai tools grid: `tmp/Kai/composeApp/src/commonMain/kotlin/com/inspiredandroid/kai/ui/settings/ToolsSettings.kt`
- Current implementation: `ui/settings/ToolsTab.kt`, `presentation/settings/SettingsViewModel.kt`
- ToolRouter singleton discussion: #120 (comment from #196 design session)
- ListToolsLocalTool bypass: PR #198

Assisted-by: Claude Opus 4.6 <noreply@anthropic.com>

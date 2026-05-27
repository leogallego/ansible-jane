# Tools UI Redesign — Follow-up Findings

**PR:** #207 (branch `worktree-196-tools-ui-redesign-spec`)
**Date:** 2026-05-27
**Source:** 7-agent E2E verification (code review, security review, code simplification, test audit, unit tests, screenshot tests, device E2E)
**Related issues:** #196 (original), #120 (ToolRouter singleton refactor)

---

## Findings Overview

All findings are **non-blocking** for PR #207 merge. They fall into three categories:
code cleanup, security hardening, and test coverage gaps.

| # | Category | Priority | Summary | Scope |
|---|----------|----------|---------|-------|
| F1 | Code | Medium | Extract shared `ToolItemRow` composable | This PR's files |
| F2 | Code | Medium | Extract `updateMcpServer` helper in SettingsViewModel | This PR's files |
| F3 | Code | Low | Remove unused `disabledTools` param from McpServerCard | This PR's files |
| F4 | Code | Low | Extract toggle set membership helper | This PR's files |
| F5 | Code | Low | Category display name map instead of `when` | This PR's files |
| F6 | Architecture | Medium | Local tools init race in SettingsViewModel | This PR's files |
| F7 | Thread Safety | Medium | `currentInstance` not volatile in McpServerManager | This PR's files |
| F8 | Thread Safety | Medium | `disabledTools` MutableSet not thread-safe in ToolRouter | Pre-existing (#120) |
| F9 | Security | Medium | URL validation missing in `addMcpServer` | This PR's files |
| F10 | Security | Low | Read-only enforcement parses label from description | Pre-existing (#120) |
| F11 | Testing | High | No dedicated test for AddMcpServerSheet | New test file |
| F12 | Testing | High | No dedicated test for McpServerManager | New test file |
| F13 | Testing | Medium | SettingsScreenTest lacks Tools tab interaction tests | Existing test file |
| F14 | Testing | Medium | No test for LocalToolsSection category ordering | New test file |
| F15 | Testing | Low | Screenshot tests missing disabled-tool and error states | Existing test file |

---

## Detailed Findings

### F1: Extract shared `ToolItemRow` composable

**Files:**
- `McpServerCard.kt:213-241`
- `LocalToolsSection.kt:125-153`

**Problem:** Both files contain nearly identical Row+Column+Text+Switch patterns for
rendering individual tool items with name, description, and enable toggle.

**Fix:** Extract a shared `ToolItemRow` composable:

```kotlin
@Composable
fun ToolItemRow(
    name: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    testTagPrefix: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium,
                 maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = description, style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                 maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Switch(checked = isEnabled, onCheckedChange = onToggle,
               modifier = Modifier.testTag("${testTagPrefix}_$name"))
    }
}
```

**Impact:** Eliminates ~16 lines of duplication. Place in `ui/settings/` package.

---

### F2: Extract `updateMcpServer` helper in SettingsViewModel

**File:** `SettingsViewModel.kt:351-368`

**Problem:** `toggleServerReadOnly` and `toggleServerEnabled` share identical
map-and-update logic, differing only in the `copy()` field:

```kotlin
// toggleServerReadOnly (lines 351-358)
val updated = instance.mcpServerUrls?.map {
    if (it.url == url) it.copy(readOnly = readOnly) else it
}
tokenManager.updateMcpConfig(instance.id, instance.mcpEnabled, updated)

// toggleServerEnabled (lines 361-368) — same pattern, different field
```

**Fix:**

```kotlin
private suspend fun updateMcpServer(
    url: String,
    transform: (McpServerConfig) -> McpServerConfig
) {
    val instance = tokenManager.activeInstance.value ?: return
    val updated = instance.mcpServerUrls?.map {
        if (it.url == url) transform(it) else it
    }
    tokenManager.updateMcpConfig(instance.id, instance.mcpEnabled, updated)
}

fun toggleServerReadOnly(url: String, readOnly: Boolean) {
    viewModelScope.launch { updateMcpServer(url) { it.copy(readOnly = readOnly) } }
}

fun toggleServerEnabled(url: String, enabled: Boolean) {
    viewModelScope.launch { updateMcpServer(url) { it.copy(enabled = enabled) } }
}
```

**Impact:** Saves 10 lines, eliminates duplication risk.

---

### F3: Remove unused `disabledTools` param from McpServerCard

**File:** `McpServerCard.kt:52`

**Problem:** `disabledTools: Set<String>` is passed as a parameter but never referenced
in the function body. Tool enabled state is computed from `McpToolUiState.isEnabled` instead.

**Fix:** Remove the parameter from `McpServerCard` and its call site in `ToolsTab.kt`.

**Impact:** Dead code removal, 1 line each in signature and call site.

---

### F4: Extract toggle set membership helper

**File:** `SettingsViewModel.kt:397-417`

**Problem:** `toggleExpandMcpServer` and `toggleExpandCategory` both contain identical
set toggle logic:

```kotlin
val updated = if (item in set) set - item else set + item
```

**Fix:** One-line helper function:

```kotlin
private fun <T> Set<T>.toggled(item: T): Set<T> =
    if (item in this) this - item else this + item
```

**Impact:** Minor readability improvement. Low priority.

---

### F5: Category display name map instead of `when`

**File:** `LocalToolsSection.kt:40-50`

**Problem:** `categoryDisplayName` uses a 9-branch `when` expression. A `Map<String, Int>`
mapping category to string resource ID would be more maintainable.

**Fix:**

```kotlin
private val CATEGORY_NAMES = mapOf(
    "JOBS" to R.string.tools_category_jobs,
    "INVENTORY" to R.string.tools_category_inventory,
    // ... etc
)

@Composable
private fun categoryDisplayName(category: String): String =
    CATEGORY_NAMES[category]?.let { stringResource(it) } ?: category
```

**Impact:** More maintainable, fewer lines. Low priority.

---

### F6: Local tools init race in SettingsViewModel

**File:** `SettingsViewModel.kt:49-156`

**Problem:** The `combine` flow (lines 55-118) and the local tools loading (lines 145-156)
run as separate coroutines in the init block. The combine block emits first with
`localTools = emptyList()` (line 79 fallback), then the local tools launch updates
with the real list. This causes a brief flash of "0 tools across 0 categories" in the UI.

**Current behavior:**
1. Combine emits with `localTools = emptyList()`, `disabledTools = emptySet()`
2. Local tools launch loads disabled set and builds tool UI states
3. `updateReady { copy(localTools = ..., disabledTools = ...) }` updates state

**Why it's acceptable now:** Standard async loading UX — identical to how instance
data loads (empty → populated). Users see the populated list within milliseconds.

**Ideal fix:** Load disabled tools + build local tool states BEFORE starting the combine
flow, then use those as the initial fallback values instead of `emptyList()`:

```kotlin
init {
    viewModelScope.launch {
        // Load once before combine
        val disabled = assistantRepository.getDisabledTools()
        val toolUiStates = localTools.map { tool ->
            LocalToolUiState(
                name = tool.spec.name,
                description = tool.spec.description,
                category = ToolRouter.getCategoryForTool(tool.spec.name) ?: "OTHER",
                isEnabled = "LOCAL:${tool.spec.name}" !in disabled
            )
        }

        combine(...) { ... ->
            val preservedLocalTools = (current as? SettingsUiState.Ready)?.localTools ?: toolUiStates
            val preservedDisabledTools = (current as? SettingsUiState.Ready)?.disabledTools ?: disabled
            // ...
        }.collect { ... }
    }
    // Remove the separate launch block at lines 145-156
}
```

**Impact:** Eliminates the brief empty-state flash. Medium priority.

---

### F7: `currentInstance` not volatile in McpServerManager

**File:** `McpServerManager.kt:26`

**Problem:** `currentInstance` is a plain `var` written in `connectAll()` (line 29) and
read in `reconnectServer()` (line 93). While both are typically called from the Main
dispatcher, the class already uses `ConcurrentHashMap` for `clients`, signaling that
multi-threaded access is possible.

**Fix:** Add `@Volatile`:

```kotlin
@Volatile
private var currentInstance: AapInstance? = null
```

**Impact:** One-line defense-in-depth fix. Medium priority.

---

### F8: `disabledTools` MutableSet not thread-safe in ToolRouter

**File:** `ToolRouter.kt:13`

**Problem:** `private val disabledTools = mutableSetOf<Pair<String, ToolSource>>()` is
a plain `MutableSet`. `setToolEnabled()` mutates it, `isToolEnabled()` / `getToolsForQuery()`
read it. If ToolRouter were a singleton, concurrent access would cause issues.

**Current mitigation:** ToolRouter is instantiated per-message in
`AssistantViewModel.sendMessage()`, so there's no concurrent access in practice.

**Scope:** This is a **pre-existing** pattern, not introduced by PR #207. The fix
belongs in #120 (ToolRouter singleton refactor), where ToolRouter becomes a singleton
and thread safety becomes mandatory.

**Fix (for #120):** Replace with `ConcurrentHashMap.newKeySet<Pair<String, ToolSource>>()`.

---

### F9: URL validation missing in `addMcpServer`

**File:** `SettingsViewModel.kt:333-339`

**Problem:** User-entered URLs are only trimmed (`url.trimEnd('/')`) but not validated
for format, scheme, or content. While `network_security_config.xml` enforces HTTPS at
the network layer, app-level validation would:
- Reject malformed URLs early (better UX)
- Enforce HTTPS before attempting connection
- Prevent very long URLs from causing UI issues

**Fix:** Add validation in `addMcpServer()`:

```kotlin
fun addMcpServer(url: String, label: String, toolset: String? = null) {
    val instance = tokenManager.activeInstance.value ?: return
    viewModelScope.launch {
        val sanitizedUrl = url.trim().trimEnd('/')
        val uri = try { java.net.URI(sanitizedUrl) } catch (_: Exception) {
            // TODO: emit validation error to UI state
            return@launch
        }
        if (uri.scheme != "https") {
            // TODO: emit validation error
            return@launch
        }
        val current = instance.mcpServerUrls?.toMutableList() ?: mutableListOf()
        current.add(McpServerConfig(url = sanitizedUrl, label = label, toolset = toolset))
        tokenManager.updateMcpConfig(instance.id, true, current)
    }
}
```

**Impact:** Defense-in-depth. Medium priority. Could also add max-length and
duplicate-label checks.

---

### F10: Read-only enforcement parses label from description

**File:** `ToolRouter.kt:315-319` (approximate, in `getToolsForQuery()`)

**Problem:** Read-only server detection parses the server label from the MCP tool's
description string (`"[$label] ..."` format set in `McpTool.kt:31`). If the description
format changes, read-only enforcement silently breaks.

**Better approach:** Store `isReadOnly` directly on `McpTool` and check it in
`ToolRouter` without parsing:

```kotlin
class McpTool(
    private val client: McpClient,
    private val mcpToolDef: McpToolDefinition,
    val serverLabel: String,
    val toolset: String? = null,
    val isReadOnly: Boolean = false  // new field
) : Tool { ... }
```

**Scope:** Pre-existing pattern, best addressed in #120 alongside the ToolRouter
singleton refactor.

---

### F11: No dedicated test for AddMcpServerSheet

**File to create:** `app/src/test/.../ui/settings/AddMcpServerSheetTest.kt`

**Missing coverage:**
- Add button disabled when name is blank
- Add button disabled when URL is blank
- Add button disabled when both are blank
- Whitespace-only input treated as blank
- `onAdd` callback receives correct parameters
- `onDismiss` called after successful add
- Toolset field optional (blank → null)

**Priority:** High — this is a user input form with validation logic.

---

### F12: No dedicated test for McpServerManager

**File to create:** `app/src/test/.../network/mcp/McpServerManagerTest.kt`

**Missing coverage:**
- `reconnectServer()` disconnects old client before reconnecting
- `reconnectServer()` removes old tools from `mcpTools` list
- `reconnectServer()` returns early when `currentInstance` is null
- `reconnectServer()` returns early when label not found
- `getToolsForServer()` returns only tools for specified label
- `getAllTools()` returns snapshot (not mutable reference)
- `disconnectAll()` clears all state
- Thread safety: concurrent `connectAll()` + `getAllTools()`

**Priority:** High — McpServerManager has complex state management and thread safety.

---

### F13: SettingsScreenTest lacks Tools tab interaction tests

**File:** `app/src/test/.../ui/settings/SettingsScreenTest.kt`

**Missing coverage:**
- Expanding/collapsing MCP server card
- Toggling per-server enable switch
- Toggling per-tool enable switch
- Opening AddMcpServerSheet via button
- Category expand/collapse in LocalToolsSection
- Scroll to Local Tools section

**Priority:** Medium — section visibility is tested, but no interaction coverage.

---

### F14: No test for LocalToolsSection category ordering

**File to create:** `app/src/test/.../ui/settings/LocalToolsSectionTest.kt`

**Missing coverage:**
- Categories render in `CATEGORY_ORDER` sequence
- Tools with unknown category are excluded (or handled gracefully)
- Empty categories are skipped
- Category count badge shows correct number

**Priority:** Medium.

---

### F15: Screenshot tests missing edge-case states

**File:** `app/src/screenshotTest/.../screens/ToolsTabScreenshots.kt`

**Missing variants:**
- Tool with `isEnabled = false` (disabled tool visual state)
- MCP server with `readOnly = false` (non-read-only visual)
- Connection error state with retry hint
- Empty state (zero MCP servers, zero local tools)
- Mixed enabled/disabled tools within a category

**Priority:** Low — 8 core variants are covered.

---

## Recommended Grouping for Implementation

### Batch 1: Quick wins (can be done together)
- F3 (remove unused param)
- F4 (toggle helper)
- F7 (`@Volatile`)

### Batch 2: Code cleanup
- F1 (shared ToolItemRow)
- F2 (updateMcpServer helper)
- F5 (category name map)

### Batch 3: Architecture improvements
- F6 (init race fix)
- F9 (URL validation)

### Batch 4: Test coverage (separate PR)
- F11 (AddMcpServerSheet tests)
- F12 (McpServerManager tests)
- F13 (SettingsScreenTest interactions)
- F14 (LocalToolsSection tests)
- F15 (screenshot edge cases)

### Deferred to #120 (ToolRouter singleton refactor)
- F8 (thread-safe disabledTools)
- F10 (read-only enforcement via McpTool field)

---

## Rejected Findings

One code review finding was **rejected** because it would re-introduce a previously
fixed bug:

**Suggestion:** Make the entire McpServerCard header Row clickable for expand/collapse.

**Reason for rejection:** This was the original BUG-2 — when `.clickable` is on the Row
containing the Switch, tapping the Switch also triggers expand/collapse. The current
implementation intentionally limits `.clickable` to the text Column and chevron Icon only.
This is the correct Compose pattern for rows with interactive controls.

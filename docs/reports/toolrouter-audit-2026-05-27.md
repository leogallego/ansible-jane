# ToolRouter Deep Audit Report

**Issue:** #171 — Deep review of MCP filtering and ToolRouter behavior
**Date:** 2026-05-27

## Executive Summary

The ToolRouter is architecturally sound. Local tool coverage is **100%** — all 61 tools are correctly registered in exactly one category with zero orphans or stale references. The overlap deduplication, read-only enforcement, and fallback logic all work correctly. However, the audit found **3 bugs** and **4 design gaps** that affect MCP tool routing for non-AAP servers and edge cases.

---

## Findings

### Bug 1: Missing EDA toolset auto-detection (Medium)

`TOOLSET_CATEGORY_MAP` defines `"event_management" → Category.EDA` but no auto-detected MCP server exists for it in `SettingsViewModel.toggleMcpEnabled()`. All other 6 toolsets have auto-detected servers. Users won't get MCP EDA tools unless they manually add a server.

**Fix:** Add to `SettingsViewModel.kt` auto-detected servers:
```kotlin
McpServerConfig(
    url = "$base/event_management/mcp", label = "EDA",
    isAutoDetected = true, readOnly = true, toolset = "event_management"
),
```

### Bug 2: Stemmer only handles plurals (Low)

`stem()` removes `-s`, `-es`, `-ies`, `-e` but NOT `-ing`, `-ed`, `-er`. So `running` stays `running`, not `run`. This works today because categories include common variants as separate keywords (both `run` and `running` are in JOBS). But any missing variant creates a silent gap.

**Impact:** Low — categories are comprehensive enough. Worth monitoring when adding new keywords.

### Bug 3: Read-only filter runs on already-disabled tools (Very Low)

MCP tools disabled by `OVERLAP_MAPPING` still go through the read-only enforcement check. No functional impact, just wasted computation.

---

### Gap 1: Non-AAP MCP tools are invisible without toolset (Critical for extensibility)

Generic MCP tools (e.g., `get_weather`, `search_web`) with no toolset and non-AAP naming will be filtered out in ALL queries. The prefix extraction assumes `prefix.resource_action` format. Tools without a dot in their name extract the full name as the resource, which won't match any category prefix.

**Options:**
- A) Add a `Category.GENERAL` fallback that catches unmatched tools
- B) Always include non-toolset, non-prefix-matched tools at low priority
- C) Require users to assign a toolset when adding non-AAP MCP servers (current implicit behavior)

### Gap 2: Read-only enforcement only checks 11 suffixes

`WRITE_SUFFIXES`: `_create`, `_update`, `_delete`, `_launch`, `_relaunch`, `_cancel`, `_partial_update`, `_approve`, `_deny`, `_copy`, `_sync`

Non-standard write actions bypass the filter: `add_`, `remove_`, `modify_`, `execute_`, `trigger_`, `start_`, `stop_`, `enable_`, `disable_`.

**Impact:** Only affects non-AAP MCP servers since AAP tools follow the `resource_action` naming. Low risk today, grows as MCP server diversity increases.

### Gap 3: Cross-category toolset routing can miss queries

`developer_integration` maps to `{JOBS, MONITORING}` but webhook-related queries match `EDA` (which has `trigger`, `webhook` keywords). An integration tool handling webhooks won't be selected for EDA queries.

Similarly, "setup integration" matches no category because "integration" isn't a keyword in any category — it only exists in `TOOLSET_CATEGORY_MAP`.

### Gap 4: Category keyword overlaps are undocumented

Multiple categories share keywords:
- `label`, `labels`, `tag`, `tags` → INVENTORY and CONFIGURATION
- `environment` → CONFIGURATION and EDA
- `group` → INVENTORY and MONITORING
- `status` → JOBS and MONITORING

This is **intentional** (union of tools from both categories) but not documented.

---

## Local Tool Coverage

**Result: 100% coverage, no issues**

| Category | Tool Count | Status |
|----------|-----------|--------|
| JOBS | 15 | All registered |
| EDA | 10 | All registered |
| INVENTORY | 7 | All registered |
| USERS | 7 | All registered |
| PLATFORM | 7 | All registered |
| CONFIGURATION | 6 | All registered |
| MONITORING | 5 | All registered |
| SECURITY | 3 | All registered |
| **Total** | **60** + `list_tools` (meta) + `ping` (in MONITORING) = **62 files, 61 routable** |

- Zero orphaned tools (not in any category)
- Zero multi-category tools (each in exactly one)
- Zero stale references (all category entries have implementations)
- `list_tools` correctly excluded from categories — used as fallback when no tools match

---

## MCP Tool Lifecycle

```
McpServerConfig (user/auto-detected)
    ↓
McpServerManager.connectServer() — MCP protocol tools/list
    ↓
McpTool created with toolset from McpServerConfig
    ↓
ToolRouter.registerMcpTools() — overlap auto-disable runs
    ↓
getToolsForQuery(query):
    1. Stem query, match categories
    2. If toolset exists AND in TOOLSET_CATEGORY_MAP → use category match
    3. Else → extract prefix from tool name, match against category resourcePrefixes
    4. Apply read-only filter (check WRITE_SUFFIXES)
    5. Apply per-tool enable/disable
    6. Cherry-pick: score by keyword overlap, boost list/get, penalize destructive
    ↓
AssistantViewModel: cap MCP tools (3-10 based on token mode)
    ↓
ChatEngine: send tool schemas to LLM, execute tool calls
```

### Fallback behavior (no category match)

| Mode | categoryMatched | isToolDiscovery | Result |
|------|-----------------|-----------------|--------|
| STANDARD | false | false | Proceed → ListToolsLocalTool fallback |
| STANDARD | false | true | Proceed → ListToolsLocalTool fallback |
| TOOLS_ONLY | false | false | Early return with guidance message |
| TOOLS_ONLY | false | true | Proceed → ListToolsLocalTool fallback |
| Any | true | false (no tools) | Early return suggesting MCP servers |

The docs claim the fallback is buggy, but it works correctly — `ListToolsLocalTool` IS reached when `categoryMatched = false` in STANDARD mode.

---

## Recommendations (prioritized)

1. **Fix Bug 1** — Add EDA auto-detected server. One line, immediate value.
2. **Document Gap 4** — Add comment block before category definitions explaining keyword overlap.
3. **Decide on Gap 1** — Choose approach for non-AAP MCP tool routing. Option C (require toolset config) is fine for now; Option B (low-priority pass-through) is better long-term.
4. **Expand read-only suffixes** (Gap 2) — Add `_add`, `_remove`, `_execute`, `_start`, `_stop` when non-AAP MCP servers become common.
5. **Fix Gap 3** — Add `integration` to CONFIGURATION keywords, add EDA to `developer_integration` mapping.
6. **Consider stemmer improvements** (Bug 2) — Only when a real user hits a keyword gap.

# ToolRouter Keyword Matching Improvement — Design Spec

## Problem

The ToolRouter uses exact word matching for category routing. This causes three classes of failures:

1. **Missed queries** — "list orgs", "any errors?", "is AAP up?" don't match any category because the exact words aren't in the keyword sets. The LLM then receives no tools and hallucinates tool calls as plain text.
2. **Wasted token budget** — MCP tools (~1500 chars/schema each) from irrelevant categories are sent to the LLM when a keyword like "audit" appears in both SECURITY and EDA. Cherry-picking only applies to local tools; MCP tools pass through unranked.
3. **Stemmer limitations** — The existing `stem()` function only handles plurals but is only used for tool ranking, not category matching. It also has bugs: "de" stems to "d" (single char wildcard), "ees" stems to "" (empty string).

Simulated 130 AAP queries: 82.3% correct, 12.3% category leaks, 1.5% missed entirely. Full simulation in `.tmp/toolrouter-simulation.md`.

## Goals

- Reduce MISS rate from 1.5% to 0%
- Reduce LEAK rate from 12.3% to under 5%  
- Cherry-pick MCP tools (not just local) to save token budget
- Fix stemmer bugs
- Add AAP-specific abbreviations (jt, ee, de, wfjt, rbac, scm)

## Non-Goals

- Sub-categories (cherry-pick within categories makes this unnecessary)
- Embedding-based routing (too complex, adds latency)
- Sending all tools to LLM (24x token increase — ~20-30K tokens vs ~800 today)
- MCP gateway/proxy (server-side infrastructure, defeats lightweight mobile app purpose)

## Design

### Change 1: Apply stemming to category matching

Currently category matching is exact: `category.keywords.any { it in queryWords }`.

Change to stem both sides: stem each query word AND each keyword, then compare stemmed forms. This handles plurals automatically — "orgs" stems to "org", "workflows" to "workflow", "credentials" to "credential".

Cache stemmed keywords per category (lazy val) since they don't change at runtime.

### Change 2: Fix stemmer bugs

Add a guard: if the stemmed result is shorter than 2 characters, return the original word unchanged. This prevents:
- "de" → "d" (would match any word starting with "d")
- "ees" → "" (empty string matches everything)

Two-character stems are allowed because AAP abbreviation plurals need them: "jts"→"jt", "des"→"de".

The stem function moves from private instance method to companion object (needed by the keyword cache).

### Change 3: Extended keywords

Based on simulation analysis and AAP domain knowledge. Keywords in **bold** are new.

**INVENTORY**: host, hosts, group, groups, inventory, inventories, infrastructure, **facts, gather, info, server, servers, machine, machines, asset, assets**

**JOBS**: job, jobs, template, templates, launch, run, schedule, schedules, workflow, playbook, **jt, wfjt, output, stdout, running, failed, started, task, tasks, command, error, errors, failure, status, playbooks, workflows, execution, executions**

**MONITORING**: health, status, monitor, metrics, log, logs, dashboard, analytics, instance, instances, mesh, topology, ping, cluster, capacity, node, **monitoring, healthy, overview, nodes, workers, alive, up, down, diagnostics, group**

**USERS**: user, users, team, teams, organization, organizations, org, role, roles, permission, permissions, member, **members, people, admin, admins, token, tokens, application, applications, app, apps, access, rbac**

**SECURITY**: credential, credentials, secret, secrets, ~~audit~~, security, compliance, policy, certificate, **creds, vault, password, passwords, key, keys, cert, certs**

Note: "audit" REMOVED from SECURITY — in AAP context, "audit" means EDA audit rules. No security-specific audit tools exist.

**CONFIGURATION**: setting, settings, configure, configuration, notification, notifications, label, labels, project, projects, execution, environment, environments, **config, ee, ees, scm, repo, repos, repository, alert, alerts, tag, tags**

**EDA**: eda, rulebook, activation, event, audit, **de, rule, rules, trigger, triggers, webhook, webhooks, stream, streams, decision, driven, rulebooks, activations, events, environment**

### Change 4: Intentional dual-category keywords

Some keywords legitimately belong in multiple categories. Rather than treating these as leaks, we make them intentional:

| Keyword | Categories | Rationale |
|---------|-----------|-----------|
| status | JOBS + MONITORING | "job status" and "system status" are both valid |
| group | INVENTORY + MONITORING | "host groups" and "instance groups" are both valid |
| environment | CONFIGURATION + EDA | "execution environments" and "decision environments" are both valid |
| execution | JOBS + CONFIGURATION | "job execution" and "execution environments" are both valid |

### Change 5: Cherry-pick both local AND MCP tools

Current behavior: `rankTools()` only applies to local tools. MCP tools from matched categories all pass through unranked.

New behavior: A unified `cherryPick()` function scores both local and MCP tools by query-word overlap with tool name parts (stemmed).

For MCP tools, the name is split on both `.` and `_` (e.g., `controller.credentials_list` → `{controller, credential, list}`).

**Scoring:**
- `overlap_count * 10` (number of stemmed name parts matching stemmed query words)
- If overlap > 0: `+3` for list/ping tools, `+1` for get/read tools, `-5` for destructive tools
- If overlap == 0: score stays 0 → tool dropped

**Fallback:** If NO tools from a category have any query-word overlap (e.g., "is everything healthy?" — no monitoring tool name contains "healthy"), fall back to all `list_*` and `ping` tools from that category. This is safe because list tools provide discovery without side effects.

### Change 6: Expand stop words

Add common prepositions and conjunctions that add noise to stemmed word sets: "of", "for", "in", "on", "to", "and", "or", "with", "from", "any", "been".

## Affected Files

| File | Change |
|------|--------|
| `assistant/engine/ToolRouter.kt` | All changes — keywords, stemming, cherry-pick, stemmer fix |
| `assistant/engine/ToolRouterTest.kt` | Update existing tests, add tests for stemming, cherry-pick, new keywords |

No other files change. The `Tool` interface, `ToolExecutor`, `ChatEngine`, and `AssistantViewModel` are unaffected — `getToolsForQuery()` returns the same `QueryResult` type.

## Expected Results (from simulation)

After changes, the 130-query simulation should show:
- MISS rate: 0% (was 1.5%) — "any errors?" and "is AAP up?" now match
- LEAK rate: <5% (was 12.3%) — cherry-pick drops irrelevant tools from leaked categories
- Correct rate: >90% (was 82.3%)
- MCP token savings: ~30-50% fewer MCP tool schemas sent per query (cherry-pick drops irrelevant ones)

### Change 7: Separate tool budgets for local and MCP

Current behavior: a single `toolLimit` (8/5/3) applies to local + MCP combined. Local tools eat into the budget first, leaving fewer MCP slots.

New behavior: local tools are exempt from the limit — all cherry-picked local tools are sent (schemas are ~300 chars, negligible). The limit applies only to MCP tools (schemas are ~1500 chars each).

**New limits (MCP only):**
- STANDARD: 10
- TOKEN_SAVER: 5
- TOOLS_ONLY: 3

This change is in `AssistantViewModel.kt`, not `ToolRouter.kt`.

## Risks

1. **False positives from short keywords** — "up", "down", "de", "ee" are short and could match unintended words. Mitigated by stemmer guard (min 2 chars) and the fact that these are uncommon words in natural language.
2. **Cherry-pick too aggressive** — If tool names don't contain query words, fallback to list_* defaults. This is safe but less precise than the ideal.
3. **Dual-category keywords increase tool count** — "status" now intentionally matches JOBS + MONITORING. But cherry-pick ensures only relevant tools from each category survive, so the net effect is positive.

## Test Plan

1. `./gradlew compileDebugKotlin` — passes
2. `./gradlew testDebugUnitTest` — passes
3. Verify existing ToolRouterTest tests still pass (update `.tools` references as needed)
4. Add tests for:
   - Stemmed matching: "orgs" matches USERS, "creds" matches SECURITY
   - Cherry-pick: "audit rules" returns EDA tools, not credential tools
   - Fallback: vague query returns list_* defaults
   - Stemmer guard: "de" doesn't stem to "d"
   - New keywords: "jt", "ee", "error", "up" match correct categories
5. Build and test on emulator: "list orgs", "any errors?", "show my creds", "is AAP up?"

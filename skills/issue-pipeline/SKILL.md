---
name: issue-pipeline
description: Autonomous end-to-end issue processing — triage, plan, review, implement, review, fix, PR
---

# Issue Pipeline

Autonomous issue execution skill. Takes GitHub issue numbers, processes each end-to-end: assess, plan, plan review, implement, implementation review, fix, PR. The only human checkpoint is merge approval.

## When to Use

- User provides one or more GitHub issue numbers and wants autonomous implementation
- User says "process these issues", "implement these", "work through this backlog"
- User invokes `/issue-pipeline #NNN #NNN ...`

## Invocation

```
/issue-pipeline #372 #369 #365
/issue-pipeline 372 369 365
```

Input: one or more GitHub issue numbers (with or without `#` prefix).

Output per issue: an open PR ready for merge (or a skipped/failed issue with an explanatory comment).

Output at completion: a pipeline run summary listing all PRs, merge order, skips, and failures.

## The Iron Law

**No implementation without triage, plan, and plan review first.**

Every issue passes through all phases in order. No skipping phases. No "this one is simple enough to just code." The pipeline exists because humans underestimate complexity — the phases catch what intuition misses.

## Pipeline Phases

0. **Dynamic Triage** — load foundation context, fetch issues, parse dependencies, build DAG, sort into chains
1. **Assess** — verify issue against current codebase, estimate scope, map skills, load on-demand context
2. **Update Issue** — post assessment comment on GitHub issue, add `pipeline/in-progress` label
3. **Plan** — generate implementation plan, save to disk
3.5. **Plan Review** — review plan against architecture rules, revise if needed
4. **Implement** — create worktree, dispatch subagents, run tests and linters
5. **Implementation Review** — 4-angle parallel review (architecture, code quality, security, skill compliance)
6. **Fix** — address review findings, re-test, re-review
7. **Create PR** — commit, push, create PR with full documentation
8. **Merge Gate** — human reviews and merges (pipeline does NOT wait)
9. **Cleanup** — update labels, log results, move to next issue

Phase 0 runs once. Phases 1-9 run per issue.

---

## Phase 0: Dynamic Triage

Runs once at pipeline start. Produces an execution plan that governs the rest of the run.

### Step 0 — Load Foundation Context

Discover and load project context dynamically. Do NOT hardcode any file paths — each step checks if the source exists before reading.

**Always-load sources** (read each if it exists):

1. **Project instructions** — read `CLAUDE.md` in the project root
2. **Global instructions** — read `~/.claude/CLAUDE.md` (non-project-specific sections: git workflow, attribution, temp files, PR rules)
3. **Architecture docs** — `find . -path "*/architecture/*" -name "*.md" -not -path "*/node_modules/*"` — read each file found
4. **CI configuration** — check in order: `.github/workflows/*.yml`, `.gitlab-ci.yml`, `Jenkinsfile`, `.circleci/config.yml`. Read whichever exists.
5. **Dependency manifests** — check for: `gradle/libs.versions.toml`, `build.gradle.kts` (root), `settings.gradle.kts`, `package.json`, `requirements.txt`, `Cargo.toml`, `go.mod`, `pyproject.toml`. Read whichever exist.
6. **Build properties** — check for: `gradle.properties`, `.env.example`, `Makefile`. Read whichever exist.
7. **Test scripts** — check for: `scripts/test*`, test-related entries in `Makefile` or `package.json` scripts. Record discovered test commands.
8. **Lint configuration** — check for: `.ruff.toml`, `.eslintrc*`, `detekt.yml`, `.editorconfig`, `ktlint` in CI, `biome.json`. Record discovered lint commands.
9. **Project skills** — `find . -path "*/skills/*/SKILL.md" -not -path "*/node_modules/*"` — read first 10 lines of each to build a skill index (name, description, when-to-use).
10. **Memory index** — if `MEMORY.md` exists (in project root or `~/.claude/projects/*/memory/`), read the index.
11. **Git state** — capture:
    - `git branch --show-current`
    - `git log --oneline -10`
    - `gh pr list --state open` (or equivalent GitHub MCP call)
12. **Module structure** — `find . -name "src" -type d -not -path "*/node_modules/*" -not -path "*/.gradle/*" | head -20` — map modules and source sets.
13. **Test infrastructure** — scan for test base classes, fakes directories, fixture files, test utilities. Record locations.

Store all loaded context as the **foundation context object**. This is passed to every subagent in subsequent phases.

### Step 1 — Fetch Issues

For each issue number in the input list:

1. Fetch via `gh issue view <N> --json title,body,labels,state,number` or GitHub MCP `issue_read`.
2. Store: number, title, body, labels, state, linked PRs.
3. Deduplicate — if the same issue number appears twice, keep one copy.
4. Validate — all issues must be from the current repository. If any issue returns an error, report it and remove from the list.

### Step 2 — Parse Dependencies

Scan each issue body for dependency references using these patterns:

**Hard dependencies** (creates a DAG edge):
- `depends on #(\d+)`
- `blocked by #(\d+)`
- `after #(\d+)`
- `prerequisite.*#(\d+)`
- `must land.*#(\d+)`

**Structural references** (creates a DAG edge):
- `spun out from #(\d+)`
- `part of #(\d+)`
- `sub-issue of #(\d+)`

**Supersedes** (informational, no edge):
- `replaces #(\d+)`
- `supersedes #(\d+)`

**Parent tracking** (informational, no edge):
- `tracks #(\d+)`

All pattern matching is case-insensitive.

### Step 3 — Classify Dependencies

For each detected reference:

| Classification | Condition | Effect |
|----------------|-----------|--------|
| **Internal dep** | Both issues are in the input list | Creates a DAG edge (referenced issue must complete first) |
| **External blocker** | Referenced issue is NOT in the input list AND is still OPEN | Marks current issue as externally blocked |
| **Resolved** | Referenced issue is CLOSED | No constraint |

To check if a referenced issue is open/closed: `gh issue view <N> --json state` or GitHub MCP.

### Step 4 — Cleanness Check

For each issue, check:

1. **Existing pipeline PR** — `gh pr list --search "closes #N"` (or equivalent). Filter results to PRs whose head branch starts with `pipeline/`. Only `pipeline/`-prefixed PRs trigger a skip. Non-pipeline PRs (from manual work) are ignored.
2. **Already closed** — if issue state is `closed`, skip.
3. **Externally blocked** — if classified as externally blocked in Step 3, skip.
4. **Already labeled** — if issue has `pipeline/awaiting-merge` label, skip (handled in a previous run).

For each skip, record the reason.

### Step 5 — Build DAG

From internal dependencies (Step 3), build a directed acyclic graph:
- Nodes = actionable issues (not skipped)
- Edges = "issue A must complete before issue B"

**Cycle detection**: run a topological sort. If a cycle is detected, report the cycle (list the issue numbers involved) and **stop the pipeline**. Do not process any issues.

### Step 6 — Sort into Chains

1. Find connected components in the DAG. Each component becomes a **chain**.
2. Within each chain, topological sort determines execution order.
3. Issues with no edges (no dependencies, nothing depends on them) become **standalone** issues (single-issue chains).

### Step 7 — Order Chains

Heuristic for which chains to execute first:

1. **CI/build chains** — issues touching CI config, build scripts, dependency updates. Lowest risk, no app code changes.
2. **Test infrastructure chains** — issues adding test utilities, fixtures, or base classes.
3. **Feature/UI chains** — issues adding or modifying functionality.
4. **Standalone issues** — fill gaps between chains, ordered by issue number.

Within a priority tier, shorter chains go first.

### Step 8 — Risk Evaluation

Evaluate each chain and assign a risk level:

| Risk | Criteria |
|------|----------|
| **LOW** | 1-2 issues, distinct files, trivial/small scope |
| **MEDIUM** | 3+ issues, some file overlap, or medium scope |
| **HIGH** | 4+ issues, significant file overlap, large scope, or tightly coupled changes |

Factors:
- **Chain length**: more links = more cascade risk
- **File overlap**: do issues in the chain touch the same files?
- **Aggregate scope**: a chain of 3 trivial issues is safer than a chain of 3 large ones
- **Coupling**: loosely coupled (just ordered by dependency) vs tightly coupled (each builds on previous code)

### Phase 0 Output

Produce and display the execution plan:

```
Execution Plan:
  Foundation context: loaded (N sources, ~M tokens)
  Chains:
    Chain 1: [#365, #345] — base: main, risk: LOW
    Chain 2: [#310, #311, #312] — base: main, risk: MEDIUM
  Standalone: [#306, #293, #340, #214]
  Skipped:
    #372: existing pipeline PR #394
    #102: externally blocked by #99 (open)
```

Each chain starts from `main`. Within a chain, each issue branches from the previous issue's branch (stacked PRs). Standalone issues branch directly from `main`.

### Triage Subagent

For projects with many issues, dispatch Phase 0 Steps 1-8 to a triage subagent using the prompt template at `triage-prompt.md`. Pass it:
- The list of issue numbers
- The foundation context from Step 0

The main agent loads foundation context (Step 0) directly, then delegates the rest to the triage subagent.

---

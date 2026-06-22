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

## Per-Issue Execution Loop

After Phase 0 produces the execution plan, process issues in execution order:

1. **Chains first**, in the order determined by Phase 0 Step 7.
2. Within each chain, issues execute in topological order (dependencies before dependents).
3. **Standalone issues** after all chains complete.

For each issue:

1. Create a task for tracking: `TaskCreate` with subject `Pipeline: #NNN — <issue title>`.
2. Set task to `in_progress`.
3. Execute Phases 1 through 9.
4. On completion: set task to `completed`.
5. On skip: update task description with skip reason, set to `completed`.
6. On failure: update task description with failure details, set to `completed`.

The task list is the pipeline's state tracker. At any point, the task list shows which issues are done, in progress, or pending.

### Branch Base Selection

- **First issue in a chain** — branch from `main` (or the project's default branch).
- **Subsequent issues in a chain** — branch from the previous issue's branch (stacked PRs).
- **Standalone issues** — branch from `main`.

---

## Phase 1: Assess

Runs at the start of each issue. Verifies the issue is still actionable against the current codebase.

### Step 1 — Re-read Issue

Read the full issue body, not just the Phase 0 summary. Identify:
- Acceptance criteria (explicit or implied)
- Open questions or ambiguities
- Referenced files, classes, or APIs
- Referenced PRs (especially recently merged ones)

### Step 2 — Codebase Verification

For each file, class, or API mentioned in the issue body:
- Does it still exist? (`find` or `grep` for it)
- Is the described problem still present?
- Are the proposed changes still compatible with current code?

If the issue is in a chain and branches from a previous issue's branch, verify against that branch (not `main`).

### Step 3 — Scope Estimation

Classify the issue:

| Scope | Criteria |
|-------|----------|
| **Trivial** | Single file, <50 lines changed, no new tests needed |
| **Small** | 2-5 files, <200 lines, straightforward tests |
| **Medium** | 5-15 files, new module or significant refactor |
| **Large** | 15+ files, architectural changes, multiple test types |

### Step 4 — Skill Mapping

Match anticipated changed file paths against the skill index built in Phase 0 (Step 0.9). For each skill in the index, check if its description or when-to-use keywords relate to the files this issue will touch.

Do NOT hardcode file-pattern-to-skill mappings. The skill index is dynamic — use the name and description from each skill's frontmatter to determine relevance.

Record all matched skills for loading in Phase 3.

### Step 5 — On-Demand Context Loading

Three sources of additional context:

**Keyword matching:**
- Scan issue title and body for keywords that match architecture docs, specs, plans, or memory entries loaded in Phase 0.
- Load matching files from `docs/superpowers/specs/`, `docs/superpowers/plans/`, or equivalent.
- Load matching memory files if the memory index suggests relevance.

**File path extraction:**
- Parse the issue body for explicit file path references: patterns like `src/...`, `app/...`, `*.kt`, `*.py`, backtick-quoted paths.
- Read each referenced file directly — this is more reliable than keyword heuristics.

**Recently merged context:**
- If the issue body references merged PRs (`PR #NNN`, `#NNN merged`, `landed in #NNN`), read their diffs via `gh pr diff <N>` to understand what recently changed.
- This provides the "previous session state" that manual prompts carry but automated pipelines normally miss.

### Step 6 — Risk Assessment

Check each risk factor:

| Factor | Check |
|--------|-------|
| Security-sensitive code | Does this touch encryption, auth, credential storage, network config? |
| Public API surface | Does this change interfaces, exported functions, or API contracts? |
| CI/CD configuration | Does this modify build scripts, workflows, or deploy config? |
| Existing tests | Could this break existing tests? Check for tests covering modified files. |

### Step 7 — Scope Exclusion

Identify related issues referenced in the body that are NOT in the pipeline input list. Record them explicitly as **out of scope** to prevent implementation drift.

Examples:
- "Follow-up issue #108 exists but is separate" → record #108 as excluded.
- "See also #200 for the UI side" → record #200 as excluded.
- "This is part of the work tracked in #50" → record #50 as excluded (unless #50 is in the input list).

These exclusions are passed to implementation subagents so they don't accidentally address related-but-separate work.

### Assessment Output

```
Assessment:
  codebase_still_matches: true/false
  scope: trivial | small | medium | large
  files_to_modify: [list of file paths]
  skills_needed: [list of skill names]
  on_demand_context_loaded: [list of additional docs loaded]
  risks: [list of identified risks]
  blockers_found: [list of unexpected blockers]
  out_of_scope: [list of related issue numbers explicitly excluded]
```

### Decision Point

If `codebase_still_matches = false` and the divergence is significant (the core premise of the issue is invalid):
1. Mark issue as **skip** with reason "Issue outdated — codebase has diverged."
2. Post a comment on the issue explaining what changed.
3. Move to the next issue.

Minor divergences (e.g., a file was renamed but the concept is the same) do not trigger a skip — note the change and adapt.

---

## Phase 2: Update Issue

Leaves a trail on the GitHub issue showing the pipeline assessed it.

### Step 1 — Post Assessment Comment

Post a comment on the GitHub issue using the template at `templates/assessment-comment.md`. Fill in all `{{placeholder}}` fields from the assessment output.

Use `gh issue comment <N> --body "<comment>"` or GitHub MCP `add_issue_comment`.

### Step 2 — Update Issue Body

Only if factual inaccuracies were found during assessment (e.g., a file path changed, a class was renamed). Append a note at the end of the issue body — do NOT rewrite the original content.

If nothing needs correction, skip this step.

### Step 3 — Add Label

Add the `pipeline/in-progress` label to the issue.
- If the label doesn't exist in the repository, create it first (color: `#0E8A16`, description: "Issue being processed by the pipeline").
- Use `gh issue edit <N> --add-label "pipeline/in-progress"` or GitHub MCP.

---

## Phase 3: Plan

Creates a detailed implementation plan before any code is written.

### Step 1 — Load Relevant Skills

Read the full content of every `SKILL.md` identified in Phase 1 Step 4 (skill mapping). These inform the plan's approach — the plan should follow the patterns and conventions documented in the matched skills.

### Step 2 — Generate Plan

If your environment has a `writing-plans` skill, invoke it. Otherwise, generate a plan covering all of the following areas:

**File-by-file changes:**
- What changes in each existing file, why, and the order of changes.
- Group related changes so the reviewer can follow the logic.

**New files:**
- Any new files to create, with their purpose and location.
- Follow existing module structure and naming conventions from the foundation context.

**Test strategy:**
- Which tests to add or modify.
- Where they go (language/framework-specific test directories discovered in Phase 0).
- What they cover (unit, integration, UI, screenshot).
- Whether tests are KMP-compatible or platform-specific.

**Build/CI changes:**
- If the issue touches dependency manifests, build config, or CI workflows.
- Specific changes needed and compatibility concerns.

**Migration/compatibility:**
- Any data migration, API changes, or backward compatibility concerns.
- If none, explicitly state "No migration needed."

### Step 3 — Save Plan

Write the plan to a file:
- Path pattern: `docs/superpowers/plans/YYYY-MM-DD-issue-NNN-<slug>.md`
- `<slug>` is derived from the issue title (lowercase, hyphens, no special characters, max 40 chars).
- Use today's date.

If the `docs/superpowers/plans/` directory doesn't exist, use whatever plans directory exists in the project, or create one at a sensible location.

### Step 4 — Validate Plan Against Scope

Cross-check the plan against the Phase 1 assessment:
- Does the plan touch files not mentioned in the assessment's `files_to_modify` list?
  - If yes and the additions are legitimate (the assessment missed them): update the assessment.
  - If yes and the additions are overreach (the plan goes beyond the issue): trim the plan.
- Does the plan address the issue's acceptance criteria?
- Does the plan accidentally address any issue in the `out_of_scope` list? If so, remove that work from the plan.

### Decision Point

If the plan reveals the issue is significantly more complex than estimated (e.g., assessed as Small but plan requires 15+ files), log a warning but proceed. The plan review will catch overreach.

---

## Phase 3.5: Plan Review

Reviews the implementation plan against architecture rules and project conventions **before any code is written**. Catching mistakes here is far cheaper than after implementation.

### Dispatch Plan Reviewer

Dispatch a subagent using the prompt template at `plan-reviewer-prompt.md`. Pass it:
- The plan file content
- The foundation context (architecture docs, service contracts, project instructions)
- The assessment report (so it knows the intended scope)

### Finding Format

The reviewer produces structured findings:

```
Finding:
  severity: critical | warning | info
  plan_section: "<section heading where the issue was found>"
  rule: "<rule name and source>"
  description: "<what's wrong>"
  suggestion: "<how to fix it>"
```

### Decision Point

| Findings | Action |
|----------|--------|
| **Critical findings > 0** | Revise the plan to address each critical finding. Re-submit for review. Max 2 revision attempts. |
| **Still critical after 2 revisions** | **Chain failure** — the issue's requirements may be incompatible with the architecture. Stop the chain. |
| **Warnings only** | Note them and carry forward to implementation. Phase 5 will re-check the actual code. |
| **No findings** | Proceed to Phase 4. |

### Revision Loop

When revising the plan:
1. Read each critical finding carefully.
2. Modify the plan to address the finding.
3. Update the plan file on disk.
4. Re-dispatch the plan reviewer with the updated plan.
5. If the revision introduces new criticals, count this as one of the 2 attempts.

---

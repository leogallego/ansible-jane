# Spec & Design Documents Archive

This is an automated archive of specification and design documents from
`main`. Maintained by the `archive-specs.yml` GitHub Actions workflow.

**Do not commit directly to this branch.**

## Archived directories

| Path | Contents |
|------|----------|
| `specs/` | Speckit feature specs (spec.md, plan.md, tasks.md, etc.) |
| `docs/specs/` | Standalone design specifications |
| `docs/superpowers/specs/` | Brainstorming design documents |
| `docs/superpowers/plans/` | Implementation plans from brainstorming |

## How it works

1. A push to `main` touching any directory above triggers the workflow.
2. Spec files are copied here preserving directory structure.
3. Spec files are removed from `main` with a `[skip ci]` commit.
4. Release tags are cut from `main`, so specs never appear in releases.

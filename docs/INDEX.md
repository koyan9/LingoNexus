# LingoNexus Documentation Index

> Updated: 2026-03-09  
> Purpose: provide a single navigation page for the current documentation set.

## Start Here

If you are new to the repository, read in this order:

1. `README.md` or `README.zh-CN.md`
2. `docs/quick-start.md`
3. `docs/architecture.md`
4. `docs/diagnostics.md`
5. `docs/todo-plan.md`

## Core Runtime Docs

| File | Purpose |
| --- | --- |
| `docs/architecture.md` | current architecture baseline, module layout, execution flow, and roadmap |
| `docs/quick-start.md` | practical integration guide for Spring Boot and standalone usage |
| `docs/requirements.md` | current product and engineering requirements |
| `docs/diagnostics.md` | runtime diagnostics and metadata usage guidance |

## Build and Verification

| File | Purpose |
| --- | --- |
| `docs/build-troubleshooting.md` | verified Windows build flow, repository bootstrap notes, and troubleshooting guidance |
| `scripts/BUILD_PRESETS.md` | generated static reference for build modes, focused profiles, and common verification commands |
| `scripts/generate-build-presets-doc.ps1` | regenerates `scripts/BUILD_PRESETS.md` from the current preset definitions |
| `scripts/verified-build.ps1` | main Windows-friendly verification entrypoint with mode/profile/help support |

## Project Status and Planning

| File | Purpose |
| --- | --- |
| `docs/project-status.md` | subsystem maturity snapshot, current risks, and latest implementation posture |
| `docs/release-notes-2026-03-09.md` | draft release/update summary for the current repository import and cleanup wave |
| `docs/todo-plan.md` | follow-up roadmap and prioritized tasks |

## Performance and Optimization

| File | Purpose |
| --- | --- |
| `docs/performance-baseline.md` | current benchmark focus and baseline notes, including Janino cache-identity baselines |
| `scripts/run-performance-baselines.ps1` | runs selected baseline tests and generates local/CI performance reports on demand |

## Which Docs Are Source-of-Truth?

For current implementation truth, prefer these files first:

- `docs/architecture.md`
- `README.md`
- `README.zh-CN.md`
- `docs/quick-start.md`
- `docs/project-status.md`
- `docs/todo-plan.md`

When a design note or dated analysis conflicts with one of the files above, treat the source-of-truth set as authoritative.

## Maintenance Rule

Whenever the public API, integration flow, isolation modes, or major architecture changes, update these files together:

- `README.md`
- `README.zh-CN.md`
- `docs/quick-start.md`
- `docs/architecture.md`
- `docs/project-status.md`
- `docs/INDEX.md`

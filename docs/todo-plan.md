# TODO Plan

> Updated: 2026-03-10  
> Purpose: define the next development steps after the current baseline.

## Current Position

The repository already provides a stable baseline:

- core runtime is present
- language sandboxes are present
- Spring Boot integration is present
- Windows-friendly verification tooling is present
- public docs are aligned to the core reader path

The next phase focuses on **stabilization first**, followed by measured capability growth.

## Milestone Overview

| Milestone | Priority | Goal | Reference |
| --- | --- | --- | --- |
| M1 External-Process Stability | P0 | tighten compatibility and ensure failure metadata stays stable across request/worker boundaries | `docs/milestone-m1-external-process-stability.md` |
| M2 Diagnostics Clarity | P1 | improve operator-facing diagnostics examples and interpretation guidance | `docs/milestone-m2-diagnostics-clarity.md` |
| M3 Hot-Path Optimization | P1 | reduce direct-path metadata allocation and preparation overhead | `docs/milestone-m3-hot-path-optimization.md` |
| M4 Performance + Hardening | P2 | extend baseline comparisons and document longer-horizon hardening targets | `docs/milestone-m4-performance-hardening.md` |

## M1 External-Process Stability (P0)

Goal: stabilize `EXTERNAL_PROCESS` behavior without changing public usage patterns.

In scope:

- compatibility and error readability improvements
- worker-side reconstruction failure classification
- focused regression tests on `errorStage` / `errorComponent` / `errorReason`

Issue drafts:

- `.github/issue-drafts/m1/01-external-process-compatibility-polish.md`
- `.github/issue-drafts/m1/02-worker-side-compatibility-metadata.md`

Out of scope:

- new language support
- transport protocol redesign
- large performance refactors

Progress note (2026-03-10): M1 regression coverage now includes worker-pool shutdown/eviction, protocol negotiation mismatch, and descriptor JSON-safe failure cases; remaining focus is broader request/worker boundary metadata and recovery edge cases.

## M2 Diagnostics Clarity (P1)

Goal: make runtime diagnostics easier to apply in operator workflows.

Issue drafts:

- `.github/issue-drafts/m2/01-diagnostics-examples-refresh.md`

## M3 Hot-Path Optimization (P1)

Goal: reduce avoidable allocations on the direct execution path.

Issue drafts:

- `.github/issue-drafts/m3/01-direct-path-metadata-allocation.md`

## M4 Performance + Hardening (P2)

Goal: extend baseline comparisons and record longer-horizon hardening targets.

Issue drafts:

- `.github/issue-drafts/m4/01-performance-baseline-extension.md`

## Legacy Reference

The earlier versioned patch-plan is kept for historical context:

- `docs/milestone-v0.1.1.md`
- `.github/issue-drafts/v0.1.1/`

## Ongoing Rule

Whenever the public API, build workflow, isolation behavior, or onboarding flow changes, update these files together:

- `README.md`
- `README.zh-CN.md`
- `docs/INDEX.md`
- `docs/quick-start.md`
- `docs/build-troubleshooting.md`
- `docs/project-status.md`
- `docs/todo-plan.md`

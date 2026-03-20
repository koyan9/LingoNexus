# TODO Plan

> Updated: 2026-03-19  
> Purpose: define the next development steps after the current baseline.

## Current Position

The repository already provides a stable baseline:

- core runtime is present
- language sandboxes are present
- Spring Boot integration is present
- Windows-friendly verification tooling is present
- public docs are aligned to the core reader path

The next phase focuses on **stabilization first**, followed by measured capability growth.

Current execution rule for milestone work:

- use reactor-aware targeted commands such as `mvn -pl <module> -am ...`
- treat `scripts/verified-build.ps1` as the wider Windows verification path
- do not interpret a downstream module run without `-am` as a product regression unless upstream artifacts are already known-good in the active local repository

## Milestone Overview

| Milestone | Priority | Status | Goal | Reference |
| --- | --- | --- | --- | --- |
| M1 External-Process Stability | P0 | in progress | tighten compatibility and ensure failure metadata stays stable across request/worker boundaries | `docs/milestone-m1-external-process-stability.md` |
| M2 Diagnostics Clarity | P1 | in progress | improve operator-facing diagnostics examples and interpretation guidance | `docs/milestone-m2-diagnostics-clarity.md` |
| M3 Hot-Path Optimization | P1 | in progress | reduce direct-path metadata allocation and preparation overhead | `docs/milestone-m3-hot-path-optimization.md` |
| M4 Performance + Hardening | P2 | in progress | extend baseline comparisons and document longer-horizon hardening targets | `docs/milestone-m4-performance-hardening.md` |

Recommended order:

1. keep the validation baseline green
2. finish M1 classification + recovery coverage
3. refresh M2 diagnostics/operator guidance against the now-stable failure model
4. land M3 hot-path reductions without metadata regressions
5. refresh M4 reports and longer-horizon hardening targets after M3 measurements are available

## M1 External-Process Stability (P0)

Goal: stabilize `EXTERNAL_PROCESS` behavior without changing public usage patterns.

Current status:

- request-side compatibility coverage is already present
- worker-pool shutdown/eviction and negotiation mismatch regression coverage is already present
- remaining work is broader request/worker metadata consistency and recovery-edge classification

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

Done when:

- request-side and worker-side compatibility failures use stable `errorStage` / `errorComponent` / `errorReason` values
- recovery-oriented failures stay distinguishable across request validation, protocol negotiation, worker borrow, and worker execution
- focused regression tests pass from the downstream testcase module with `-am`

Recommended validation:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am -Dtest=ExternalProcessCompatibilityTest,ProtocolNegotiationFeatureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## M2 Diagnostics Clarity (P1)

Goal: make runtime diagnostics easier to apply in operator workflows.

Current status:

- `docs/diagnostics.md` already documents the current API surface and major failure families
- remaining work is tightening reader flow, example emphasis, and operator decision guidance after M1 reason-code stability is locked

Issue drafts:

- `.github/issue-drafts/m2/01-diagnostics-examples-refresh.md`

Done when:

- diagnostics examples match the current `EngineDiagnostics` / `ExternalProcessStatistics` surface
- reason-code interpretation reflects the stabilized M1 failure model
- the main reader path points operators from failure symptoms to the right diagnostics fields quickly

Recommended validation:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am -Dtest=EngineDiagnosticsFeatureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## M3 Hot-Path Optimization (P1)

Goal: reduce avoidable allocations on the direct execution path.

Current status:

- direct-path optimization scope is intentionally narrow
- active metadata-policy/category handling work in `DefaultScriptExecutor` should be treated as part of this hot-path review, not as a separate redesign track
- in-process execution preparation has already dropped one intermediate merge/copy step; remaining work is around result-metadata handling and other small direct-path allocations
- `DefaultScriptExecutor` now also pre-resolves result-metadata category flags into one execution-time plan; remaining work should stay focused on similarly small, semantics-preserving reductions
- default metadata-plan and isolation-mode derived values are now cached in `DefaultScriptExecutor`; remaining work should continue to favor cached/static execution-path decisions over repeated per-call derivation

Issue drafts:

- `.github/issue-drafts/m3/01-direct-path-metadata-allocation.md`

Done when:

- direct execution avoids redundant metadata/category preparation work
- result metadata content remains stable for existing profiles and category overrides
- large-context baseline tests remain green after the change

Recommended validation:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am -Dtest=LargeContextPerformanceBaselineTest,LargeContextIsolationModeComparisonBaselineTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## M4 Performance + Hardening (P2)

Goal: extend baseline comparisons and record longer-horizon hardening targets.

Current status:

- the baseline suite and report workflow already exist
- a post-M3 measurement refresh was recorded on 2026-03-19 and is now the active baseline reference
- the active suite now includes direct-path metadata-profile, context-source, module-usage, and failure-diagnostics comparisons

Issue drafts:

- `.github/issue-drafts/m4/01-performance-baseline-extension.md`

Done when:

- the benchmark suite and report workflow described in docs match the tests/scripts that are actually used
- comparison guidance covers direct, isolated-thread, and external-process paths
- longer-horizon hardening targets are captured separately from immediate M1-M3 work

Recommended validation:

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/run-performance-baselines.ps1 -SkipVerifiedBuild
```

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
- `docs/diagnostics.md`
- `docs/performance-baseline.md`

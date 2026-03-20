# M3 Hot-Path Optimization

> Updated: 2026-03-19  
> Purpose: reduce avoidable allocations on the direct execution path.

## Status

Scoped.

## Milestone Goal

Reduce hot-path overhead without changing public behavior.

Success means:

- direct execution avoids redundant metadata creation
- request preparation and metadata handling are leaner
- no regressions in result metadata output

Current progress:

- scope is intentionally limited to small, safe reductions around direct execution
- current metadata policy/category handling work in `DefaultScriptExecutor` should be treated as part of this milestone's measurement boundary
- the large-context baseline already exists and should be reused instead of inventing a new benchmark family first
- in-process execution preparation now avoids building an intermediate merged variable `HashMap` before constructing the execution `ScriptContext`
- direct execution now also pre-resolves result-metadata category flags into one execution-time plan instead of repeating `EnumSet.contains(...)` checks across the hot path
- `DefaultScriptExecutor` now caches default metadata-plan and isolation-mode derived values instead of rebuilding them on every execution

## Issue Drafts

- `.github/issue-drafts/m3/01-direct-path-metadata-allocation.md`

## In Scope

- remove redundant metadata allocation in `DefaultScriptExecutor`
- identify small, safe reductions in direct-path allocation
- reduce avoidable category parsing / metadata initialization / merge-copy work when the direct path does not need it

## Out of Scope

- large-scale performance refactors
- isolation-mode behavior changes
- new public metadata controls

## Validation

- run direct-path focused baselines:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am -Dtest=LargeContextPerformanceBaselineTest,LargeContextIsolationModeComparisonBaselineTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- run full `mvn test` at milestone completion

# perf: reduce direct-path metadata allocation

## Summary

Reduce avoidable metadata allocation in the direct execution path without changing result behavior.

## Why

Direct execution runs on the hot path; redundant metadata creation adds overhead under load.

## Scope

- remove redundant metadata initialization in `DefaultScriptExecutor`
- keep result metadata output unchanged

## Done When

- direct-path metadata allocation no longer duplicates work
- no changes to public result metadata content

## Validation

- run direct-path focused tests

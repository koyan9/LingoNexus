# M3 Hot-Path Optimization

> Updated: 2026-03-10  
> Purpose: reduce avoidable allocations on the direct execution path.

## Milestone Goal

Reduce hot-path overhead without changing public behavior.

Success means:

- direct execution avoids redundant metadata creation
- request preparation and metadata handling are leaner
- no regressions in result metadata output

## Issue Drafts

- `.github/issue-drafts/m3/01-direct-path-metadata-allocation.md`

## In Scope

- remove redundant metadata allocation in `DefaultScriptExecutor`
- identify small, safe reductions in direct-path allocation

## Out of Scope

- large-scale performance refactors
- isolation-mode behavior changes

## Validation

- run direct-path focused tests
- run full `mvn test` at milestone completion

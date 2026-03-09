# test: extend external-process failure metadata regression coverage

## Summary

Extend regression coverage for structured external-process failure metadata so key failure paths continue to assert `errorStage`, `errorComponent`, and `errorReason` directly.

## Why

The current baseline depends on structured failure metadata for diagnostics and troubleshooting. `v0.1.1` should lock that behavior down more explicitly.

## Scope

- add or extend tests for request-side payload incompatibility
- add or extend tests for worker-side execution failure metadata
- add or extend tests for custom extension incompatibility classification

## Done When

- key external-process failure paths have direct metadata assertions
- testcase coverage includes both request-side and worker-side failures relevant to `v0.1.1`

## Checklist

- [ ] add request-side incompatibility metadata assertions where coverage is still thin
- [ ] add worker execution failure metadata assertions where useful
- [ ] add custom extension incompatibility classification assertions
- [ ] verify no flaky behavior is introduced in focused test runs

## Out of Scope

- broad testcase restructuring
- unrelated benchmark/stress additions

## Validation

- run focused external-process feature tests
- verify failures still classify consistently after any wording changes


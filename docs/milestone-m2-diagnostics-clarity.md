# M2 Diagnostics Clarity

> Updated: 2026-03-19  
> Purpose: strengthen operator-facing diagnostics examples and interpretation guidance.

## Status

In progress.

## Milestone Goal

Make runtime diagnostics easier to apply during troubleshooting and monitoring.

Success means:

- `docs/diagnostics.md` examples reflect the current API
- external-process failure reason guidance is clear and actionable
- documentation stays aligned with the current implementation surface

Current progress:

- `docs/diagnostics.md` already covers the current diagnostics objects and major external-process reason families
- example and interpretation coverage is being tightened against the stabilized M1 failure model rather than expanded with new APIs
- the runnable diagnostics example is being updated to demonstrate successful execution plus request-side and worker-side failure families

## Issue Drafts

- `.github/issue-drafts/m2/01-diagnostics-examples-refresh.md`

## In Scope

- example updates for `EngineDiagnostics` / `ExternalProcessStatistics`
- clearer interpretation notes for failure reasons and counts
- doc alignment checks across the core reader path
- operator guidance that tells readers which diagnostics field to inspect first for each failure family

## Out of Scope

- new diagnostics APIs or metrics streaming
- major refactors of statistics collection

## Done When

- `docs/diagnostics.md` matches the current API surface and failure families
- example guidance remains consistent with `DiagnosticsExample` and diagnostics-related feature tests
- docs help an operator distinguish payload, handshake, borrow, and worker-side failures quickly

## Validation

- review docs against actual API signatures
- run focused diagnostics coverage:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am -Dtest=EngineDiagnosticsFeatureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- run full `mvn test` at milestone completion

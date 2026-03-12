# M2 Diagnostics Clarity

> Updated: 2026-03-10  
> Purpose: strengthen operator-facing diagnostics examples and interpretation guidance.

## Milestone Goal

Make runtime diagnostics easier to apply during troubleshooting and monitoring.

Success means:

- `docs/diagnostics.md` examples reflect the current API
- external-process failure reason guidance is clear and actionable
- documentation stays aligned with the current implementation surface

## Issue Drafts

- `.github/issue-drafts/m2/01-diagnostics-examples-refresh.md`

## In Scope

- example updates for `EngineDiagnostics` / `ExternalProcessStatistics`
- clearer interpretation notes for failure reasons and counts
- doc alignment checks across the core reader path

## Out of Scope

- new diagnostics APIs or metrics streaming
- major refactors of statistics collection

## Validation

- review docs against actual API signatures
- run full `mvn test` at milestone completion

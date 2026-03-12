# docs: refresh diagnostics examples and operator guidance

## Summary

Align `docs/diagnostics.md` with the current diagnostics surface and expand guidance for external-process failure interpretation.

## Why

Diagnostics are now richer than earlier docs; the guidance should remain practical and aligned with real output.

## Scope

- update example usage for `EngineDiagnostics` and `ExternalProcessStatistics`
- keep the reason code list and interpretation guide aligned with current behavior

## Done When

- diagnostics examples compile against the current API surface
- external-process reason codes and interpretation notes are up to date
- no doc drift is introduced in the core reader path

## Validation

- manual doc review against current API

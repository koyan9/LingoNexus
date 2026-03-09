# fix: polish external-process compatibility edge cases

## Summary

Polish small `EXTERNAL_PROCESS` compatibility edge cases so failures remain readable and structurally classified without changing public usage patterns.

## Why

`v0.1.1` is a patch release focused on stabilization. External-process compatibility is one of the highest-value stability areas because request/response boundary failures are user-visible and operationally important.

## Scope

- review request-side compatibility paths in `ExternalProcessExecutionRequestFactory`
- review response-side compatibility and fallback behavior in the external worker path
- improve wording only where it makes operator-facing failures clearer
- keep the current public API and isolation semantics unchanged

## Done When

- request/response JSON-safe boundary messages are specific and readable
- custom `SecurityPolicy` and `ScriptModule` incompatibility messages are consistent with the current classification model
- no regression is introduced to `errorStage`, `errorComponent`, or `errorReason`

## Checklist

- [ ] review request-side compatibility checks and current error messages
- [ ] review response-side compatibility/fallback paths
- [ ] tighten wording for ambiguous compatibility failures if needed
- [ ] verify stage/component/reason metadata remain stable

## Out of Scope

- new transport protocol support
- broad payload-model redesign
- breaking public API changes

## Validation

- re-run focused external-process compatibility tests
- confirm no new doc drift is introduced in diagnostics/build docs


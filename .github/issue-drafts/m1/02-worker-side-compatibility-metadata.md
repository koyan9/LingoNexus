# test/fix: ensure worker-side compatibility failures carry structured metadata

## Summary

Worker-side reconstruction failures should emit structured `errorStage`, `errorComponent`, and `errorReason` metadata so operators can distinguish request-side vs worker-side incompatibilities.

## Why

Request-side compatibility checks already emit structured diagnostics. When a failure happens inside the worker (for example during module or policy reconstruction), the metadata must stay equally stable.

## Scope

- classify worker-side `SecurityPolicy` reconstruction failures
- classify worker-side `ScriptModule` reconstruction failures
- add regression tests for the structured metadata

## Done When

- worker-side reconstruction failures populate `errorStage`, `errorComponent`, and `errorReason`
- tests cover both security policy and module reconstruction failure paths
- failures remain classified as `ExternalProcessCompatibilityException`

## Checklist

- [ ] add compatibility classification for worker-side reconstruction failures
- [ ] add regression tests for both security policy and module reconstruction failure cases
- [ ] update diagnostics docs to list the new reason codes

## Validation

- run external-process compatibility tests

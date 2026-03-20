# M1 External-Process Stability

> Updated: 2026-03-19  
> Purpose: stabilize `EXTERNAL_PROCESS` behavior without changing public usage patterns.

## Status

In progress.

## Milestone Goal

M1 focuses on compatibility and failure metadata stability across request/worker boundaries.

Success means:

- request-side compatibility failures are clearly classified
- worker-side reconstruction failures emit structured metadata
- no breaking changes to public API or integration flow

Current progress:

- request-side JSON-safe validation already emits structured metadata
- regression coverage already includes worker-pool shutdown/eviction behavior
- regression coverage already includes protocol negotiation mismatch behavior
- descriptor JSON-safe failure cases are already covered
- worker-returned structured failures now refresh the matching latest failure snapshot by stage instead of only contributing aggregated counts
- facade-level integration coverage now includes saturated-pool borrow-timeout diagnostics
- request-factory compatibility failures now contribute to aggregated diagnostics counts without being misclassified as borrow/worker failures
- remaining work is broader request/worker boundary consistency and recovery-edge classification

## Issue Drafts

- `.github/issue-drafts/m1/01-external-process-compatibility-polish.md`
- `.github/issue-drafts/m1/02-worker-side-compatibility-metadata.md`

## In Scope

- tighten JSON-safe boundary and descriptor error readability
- align compatibility failure reasons across request/worker execution
- add/extend regression tests for `errorStage`, `errorComponent`, and `errorReason`
- keep recovery-oriented failures distinguishable across request validation, protocol negotiation, worker borrow, and worker execution

## Implementation Tracks

### 1. Request-side compatibility

- keep request payload validation stable for variables, metadata, policy descriptors, and module descriptors
- preserve structured metadata such as `errorStage`, `errorComponent`, `errorReason`, and detail fields when available

### 2. Worker-side reconstruction

- keep `SecurityPolicy` and `ScriptModule` reconstruction failures classified as structured compatibility failures
- verify worker-side failures remain distinguishable from request-side validation failures

### 3. Boundary recovery and aggregation

- confirm worker borrow / negotiation / execution failures continue to update latest failure-reason snapshots and aggregated reason counts
- avoid collapsing boundary failures into generic exceptions when the failure family is already known

## Out of Scope

- new language support
- transport protocol replacement
- large performance refactors
- major Spring Boot feature expansion

## Done When

- stable `errorStage` / `errorComponent` / `errorReason` values are present for the main request/worker boundary failure families
- worker-side reconstruction failures remain classified as compatibility failures rather than generic execution failures
- focused regression tests pass from the downstream testcase module with `-am`

## Validation

- run focused compatibility and negotiation tests:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am -Dtest=ExternalProcessCompatibilityTest,ProtocolNegotiationFeatureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- run full `mvn test` at milestone completion

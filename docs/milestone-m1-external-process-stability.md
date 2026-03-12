# M1 External-Process Stability

> Updated: 2026-03-10  
> Purpose: stabilize `EXTERNAL_PROCESS` behavior without changing public usage patterns.

## Milestone Goal

M1 focuses on compatibility and failure metadata stability across request/worker boundaries.

Success means:

- request-side compatibility failures are clearly classified
- worker-side reconstruction failures emit structured metadata
- no breaking changes to public API or integration flow

## Issue Drafts

- `.github/issue-drafts/m1/01-external-process-compatibility-polish.md`
- `.github/issue-drafts/m1/02-worker-side-compatibility-metadata.md`

## In Scope

- tighten JSON-safe boundary and descriptor error readability
- align compatibility failure reasons across request/worker execution
- add/extend regression tests for `errorStage`, `errorComponent`, and `errorReason`

## Out of Scope

- new language support
- transport protocol replacement
- large performance refactors
- major Spring Boot feature expansion

## Validation

- run external-process compatibility tests
- run full `mvn test` at milestone completion

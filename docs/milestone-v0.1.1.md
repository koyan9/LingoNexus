# `v0.1.1` Milestone Breakdown

> Updated: 2026-03-09  
> Purpose: convert the `v0.1.1` patch target into a concrete, issue-ready milestone checklist.

## Milestone Goal

`v0.1.1` is a patch release focused on **stabilizing the current baseline** without changing public usage patterns.

Success for this milestone means:

- external-process edge cases are easier to diagnose
- structured failure metadata stays stable in key boundary scenarios
- build/verification tooling remains reliable and well documented
- no breaking public API or integration flow changes are introduced

## Milestone Scope

### In Scope

- small `EXTERNAL_PROCESS` compatibility fixes
- diagnostics and metadata clarity improvements
- focused regression tests for stability-sensitive paths
- documentation and verification workflow polish

### Out of Scope

- new language support
- public API redesign
- protocol-layer replacement
- large performance refactors
- major Spring Boot feature expansion

## Issue Candidates

Draft issue files:

- `.github/issue-drafts/v0.1.1/01-external-process-compatibility-polish.md`
- `.github/issue-drafts/v0.1.1/02-failure-metadata-regression-tests.md`
- `.github/issue-drafts/v0.1.1/03-diagnostics-examples-and-operator-guidance.md`
- `.github/issue-drafts/v0.1.1/04-verified-build-workflow-reliability-polish.md`
- `.github/issue-drafts/v0.1.1/05-core-doc-alignment-pass.md`

## 1. External-process compatibility polish

**Suggested issue title**  
`fix: polish external-process compatibility edge cases`

**Done when**

- request/response JSON-safe boundary messages remain specific and readable
- custom `SecurityPolicy` and `ScriptModule` incompatibility messages are consistent with the current classification model
- no regression is introduced to existing structured failure metadata fields

**Checklist**

- review current request-side compatibility paths in `ExternalProcessExecutionRequestFactory`
- review response-side compatibility/fallback behavior in the external worker path
- improve message wording only where it increases operator clarity
- verify that error stage/component/reason remain stable in these paths

## 2. Failure metadata stability regression tests

**Suggested issue title**  
`test: extend external-process failure metadata regression coverage`

**Done when**

- key failure paths have direct assertions on `errorStage`, `errorComponent`, and `errorReason`
- testcase coverage includes both request-side and worker-side failures relevant to `v0.1.1`

**Checklist**

- add/extend focused tests around request payload incompatibility
- add/extend focused tests around worker execution failure metadata
- add/extend focused tests around custom extension incompatibility classification

## 3. Diagnostics examples and operator guidance

**Suggested issue title**  
`docs: improve diagnostics examples for operators`

**Done when**

- `docs/diagnostics.md` remains aligned with the actual diagnostics surface
- examples clearly show how to inspect cache, thread pools, worker pools, and latest failure reasons
- build/verification docs still point readers to the right diagnostics entrypoints

**Checklist**

- review `docs/diagnostics.md` against the current public diagnostics API
- expand examples only where they add practical troubleshooting value
- keep examples concise and implementation-aligned

## 4. Verification workflow reliability polish

**Suggested issue title**  
`chore: harden verified-build workflow for patch release maintenance`

**Done when**

- `scripts/verified-build.ps1` still supports all current modes, profiles, and help/refresh commands
- `scripts/BUILD_PRESETS.md` can be regenerated cleanly
- no stale references remain between build docs and generated preset docs

**Checklist**

- smoke-test at least one mode path and one profile path after changes
- smoke-test `-ListModes`, `-ListProfiles`, and `-RefreshDocsOnly`
- regenerate `scripts/BUILD_PRESETS.md` if preset definitions change

## 5. Core doc alignment pass

**Suggested issue title**  
`docs: align core onboarding and build docs for v0.1.1`

**Done when**

- `README.md`, `README.zh-CN.md`, `docs/INDEX.md`, `docs/quick-start.md`, `docs/build-troubleshooting.md`, and `docs/todo-plan.md` remain mutually consistent
- no references remain to removed historical docs or generated local artifacts

**Checklist**

- re-check top-level reader path after any wording changes
- keep Chinese and English reader-path sections structurally aligned
- keep docs focused; avoid reintroducing historical analysis files into the main path

## Recommended Delivery Order

1. external-process compatibility polish
2. failure metadata regression tests
3. verification workflow reliability polish
4. diagnostics examples and operator guidance
5. core doc alignment pass

## Release Gate

Before cutting `v0.1.1`, verify that:

- the repository stays free of local build artifacts and verification caches
- the core reader path remains the compact documentation set listed in `docs/INDEX.md`
- at least one focused verification path is re-run successfully after the final changes
- `scripts/BUILD_PRESETS.md` matches the current preset definitions

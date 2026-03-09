# TODO Plan

> Updated: 2026-03-09  
> Purpose: define the next development steps after the first complete repository baseline.

## Current Position

The repository now has a stable baseline:

- core runtime is present
- language sandboxes are present
- Spring Boot integration is present
- Windows-friendly verification tooling is present
- public docs have been reduced to the core reader path

The next step is not broad expansion. It is **stabilization first**, followed by measured capability growth.

## `v0.1.1` Patch Target

Goal: stabilize the current baseline without changing public usage patterns.

Detailed issue-ready breakdown: `docs/milestone-v0.1.1.md`

### In Scope

- tighten `EXTERNAL_PROCESS` compatibility edge cases and error readability
- verify structured failure metadata remains stable across request/worker boundary scenarios
- improve diagnostics examples and small metadata/documentation gaps
- continue hardening `scripts/verified-build.ps1` and related preset/help/doc refresh paths
- keep `README.md`, `README.zh-CN.md`, `docs/INDEX.md`, and build docs aligned with the implemented workflow

### Candidate Tasks

- add small compatibility fixes for request/response JSON-safe boundary handling
- strengthen custom `SecurityPolicy` / `ScriptModule` incompatibility messages where needed
- add a few more focused tests around external-process failure metadata stability
- improve operator-facing examples for `EngineDiagnostics` and `ExternalProcessStatistics`
- clean any remaining small wording drift in onboarding/build docs

### Out of Scope

- new language support
- public API redesign
- transport protocol replacement
- large performance refactors
- major Spring Boot feature expansion

## `v0.2.0` Capability Target

Goal: expand core runtime capability without breaking the baseline.

### Focus Areas

- broaden `EXTERNAL_PROCESS` compatibility beyond the current descriptor-focused and JSON-safe-heavy path
- strengthen worker reuse / replacement / health-check recovery under broader scenarios
- improve operator-oriented diagnostics and failure interpretation
- reduce hot-path metadata allocation cost in direct execution
- formalize comparative performance baselines across isolation modes

### Candidate Tasks

- add structured pre-dispatch checks for more unsupported payload categories
- improve external worker recovery diagnostics and replacement visibility
- separate required result metadata from optional diagnostics metadata where useful
- extend Janino cache-identity and isolation-mode baseline coverage

## `v0.3.0` Maturity Target

Goal: move from a good developer baseline to a stronger operational baseline.

### Focus Areas

- evaluate protocol-layer improvements for external-process transport
- evaluate stronger worker lifecycle and scaling behavior
- improve production-facing guidance for Spring Boot and operator scenarios
- strengthen benchmark/report workflows and longer-running stress validation

### Candidate Tasks

- evaluate binary-safe transport options such as CBOR
- evaluate configurable worker prewarm / idle scaling strategies
- expand stress/performance comparisons across more real-world workloads
- improve release/process documentation for future version cadence

## Ongoing Rule

Whenever the public API, build workflow, isolation behavior, or onboarding flow changes, update these files together:

- `README.md`
- `README.zh-CN.md`
- `docs/INDEX.md`
- `docs/quick-start.md`
- `docs/build-troubleshooting.md`
- `docs/project-status.md`
- `docs/todo-plan.md`

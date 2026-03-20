# LingoNexus Project Status (2026-03-19)

> Purpose: current implementation snapshot, replacing the old build-out status draft.

## Overall Status

The repository is no longer a scaffold. It is a working script engine codebase with a stable main execution path. The current public facade is `LingoNexusExecutor`.

Current high-level status:

- core execution pipeline: available
- multi-language support: available
- Spring Boot integration: available
- runtime diagnostics: available and much richer than the earlier project stage
- external-process isolation: implemented, recovery-tested, and actively improving
- public docs: being aligned with the current codebase
- result-metadata policy/category handling: actively expanding and being hardened across core + Spring Boot configuration

## Subsystem Status

| Subsystem | Status | Notes |
| --- | --- | --- |
| API contracts | stable | public config, result, exception, statistics, and isolation contracts are in place |
| Core execution | stable and evolving | builder, executor, cache, preparation service, diagnostics, and external-process runtime are implemented |
| Groovy sandbox | available | one of the most mature language implementations |
| JavaScript sandbox | available | current implementation uses GraalJS |
| JavaExpr sandbox | available | supports expression-oriented scenarios |
| Java sandbox | available | Java script execution support is present |
| Kotlin sandbox | available | integrated, with continued cross-language validation still useful |
| Built-in modules | available | `math`, `str`, `date`, `json`, `col`, `convert`, `validator`, `formatter`, `codec` |
| Spring Boot starter | available | thin integration layer over the core runtime |
| Examples | available | 6 example classes in the current snapshot |
| Testcase (No Spring) | strong | about 80 test classes in the current snapshot |
| Testcase (Spring Boot) | basic but useful | about 8 test classes in the current snapshot |
| Docs | improving | milestone docs and build guidance are being converted from roadmap notes into executable engineering guidance |

## Recent Major Milestones

Recent major milestones visible in the current codebase include:

- unified lifecycle support via `LifecycleAware`
- per-engine thread-pool isolation instead of hidden singleton behavior
- unified isolation model: `AUTO`, `DIRECT`, `ISOLATED_THREAD`, `EXTERNAL_PROCESS`
- common isolation entrypoints shared across language sandboxes
- initial `EXTERNAL_PROCESS` implementation
- upgrade from one-shot process spawning to a persistent worker pool
- JSON length-prefixed worker protocol
- worker health checks, borrow/return validation, and prewarm support
- richer timing metadata and diagnostics output
- structured external-process failure reasons, latest failure snapshots, and aggregated failure-reason counts
- Janino-aware cache identity so Java scripts no longer incorrectly share cache entries across incompatible context type shapes

## Snapshot Notes

Approximate snapshot metrics from the current tree:

- 8 top-level Maven modules
- 5 script language submodules
- 2 testcase submodules
- 6 example classes
- about 99 testcase classes under `lingonexus-testcase`
- about 73 main Java files under `lingonexus-api`
- about 32 main Java files under `lingonexus-core`

These are point-in-time numbers for 2026-03-19 and may change later.

Latest performance baseline guidance is recorded in `docs/performance-baseline.md` (doc refreshed 2026-03-19; latest numeric snapshot refreshed 2026-03-19 21:51:43).

## Current Risks and Constraints

### Verification baseline

Focused verification is currently reliable when it keeps the reactor dependency graph attached:

- use `mvn -pl <downstream-module> -am ...` for targeted compile/test runs
- use `scripts/verified-build.ps1` when Windows local-repository state or broader module coverage needs to be stabilized

Recently re-verified commands:

```bash
mvn -q -pl lingonexus-api,lingonexus-core,lingonexus-spring-boot-starter -am -DskipTests compile
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am -Dtest=ExternalProcessCompatibilityTest,EngineDiagnosticsFeatureTest -Dsurefire.failIfNoSpecifiedTests=false test
mvn -q test -Dsurefire.failIfNoSpecifiedTests=false
```

Runs that target a downstream module without `-am` should not be treated as product regressions unless upstream artifacts are already known-good in the active local repository.

Focused `-pl ... -am` verification is still the recommended day-to-day loop, but the current working tree has also passed a repository-level root `mvn test` run in this environment.

### External-process compatibility

The main risk area is not whether external-process mode runs, but whether it behaves predictably across payload shapes and extension points.

Main gaps:

- non-JSON-safe values remain constrained
- custom `SecurityPolicy` support across workers still needs more compatibility work
- dynamic module reconstruction and mirroring can still be improved

### Diagnostics depth

Current diagnostics are now substantially more useful than before:

- latest protocol negotiation failure reason is exposed
- latest borrow failure reason is exposed
- latest worker execution failure reason is exposed
- aggregated failure-reason counts are exposed

The remaining gap is not the absence of these signals, but making them easier to operationalize and compare over time.

### In-progress integration changes

The current engineering wave is not limited to documentation:

- result-metadata category/policy handling is being hardened in `DefaultScriptExecutor`
- Spring Boot property binding is being expanded around sandbox selection and metadata controls

Milestone execution should assume these areas are active and integrate with them rather than bypassing them.

### Documentation drift

Legacy docs historically referenced:

- `ScriptEngineFacade`
- Rhino JavaScript execution
- outdated module planning
- large lists of files that were "still to be created"

This drift is being removed, but it remains a maintenance risk if not kept in sync.

## Best Next Steps

Recommended milestone order (aligned with `docs/todo-plan.md`):

1. keep the reactor-aware verification baseline green
2. M1: external-process compatibility + worker-side failure metadata stability
3. M2: diagnostics examples and operator guidance
4. M3: direct-path metadata allocation reduction
5. M4: performance baseline expansion and longer-horizon worker hardening

Progress note (2026-03-10): M1 regression coverage now includes worker-pool shutdown/eviction behavior and protocol negotiation mismatch cases; remaining focus is broader request/worker boundary metadata and recovery edge cases.

Current milestone posture:

- M1: in progress
- M2: in progress on the docs/example reader path
- M3: in progress with an initial in-process preparation-path allocation reduction landed
- M4: in progress with a post-M3 baseline refresh recorded on 2026-03-19

Recent M1 hardening additions in the current working tree:

- worker-returned structured failures now update the matching latest diagnostics snapshot by stage
- saturated-pool borrow-timeout behavior is covered through the public facade + diagnostics path
- request-factory compatibility failures now contribute to external-process aggregated diagnostics counts without borrowing a worker

Recent M3 hot-path additions in the current working tree:

- in-process execution preparation no longer builds one intermediate merged variable map before constructing the execution context
- direct execution now pre-resolves result-metadata category flags into a compact execution-time plan inside `DefaultScriptExecutor`
- `DefaultScriptExecutor` now caches default metadata-plan and isolation-mode derived values instead of rebuilding them per execution

Recent M4 baseline additions in the current working tree:

- `scripts/run-performance-baselines.ps1 -SkipVerifiedBuild` has been rerun against the current post-M3 tree
- `docs/performance-baseline.md` now reflects the 2026-03-19 numeric snapshot
- the default baseline suite now includes metadata-profile, context-source, module-usage, and failure-diagnostics direct-path comparisons
- the latest generated report timestamp is 2026-03-19 21:51:43 and is recorded under `docs/performance-reports/latest-performance-report.md`

## Status Conclusion

As of 2026-03-19, LingoNexus is best described as:

**a functioning script engine with a stable architecture, active external-process hardening work, and documentation that is being aligned with the real implementation.**

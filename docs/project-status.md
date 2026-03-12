# LingoNexus Project Status (2026-03-10)

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
| Docs | improving | legacy docs had drifted from implementation and are being corrected |

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

These are point-in-time numbers for 2026-03-10 and may change later.

Latest performance baseline snapshot is recorded in `docs/performance-baseline.md` (2026-03-10).

## Current Risks and Constraints

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

### Documentation drift

Legacy docs historically referenced:

- `ScriptEngineFacade`
- Rhino JavaScript execution
- outdated module planning
- large lists of files that were "still to be created"

This drift is being removed, but it remains a maintenance risk if not kept in sync.

## Best Next Steps

Recommended milestone order (aligned with `docs/todo-plan.md`):

1. M1: external-process compatibility + worker-side failure metadata stability
2. M2: diagnostics examples and operator guidance
3. M3: direct-path metadata allocation reduction
4. M4: performance baseline expansion and longer-horizon worker hardening

Progress note (2026-03-10): M1 regression coverage now includes worker-pool shutdown/eviction behavior and protocol negotiation mismatch cases; remaining focus is broader request/worker boundary metadata and recovery edge cases.

## Status Conclusion

As of 2026-03-10, LingoNexus is best described as:

**a functioning script engine with a stable architecture, active external-process hardening work, and documentation that is being aligned with the real implementation.**

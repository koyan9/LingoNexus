# LingoNexus Release Notes (2026-03-20)

> Status: draft release/update summary for the current stabilization and hardening wave.

## Highlights

- Hardened `EXTERNAL_PROCESS` failure classification so request-side, negotiation-side, borrow-side, and worker-side failures remain distinguishable through stable structured metadata.
- Improved diagnostics guidance and examples so operators can move from `ScriptResult.metadata` to the right diagnostics snapshot more directly.
- Reduced direct-path overhead in the in-process execution path by removing avoidable merge/copy work and caching metadata-plan derived decisions.
- Expanded the benchmark suite so performance regression tracking now covers metadata profiles, context-source distribution, module-heavy scripts, and diagnostics-heavy failure paths.

## Runtime and Diagnostics

- Request-factory compatibility failures now contribute to `ExternalProcessStatistics.failureReasonCounts` without being misclassified as borrow/worker failures.
- Worker-returned structured failures now refresh the matching latest diagnostics snapshot by `errorStage`.
- Public facade coverage now includes saturated-pool borrow timeout diagnostics through `EngineDiagnostics` / `ExternalProcessStatistics`.
- `DiagnosticsExample.java` now demonstrates:
  - successful execution
  - request-side compatibility failure
  - worker timeout failure

## Hot-Path Optimization

- `ExecutionPreparationService` no longer builds one intermediate merged variable map before constructing the in-process execution context.
- `DefaultScriptExecutor` now pre-resolves metadata-category flags into one execution-time plan.
- `DefaultScriptExecutor` now caches default metadata-plan and isolation-mode derived values instead of rebuilding them on each execution.

## Performance Baselines

The default baseline suite now covers:

- external worker reuse
- isolation-mode comparison
- large-context comparison
- Janino cache identity
- metadata-profile comparison
- context-source comparison
- module-usage comparison
- failure-diagnostics comparison

Latest generated report in this environment:

- `docs/performance-reports/latest-performance-report.md`
- generated at `2026-03-19 21:51:43`

Selected repeated averages from the current report:

- External-process reuse: `2.00 ms`
- Isolation mode comparison: `DIRECT 0.40 ms`, `ISOLATED_THREAD 0.60 ms`, `EXTERNAL_PROCESS 2.45 ms`
- Large-context comparison: `DIRECT 0.20 ms`, `ISOLATED_THREAD 0.50 ms`, `EXTERNAL_PROCESS 4.00 ms`
- Metadata profiles: `BASIC 0.10 ms`, `TIMING 0.05 ms`, `FULL 0.10 ms`
- Context source: `global-heavy 0.50 ms`, `request-heavy 0.30 ms`
- Module usage: `simple arithmetic 0.05 ms`, `module-heavy 0.40 ms`
- Failure diagnostics: `success-full 0.05 ms`, `error-diagnostics-only 0.00 ms`, `failure-full 0.00 ms`

## Validation

This working tree has passed:

- focused no-spring regression subsets for M1/M2/M3/M4 work
- `mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am test -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -q -pl lingonexus-testcase/lingonexus-testcase-springboot -am test -Dsurefire.failIfNoSpecifiedTests=false`
- `mvn -q -pl lingonexus-examples -am -DskipTests compile`
- `mvn -q test -Dsurefire.failIfNoSpecifiedTests=false`

## Remaining Focus

- keep M1 open until request/worker boundary recovery edge cases feel fully exhausted
- keep M2 focused on operator readability rather than new diagnostics API surface
- keep M3 limited to small semantics-preserving direct-path reductions
- keep M4 focused on report interpretation, longer-horizon hardening targets, and deciding whether external-process failure-path benchmarks should be added next

# Performance Baseline

> Updated: 2026-03-19  
> Status: current reference note for benchmark focus and baseline interpretation.

## Purpose

This document captures how to observe the impact of the current runtime architecture, especially:

- in-process execution
- isolated-thread execution
- external-process worker reuse
- worker-local executor cache reuse

## Milestone Alignment

This document is the performance reference for M4 (Performance + Hardening). Update it whenever new baseline focus or comparison targets are added.

Current status:

- baseline suite present
- report workflow present
- a post-M3 measurement refresh was recorded on 2026-03-19
- the current M3 hot-path pass already removed one direct in-process merge/copy step in execution preparation; future measurements should treat that as the new baseline
- the current M3 pass also pre-resolves result-metadata category flags inside `DefaultScriptExecutor`; benchmark interpretation should therefore compare future runs against this lower-overhead metadata path
- the current M3 pass also caches default metadata-plan and isolation-mode derived values in `DefaultScriptExecutor`, so direct-path comparisons should use this as the post-optimization baseline

## Current Baseline Focus

The most important current baseline is external-process repeated execution after worker-local executor reuse.

Relevant test:

- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ExternalProcessPerformanceBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/IsolationModeComparisonBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/LargeContextPerformanceBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/LargeContextIsolationModeComparisonBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/JavaJaninoCacheIdentityPerformanceBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ResultMetadataProfilePerformanceBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ContextSourceDistributionPerformanceBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ModuleUsageComplexityPerformanceBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/FailureDiagnosticsPerformanceBaselineTest.java`

## Isolation Mode Comparison

The project now includes a comparison-oriented baseline for:

- `DIRECT`
- `ISOLATED_THREAD`
- `EXTERNAL_PROCESS`

This comparison is intended to show relative repeated-execution cost, not to enforce a fixed absolute number.

## Large Context Baseline

A dedicated baseline now targets the direct execute hot path under larger request pressure:

- large global-variable snapshots
- large request-variable maps
- large metadata maps
- module-heavy bindings

This baseline is intended to make changes around `DefaultScriptExecutor#execute`, `ExecutionPreparationService`, and related merge/copy behavior easier to observe without the noise of external-process transport.

A companion comparison baseline now measures the same larger context shape across `DIRECT`, `ISOLATED_THREAD`, and `EXTERNAL_PROCESS`, using targeted Groovy-only sandbox loading so worker startup reflects the requested language rather than all installed engines.

That targeted loading path is now driven by sandbox language metadata rather than hard-coded implementation class names, which makes the optimization safer for future provider changes.

## Janino Cache-Identity Baseline

A dedicated Janino baseline now measures two Java-specific scenarios:

- repeated execution with a stable variable type shape
- repeated execution while alternating between different variable type shapes for the same script

This baseline is intended to confirm two things at once:

- repeated executions still reuse the compiled-script cache when the Java context type shape is stable
- the runtime now keeps separate cache entries when the same Java script is compiled against different variable type shapes

This is particularly important for the Janino-backed Java path because its compilation identity depends on `ScriptContext` parameter names and runtime types.

## Metadata Profile Baseline

A dedicated baseline now measures direct execution across the main result-metadata profiles:

- `basic`
- `timing`
- `full`

This baseline is intended to quantify the overhead of richer result metadata after the latest M3 hot-path reductions.

## Context Source Baseline

A dedicated baseline now compares direct execution when the same script shape runs against:

- a global-heavy context
- a request-heavy context

This baseline is intended to make variable-source distribution visible as a preparation-path cost, independent of external-process transport.

## Module Usage Baseline

A dedicated baseline now compares direct execution between:

- a simple arithmetic script with no runtime modules
- a module-heavy script that references many registered module bindings

This baseline is intended to quantify the marginal cost of module injection and module-bound script shapes without the noise of external isolation.

## Failure Diagnostics Baseline

A dedicated baseline now compares direct execution across:

- a successful `full` metadata path
- a failure path with `ERROR_DIAGNOSTICS` only
- a failure path with `full` metadata

This baseline is intended to show the cost of rich failure diagnostics relative to an already observability-heavy success path.

## Report Workflow

A reportable workflow is now available through:

- `scripts/run-performance-baselines.ps1`

The script runs the selected baseline tests, captures structured `BENCHMARK|...` lines, stores the raw console log, and generates a Markdown report under `docs/performance-reports/`.

Generated reports are intended to be local or CI artifacts and can be regenerated on demand; they do not need to remain committed in the repository.

Structured benchmark output is intentionally benchmark-specific: shared fields such as cold/repeated timing are kept consistent where possible, while each benchmark may emit extra fields that matter for that scenario (for example worker-cache metrics or Janino type-shape metrics).

Default coverage includes:

- `ExternalProcessPerformanceBaselineTest`
- `IsolationModeComparisonBaselineTest`
- `LargeContextPerformanceBaselineTest`
- `LargeContextIsolationModeComparisonBaselineTest`
- `JavaJaninoCacheIdentityPerformanceBaselineTest`
- `ResultMetadataProfilePerformanceBaselineTest`
- `ContextSourceDistributionPerformanceBaselineTest`
- `ModuleUsageComplexityPerformanceBaselineTest`
- `FailureDiagnosticsPerformanceBaselineTest`

Typical usage:

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/run-performance-baselines.ps1 -SkipVerifiedBuild
```

Or with the verified build workflow and dedicated Maven repo:

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/run-performance-baselines.ps1 -UseDedicatedRepo
```

For focused downstream execution without the wrapper script, keep the upstream reactor attached:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am -Dtest=LargeContextPerformanceBaselineTest,LargeContextIsolationModeComparisonBaselineTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## What to Observe

For external-process mode, compare:

- first execution latency
- repeated execution average latency
- executor cache hits / misses
- worker discard / eviction counts

The first execution includes more setup cost. Repeated executions should reflect the benefit of:

- persistent worker reuse
- worker-local executor reuse
- compile cache reuse inside the worker-local engine

## Latest Baseline Snapshot (2026-03-19)

- Source: `docs/performance-reports/latest-performance-report.md`
- Environment: Windows 10.0.26200.0, Java 17.0.6, Maven 3.9.6, CPU i9-13980HX, Memory 31.63 GB
- Verification path: direct test run via `scripts/run-performance-baselines.ps1 -SkipVerifiedBuild`
- External-process reuse: cold 770 ms, repeated avg 2.00 ms, executorCacheHits 20, executorCacheMisses 1, discard 0, eviction 0
- Isolation mode comparison (repeated avg): DIRECT 0.40 ms, ISOLATED_THREAD 0.60 ms, EXTERNAL_PROCESS 2.45 ms
- Large-context comparison (repeated avg): DIRECT 0.20 ms, ISOLATED_THREAD 0.50 ms, EXTERNAL_PROCESS 4.00 ms
- Large-context direct (repeated avg): small 0.15 ms, large 1.00 ms
- Janino cache identity: stable avg 0.10 ms, alternating avg 0.15 ms, stable cacheMisses 1, alternating cacheMisses 2
- Metadata profile direct (repeated avg): BASIC 0.10 ms, TIMING 0.05 ms, FULL 0.10 ms
- Context source direct (repeated avg): global-heavy 0.50 ms, request-heavy 0.30 ms
- Module usage direct (repeated avg): simple arithmetic 0.05 ms, module-heavy 0.40 ms
- Failure diagnostics direct (repeated avg): success-full 0.05 ms, failure-error-diagnostics-only 0.00 ms, failure-full 0.00 ms

Compared with the earlier 2026-03-10 snapshot:

- external-process repeated reuse remains in the same range while cold start improved in this environment
- large-context direct repeated cost is still the main direct-path hotspot to watch after the latest M3 changes
- isolation-mode and large-context comparisons remain trend-stable enough to use for regression detection, but not as fixed targets

These numbers are environment-specific and should be used for relative trend comparisons rather than absolute targets.

## Example Interpretation

If repeated average latency drops significantly compared with the first request, the optimization is working as intended.

If executor cache hits remain at `0`, then one of the following is likely happening:

- executor signatures are changing between requests
- worker instances are being discarded too aggressively
- idle TTL / cache TTL settings are too small

## Related Diagnostics

Use `LingoNexusExecutor#getDiagnostics()` and inspect:

- `externalProcessStatistics.executorCacheHits`
- `externalProcessStatistics.executorCacheMisses`
- `externalProcessStatistics.executorCacheEvictions`
- `externalProcessStatistics.discardCount`
- `externalProcessStatistics.evictionCount`

## Next Benchmark Targets

- compare cold-start vs warmed-worker throughput
- compare module-heavy scripts vs simple arithmetic scripts
- compare external-process with and without worker-local executor cache reuse
- compare Janino stable-shape reuse vs highly variable-shape workloads under the same Java script
- compare failure diagnostics cost in external-process mode versus direct mode

# M4 Performance + Hardening

> Updated: 2026-03-19  
> Purpose: extend performance baselines and document longer-horizon hardening targets.

## Status

In progress.

## Milestone Goal

Strengthen baseline comparisons and document forward-looking hardening work.

Success means:

- baseline guidance remains consistent across isolation modes
- performance reports remain reproducible
- long-horizon hardening targets are captured in docs

Current progress:

- the baseline suite and reporting workflow already exist
- a post-M3 measurement refresh is now recorded in `docs/performance-baseline.md` and `docs/performance-reports/latest-performance-report.md`
- the benchmark suite now includes direct-path result-metadata profile comparison (`basic` / `timing` / `full`)
- the benchmark suite now also includes direct-path context-source comparison (`global-heavy` vs `request-heavy`)
- the benchmark suite now also includes direct-path module-usage comparison (`simple arithmetic` vs `module-heavy`)
- the benchmark suite now also includes direct-path failure-diagnostics comparison (`error-diagnostics-only` vs `full`)
- the current local refresh timestamp is 2026-03-19 21:51:43 and now includes metadata-profile, context-source, module-usage, and failure-diagnostics benchmarks in the generated report
- the remaining work is mostly interpretation, comparison coverage, and future hardening capture rather than new runtime mechanics

## Issue Drafts

- `.github/issue-drafts/m4/01-performance-baseline-extension.md`

## In Scope

- doc updates for baseline interpretation
- capture new benchmark focus areas
- keep the report-generation workflow aligned with the tests and scripts actually used for local/CI benchmarking

## Out of Scope

- new transport protocol implementations
- large-scale worker lifecycle changes
- performance claims that are not backed by the current benchmark suite

## Validation

- run the baseline report workflow when numbers need to be refreshed:

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/run-performance-baselines.ps1 -SkipVerifiedBuild
```

- run full `mvn test` at milestone completion

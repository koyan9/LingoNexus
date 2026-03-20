# docs: extend performance baseline guidance

## Summary

Refresh `docs/performance-baseline.md` with any new baseline focus and long-horizon hardening notes.

## Why

Performance baselines are the anchor for optimization and regression detection; guidance should stay aligned with the current benchmark suite.

## Scope

- update baseline interpretation notes if needed
- capture any new benchmark targets or comparison focus
- keep the direct-path metadata-profile comparison baseline (`basic` / `timing` / `full`) in the default suite once it lands
- keep the direct-path context-source and module-usage comparison baselines in the default suite once they land
- keep the direct-path failure-diagnostics comparison baseline in the default suite once it lands

## Done When

- performance baseline doc reflects the current benchmark suite
- next benchmark targets are clear and actionable
- the generated report includes the metadata-profile benchmark alongside the existing suite
- the generated report includes the new direct-path context-source and module-usage comparisons alongside the existing suite
- the generated report includes the failure-diagnostics comparison alongside the existing suite

## Validation

- optional run of `scripts/run-performance-baselines.ps1`

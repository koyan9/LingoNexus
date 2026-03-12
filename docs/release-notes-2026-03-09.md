# LingoNexus Release Notes (2026-03-09)

> Status: draft release/update summary for the current repository import and cleanup wave.

> GitHub release draft: `.github/release-drafts/release-2026-03-09.md`

## Highlights

- Imported the full LingoNexus runtime, including `lingonexus-api`, `lingonexus-core`, language sandboxes, Spring Boot starter, examples, and testcase modules.
- Consolidated repository docs to a smaller core set centered on architecture, quick start, diagnostics, build troubleshooting, project status, requirements, performance baseline, and todo planning.
- Added a Windows-friendly verification workflow through `scripts/verified-build.ps1` with reusable modes, focused profiles, built-in help, and generated preset documentation.
- Added generated preset reference support through `scripts/generate-build-presets-doc.ps1` and `scripts/BUILD_PRESETS.md`.

## Documentation Changes

- Kept these files as the primary reader path:
  - `README.md`
  - `README.zh-CN.md`
  - `docs/INDEX.md`
  - `docs/architecture.md`
  - `docs/quick-start.md`
  - `docs/diagnostics.md`
  - `docs/build-troubleshooting.md`
  - `docs/project-status.md`
  - `docs/requirements.md`
  - `docs/performance-baseline.md`
  - `docs/todo-plan.md`
- Removed older summary, alignment, and design-analysis documents that no longer sit on the main reader path.
- Simplified README navigation so new readers can quickly reach docs, build guidance, and verification presets.

## Build and Verification

- Added `.gitignore` and `.gitattributes` for cleaner repository hygiene and LF-oriented text handling.
- Added `verified-build.ps1` support for:
  - mode presets: `Core`, `Selective`, `Quick`, `Full`
  - focused profiles: `ExternalProcess`, `Diagnostics`, `SpringBoot`, `Performance`
  - discovery commands: `-ListModes`, `-ListProfiles`, `-ShowMode`, `-ShowProfile`
  - documentation refresh commands: `-RefreshDocs`, `-RefreshDocsOnly`
- Kept `scripts/run-performance-baselines.ps1` as the on-demand benchmark/report entrypoint.

## Cleanup

- Removed local verification repositories, build outputs, generated performance reports, temporary logs, IDE directories, and other machine-specific artifacts from the working tree before commit preparation.
- Reduced `docs/` to the core documentation set needed for onboarding, architecture understanding, troubleshooting, and planned follow-up work.

## Appendix: Performance Baseline Snapshot (2026-03-10)

- Reference: `docs/performance-baseline.md`
- External-process reuse: cold 808 ms, repeated avg 2.00 ms, executor cache hits 20, misses 1
- Isolation mode comparison (repeated avg): DIRECT 0.35 ms, ISOLATED_THREAD 0.50 ms, EXTERNAL_PROCESS 1.75 ms
- Large-context comparison (repeated avg): DIRECT 0.50 ms, ISOLATED_THREAD 0.80 ms, EXTERNAL_PROCESS 3.70 ms
- Note: values are environment-specific and intended for trend comparison only

## Notes

- This release/update is primarily a repository import, cleanup, and developer-experience baseline rather than a single runtime feature drop.
- Generated performance reports are now treated as local or CI artifacts and can be regenerated on demand instead of being kept in the repository.

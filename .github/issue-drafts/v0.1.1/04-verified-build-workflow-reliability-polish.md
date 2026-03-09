# chore: harden verified-build workflow for patch release maintenance

## Summary

Polish the verified build workflow so all current modes, profiles, help commands, and doc refresh paths remain reliable during `v0.1.1` maintenance work.

## Why

The repository now depends on `scripts/verified-build.ps1` and generated preset docs as part of the main developer workflow. This needs continued stability in patch releases.

## Scope

- keep all current mode/profile/help/refresh paths healthy
- keep `scripts/BUILD_PRESETS.md` regenerable
- remove any stale references between build docs and generated preset docs

## Done When

- `scripts/verified-build.ps1` still supports all current modes, profiles, and help/refresh commands
- `scripts/BUILD_PRESETS.md` can be regenerated cleanly
- no stale references remain between build docs and generated preset docs

## Checklist

- [ ] smoke-test at least one mode path after changes
- [ ] smoke-test at least one profile path after changes
- [ ] smoke-test `-ListModes`, `-ListProfiles`, and `-RefreshDocsOnly`
- [ ] regenerate `scripts/BUILD_PRESETS.md` if preset definitions change

## Out of Scope

- redesigning the verification workflow from scratch
- adding unrelated CI systems or deployment automation

## Validation

- run representative verified-build commands
- confirm generated preset docs remain in sync with definitions


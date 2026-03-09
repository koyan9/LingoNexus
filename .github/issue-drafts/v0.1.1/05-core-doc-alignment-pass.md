# docs: align core onboarding and build docs for v0.1.1

## Summary

Do a focused alignment pass across the core reader-path docs so `v0.1.1` keeps onboarding and build guidance consistent and compact.

## Why

The repository now has a cleaner main documentation path. `v0.1.1` should preserve that clarity and avoid reintroducing drift.

## Scope

- review the top-level reader path for wording drift
- keep Chinese and English reader-path sections structurally aligned
- avoid reintroducing removed historical documents into the main navigation path

## Done When

- `README.md`, `README.zh-CN.md`, `docs/INDEX.md`, `docs/quick-start.md`, `docs/build-troubleshooting.md`, and `docs/todo-plan.md` remain mutually consistent
- no references remain to removed historical docs or generated local artifacts

## Checklist

- [ ] review top-level reader-path docs after `v0.1.1` edits
- [ ] keep mirrored sections in English and Chinese structurally aligned
- [ ] check README / INDEX / build docs for stale links

## Out of Scope

- writing new historical design notes
- expanding the docs surface beyond the current core set

## Validation

- read the main reader path in order
- confirm all referenced files still exist and match current implementation


# docs: improve diagnostics examples for operators

## Summary

Improve operator-facing diagnostics examples so readers can more easily interpret cache state, thread pools, worker pools, and latest failure reasons.

## Why

The diagnostics surface is already useful, but `v0.1.1` should make the documented usage easier to operationalize without adding new APIs.

## Scope

- review `docs/diagnostics.md` against the current diagnostics surface
- expand examples only where they add practical troubleshooting value
- keep examples concise and implementation-aligned

## Done When

- `docs/diagnostics.md` remains aligned with the current public diagnostics API
- examples clearly show how to inspect cache, thread pools, worker pools, and latest failure reasons
- build/verification docs still point readers to the correct diagnostics entrypoints

## Checklist

- [ ] review existing diagnostics examples for drift or ambiguity
- [ ] improve operator-facing examples where helpful
- [ ] verify references from README / build docs remain correct

## Out of Scope

- adding new diagnostics APIs just for docs
- broad architecture doc expansion

## Validation

- review docs against the current codebase
- confirm example snippets and field names still match implementation


# TODO Plan

## Near Term

- Evaluate whether request metadata can move to a lighter-weight view to avoid per-execute metadata map copying in the direct hot path.
- Split security policy evaluation into compile-time, request-time, and runtime phases.
- Add startup retry / replacement metrics to logs or monitoring output.
- Add focused tests for external-process failure metadata and descriptor factory reconstruction.
- Add focused tests for:
  - worker reuse
  - worker replacement after timeout
  - health-check failure recovery
- Add diagnostics examples showing how to read engine and worker-pool runtime state.
- Add richer performance result recording and benchmark comparison notes for `DIRECT`, `ISOLATED_THREAD`, and `EXTERNAL_PROCESS`.
- Extend Janino benchmark coverage beyond stable/alternating type-shape baselines.
- Add protocol-negotiation history aggregation so diagnostics can report the most recent mismatch reason, not only a cumulative failure count.
- Reduce remaining direct-path metadata allocations by separating required result metadata from optional diagnostics metadata.
- Resolve the environment-specific Maven/classpath issue that currently blocks reliable Spring Boot starter/testcase validation on this machine.

## Mid Term

- Expand external-process compatibility beyond descriptor + factory / consumer reconstruction.
- Add structured compatibility checks for non-JSON-safe values before dispatch.
- Improve external-process error classification and diagnostics.

## Longer Term

- Evaluate CBOR or another binary-safe protocol for worker transport.
- Consider process-level sandbox hardening and OS-level restrictions.
- Add external worker idle scaling and configurable prewarm behavior.
- Build performance benchmarks comparing:
  - `DIRECT`
  - `ISOLATED_THREAD`
  - `EXTERNAL_PROCESS`

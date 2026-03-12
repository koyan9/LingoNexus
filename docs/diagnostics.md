# Diagnostics API

> Updated: 2026-03-10  
> Doc navigation: `docs/INDEX.md`  
> Architecture baseline: `docs/architecture.md`  
> Quick integration guide: `docs/quick-start.md`


## Purpose

LingoNexus exposes runtime diagnostics so callers can inspect:

- execution counters
- cache size
- async / isolated thread-pool state
- external-process worker-pool state

## API Surface

Use the following methods on `LingoNexusExecutor`:

- `getStatistics()`
- `getDiagnostics()`

## Example

```java
LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);

EngineStatistics statistics = executor.getStatistics();
EngineDiagnostics diagnostics = executor.getDiagnostics();

System.out.println("Total executions: " + statistics.getTotalExecutions());
System.out.println("Success rate: " + statistics.getSuccessRate());
System.out.println("Cache hit rate: " + statistics.getCacheHitRate());

System.out.println("Cache size: " + diagnostics.getCacheSize());
System.out.println("Async pool: " + diagnostics.getAsyncExecutorStatistics());
System.out.println("Isolated pool: " + diagnostics.getIsolatedExecutorStatistics());

if (diagnostics.getExternalProcessStatistics() != null) {
    System.out.println("External max workers: " + diagnostics.getExternalProcessStatistics().getMaxWorkers());
    System.out.println("External created workers: " + diagnostics.getExternalProcessStatistics().getCreatedWorkers());
    System.out.println("External idle workers: " + diagnostics.getExternalProcessStatistics().getIdleWorkers());
    System.out.println("External borrow count: " + diagnostics.getExternalProcessStatistics().getBorrowCount());
    System.out.println("External borrow timeouts: " + diagnostics.getExternalProcessStatistics().getBorrowTimeoutCount());
    System.out.println("External discard count: " + diagnostics.getExternalProcessStatistics().getDiscardCount());
    System.out.println("External eviction count: " + diagnostics.getExternalProcessStatistics().getEvictionCount());
    System.out.println("Worker protocol version: " + diagnostics.getExternalProcessStatistics().getWorkerProtocolVersion());
    System.out.println("Worker protocol capabilities: " + diagnostics.getExternalProcessStatistics().getSupportedTransportProtocolCapabilities());
    System.out.println("Worker serializer contracts: " + diagnostics.getExternalProcessStatistics().getSupportedTransportSerializerContractIds());
    System.out.println("Protocol negotiation failures: " + diagnostics.getExternalProcessStatistics().getProtocolNegotiationFailureCount());
    System.out.println("Latest protocol negotiation failure reason: " + diagnostics.getExternalProcessStatistics().getLatestProtocolNegotiationFailureReason());
    System.out.println("Latest borrow failure reason: " + diagnostics.getExternalProcessStatistics().getLatestBorrowFailureReason());
    System.out.println("Latest worker execution failure reason: " + diagnostics.getExternalProcessStatistics().getLatestWorkerExecutionFailureReason());
    System.out.println("Failure reason counts: " + diagnostics.getExternalProcessStatistics().getFailureReasonCounts());
    System.out.println("Worker-local executor cache size: " + diagnostics.getExternalProcessStatistics().getExecutorCacheSize());
    System.out.println("Worker-local executor cache hits: " + diagnostics.getExternalProcessStatistics().getExecutorCacheHits());
    System.out.println("Worker-local executor cache misses: " + diagnostics.getExternalProcessStatistics().getExecutorCacheMisses());
    System.out.println("Worker-local executor cache evictions: " + diagnostics.getExternalProcessStatistics().getExecutorCacheEvictions());
}
```

## Common External-Process Reason Codes

When external-process execution fails, `ScriptResult.metadata` may now include `errorReason` values such as:

- `request_payload_not_json_safe`
- `security_policy_not_external_process_compatible`
- `security_policy_descriptor_not_json_safe`
- `security_policy_descriptor_load_failed`
- `script_module_not_external_process_compatible`
- `script_module_descriptor_not_json_safe`
- `script_module_descriptor_load_failed`
- `response_payload_not_json_safe`
- `external_process_compatibility_failure`
- `protocol_handshake_failed`
- `protocol_capability_mismatch`
- `serializer_contract_mismatch`
- `protocol_negotiation_failure`
- `worker_execution_timeout`
- `worker_startup_failed`
- `worker_borrow_timeout`
- `worker_borrow_interrupted`
- `worker_borrow_failed`
- `worker_pool_shutdown`
- `worker_unavailable`
- `worker_execution_failed`
- `worker_terminated_unexpectedly`

These reason codes are intended to be more stable for troubleshooting and alerting than raw exception messages alone.

Request-side compatibility checks now cover not only variables and metadata, but also custom `SecurityPolicy` and runtime `ScriptModule` descriptor export / reconstruction requirements before worker dispatch.

Response-side compatibility is now slightly more forgiving than request-side compatibility: unsupported leaf values in response payloads may be stringified, but structurally incompatible response shapes such as non-string map keys still fail with `response_payload_not_json_safe`.

## Failure Interpretation Guide

Use the fields in `ScriptResult.metadata` and `EngineDiagnostics.getExternalProcessStatistics()` together:

### 1. Payload compatibility problem

Typical signals:

- `errorStage = request_validation`
- `errorComponent = external-process-compatibility`
- `errorReason = request_payload_not_json_safe`
- `errorDetailReason = map_key_not_string` or `value_type_not_json_safe`

Interpretation:

- the request never reached real worker execution
- the payload shape is incompatible with current JSON-safe transport rules
- inspect the error message path such as `$.variables...` or `$.metadata...`
- `errorPath`, `errorValueType`, and `errorDetailReason` provide structured diagnostics when JSON-safe validation fails

Action:

- convert custom objects to JSON-safe maps / lists / strings before dispatch
- reduce nested non-primitive values in request variables and metadata

### 2. Handshake or protocol mismatch

Typical signals:

- `errorStage = protocol_negotiation`
- `errorComponent = external-worker-handshake`
- `errorReason = protocol_handshake_failed`, `protocol_capability_mismatch`, `serializer_contract_mismatch`, or `protocol_negotiation_failure`
- diagnostics fields such as `latestProtocolNegotiationFailureReason`

Interpretation:

- the worker process was reachable, but transport expectations did not match
- the most useful first check is the latest negotiation failure reason plus supported capabilities/contracts

Action:

- compare required protocol capabilities with worker-supported capabilities
- compare required serializer contracts with worker-supported serializer contracts
- verify sandbox/provider selection and external-process compatibility filters

### 3. Borrow-side worker problem

Typical signals:

- `errorStage = borrow_worker`
- `errorComponent = external-worker-pool`
- `errorReason = worker_startup_failed`, `worker_borrow_timeout`, `worker_borrow_interrupted`, `worker_borrow_failed`, or `worker_pool_shutdown`
- diagnostics fields such as `latestBorrowFailureReason` and `failureReasonCounts`

Interpretation:

- the failure happened before the request could be handed to a healthy worker
- this usually points to startup instability, thread interruption, or lifecycle misuse

Action:

- check startup retry settings and local Java/classpath availability
- `worker_borrow_timeout` indicates the pool hit its borrow wait limit (check `externalProcessBorrowTimeoutMs`, pool size, and worker health)
- inspect whether the executor or worker pool was already shutting down
- watch `startupFailureCount`, `healthCheckFailureCount`, and aggregated `failureReasonCounts`

Quick distinction:

- `worker_borrow_timeout`: pool is saturated or workers are unhealthy; increase pool size or fix worker health.
- `worker_startup_failed`: new workers could not launch; check Java/classpath and startup retries.

### 4. Worker execution problem

Typical signals:

- `errorStage = worker_execution`
- `errorComponent = external-worker`
- `errorReason = worker_execution_timeout`, `worker_unavailable`, `worker_execution_failed`, `worker_terminated_unexpectedly`,
  `security_policy_descriptor_load_failed`, `script_module_descriptor_load_failed`, or `response_payload_not_json_safe`
- diagnostics fields such as `latestWorkerExecutionFailureReason`

Interpretation:

- the request made it past borrow/handshake, but worker-side execution or extension reconstruction failed

Action:

- distinguish timeout from crash/unavailability first
- check whether discard count increased after the failure
- confirm custom security policies and modules can be reconstructed on the worker classpath
- use `failureReasonCounts` to see whether this is a recurring class of problem

### 5. How to use aggregated counts

`failureReasonCounts` is useful for trend-level diagnosis:

- if one reason dominates, investigate that class first
- if counts are spread across many reasons, suspect broader environment instability or mixed misconfiguration
- if only the latest reason changes but counts stay low, the issue may be transient rather than systemic
- if the map includes `failure_reason_overflow`, it means unique reasons exceeded the tracking cap (128); use it as a signal to inspect logs for rare or highly variable failures

## Related Configuration

In external-process mode, the following sandbox settings now affect runtime behavior:

- `externalProcessPoolSize`
- `externalProcessStartupRetries`
- `externalProcessPrewarmCount`
- `externalProcessIdleTtlMs`
- `externalProcessBorrowTimeoutMs`
- `externalProcessExecutorCacheMaxSize`
- `externalProcessExecutorCacheIdleTtlMs`

Result metadata detail can also be controlled through:

- engine-level profile: `LingoNexusConfig.builder().resultMetadataProfile(ResultMetadataProfile.TIMING)`
- request-level profile override: `ScriptContext.builder().putMetadata("resultMetadataProfile", "full")`
- engine-level category masks: `LingoNexusConfig.builder().resultMetadataCategory(ResultMetadataCategory.TIMING, ResultMetadataCategory.THREAD)`
- request-level category masks: `ScriptContext.builder().putMetadata("resultMetadataCategories", "timing,thread")`
- engine-level named policy: `LingoNexusConfig.builder().resultMetadataPolicy(ResultMetadataPolicy.DEBUG)`
- request-level named policy override: `ScriptContext.builder().putMetadata("resultMetadataPolicy", "timing")`
- custom inherited policy template: `resultMetadataPolicyTemplate(ResultMetadataPolicyTemplate.builder().name("team-timing-thread").parentPolicyName("timing").category(ResultMetadataCategory.THREAD).build())`
- shared preset registry: `ResultMetadataPolicyRegistry.create().registerTemplate(...)` then `resultMetadataPolicyRegistry(registry)`
- preset loader: `ResultMetadataPolicyRegistryLoader.load(properties)` or `registry.registerProperties(properties)`
- backward-compatible boolean shortcut: `detailedResultMetadataEnabled(false)` still maps to `basic`

Spring Boot starter binding now also supports YAML-style configuration such as:

- `lingonexus.metadata.profile=TIMING`
- `lingonexus.metadata.policy=DEBUG`
- `lingonexus.metadata.policy-name=teamTimingThread`
- `lingonexus.metadata.policy-templates.teamTimingThread.parent-policy-name=timing`
- `lingonexus.metadata.policy-templates.teamTimingThread.categories=THREAD`

## Example Class

- `lingonexus-examples/src/main/java/io/github/koyan9/lingonexus/examples/DiagnosticsExample.java`
- Runs a simple external-process configuration and prints engine / worker-pool diagnostics.

## Related Benchmark Notes

- `docs/performance-baseline.md` describes how to interpret worker reuse and executor cache metrics.

## Notes

- Diagnostics are snapshots, not streaming metrics.
- External worker statistics are most relevant when `isolationMode` is `EXTERNAL_PROCESS`.
- Values may change quickly under concurrent load.
- Worker-side failures returned in responses also contribute to `failureReasonCounts` and latest failure reason tracking.
- Worker handshake data is now available from the first successful worker borrow because new workers perform an eager health-check before entering the reusable pool.
- JSON-safe compatibility failures may include `errorPath`, `errorValueType`, and `errorDetailReason` for structured payload diagnostics.
- For `DIRECT` and `ISOLATED_THREAD` executors that never touch external isolation, external statistics remain a zero-state snapshot until the external runtime is actually initialized.
- `basic` keeps core result metadata such as script engine, isolation mode, cache-hit, and error basics.
- `timing` adds compile/execution/queue/wall/total timing fields but still suppresses thread/module/diagnostic-detail fields.
- `full` adds the remaining diagnostic fields such as thread info, modules used, security-check count, and detailed error stage/component metadata.
- `errorStage` / `errorComponent` / `errorReason` are emitted only when `ERROR_DIAGNOSTICS` is enabled (explicitly or via `full`).
- Category masks override profiles when both are provided and allow combinations such as timing-only, module-only, or error-diagnostics-only output.
- Named policies are preset mappings layered above profiles and masks: `minimal -> basic`, `timing -> timing`, `debug -> full`.
- Custom policy templates can inherit from built-in named policies or other custom templates; request-level named overrides are normalized to category masks before external-process dispatch so worker-side execution stays consistent.
- Shared registries let multiple executors reuse one template catalog; local templates registered directly on a config still override registry entries with the same name.
- Loader format uses keys like `resultMetadataPolicies.<name>.parent` and `resultMetadataPolicies.<name>.categories=timing,thread`.
- The Spring Boot binding added in `lingonexus-spring-boot-starter` maps these concepts directly into `LingoNexusConfig`, but validation of the starter/testcase modules is currently limited by a local Maven repository/classpath issue in this environment.

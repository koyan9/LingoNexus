# Requirements

> Updated: 2026-03-08  
> Purpose: describe the current product and engineering requirements for the implemented LingoNexus codebase.

## Product Goals

LingoNexus should provide a script execution engine for Java applications that is:

- secure enough for production-oriented embedding scenarios
- extensible across multiple script languages and runtime modules
- operable through clear diagnostics and lifecycle control
- usable in both standalone Java and Spring Boot applications

## Functional Requirements

### Core Execution

The engine must:

- execute scripts synchronously
- execute scripts asynchronously
- support batch execution
- return structured results through `ScriptResult`
- expose status, value, error message, execution time, and metadata

### Language Support

The runtime must support the following language families through a unified API:

- Groovy
- JavaScript
- Java
- Java expression
- Kotlin

### Configuration and Assembly

The runtime must provide:

- caller-facing configuration through `LingoNexusConfig`
- normalized runtime configuration through `EngineConfig`
- standalone assembly through `LingoNexusBuilder`
- Spring Boot property binding through `lingonexus-spring-boot-starter`

### Security and Isolation

The runtime must support:

- script size limits
- timeout control
- class whitelist / blacklist configuration
- security policy validation before execution
- multiple isolation modes:
  - `AUTO`
  - `DIRECT`
  - `ISOLATED_THREAD`
  - `EXTERNAL_PROCESS`

### Extensibility

The runtime must allow:

- SPI-based sandbox discovery
- SPI-based module discovery
- runtime module registration and unregister
- configurable global variables
- metadata policy customization for result output detail

### Observability and Operations

The runtime must expose:

- lifecycle control via shutdown / close semantics
- timing-oriented result metadata
- engine-level statistics via `EngineStatistics`
- runtime diagnostics via `EngineDiagnostics`
- external worker-pool diagnostics via `ExternalProcessStatistics`

## Non-Functional Requirements

### Performance

The runtime should provide:

- compiled-script caching for supported execution paths
- low-overhead direct execution for simple scenarios
- reusable worker pools for external-process mode
- minimal avoidable copying on hot execution paths

### Compatibility

The runtime should:

- keep public execution semantics stable across supported languages
- allow multiple providers for the same language with deterministic selection
- support language/provider filtering by capability metadata

### Operability

The runtime should:

- classify failures clearly enough for troubleshooting
- expose enough runtime state to inspect cache, thread pools, and worker pools
- avoid hidden global runtime state where instance-scoped state is safer

## Current Constraints

The following constraints are currently part of the real implementation and must be treated as known limitations, not bugs in the docs:

- `EXTERNAL_PROCESS` works best with JSON-safe variables, metadata, and result values
- cross-process compatibility for custom `SecurityPolicy` implementations still has practical limits
- dynamic module reconstruction in external workers still has room to improve
- module-scoped validation is more reliable than full-repo validation in the current environment

## Explicit Non-Goals

The current codebase does not promise:

- full OS-level sandboxing
- arbitrary object-graph transport across external workers
- a business-rule DSL beyond the provided script languages
- fully transparent cross-process support for every custom extension type

## References

- `docs/architecture.md`
- `docs/quick-start.md`
- `docs/diagnostics.md`
- `docs/todo-plan.md`

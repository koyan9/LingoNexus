# Build Presets Reference

> Generated from `scripts/build-modes.ps1` and `scripts/build-profiles.ps1`.
> Generated at: 2026-03-09 00:34:09 +08:00
> Regenerate with: `powershell.exe -ExecutionPolicy Bypass -File scripts/generate-build-presets-doc.ps1`

## Purpose

This document is a static quick-reference for the Windows-friendly build and verification presets used by `scripts/verified-build.ps1`.

## Modes

| Mode | Description | Overrides |
| --- | --- | --- |
| `Core` | Installs only the root POM plus lingonexus-api and lingonexus-core for the fastest runtime iteration loop. | `SkipExamples=True, SkipModules=True, SkipScripts=True, SkipTestcases=True, SkipUtils=True` |
| `Full` | Runs the full module-by-module build flow, including testcase compilation. | `none` |
| `Quick` | Builds core plus downstream dependencies but skips examples and testcase compilation for a faster sanity pass. | `SkipExamples=True, SkipTestcases=True` |
| `Selective` | Respects explicit -Skip* flags and acts as the base mode for focused profiles. | `none` |

## Profiles

### `Diagnostics`

Focused verification for engine diagnostics, execution statistics, metadata policies, and result output behavior.

| Plan | Module | Tests |
| --- | --- | --- |
| `Diagnostics` | `lingonexus-testcase/lingonexus-testcase-nospring` | `EngineDiagnosticsFeatureTest, ExecutionStatisticsFeatureTest, MetadataUsageExampleTest, ProtocolNegotiationFeatureTest, ResultMetadataCategoryFeatureTest, ResultMetadataConfigurationFeatureTest, ResultMetadataPolicyFeatureTest, ResultMetadataPolicyRegistryFeatureTest, ResultMetadataPolicyRegistryLoaderFeatureTest, ResultMetadataProfileFeatureTest` |

### `ExternalProcess`

Focused verification for external-process compatibility, worker-pool behavior, diagnostics, and protocol negotiation.

| Plan | Module | Tests |
| --- | --- | --- |
| `ExternalProcess` | `lingonexus-testcase/lingonexus-testcase-nospring` | `ExternalProcessExecutionRequestFactoryFeatureTest, ExternalProcessScriptExecutorFeatureTest, ExternalProcessWorkerPoolFeatureTest, ExternalProcessCompatibilityTest, ExternalProcessIsolationTest, EngineDiagnosticsFeatureTest, ProtocolNegotiationFeatureTest` |

### `Performance`

Focused verification for No-Spring performance baselines plus Spring Boot throughput and stress scenarios.

| Plan | Module | Tests |
| --- | --- | --- |
| `Performance NoSpring` | `lingonexus-testcase/lingonexus-testcase-nospring` | `ExternalProcessPerformanceBaselineTest, IsolationModeComparisonBaselineTest, LargeContextPerformanceBaselineTest, LargeContextIsolationModeComparisonBaselineTest, JavaJaninoCacheIdentityPerformanceBaselineTest` |
| `Performance SpringBoot` | `lingonexus-testcase/lingonexus-testcase-springboot` | `SpringBootPerformanceTest, SpringBootStressTest` |

### `SpringBoot`

Focused verification for the Spring Boot starter, configuration binding, modules integration, async execution, and batch execution.

| Plan | Module | Tests |
| --- | --- | --- |
| `SpringBoot` | `lingonexus-testcase/lingonexus-testcase-springboot` | `SpringBootBasicIntegrationTest, SpringBootConfigurationTest, SpringBootModulesIntegrationTest, SpringBootAsyncExecutionTest, SpringBootBatchExecutionTest` |

## Discovery Commands

```powershell
Get-Help .\scripts\verified-build.ps1 -Detailed
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListModes
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListProfiles
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ShowMode Quick
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ShowProfile Performance
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -ListProfiles -RefreshDocs
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -RefreshDocsOnly
```

## Common Commands

```powershell
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Core
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Quick
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile ExternalProcess -UseDedicatedRepo
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile Diagnostics -UseDedicatedRepo
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile SpringBoot -UseDedicatedRepo
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Selective -Profile Performance -UseDedicatedRepo
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Full -UseDedicatedRepo
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -Mode Core -RefreshDocs
powershell.exe -ExecutionPolicy Bypass -File scripts/verified-build.ps1 -RefreshDocsOnly
```

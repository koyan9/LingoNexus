function Get-BuildProfiles {
    return @{
        ExternalProcess = @{
            Description = 'Focused verification for external-process compatibility, worker-pool behavior, diagnostics, and protocol negotiation.'
            Plans = @(
                @{
                    Name = 'ExternalProcess'
                    Module = 'lingonexus-testcase/lingonexus-testcase-nospring'
                    Tests = @(
                        'ExternalProcessExecutionRequestFactoryFeatureTest',
                        'ExternalProcessScriptExecutorFeatureTest',
                        'ExternalProcessWorkerPoolFeatureTest',
                        'ExternalProcessCompatibilityTest',
                        'ExternalProcessIsolationTest',
                        'EngineDiagnosticsFeatureTest',
                        'ProtocolNegotiationFeatureTest'
                    )
                }
            )
        }
        Diagnostics = @{
            Description = 'Focused verification for engine diagnostics, execution statistics, metadata policies, and result output behavior.'
            Plans = @(
                @{
                    Name = 'Diagnostics'
                    Module = 'lingonexus-testcase/lingonexus-testcase-nospring'
                    Tests = @(
                        'EngineDiagnosticsFeatureTest',
                        'ExecutionStatisticsFeatureTest',
                        'MetadataUsageExampleTest',
                        'ProtocolNegotiationFeatureTest',
                        'ResultMetadataCategoryFeatureTest',
                        'ResultMetadataConfigurationFeatureTest',
                        'ResultMetadataPolicyFeatureTest',
                        'ResultMetadataPolicyRegistryFeatureTest',
                        'ResultMetadataPolicyRegistryLoaderFeatureTest',
                        'ResultMetadataProfileFeatureTest'
                    )
                }
            )
        }
        SpringBoot = @{
            Description = 'Focused verification for the Spring Boot starter, configuration binding, modules integration, async execution, and batch execution.'
            Plans = @(
                @{
                    Name = 'SpringBoot'
                    Module = 'lingonexus-testcase/lingonexus-testcase-springboot'
                    Tests = @(
                        'SpringBootBasicIntegrationTest',
                        'SpringBootConfigurationTest',
                        'SpringBootModulesIntegrationTest',
                        'SpringBootAsyncExecutionTest',
                        'SpringBootBatchExecutionTest'
                    )
                }
            )
        }
        Performance = @{
            Description = 'Focused verification for No-Spring performance baselines plus Spring Boot throughput and stress scenarios.'
            Plans = @(
                @{
                    Name = 'Performance NoSpring'
                    Module = 'lingonexus-testcase/lingonexus-testcase-nospring'
                    Tests = @(
                        'ExternalProcessPerformanceBaselineTest',
                        'IsolationModeComparisonBaselineTest',
                        'LargeContextPerformanceBaselineTest',
                        'LargeContextIsolationModeComparisonBaselineTest',
                        'JavaJaninoCacheIdentityPerformanceBaselineTest'
                    )
                },
                @{
                    Name = 'Performance SpringBoot'
                    Module = 'lingonexus-testcase/lingonexus-testcase-springboot'
                    Tests = @(
                        'SpringBootPerformanceTest',
                        'SpringBootStressTest'
                    )
                }
            )
        }
    }
}

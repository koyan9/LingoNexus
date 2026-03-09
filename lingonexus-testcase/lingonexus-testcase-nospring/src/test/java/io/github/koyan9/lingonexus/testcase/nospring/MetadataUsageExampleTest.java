/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.koyan9.lingonexus.testcase.nospring;

import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.MetadataKeys;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Metadata 使用示例测试
 * <p>演示如何使用 ScriptContext 和 ScriptResult 的 metadata 功能</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-26
 */
@DisplayName("Metadata Usage Examples")
public class MetadataUsageExampleTest {

    private LingoNexusExecutor facade;

    @BeforeEach
    void setUp() {
        CacheConfig cacheConfig = CacheConfig.builder()
                .enabled(true)
                .build();
        SandboxConfig sandboxConfig = SandboxConfig.builder()
                .build();
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(cacheConfig)
                .sandboxConfig(sandboxConfig)
                .build();

        facade = LingoNexusBuilder.loadInstance(config);
    }

    @Test
    @DisplayName("Example 1: Request Tracking with Metadata")
    void example1_requestTracking() {
        // Given - 创建带有请求追踪信息的上下文
        String requestId = "req-" + UUID.randomUUID();
        String userId = "user-001";

        ScriptContext context = ScriptContext.builder()
                .put("orderId", "ORDER-12345")
                .putMetadata("requestId", requestId)
                .putMetadata("userId", userId)
                .putMetadata("timestamp", System.currentTimeMillis())
                .build();

        // When - 执行脚本
        ScriptResult result = facade.execute(
                "orderId.toUpperCase()",
                "groovy",
                context
        );

        // Then - 验证结果和元数据
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("ORDER-12345");

        // 验证输入元数据可以被读取
        Map<String, Object> inputMetadata = context.getMetadata();
        assertThat(inputMetadata.get("requestId")).isEqualTo(requestId);
        assertThat(inputMetadata.get("userId")).isEqualTo(userId);
        assertThat(inputMetadata.get("timestamp")).isNotNull();

        // 验证输出元数据
        Map<String, Object> outputMetadata = result.getMetadata();
        assertThat(outputMetadata).isNotNull();
        assertThat(outputMetadata.get(ResultMetadataKeys.SCRIPT_ENGINE)).isEqualTo("groovy");
        assertThat(outputMetadata.get(ResultMetadataKeys.THREAD_ID)).isNotNull();
        assertThat(outputMetadata.get(ResultMetadataKeys.THREAD_NAME)).isNotNull();
        assertThat(outputMetadata.get(ResultMetadataKeys.CACHE_HIT)).isNotNull();
        assertThat(outputMetadata.get(ResultMetadataKeys.COMPILE_TIME)).isNotNull();
        assertThat(outputMetadata.get(ResultMetadataKeys.EXECUTION_TIME)).isNotNull();
        assertThat(outputMetadata.get(ResultMetadataKeys.TOTAL_TIME)).isNotNull();
        assertThat(outputMetadata.get(ResultMetadataKeys.MODULES_USED)).isNotNull();

        System.out.println("Request ID: " + inputMetadata.get("requestId"));
        System.out.println("User ID: " + inputMetadata.get("userId"));
        System.out.println("Execution Time: " + result.getExecutionTime() + "ms");
        System.out.println("Output Metadata: " + outputMetadata);
    }

    @Test
    @DisplayName("Example 2: Debug Mode with Metadata")
    void example2_debugMode() {
        // Given - 开启调试模式
        ScriptContext context = ScriptContext.builder()
                .put("input", "hello world")
                .putMetadata("debugMode", true)
                .putMetadata("logLevel", "DEBUG")
                .build();

        // When - 执行脚本
        ScriptResult result = facade.execute(
                "input.toUpperCase()",
                "groovy",
                context
        );

        // Then - 在调试模式下输出详细信息
        if (Boolean.TRUE.equals(context.getMetadata().get("debugMode"))) {
            System.out.println("=== Debug Information ===");
            System.out.println("Input: " + context.getVariable("input"));
            System.out.println("Output: " + result.getValue());
            System.out.println("Execution Time: " + result.getExecutionTime() + "ms");
            System.out.println("Status: " + result.getStatus());

            // 输出元数据信息
            Map<String, Object> outputMetadata = result.getMetadata();
            System.out.println("Script Engine: " + outputMetadata.get(ResultMetadataKeys.SCRIPT_ENGINE));
            System.out.println("Cache Hit: " + outputMetadata.get(ResultMetadataKeys.CACHE_HIT));
            System.out.println("Compile Time: " + outputMetadata.get(ResultMetadataKeys.COMPILE_TIME));
            System.out.println("Execution Time: " + outputMetadata.get(ResultMetadataKeys.EXECUTION_TIME));
            System.out.println("Modules Used: " + outputMetadata.get(ResultMetadataKeys.MODULES_USED));
        }

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("HELLO WORLD");
    }

    @Test
    @DisplayName("Example 3: Multi-tenant Isolation with Metadata")
    void example3_multiTenantIsolation() {
        // Given - 租户 A 的上下文
        ScriptContext contextA = ScriptContext.builder()
                .put("data", "Tenant A Data")
                .putMetadata("tenantId", "tenant-a")
                .putMetadata("environment", "production")
                .build();

        // Given - 租户 B 的上下文
        ScriptContext contextB = ScriptContext.builder()
                .put("data", "Tenant B Data")
                .putMetadata("tenantId", "tenant-b")
                .putMetadata("environment", "production")
                .build();

        // When - 分别执行
        ScriptResult resultA = facade.execute("data", "groovy", contextA);
        ScriptResult resultB = facade.execute("data", "groovy", contextB);

        // Then - 验证隔离
        assertThat(resultA.getValue()).isEqualTo("Tenant A Data");
        assertThat(resultB.getValue()).isEqualTo("Tenant B Data");

        assertThat(contextA.getMetadata().get("tenantId")).isEqualTo("tenant-a");
        assertThat(contextB.getMetadata().get("tenantId")).isEqualTo("tenant-b");
    }

    @Test
    @DisplayName("Example 4: Performance Monitoring with Metadata")
    void example4_performanceMonitoring() {
        // Given - 带性能监控标记的上下文
        ScriptContext context = ScriptContext.builder()
                .put("n", 1000)
                .putMetadata("monitoringEnabled", true)
                .putMetadata("performanceThreshold", 100L) // 100ms
                .build();

        // When - 执行脚本
        long startTime = System.currentTimeMillis();
        ScriptResult result = facade.execute(
                "(1..n).sum()",
                "groovy",
                context
        );
        long totalTime = System.currentTimeMillis() - startTime;

        // Then - 性能监控
        if (Boolean.TRUE.equals(context.getMetadata().get("monitoringEnabled"))) {
            Long threshold = (Long) context.getMetadata().get("performanceThreshold");

            System.out.println("=== Performance Monitoring ===");
            System.out.println("Total Time: " + totalTime + "ms");
            System.out.println("Execution Time: " + result.getExecutionTime() + "ms");
            System.out.println("Threshold: " + threshold + "ms");

            if (totalTime > threshold) {
                System.out.println("WARNING: Execution time exceeded threshold!");
            }

            // 输出性能指标
            Map<String, Object> outputMetadata = result.getMetadata();
            System.out.println("Compile Time: " + outputMetadata.get(ResultMetadataKeys.COMPILE_TIME));
            System.out.println("Execution Time: " + outputMetadata.get(ResultMetadataKeys.EXECUTION_TIME));
            System.out.println("Total Time: " + outputMetadata.get(ResultMetadataKeys.TOTAL_TIME));
            System.out.println("Cache Hit: " + outputMetadata.get(ResultMetadataKeys.CACHE_HIT));
        }

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Example 5: Business Context with Metadata")
    void example5_businessContext() {
        // Given - 业务上下文
        ScriptContext context = ScriptContext.builder()
                .put("amount", 100.0)
                .put("currency", "USD")
                .putMetadata("businessScene", "payment")
                .putMetadata("version", "v1.0")
                .putMetadata("region", "US")
                .build();

        // When - 执行脚本
        ScriptResult result = facade.execute(
                "amount * 1.1", // 加 10% 手续费
                "groovy",
                context
        );

        // Then - 验证业务上下文
        assertThat(result.isSuccess()).isTrue();
        assertThat((Double) result.getValue()).isCloseTo(110.0, org.assertj.core.data.Offset.offset(0.01));

        Map<String, Object> metadata = context.getMetadata();
        assertThat(metadata.get("businessScene")).isEqualTo("payment");
        assertThat(metadata.get("version")).isEqualTo("v1.0");
        assertThat(metadata.get("region")).isEqualTo("US");

        // 可以根据业务场景进行不同的处理
        String scene = (String) metadata.get("businessScene");
        if ("payment".equals(scene)) {
            System.out.println("Processing payment scenario");
            System.out.println("Amount: " + context.getVariable("amount"));
            System.out.println("Currency: " + context.getVariable("currency"));
            System.out.println("Result: " + result.getValue());
        }
    }

    @Test
    @DisplayName("Example 6: Metadata Propagation")
    void example6_metadataPropagation() {
        // Given - 创建带元数据的上下文
        ScriptContext context = ScriptContext.builder()
                .put("value", 42)
                .putMetadata("correlationId", "corr-12345")
                .putMetadata("source", "api-gateway")
                .build();

        // When - 执行多个脚本，传递元数据
        ScriptResult result1 = facade.execute("value * 2", "groovy", context);
        ScriptResult result2 = facade.execute("value * 3", "groovy", context);

        // Then - 验证元数据在多次调用中保持一致
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result2.isSuccess()).isTrue();

        String correlationId = (String) context.getMetadata().get("correlationId");
        assertThat(correlationId).isEqualTo("corr-12345");

        System.out.println("Correlation ID: " + correlationId);
        System.out.println("Result 1: " + result1.getValue());
        System.out.println("Result 2: " + result2.getValue());
    }
}

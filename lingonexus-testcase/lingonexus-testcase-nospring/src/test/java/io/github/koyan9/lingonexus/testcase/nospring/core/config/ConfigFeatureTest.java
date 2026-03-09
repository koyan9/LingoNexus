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
package io.github.koyan9.lingonexus.testcase.nospring.core.config;

import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;
import io.github.koyan9.lingonexus.api.security.ValidationResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 CacheConfig.enabled 和 SandboxConfig.enabled 功能
 *
 * @author lingonexus Team
 */
@DisplayName("Config Feature Tests")
class ConfigFeatureTest {

    @Test
    @DisplayName("Should disable cache when CacheConfig.enabled = false")
    void shouldDisableCacheWhenConfigDisabled() {
        // Given - 创建禁用缓存的配置
        CacheConfig cacheConfig = CacheConfig.builder()
                .enabled(false)
                .build();

        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(cacheConfig)
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);

        // When - 执行相同脚本两次
        String script = "return 1 + 1";
        Map<String, Object> vars = new HashMap<>();

        ScriptResult result1 = executor.execute(script, ScriptLanguage.GROOVY.name(), ScriptContext.of(vars));
        ScriptResult result2 = executor.execute(script, ScriptLanguage.GROOVY.name(), ScriptContext.of(vars));

        // Then - 两次执行都应该成功（即使缓存被禁用）
        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals(2, result1.getValue());
        assertEquals(2, result2.getValue());
    }

    @Test
    @DisplayName("Should enable cache when CacheConfig.enabled = true")
    void shouldEnableCacheWhenConfigEnabled() {
        // Given - 创建启用缓存的配置
        CacheConfig cacheConfig = CacheConfig.builder()
                .enabled(true)
                .build();

        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(cacheConfig)
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);

        // When - 执行相同脚本两次
        String script = "return 2 + 2";
        Map<String, Object> vars = new HashMap<>();

        ScriptResult result1 = executor.execute(script, ScriptLanguage.GROOVY.name(), ScriptContext.of(vars));
        ScriptResult result2 = executor.execute(script, ScriptLanguage.GROOVY.name(), ScriptContext.of(vars));

        // Then - 两次执行都应该成功，第二次应该使用缓存
        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals(4, result1.getValue());
        assertEquals(4, result2.getValue());
        // 第一次执行不应该命中缓存
        assertFalse((Boolean) result1.getMetadata().get("cacheHit"));
        // 第二次执行应该命中缓存
        assertTrue((Boolean) result2.getMetadata().get("cacheHit"));
    }

    @Test
    @DisplayName("Should run custom security policy on every execution even with cache hit")
    void shouldRunCustomSecurityPolicyOnEveryExecutionEvenWithCacheHit() {
        AtomicInteger invocationCount = new AtomicInteger(0);
        SecurityPolicy countingPolicy = new SecurityPolicy() {
            @Override
            public ValidationResult validate(String script, String language, ScriptContext context, SandboxConfig config) {
                invocationCount.incrementAndGet();
                return ValidationResult.success();
            }

            @Override
            public String getName() {
                return "test";
            }
        };

        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(CacheConfig.builder().enabled(true).build())
                .sandboxConfig(SandboxConfig.builder().enabled(true).build())
                .addSecurityPolicy(countingPolicy)
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);
        ScriptContext context = ScriptContext.of(new HashMap<String, Object>());

        ScriptResult result1 = executor.execute("return 3 + 3", ScriptLanguage.GROOVY.name(), context);
        ScriptResult result2 = executor.execute("return 3 + 3", ScriptLanguage.GROOVY.name(), context);

        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals(2, invocationCount.get());
        assertTrue((Boolean) result2.getMetadata().get("cacheHit"));
    }

    @Test
    @DisplayName("Should skip security checks when SandboxConfig.enabled = false")
    void shouldSkipSecurityChecksWhenSandboxDisabled() {
        // Given - 创建禁用沙箱的配置，并添加黑名单
        Set<String> blacklist = new HashSet<>();
        blacklist.add("java.io.*");

        SandboxConfig sandboxConfig = SandboxConfig.builder()
                .enabled(false)  // 禁用沙箱安全检查
                .classBlacklist(blacklist)
                .build();

        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .sandboxConfig(sandboxConfig)
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);

        // When - 执行一个正常的脚本（不涉及黑名单类）
        String script = "return 'Hello World'";
        Map<String, Object> vars = new HashMap<>();

        ScriptResult result = executor.execute(script, ScriptLanguage.GROOVY.name(), ScriptContext.of(vars));

        // Then - 应该成功执行（安全检查被跳过）
        assertTrue(result.isSuccess());
        assertEquals("Hello World", result.getValue());
    }

    @Test
    @DisplayName("Should enforce security checks when SandboxConfig.enabled = true")
    void shouldEnforceSecurityChecksWhenSandboxEnabled() {
        // Given - 创建启用沙箱的配置
        SandboxConfig sandboxConfig = SandboxConfig.builder()
                .enabled(true)  // 启用沙箱安全检查
                .maxScriptSize(100)  // 设置最大脚本大小为 100 字节
                .build();

        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .sandboxConfig(sandboxConfig)
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);

        // When - 执行一个超过大小限制的脚本
        String largeScript = "return '" + "a".repeat(200) + "'";
        Map<String, Object> vars = new HashMap<>();

        ScriptResult result = executor.execute(largeScript, ScriptLanguage.GROOVY.name(), ScriptContext.of(vars));

        // Then - 应该失败（安全检查生效）
        assertFalse(result.isSuccess());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should create isolated instances with createNewInstance()")
    void shouldCreateIsolatedInstancesWithCreateNewInstance() {
        // Given - 创建两个不同配置的实例
        CacheConfig cacheConfig1 = CacheConfig.builder()
                .enabled(true)
                .build();

        LingoNexusConfig config1 = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(cacheConfig1)
                .build();

        CacheConfig cacheConfig2 = CacheConfig.builder()
                .enabled(false)
                .build();

        LingoNexusConfig config2 = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.JAVASCRIPT)
                .cacheConfig(cacheConfig2)
                .build();

        LingoNexusExecutor executor1 = LingoNexusBuilder.createNewInstance(config1);
        LingoNexusExecutor executor2 = LingoNexusBuilder.createNewInstance(config2);

        // When - 在两个实例中执行脚本
        String script = "1 + 1";
        Map<String, Object> vars = new HashMap<>();

        ScriptResult result1 = executor1.execute(script, ScriptLanguage.GROOVY.name(), ScriptContext.of(vars));
        ScriptResult result2 = executor2.execute(script, ScriptLanguage.JAVASCRIPT.name(), ScriptContext.of(vars));

        // Then - 两个实例应该完全隔离，各自正常工作
        assertTrue(result1.isSuccess());
        assertTrue(result2.isSuccess());
        assertEquals(2, result1.getValue());
        assertEquals(2, result2.getValue());
    }
}

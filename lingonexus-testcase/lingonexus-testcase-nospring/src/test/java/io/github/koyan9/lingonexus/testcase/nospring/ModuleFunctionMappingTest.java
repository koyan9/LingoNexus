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
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模块函数映射测试
 * <p>验证 ScriptModule.getFunctions() 的设计约束：
 * <ul>
 *   <li>Map 的 key 必须与模块的公共方法名一致</li>
 *   <li>脚本执行时直接调用模块对象的公共方法</li>
 *   <li>getFunctions() 主要用于元数据查询和文档</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-26
 */
@DisplayName("Module Function Mapping Tests")
public class ModuleFunctionMappingTest {

    private LingoNexusExecutor facade;

    @BeforeEach
    void setUp() {
        CacheConfig cacheConfig = CacheConfig.builder()
                .enabled(false)
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
    @DisplayName("Should call method by actual method name, not by getFunctions() key")
    void shouldCallMethodByActualMethodName() {
        // Given - 创建一个模块，getFunctions() 的 key 与方法名一致
        facade.registerModule(new TestModule());

        // When - 使用实际方法名调用
        ScriptResult result = facade.execute(
                "testModule.process(input)",
                "groovy",
                ScriptContext.of(Map.of("input", "hello"))
        );

        // Then - 应该成功
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("HELLO");
    }

    @Test
    @DisplayName("Should demonstrate that getFunctions() key must match method name")
    void shouldDemonstrateGetFunctionsKeyConstraint() {
        // Given - 创建一个模块，getFunctions() 的 key 与方法名不一致
        facade.registerModule(new TestModuleWithMismatchedKey());

        // When - 尝试使用 getFunctions() 中的 key 调用（这会失败）
        ScriptResult result1 = facade.execute(
                "testModule2.processAlias(input)",  // 使用 getFunctions() 中的 key
                "groovy",
                ScriptContext.of(Map.of("input", "hello"))
        );

        // Then - 应该失败，因为没有 processAlias 方法
        assertThat(result1.isSuccess()).isFalse();

        // When - 使用实际方法名调用
        ScriptResult result2 = facade.execute(
                "testModule2.process(input)",  // 使用实际方法名
                "groovy",
                ScriptContext.of(Map.of("input", "hello"))
        );

        // Then - 应该成功
        assertThat(result2.isSuccess()).isTrue();
        assertThat(result2.getValue()).isEqualTo("HELLO");
    }

    @Test
    @DisplayName("Should use ModuleRegistry.getFunction() to query function metadata")
    void shouldUseModuleRegistryToQueryFunctionMetadata() {
        // Given
        TestModule module = new TestModule();
        facade.registerModule(module);

        // When - 通过 ModuleRegistry 查询函数元数据
        Map<String, Object> functions = module.getFunctions();

        // Then - getFunctions() 返回的 Map 可用于元数据查询
        assertThat(functions).containsKey("process");
        assertThat(functions.get("process")).isInstanceOf(Function.class);
    }

    /**
     * 测试模块 - getFunctions() 的 key 与方法名一致（正确用法）
     */
    public static class TestModule implements ScriptModule {
        @Override
        public String getName() {
            return "testModule";
        }

        @Override
        public Map<String, Object> getFunctions() {
            Map<String, Object> map = new HashMap<>();
            // key "process" 与方法名 process() 一致
            map.put("process", (Function<String, String>) this::process);
            return map;
        }

        public String process(String input) {
            return input.toUpperCase();
        }

        @Override
        public boolean hasFunction(String functionName) {
            return "process".equals(functionName);
        }
    }

    /**
     * 测试模块 - getFunctions() 的 key 与方法名不一致（演示约束）
     */
    public static class TestModuleWithMismatchedKey implements ScriptModule {
        @Override
        public String getName() {
            return "testModule2";
        }

        @Override
        public Map<String, Object> getFunctions() {
            Map<String, Object> map = new HashMap<>();
            // key "processAlias" 与方法名 process() 不一致
            // 这会导致脚本无法通过 "processAlias" 调用方法
            map.put("processAlias", (Function<String, String>) this::process);
            return map;
        }

        public String process(String input) {
            return input.toUpperCase();
        }

        @Override
        public boolean hasFunction(String functionName) {
            return "processAlias".equals(functionName);
        }
    }
}

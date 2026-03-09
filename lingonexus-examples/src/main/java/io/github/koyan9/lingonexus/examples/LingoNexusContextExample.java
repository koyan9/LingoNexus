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
package io.github.koyan9.lingonexus.examples;

import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.LingoNexusContext;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;

/**
 * LingoNexusContext 使用示例
 * <p>演示如何使用 LingoNexusContext 全局访问配置和执行器</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-03-03
 */
public class LingoNexusContextExample {

    public static void main(String[] args) {
        System.out.println("=== LingoNexusContext 使用示例 ===\n");

        // 1. 创建配置
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(CacheConfig.builder()
                        .enabled(true)
                        .maxSize(100)
                        .build())
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(5000)
                        .build())
                .build();

        // 2. 通过 LingoNexusBuilder 初始化（会自动设置全局上下文）
        LingoNexusExecutor executor = LingoNexusBuilder.loadInstance(config);
        System.out.println("✓ LingoNexus 已初始化");

        // 3. 在任何地方通过 LingoNexusContext 获取配置
        LingoNexusConfig globalConfig = LingoNexusContext.getConfig();
        System.out.println("✓ 从全局上下文获取配置:");
        System.out.println("  - 默认语言: " + globalConfig.getDefaultLanguage());
        System.out.println("  - 缓存启用: " + globalConfig.getCacheConfig().isEnabled());
        System.out.println("  - 沙箱启用: " + globalConfig.getSandboxConfig().isEnabled());

        // 4. 在任何地方通过 LingoNexusContext 获取执行器
        LingoNexusExecutor globalExecutor = LingoNexusContext.getExecutor();
        System.out.println("\n✓ 从全局上下文获取执行器");

        // 5. 使用全局执行器执行脚本
        String script = "return 'Hello from LingoNexusContext!'";
        ScriptContext context = ScriptContext.builder().build();
        ScriptResult result = globalExecutor.execute(script, "groovy", context);

        System.out.println("\n✓ 执行脚本结果:");
        System.out.println("  - 状态: " + result.getStatus());
        System.out.println("  - 结果: " + result.getValue());

        // 6. 检查是否已初始化
        boolean initialized = LingoNexusContext.isInitialized();
        System.out.println("\n✓ 上下文已初始化: " + initialized);

        // 7. 演示在其他方法中使用全局上下文
        executeScriptFromAnotherMethod();

        System.out.println("\n=== 示例完成 ===");
    }

    /**
     * 演示在其他方法中使用全局上下文
     */
    private static void executeScriptFromAnotherMethod() {
        System.out.println("\n--- 在其他方法中使用全局上下文 ---");

        // 直接从全局上下文获取执行器，无需传递参数
        LingoNexusExecutor executor = LingoNexusContext.getExecutor();

        String script = "return 1 + 2 + 3";
        ScriptContext context = ScriptContext.builder().build();
        ScriptResult result = executor.execute(script, "groovy", context);

        System.out.println("✓ 计算结果: " + result.getValue());
    }
}

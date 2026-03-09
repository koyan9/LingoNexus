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
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * LingoNexus 快速入门示例
 *
 * <p>演示如何在无 Spring 环境下使用 LingoNexus 脚本引擎</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class QuickStartExample {

    public static void main(String[] args) {
        // 1. 创建完整的引擎配置
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(CacheConfig.builder().enabled(true).build())
                .sandboxConfig(SandboxConfig.builder().build())
                .build();

        // 2. 创建脚本引擎（使用新的 API：通过类自动构造 Sandbox）
        LingoNexusExecutor engine = LingoNexusBuilder.loadInstance(config);

        System.out.println("=== LingoNexus Quick Start Example ===\n");

        // 3. 简单表达式执行
        System.out.println("--- Basic Expression ---");
        Object result = engine.execute("1 + 2 * 3", "groovy", ScriptContext.of(new HashMap<>()));
        System.out.println("1 + 2 * 3 = " + result);

        // 4. 带变量的脚本执行
        System.out.println("\n--- Variables ---");
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", "LingoNexus");
        vars.put("version", "1.0.0");
        result = engine.execute("\"Hello, ${name} ${version}!\"", "groovy", ScriptContext.of(vars));
        System.out.println("Template result: " + result);

        // 5. 使用工具模块
        System.out.println("\n--- Utility Modules ---");

        // 数学运算
        result = engine.execute("math.add(0.1, 0.2)", "groovy", ScriptContext.of(new HashMap<>()));
        System.out.println("math.add(0.1, 0.2) = " + result);

        // 字符串处理
        Map<String, Object> strVars = new HashMap<>();
        strVars.put("text", "  Hello World  ");
        result = engine.execute("str.trim(text)", "groovy", ScriptContext.of(strVars));
        System.out.println("str.trim('  Hello World  ') = '" + result + "'");

        // 日期处理
        result = engine.execute("date.now('yyyy-MM-dd')", "groovy", ScriptContext.of(new HashMap<>()));
        System.out.println("date.now('yyyy-MM-dd') = " + result);

        // 6. 复杂业务逻辑
        System.out.println("\n--- Business Logic ---");
        String script = """
                def price = basePrice * quantity
                def discount = price > 1000 ? 0.1 : 0
                def finalPrice = math.multiply(price, (1 - discount))
                return math.scale(finalPrice, 2, 'HALF_UP')
                """;
        Map<String, Object> orderVars = new HashMap<>();
        orderVars.put("basePrice", 299.99);
        orderVars.put("quantity", 5);
        result = engine.execute(script, "groovy", ScriptContext.of(orderVars));
        System.out.println("Order calculation: basePrice=299.99, quantity=5 => finalPrice=" + result);

        // 7. 支持的语言列表
        System.out.println("\n--- Supported Languages ---");
        System.out.println("Languages: " + engine.getSupportedLanguages());

        System.out.println("\n=== Example Completed ===");
    }
}

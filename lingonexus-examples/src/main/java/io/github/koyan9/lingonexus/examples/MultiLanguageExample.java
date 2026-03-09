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
 * 多语言脚本执行示例
 *
 * <p>演示如何在同一引擎中执行多种脚本语言</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class MultiLanguageExample {

    public static void main(String[] args) {
        // 创建引擎配置
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(CacheConfig.builder().enabled(true).build())
                .sandboxConfig(SandboxConfig.builder().build())
                .build();

        // 创建支持多语言的脚本引擎
        LingoNexusExecutor engine = LingoNexusBuilder.loadInstance(config);

        System.out.println("=== Multi-Language Script Execution Example ===\n");
        System.out.println("Supported languages: " + engine.getSupportedLanguages() + "\n");

        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 10);
        vars.put("b", 5);

        // 1. Groovy 脚本
        System.out.println("--- Groovy ---");
        String groovyScript = """
                def result = a + b
                return "Groovy says: ${a} + ${b} = ${result}"
                """;
        Object result = engine.execute(groovyScript, "groovy", ScriptContext.of(vars));
        System.out.println(result);

        // 2. JavaScript 脚本
        System.out.println("\n--- JavaScript ---");
        String jsScript = """
                var result = a + b;
                'JavaScript says: ' + a + ' + ' + b + ' = ' + result;
                """;
        result = engine.execute(jsScript, "javascript", ScriptContext.of(vars));
        System.out.println(result);

        // 3. Java 表达式 (liquor-eval)
        System.out.println("\n--- Java Expression ---");
        String javaExpr = "\"Java says: \" + a + \" + \" + b + \" = \" + (a + b)";
        result = engine.execute(javaExpr, "javaexpr", ScriptContext.of(vars));
        System.out.println(result);

        // 4. 各语言的特性展示
        System.out.println("\n--- Language Features ---");

        // Groovy: 闭包和集合操作
        System.out.println("\nGroovy - Closure and Collection:");
        String groovyClosure = """
                def numbers = [1, 2, 3, 4, 5]
                def doubled = numbers.collect { it * 2 }
                return doubled.join(', ')
                """;
        result = engine.execute(groovyClosure, "groovy", ScriptContext.of(new HashMap<>()));
        System.out.println("Doubled: " + result);

        // JavaScript: Array methods
        System.out.println("\nJavaScript - Array Methods:");
        String jsArray = """
                var numbers = [1, 2, 3, 4, 5];
                var doubled = numbers.map(function(n) { return n * 2; });
                doubled.join(', ');
                """;
        result = engine.execute(jsArray, "javascript", ScriptContext.of(new HashMap<>()));
        System.out.println("Doubled: " + result);

        // 5. 跨语言数据共享
        System.out.println("\n--- Cross-Language Data Sharing ---");
        Map<String, Object> sharedData = new HashMap<>();
        sharedData.put("user", Map.of("name", "Alice", "age", 25));

        String groovyAccess = "\"Groovy: ${user.name} is ${user.age} years old\"";
        System.out.println(engine.execute(groovyAccess, "groovy", ScriptContext.of(sharedData)));

        String jsAccess = "'JavaScript: ' + user.name + ' is ' + user.age + ' years old'";
        System.out.println(engine.execute(jsAccess, "javascript", ScriptContext.of(sharedData)));

        System.out.println("\n=== Example Completed ===");
    }
}

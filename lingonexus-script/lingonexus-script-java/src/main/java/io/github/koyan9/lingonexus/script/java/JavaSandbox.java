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

package io.github.koyan9.lingonexus.script.java;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.compiled.CompileFunction;
import io.github.koyan9.lingonexus.api.compiled.DefaultCompiledScript;
import io.github.koyan9.lingonexus.api.compiled.ExecuteFunction;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.exception.ScriptCompilationException;
import io.github.koyan9.lingonexus.api.exception.ScriptRuntimeException;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.sandbox.AbstractScriptSandbox;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxProviderMetadata;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.security.RestrictedClassLoader;
import io.github.koyan9.lingonexus.api.security.StaticSecurityChecker;
import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.ScriptEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java script sandbox implementation using Janino with ClassLoader-based access control
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Full Java syntax support (classes, methods, loops, etc.)</li>
 *   <li>High performance with compilation to bytecode</li>
 *   <li>Lightweight dependency (~300KB)</li>
 *   <li>Runtime class access control via custom ClassLoader</li>
 *   <li>Context-aware compilation: compile() with ScriptContext for true compilation</li>
 * </ul>
 *
 * <p>Security: Uses a custom ClassLoader to control class loading at runtime,
 * which is more secure and performant than static analysis of import statements.</p>
 *
 * <p>Caching: Script compilation is managed by the external ScriptCacheManager,
 * which provides unified caching across all script languages.</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-28
 */
@SandboxProviderMetadata(providerId = "janino-java", priority = 50, hostAccessMode = SandboxHostAccessMode.BYTECODE_COMPILER, hostRestrictionMode = SandboxHostRestrictionMode.MODERATE, supportsEngineCache = false, externalProcessCompatible = true, resultTransportMode = SandboxResultTransportMode.JSON_SAFE_RESULT_AND_METADATA, languages = {"java", "janino"})
public class JavaSandbox extends AbstractScriptSandbox {

    private static final Logger logger = LoggerFactory.getLogger(JavaSandbox.class);
    private final RestrictedClassLoader restrictedClassLoader;

    public JavaSandbox(EngineConfig engineConfig) {
        super(engineConfig);

        // Build allowed patterns for Java framework classes
        Set<String> allowedPatterns = new HashSet<>();
        allowedPatterns.add("java.**");
        allowedPatterns.add("javax.**");
        allowedPatterns.add("org.codehaus.janino.**");
        allowedPatterns.add("org.codehaus.commons.compiler.**");
        allowedPatterns.add("io.github.koyan9.lingonexus.**");

        this.restrictedClassLoader = new RestrictedClassLoader(
                sandboxConfig.isEnabled(),
                sandboxConfig.getClassWhitelist(),
                sandboxConfig.getClassBlacklist(),
                allowedPatterns,
                Thread.currentThread().getContextClassLoader()
        );
        logger.info("JavaSandbox initialized | Security: ClassLoader-based | Context-aware compilation: enabled");
    }

    @Override
    public List<String> getSupportedLanguages() {
        return ScriptLanguage.JAVA.getAliases();
    }

    @Override
    public boolean supports(String language) {
        return ScriptLanguage.JAVA.matches(language);
    }

    @Override
    public CompiledScript compile(String script, ScriptContext context) {
        logger.debug("Compiling Java script with context: length={}", script != null ? script.length() : 0);

        // Static security check (if enabled)
        if (sandboxConfig.isEnabled()) {
            StaticSecurityChecker.checkScript(script, sandboxConfig.getClassBlacklist(), sandboxConfig.getClassWhitelist());
        }

        try {
            final boolean isExpression = isSimpleExpression(script);

            if (isExpression) {
                // Use ExpressionEvaluator for simple expressions
                return compileExpressionWithContext(script, context);
            } else {
                // Use ScriptEvaluator for full scripts
                return compileScriptWithContext(script, context);
            }
        } catch (Exception e) {
            logger.error("Java compilation failed: {}", e.getMessage(), e);
            throw new ScriptCompilationException("Failed to compile Java script", e);
        }
    }


    /**
     * Determine if the script is a simple expression
     */
    private boolean isSimpleExpression(String script) {
        if (script == null || script.trim().isEmpty()) {
            return true;
        }
        String trimmed = script.trim();
        // If contains keywords like class, import, return, or semicolons/braces, treat as script
        return !trimmed.contains(";")
                && !trimmed.contains("class ")
                && !trimmed.contains("import ")
                && !trimmed.contains("return ")
                && !trimmed.contains("{")
                && !trimmed.contains("for ")
                && !trimmed.contains("while ")
                && !trimmed.contains("if ");
    }

    /**
     * Compile a simple expression with context (真正的编译)
     */
    private CompiledScript compileExpressionWithContext(String expression, ScriptContext context) throws Exception {
        // 定义编译函数：创建并编译 ExpressionEvaluator
        CompileFunction compileFunction = (src, ctx) -> {
            ExpressionEvaluator evaluator = new ExpressionEvaluator();
            evaluator.setExpressionType(Object.class);
            evaluator.setParentClassLoader(restrictedClassLoader);

            // Set parameters based on context
            Map<String, Object> variables = ctx != null ? ctx.getVariables() : null;
            if (variables != null && !variables.isEmpty()) {
                String[] paramNames = variables.keySet().toArray(new String[0]);
                Class<?>[] paramTypes = new Class<?>[paramNames.length];
                for (int i = 0; i < paramNames.length; i++) {
                    Object value = variables.get(paramNames[i]);
                    paramTypes[i] = value != null ? value.getClass() : Object.class;
                }
                evaluator.setParameters(paramNames, paramTypes);
            }

            // 真正的编译发生在这里
            evaluator.cook(src);
            return evaluator;
        };

        // 定义执行函数：使用已编译的 Evaluator 执行
        ExecuteFunction executeFunction = (compiled, ctx) -> {
            ExpressionEvaluator evaluator = (ExpressionEvaluator) compiled;
            try {
                return executeWithIsolation(ctx, () -> evaluateExpression(evaluator, ctx), restrictedClassLoader);
            } catch (InvocationTargetException e) {
                logger.error("Java expression execution failed: {}", e.getTargetException().getMessage(), e);
                throw new ScriptRuntimeException("Failed to execute Java expression", e.getTargetException());
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                logger.error("Java expression execution failed: {}", e.getMessage(), e);
                throw new ScriptRuntimeException("Failed to execute Java expression", e);
            }
        };

        // 使用 DefaultCompiledScript 替代匿名内部类
        return new DefaultCompiledScript(
                ScriptLanguage.JAVA,
                expression,
                context,
                compileFunction,
                executeFunction
        );
    }

    /**
     * Compile a full script with context (真正的编译)
     */
    private CompiledScript compileScriptWithContext(String script, ScriptContext context) throws Exception {
        // 定义编译函数：创建并编译 ScriptEvaluator
        CompileFunction compileFunction = (src, ctx) -> {
            ScriptEvaluator evaluator = new ScriptEvaluator();
            evaluator.setReturnType(Object.class);
            evaluator.setParentClassLoader(restrictedClassLoader);

            // Set parameters based on context
            Map<String, Object> variables = ctx != null ? ctx.getVariables() : null;
            if (variables != null && !variables.isEmpty()) {
                String[] paramNames = variables.keySet().toArray(new String[0]);
                Class<?>[] paramTypes = new Class<?>[paramNames.length];
                for (int i = 0; i < paramNames.length; i++) {
                    Object value = variables.get(paramNames[i]);
                    paramTypes[i] = value != null ? value.getClass() : Object.class;
                }
                evaluator.setParameters(paramNames, paramTypes);
            }

            // 真正的编译发生在这里
            evaluator.cook(src);
            return evaluator;
        };

        // 定义执行函数：使用已编译的 Evaluator 执行
        ExecuteFunction executeFunction = (compiled, ctx) -> {
            ScriptEvaluator evaluator = (ScriptEvaluator) compiled;
            try {
                return executeWithIsolation(ctx, () -> evaluateScript(evaluator, ctx), restrictedClassLoader);
            } catch (InvocationTargetException e) {
                logger.error("Java script execution failed: {}", e.getTargetException().getMessage(), e);
                throw new ScriptRuntimeException("Failed to execute Java script", e.getTargetException());
            } catch (Exception e) {
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                logger.error("Java script execution failed: {}", e.getMessage(), e);
                throw new ScriptRuntimeException("Failed to execute Java script", e);
            }
        };

        // 使用 DefaultCompiledScript 替代匿名内部类
        return new DefaultCompiledScript(
                ScriptLanguage.JAVA,
                script,
                context,
                compileFunction,
                executeFunction
        );
    }

    private Object evaluateExpression(ExpressionEvaluator evaluator, ScriptContext context) throws Exception {
        Map<String, Object> variables = context != null ? context.getVariables() : null;
        if (variables != null && !variables.isEmpty()) {
            Object[] paramValues = variables.values().toArray();
            return evaluator.evaluate(paramValues);
        }
        return evaluator.evaluate(new Object[0]);
    }

    private Object evaluateScript(ScriptEvaluator evaluator, ScriptContext context) throws Exception {
        Map<String, Object> variables = context != null ? context.getVariables() : null;
        if (variables != null && !variables.isEmpty()) {
            Object[] paramValues = variables.values().toArray();
            return evaluator.evaluate(paramValues);
        }
        return evaluator.evaluate(new Object[0]);
    }

}

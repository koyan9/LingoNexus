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

package io.github.koyan9.lingonexus.script.kotlin;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.compiled.CompileFunction;
import io.github.koyan9.lingonexus.api.compiled.DefaultCompiledScript;
import io.github.koyan9.lingonexus.api.compiled.ExecuteFunction;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.exception.ScriptCompilationException;
import io.github.koyan9.lingonexus.api.exception.ScriptRuntimeException;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.sandbox.AbstractScriptSandbox;
import io.github.koyan9.lingonexus.api.security.StaticSecurityChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;
import java.util.List;

/**
 * Kotlin sandbox implementation using JSR-223 API for Kotlin script execution
 *
 * <p><strong>Security Model:</strong> This implementation uses static security checks
 * via {@link StaticSecurityChecker} to validate script safety before compilation.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Full Kotlin language support</li>
 *   <li>JSR-223 compatible</li>
 *   <li>Variable binding support</li>
 *   <li>Script compilation and caching</li>
 *   <li>Static security check before compilation</li>
 * </ul>
 *
 * <p><strong>Security:</strong></p>
 * <ul>
 *   <li>Static security check: Pre-compilation source code analysis (detects FQN and imported classes)</li>
 *   <li>Whitelist/blacklist validation before script execution</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-10
 */
@Deprecated
public class KotlinSandbox extends AbstractScriptSandbox {

    private static final Logger logger = LoggerFactory.getLogger(KotlinSandbox.class);

    private final ScriptEngineManager scriptEngineManager;

    /**
     * Constructor for KotlinSandbox
     *
     * <p><strong>Security Model:</strong> This implementation relies on static security checks
     * via {@link StaticSecurityChecker} to validate script safety before compilation.</p>
     *
     * @param engineConfig engine configuration with whitelist/blacklist
     */
    public KotlinSandbox(EngineConfig engineConfig) {
        super(engineConfig);

        // Use normal ClassLoader for ScriptEngineManager initialization
        this.scriptEngineManager = new ScriptEngineManager(Thread.currentThread().getContextClassLoader());

        logger.info("KotlinSandbox initialized with static security checks, whitelist={}, blacklist={}",
                sandboxConfig.getClassWhitelist().size(), sandboxConfig.getClassBlacklist().size());
    }

    @Override
    public List<String> getSupportedLanguages() {
        return ScriptLanguage.KOTLIN.getAliases();
    }

    @Override
    public boolean supports(String language) {
        return ScriptLanguage.KOTLIN.matches(language);
    }

    @Override
    public CompiledScript compile(String script, ScriptContext context) {
        logger.debug("Compiling Kotlin script: length={}", script != null ? script.length() : 0);

        // Static security check (if enabled)
        if (sandboxConfig.isEnabled()) {
            StaticSecurityChecker.checkScript(script, sandboxConfig.getClassBlacklist(), sandboxConfig.getClassWhitelist());
        }

        ScriptEngine engine = getKotlinEngine();

        // Try to compile using JSR-223 Compilable interface
        if (engine instanceof Compilable compilable) {
            // 编译函数：调用 scriptEngine.compile(script) 作为真正的编译
            CompileFunction compileFunction = (src, ctx) -> compilable.compile(src);

            // 执行函数：调用 ((javax.script.CompiledScript)compiled).eval(bindings) 执行
            ExecuteFunction executeFunction = (compiled, ctx) -> {
                Bindings bindings = createBindings(ctx);
                try {
                    return ((javax.script.CompiledScript) compiled).eval(bindings);
                } catch (ScriptException e) {
                    logger.error("Kotlin script execution failed: {}", e.getMessage(), e);
                    throw new ScriptRuntimeException("Failed to execute Kotlin script", e);
                }
            };

            return new DefaultCompiledScript(
                    ScriptLanguage.KOTLIN,
                    script,
                    context,
                    compileFunction,
                    executeFunction
            );
        } else {
            // Fallback: store script source for later interpretation
            CompileFunction compileFunction = (src, ctx) -> src; // 不支持编译，直接返回源码

            ExecuteFunction executeFunction = (compiled, ctx) -> {
                String scriptSource = (String) compiled;
                ScriptEngine scriptEngine = getKotlinEngine();
                Bindings bindings = createBindings(ctx);
                try {
                    return scriptEngine.eval(scriptSource, bindings);
                } catch (ScriptException e) {
                    logger.error("Kotlin execution failed: {}", e.getMessage(), e);
                    throw new ScriptRuntimeException("Failed to execute Kotlin script", e);
                }
            };

            return new DefaultCompiledScript(
                    ScriptLanguage.KOTLIN,
                    script,
                    context,
                    compileFunction,
                    executeFunction
            );
        }
    }


    /**
     * Get Kotlin script engine from JSR-223
     */
    private ScriptEngine getKotlinEngine() {
        ScriptEngine engine = scriptEngineManager.getEngineByExtension("kts");
        if (engine == null) {
            engine = scriptEngineManager.getEngineByName("kotlin");
        }
        if (engine == null) {
            throw new RuntimeException("Kotlin script engine not found. " +
                    "Make sure kotlin-scripting-jsr223 is on the classpath.");
        }
        return engine;
    }

    /**
     * Create JSR-223 bindings from script context
     */
    private Bindings createBindings(ScriptContext context) {
        Bindings bindings = new SimpleBindings();
        if (context != null && context.getVariables() != null) {
            bindings.putAll(context.getVariables());
        }
        return bindings;
    }
}

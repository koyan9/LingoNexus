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

package io.github.koyan9.lingonexus.script.javascript;

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
import io.github.koyan9.lingonexus.api.security.StaticSecurityChecker;
import org.graalvm.polyglot.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * GraalJS sandbox implementation for JavaScript script execution with class access control
 *
 * <p>Features:</p>
 * <ul>
 *   <li>High performance JavaScript execution via GraalJS</li>
 *   <li>Java class access control via SandboxConfig whitelist/blacklist</li>
 *   <li>Host access control for security</li>
 *   <li>Script caching support</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-06
 */
@SandboxProviderMetadata(providerId = "graaljs", priority = 50, hostAccessMode = SandboxHostAccessMode.POLYGLOT_HOST, hostRestrictionMode = SandboxHostRestrictionMode.MODERATE, supportsEngineCache = false, externalProcessCompatible = true, resultTransportMode = SandboxResultTransportMode.JSON_SAFE_RESULT_AND_METADATA, languages = {"javascript", "js", "ecmascript"})
public class GraalJSSandbox extends AbstractScriptSandbox {

    private static final Logger logger = LoggerFactory.getLogger(GraalJSSandbox.class);
    private static final String LANGUAGE_ID = "js";

    private final Engine graalEngine;

    public GraalJSSandbox(EngineConfig engineConfig) {
        super(engineConfig);
        this.graalEngine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();
        logger.info("GraalJSSandbox initialized with class access control");
    }

    @Override
    public List<String> getSupportedLanguages() {
        return ScriptLanguage.JAVASCRIPT.getAliases();
    }

    @Override
    public boolean supports(String language) {
        return ScriptLanguage.JAVASCRIPT.matches(language);
    }

    @Override
    public CompiledScript compile(String script, ScriptContext context) {
        ensureActive();
        logger.debug("Compiling JavaScript script: length={}", script != null ? script.length() : 0);

        // Static security check (if enabled)
        if (sandboxConfig.isEnabled()) {
            StaticSecurityChecker.checkScript(script, sandboxConfig.getClassBlacklist(), sandboxConfig.getClassWhitelist());
        }

        try {
            return new DefaultCompiledScript(
                    ScriptLanguage.JAVASCRIPT,
                    script,
                    context,
                    (src, ctx) -> {
                        // 真正的编译：使用 GraalJS 解析脚本为 Source 对象
                        // GraalJS 的 Source 对象本身就是编译后的结果，不需要执行
                        return Source.newBuilder(LANGUAGE_ID, src, "script.js")
                                .cached(true)
                                .build();
                    },
                    (compiled, ctx) -> {
                        // 执行逻辑：使用已编译的 Source 对象执行
                        Source source = (Source) compiled;

                        try {
                            return executeWithIsolation(ctx, () -> {
                                try (Context graalContext = createContext()) {
                                    bindVariables(graalContext, ctx);
                                    Value result = graalContext.eval(source);
                                    return convertValue(result);
                                }
                            });
                        } catch (Exception e) {
                            if (e instanceof RuntimeException) {
                                throw (RuntimeException) e;
                            }
                            logger.error("JavaScript execution failed: {}", e.getMessage(), e);
                            throw new ScriptRuntimeException("Failed to execute JavaScript script", e);
                        }
                    }
            );
        } catch (Exception e) {
            logger.error("JavaScript compilation failed: {}", e.getMessage(), e);
            throw new ScriptCompilationException("Failed to compile JavaScript script", e);
        }
    }

    @Override
    protected void onShutdown() {
        graalEngine.close();
    }


    /**
     * Create a new GraalJS context with security configuration
     */
    private Context createContext() {
        Context.Builder builder = Context.newBuilder(LANGUAGE_ID)
                .engine(graalEngine)
                .allowHostAccess(HostAccess.newBuilder()
                        .allowPublicAccess(true)
                        .allowArrayAccess(true)
                        .allowListAccess(true)
                        .allowMapAccess(true)
                        .build())
                .allowHostClassLoading(true)
                .option("js.ecmascript-version", "2022");

        // Only apply class access control if sandbox is enabled
        if (sandboxConfig.isEnabled()) {
            Predicate<String> classFilter = this::isClassAllowed;
            builder.allowHostClassLookup(classFilter);
        } else {
            // If sandbox is disabled, allow all class lookups
            builder.allowHostClassLookup(className -> true);
        }

        return builder.build();
    }

    /**
     * Check if a class is allowed to be accessed
     */
    private boolean isClassAllowed(String className) {
        if (className == null) {
            return false;
        }

        Set<String> blacklist = sandboxConfig.getClassBlacklist();
        Set<String> whitelist = sandboxConfig.getClassWhitelist();

        // Check blacklist first (higher priority)
        for (String blackPattern : blacklist) {
            if (matchesPattern(className, blackPattern)) {
                logger.debug("Class denied by blacklist: {}", className);
                return false;
            }
        }

        // Check whitelist
        for (String whitePattern : whitelist) {
            if (matchesPattern(className, whitePattern)) {
                logger.debug("Class allowed by whitelist: {}", className);
                return true;
            }
        }

        logger.debug("Class not in whitelist: {}", className);
        return false;
    }

    /**
     * Check if class name matches pattern (supports wildcard *)
     */
    private boolean matchesPattern(String className, String pattern) {
        if (pattern.endsWith(".*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return className.startsWith(prefix + ".");
        } else {
            return className.equals(pattern);
        }
    }

    /**
     * Bind script context variables to JavaScript context
     */
    private void bindVariables(Context context, ScriptContext scriptContext) {
        if (scriptContext != null && scriptContext.getVariables() != null) {
            Value bindings = context.getBindings(LANGUAGE_ID);
            for (Map.Entry<String, Object> entry : scriptContext.getVariables().entrySet()) {
                bindings.putMember(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Convert GraalJS Value to Java object
     */
    private Object convertValue(Value value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        if (value.isNumber()) {
            if (value.fitsInInt()) {
                return value.asInt();
            }
            if (value.fitsInLong()) {
                return value.asLong();
            }
            return value.asDouble();
        }
        if (value.isString()) {
            return value.asString();
        }
        if (value.hasArrayElements()) {
            int size = (int) value.getArraySize();
            Object[] array = new Object[size];
            for (int i = 0; i < size; i++) {
                array[i] = convertValue(value.getArrayElement(i));
            }
            return array;
        }
        if (value.isHostObject()) {
            return value.asHostObject();
        }
        return value.toString();
    }
}

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

package io.github.koyan9.lingonexus.script.javaexpr;

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
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxProviderMetadata;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.security.RestrictedClassLoader;
import io.github.koyan9.lingonexus.api.security.StaticSecurityChecker;
import org.noear.liquor.eval.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Java expression sandbox implementation using liquor-eval for Java expression evaluation
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Near-native performance with compilation caching</li>
 *   <li>Support for simple expressions (not full scripts)</li>
 *   <li>Java class import control</li>
 *   <li>Lightweight dependency (~100KB)</li>
 * </ul>
 *
 * <p>Caching Strategy:</p>
 * <ul>
 *   <li>Two-layer caching for optimal performance</li>
 *   <li>Layer 1: liquor-eval built-in cache (controlled by enableEngineCache in SandboxConfig)</li>
 *   <li>Layer 2: ScriptCacheManager at ScriptExecutor layer (Caffeine cache)</li>
 * </ul>
 *
 * <p>Limitations:</p>
 * <ul>
 *   <li>Does not support complex statements (return, loops, class definitions)</li>
 *   <li>Best suited for simple business rule expressions</li>
 * </ul>
 *
 * <p>For full Java script support, use JavaSandbox instead.</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
@SandboxProviderMetadata(providerId = "javaexpr", priority = 50, hostAccessMode = SandboxHostAccessMode.EXPRESSION_ENGINE, hostRestrictionMode = SandboxHostRestrictionMode.STRICT, supportsEngineCache = true, externalProcessCompatible = true, resultTransportMode = SandboxResultTransportMode.JSON_SAFE_RESULT_AND_METADATA, languages = {"javaexpr", "jexpr"})
public class JavaExprSandbox extends AbstractScriptSandbox {

    private static final Logger logger = LoggerFactory.getLogger(JavaExprSandbox.class);

    private final boolean useCache;
    private final Evaluator evaluator;
    private final RestrictedClassLoader restrictedClassLoader;

    public JavaExprSandbox(EngineConfig engineConfig) {
        super(engineConfig);
        this.useCache = sandboxConfig.isEnableEngineCache();

        // Build allowed patterns for Java framework classes
        Set<String> allowedPatterns = new HashSet<>();
        allowedPatterns.add("java.**");
        allowedPatterns.add("javax.**");
        allowedPatterns.add("org.noear.liquor.**");
        allowedPatterns.add("io.github.koyan9.lingonexus.**");

        // Create RestrictedClassLoader with whitelist/blacklist
        // System classes (java.*, javax.*, etc.) are allowed to pass through
        // Only non-system classes are checked against whitelist/blacklist
        restrictedClassLoader = new RestrictedClassLoader(
                sandboxConfig.isEnabled(),
                sandboxConfig.getClassWhitelist(),
                sandboxConfig.getClassBlacklist(),
                allowedPatterns,
                Thread.currentThread().getContextClassLoader()
        );

        // Create custom LiquorEvaluator instance with RestrictedClassLoader
        this.evaluator = new LiquorEvaluator(restrictedClassLoader);

        logger.info("JavaExprSandbox initialized | Security: ClassLoader-based | Whitelist: {} patterns | Blacklist: {} patterns | Framework allowed: {} patterns",
                sandboxConfig.getClassWhitelist().size(), sandboxConfig.getClassBlacklist().size(), allowedPatterns.size());
    }

    @Override
    public List<String> getSupportedLanguages() {
        return ScriptLanguage.JAVAEXPR.getAliases();
    }

    @Override
    public boolean supports(String language) {
        return ScriptLanguage.JAVAEXPR.matches(language);
    }

    @Override
    public CompiledScript compile(String script, ScriptContext context) {
        logger.debug("Compiling Java script: length={}", script != null ? script.length() : 0);

        // Static security check (if enabled)
        if (sandboxConfig.isEnabled()) {
            StaticSecurityChecker.checkScript(script, sandboxConfig.getClassBlacklist(), sandboxConfig.getClassWhitelist());
        }

        try {
            // 编译函数：调用 liquor-eval 的 compile 方法
            CompileFunction compileFunction = (src, ctx) -> {
                Map<String, Object> variables = ctx != null ? ctx.getVariables() : null;
                boolean shouldSetReturnType = shouldSetReturnType(src);
                CodeSpec execSpec = createCodeSpec(src, variables, shouldSetReturnType);
                return evaluator.compile(execSpec);
            };

            // 执行函数：调用 Execable.exec() 方法
            ExecuteFunction executeFunction = (compiled, ctx) -> {
                try {
                    return executeWithIsolation(
                            ctx,
                            () -> ((Execable) compiled).exec(ctx != null ? ctx.getVariables() : null),
                            restrictedClassLoader
                    );
                } catch (Exception e) {
                    if (e instanceof RuntimeException) {
                        throw (RuntimeException) e;
                    }
                    logger.error("Java script execution failed: {}", e.getMessage(), e);
                    throw new ScriptRuntimeException("Failed to execute Java script", e);
                }
            };

            return new DefaultCompiledScript(
                    ScriptLanguage.JAVAEXPR,
                    script,
                    context,
                    compileFunction,
                    executeFunction
            );
        } catch (Exception e) {
            logger.error("Java compilation failed: {}", e.getMessage(), e);
            throw new ScriptCompilationException("Failed to compile Java script", e);
        }
    }


    /**
     * Create CodeSpec with parameters and imports
     *
     * @param script the script source code
     * @param variables the variables to pass to the script
     * @param setReturnType whether to set return type to Object.class
     * @return configured CodeSpec
     */
    private CodeSpec createCodeSpec(String script, Map<String, Object> variables, boolean setReturnType) {
        CodeSpec codeSpec = new CodeSpec(script);

        // Set cache option
        codeSpec.cached(useCache);

        // Add default imports
//        Class<?>[] imports = defaultImports.toArray(new Class<?>[0]);
//        codeSpec.imports(imports);

        // Add parameters if available
        if (variables != null && !variables.isEmpty()) {
            List<ParamSpec> params = new ArrayList<>();
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();
                Class<?> paramType = paramValue != null ? paramValue.getClass() : Object.class;
                params.add(new ParamSpec(paramName, paramType));
            }
            codeSpec.parameters(params.toArray(new ParamSpec[0]));
        }

        // Set return type if requested
        // For scripts that need a return value, set it to Object.class
        // For scripts without return (like forEach), don't set it
        if (setReturnType) {
            codeSpec.returnType(Object.class);
        }

        return codeSpec;
    }

    /**
     * Determine if the script should have a return type set
     * <p>Analysis rules based on liquor-eval's behavior:</p>
     * <ul>
     *   <li>If script has no semicolon → it's an expression → needs return type</li>
     *   <li>If script has semicolon → analyze the outermost level code:
     *     <ul>
     *       <li>If outermost level ends with "return" statement → needs return type</li>
     *       <li>Otherwise → no return type (void method)</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param script the script to analyze
     * @return true if return type should be set
     */
    private boolean shouldSetReturnType(String script) {
        if (script == null || script.trim().isEmpty()) {
            return false;
        }

        String trimmedScript = script.trim();

        // 1. If script has no semicolon, it's an expression → needs return type
        // liquor-eval will automatically add "return" prefix for expressions
        if (!trimmedScript.contains(";")) {
            return true;
        }

        // 2. For scripts with semicolons, analyze the outermost level code
        // Remove string literals and comments to avoid false positives
        String cleanScript = removeStringLiteralsAndComments(trimmedScript);

        // 3. Find the last statement at the outermost level
        // This is a heuristic approach: check if the script ends with a return statement
        String lastStatement = getLastOutermostStatement(cleanScript);

        // 4. Check if the last outermost statement is a return statement
        if (lastStatement.trim().startsWith("return ")) {
            return true;
        }

        return false;
    }

    /**
     * Get the last statement at the outermost level (not inside methods/classes/lambdas)
     * <p>This is a heuristic approach that works for most common cases:</p>
     * <ul>
     *   <li>Find the last semicolon</li>
     *   <li>Work backwards to find the statement start</li>
     *   <li>Check if this statement is at the outermost level (not inside braces)</li>
     * </ul>
     *
     * @param script the cleaned script (without strings and comments)
     * @return the last outermost statement
     */
    private String getLastOutermostStatement(String script) {
        // Find the last semicolon
        int lastSemicolon = script.lastIndexOf(';');
        if (lastSemicolon == -1) {
            return "";
        }

        // Count braces to determine nesting level
        // We want to find statements at nesting level 0 (outermost)
        // Track both the last and previous outermost semicolon positions
        int braceLevel = 0;
        int lastOutermostSemicolon = -1;
        int prevOutermostSemicolon = -1;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);

            if (c == '{') {
                braceLevel++;
            } else if (c == '}') {
                braceLevel--;
            } else if (c == ';' && braceLevel == 0) {
                // This is a statement end at outermost level
                prevOutermostSemicolon = lastOutermostSemicolon;
                lastOutermostSemicolon = i;
            }
        }

        // If no outermost semicolon found, return empty
        if (lastOutermostSemicolon == -1) {
            return "";
        }

        // Extract the last outermost statement (between previous and last semicolon)
        int statementStart = prevOutermostSemicolon + 1;

        return script.substring(statementStart, lastOutermostSemicolon).trim();
    }

    /**
     * Remove string literals and comments from script to avoid false positives
     *
     * @param script the script to process
     * @return script with string literals and comments removed
     */
    private String removeStringLiteralsAndComments(String script) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        char stringDelimiter = 0;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            char next = (i + 1 < script.length()) ? script.charAt(i + 1) : 0;

            // Handle escape sequences
            if ((inString || inChar) && c == '\\' && next != 0) {
                i++; // Skip next character
                continue;
            }

            // Handle string literals
            if (!inChar && !inLineComment && !inBlockComment) {
                if (c == '"' && !inString) {
                    inString = true;
                    stringDelimiter = '"';
                    continue;
                } else if (c == '"' && inString && stringDelimiter == '"') {
                    inString = false;
                    continue;
                }
            }

            // Handle char literals
            if (!inString && !inLineComment && !inBlockComment) {
                if (c == '\'' && !inChar) {
                    inChar = true;
                    continue;
                } else if (c == '\'' && inChar) {
                    inChar = false;
                    continue;
                }
            }

            // Handle comments
            if (!inString && !inChar) {
                if (c == '/' && next == '/' && !inBlockComment) {
                    inLineComment = true;
                    i++; // Skip next '/'
                    continue;
                } else if (c == '/' && next == '*' && !inLineComment) {
                    inBlockComment = true;
                    i++; // Skip next '*'
                    continue;
                } else if (c == '*' && next == '/' && inBlockComment) {
                    inBlockComment = false;
                    i++; // Skip next '/'
                    continue;
                } else if (c == '\n' && inLineComment) {
                    inLineComment = false;
                    result.append(c); // Keep newline
                    continue;
                }
            }

            // Append character if not in string/comment
            if (!inString && !inChar && !inLineComment && !inBlockComment) {
                result.append(c);
            }
        }

        return result.toString();
    }

}

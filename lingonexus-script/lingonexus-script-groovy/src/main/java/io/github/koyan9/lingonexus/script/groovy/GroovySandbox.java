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

package io.github.koyan9.lingonexus.script.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;
import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.compiled.DefaultCompiledScript;
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
import io.github.koyan9.lingonexus.api.security.ClassLoaderScope;
import io.github.koyan9.lingonexus.api.security.RestrictedClassLoader;
import io.github.koyan9.lingonexus.api.security.StaticSecurityChecker;
import org.codehaus.groovy.ast.expr.ClassExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.StaticMethodCallExpression;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Groovy sandbox implementation with class access control.
 */
@SandboxProviderMetadata(providerId = "groovy", priority = 50, hostAccessMode = SandboxHostAccessMode.JVM_CLASSLOADER, hostRestrictionMode = SandboxHostRestrictionMode.STRICT, supportsEngineCache = false, externalProcessCompatible = true, resultTransportMode = SandboxResultTransportMode.JSON_SAFE_RESULT_AND_METADATA, languages = {"groovy"})
public class GroovySandbox extends AbstractScriptSandbox {

    private static final Logger logger = LoggerFactory.getLogger(GroovySandbox.class);

    private final CompilerConfiguration compilerConfiguration;
    private final RestrictedClassLoader restrictedClassLoader;

    public GroovySandbox(EngineConfig engineConfig) {
        super(engineConfig);
        Set<String> allowedPatterns = new HashSet<>();
        allowedPatterns.add("groovy.**");
        allowedPatterns.add("org.codehaus.groovy.**");
        allowedPatterns.add("java.**");
        allowedPatterns.add("javax.**");
        allowedPatterns.add("io.github.koyan9.lingonexus.**");

        this.compilerConfiguration = createCompilerConfiguration(sandboxConfig);

        this.restrictedClassLoader = new RestrictedClassLoader(
                sandboxConfig.isEnabled(),
                sandboxConfig.getClassWhitelist(),
                sandboxConfig.getClassBlacklist(),
                allowedPatterns,
                Thread.currentThread().getContextClassLoader()
        );

        logger.info(
                "GroovySandbox initialized | Security: AST + Runtime | Whitelist: {} patterns | Blacklist: {} patterns | Framework allowed: {} patterns",
                sandboxConfig.getClassWhitelist().size(),
                sandboxConfig.getClassBlacklist().size(),
                allowedPatterns.size()
        );
    }

    private CompilerConfiguration createCompilerConfiguration(SandboxConfig config) {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();

        ImportCustomizer importCustomizer = new ImportCustomizer();
        configureImports(importCustomizer, config);
        compilerConfig.addCompilationCustomizers(importCustomizer);

        SecureASTCustomizer secureASTCustomizer = new SecureASTCustomizer();
        configureSecureAST(secureASTCustomizer, config);
        compilerConfig.addCompilationCustomizers(secureASTCustomizer);

        return compilerConfig;
    }

    private void configureImports(ImportCustomizer importCustomizer, SandboxConfig config) {
        Set<String> whitelist = config.getClassWhitelist();
        for (String pattern : whitelist) {
            if (pattern.endsWith(".*") || pattern.endsWith(".**")) {
                String pkg = pattern.endsWith(".*")
                        ? pattern.substring(0, pattern.length() - 2)
                        : pattern.substring(0, pattern.length() - 3);
                importCustomizer.addStarImports(pkg);
                logger.debug("Added star import: {}", pkg);
            }
        }
    }

    private void configureSecureAST(SecureASTCustomizer secureASTCustomizer, SandboxConfig config) {
        secureASTCustomizer.setPackageAllowed(false);
        secureASTCustomizer.setClosuresAllowed(true);
        secureASTCustomizer.setMethodDefinitionAllowed(true);

        Set<String> blacklist = config.getClassBlacklist();
        List<String> importsBlacklist = new ArrayList<>(blacklist);
        secureASTCustomizer.setDisallowedImports(importsBlacklist);

        if (!blacklist.isEmpty()) {
            secureASTCustomizer.addExpressionCheckers(expression -> {
                String className = null;

                if (expression instanceof ConstructorCallExpression call) {
                    className = call.getType().getName();
                } else if (expression instanceof StaticMethodCallExpression call) {
                    className = call.getOwnerType().getName();
                } else if (expression instanceof ClassExpression classExpr) {
                    className = classExpr.getType().getName();
                }

                if (className != null && blacklist.contains(className)) {
                    throw new SecurityException("Access to blacklisted class denied: " + className);
                }

                return true;
            });
        }

        logger.debug("Configured SecureASTCustomizer with AST expression checkers: blacklist={}", blacklist.size());
    }

    @Override
    public List<String> getSupportedLanguages() {
        return ScriptLanguage.GROOVY.getAliases();
    }

    @Override
    public boolean supports(String language) {
        return ScriptLanguage.GROOVY.matches(language);
    }

    @Override
    public CompiledScript compile(String script, ScriptContext context) {
        logger.debug("Compiling Groovy script: length={}", script != null ? script.length() : 0);

        if (sandboxConfig.isEnabled()) {
            StaticSecurityChecker.checkScript(script, sandboxConfig.getClassBlacklist(), sandboxConfig.getClassWhitelist());
        }

        try {
            return new DefaultCompiledScript(
                    ScriptLanguage.GROOVY,
                    script,
                    context,
                    (src, ctx) -> {
                        try (GroovyClassLoader compileClassLoader = new GroovyClassLoader(restrictedClassLoader, compilerConfiguration)) {
                            Class<?> scriptClass = compileClassLoader.parseClass(src);
                            logger.debug("Compiled Groovy script to class: {}", scriptClass.getName());
                            return scriptClass;
                        }
                    },
                    (compiled, ctx) -> {
                        Class<?> scriptClass = (Class<?>) compiled;

                        Script groovyScript;
                        try {
                            groovyScript = ClassLoaderScope.call(
                                    restrictedClassLoader,
                                    () -> (Script) scriptClass.getDeclaredConstructor().newInstance()
                            );
                        } catch (Exception e) {
                            throw new ScriptRuntimeException("Failed to create Groovy script instance from compiled class", e);
                        }

                        try {
                            return executeWithIsolation(
                                    ctx,
                                    () -> bindVariablesAndRun(groovyScript, ctx),
                                    restrictedClassLoader
                            );
                        } catch (Exception e) {
                            if (e instanceof RuntimeException) {
                                throw (RuntimeException) e;
                            }
                            throw new ScriptRuntimeException("Failed to execute Groovy script", e);
                        }
                    }
            );
        } catch (Exception e) {
            logger.error("Groovy compilation failed: {}", e.getMessage(), e);
            throw new ScriptCompilationException("Failed to compile Groovy script", e);
        }
    }

    private Object bindVariablesAndRun(Script groovyScript, ScriptContext variables) {
        for (Map.Entry<String, Object> entry : variables.getVariables().entrySet()) {
            groovyScript.getBinding().setVariable(entry.getKey(), entry.getValue());
        }
        return groovyScript.run();
    }
}

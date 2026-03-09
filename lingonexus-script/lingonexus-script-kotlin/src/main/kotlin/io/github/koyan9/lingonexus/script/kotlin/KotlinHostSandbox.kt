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

package io.github.koyan9.lingonexus.script.kotlin

import io.github.koyan9.lingonexus.api.compiled.CompiledScript
import io.github.koyan9.lingonexus.api.compiled.CompileFunction
import io.github.koyan9.lingonexus.api.compiled.DefaultCompiledScript
import io.github.koyan9.lingonexus.api.compiled.ExecuteFunction
import io.github.koyan9.lingonexus.api.config.EngineConfig
import io.github.koyan9.lingonexus.api.context.ScriptContext
import io.github.koyan9.lingonexus.api.exception.ScriptCompilationException
import io.github.koyan9.lingonexus.api.exception.ScriptRuntimeException
import io.github.koyan9.lingonexus.api.exception.ScriptSecurityException
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage
import io.github.koyan9.lingonexus.api.sandbox.AbstractScriptSandbox
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxProviderMetadata
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode
import io.github.koyan9.lingonexus.api.security.RestrictedClassLoader
import io.github.koyan9.lingonexus.api.security.StaticSecurityChecker
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

/**
 * Production-ready Kotlin sandbox with multi-layer security enforcement
 *
 * <p><strong>Three-Layer Security Model:</strong></p>
 * <ul>
 *   <li>Layer 1: Static source code analysis (StaticSecurityChecker) - Fast pre-check</li>
 *   <li>Layer 2: Compilation-time class loading control (RestrictedClassLoader) - Early detection</li>
 *   <li>Layer 3: Runtime class loading control (Isolated thread + RestrictedClassLoader) - Final protection</li>
 * </ul>
 *
 * <p><strong>Key Features:</strong></p>
 * <ul>
 *   <li>✅ Early security violation detection at compilation time</li>
 *   <li>✅ Thread-safe execution with isolated threads</li>
 *   <li>✅ Full module support via Kotlin script API</li>
 *   <li>✅ No thread pollution - main thread ClassLoader unchanged</li>
 *   <li>✅ Comprehensive error handling and reporting</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-11
 */
@SandboxProviderMetadata(providerId = "kotlin-host", priority = 50, hostAccessMode = SandboxHostAccessMode.SCRIPTING_HOST, hostRestrictionMode = SandboxHostRestrictionMode.MODERATE, supportsEngineCache = false, externalProcessCompatible = true, resultTransportMode = SandboxResultTransportMode.JSON_SAFE_RESULT_AND_METADATA, languages = ["kotlin", "kts"])
class KotlinHostSandbox(engineConfig: EngineConfig) : AbstractScriptSandbox(engineConfig) {

    companion object {
        private val logger = LoggerFactory.getLogger(KotlinHostSandbox::class.java)

        // Thread-local registry for complex objects
        private val variableRegistry = ThreadLocal.withInitial<MutableMap<String, Any>> { mutableMapOf() }

        @JvmStatic
        fun getVariable(id: String): Any? {
            return variableRegistry.get()[id]
        }

        @JvmStatic
        fun clearVariables() {
            variableRegistry.get().clear()
        }
    }

    private val scriptingHost = BasicJvmScriptingHost()
    private val restrictedCompilationConfiguration: ScriptCompilationConfiguration
    private val restrictedClassLoader: RestrictedClassLoader
    private val evaluationConfig: ScriptEvaluationConfiguration

    init {
        // Build allowed patterns for Kotlin script engine
        // These are framework/engine classes required for Kotlin scripts to function
        val allowedPatterns = mutableSetOf<String>()

        // JDK standard library classes
        allowedPatterns.add("java.**")
        allowedPatterns.add("javax.**")
        allowedPatterns.add("jdk.**")
        allowedPatterns.add("sun.**")
        allowedPatterns.add("com.sun.**")

        // Kotlin runtime and compiler classes
        allowedPatterns.add("kotlin.**")
        allowedPatterns.add("org.jetbrains.kotlin.**")

        // Kotlin script compiler generated classes (in default package)
        allowedPatterns.add("Script")
        allowedPatterns.add("Script$*")
        allowedPatterns.add("Line_*")

        // LingoNexus framework classes (modules, utils, API, etc.)
        // These include ScriptModule implementations that users can register
        allowedPatterns.add("io.github.koyan9.lingonexus.**")

        // Create RestrictedClassLoader for both compilation and execution
        restrictedClassLoader = RestrictedClassLoader(
            sandboxConfig.classWhitelist,
            sandboxConfig.classBlacklist,
            allowedPatterns,
            Thread.currentThread().contextClassLoader
        )

        // Use RestrictedClassLoader for compilation (early security detection)
        restrictedCompilationConfiguration = ScriptCompilationConfiguration {
            jvm {
                dependenciesFromClassloader(
                    classLoader = restrictedClassLoader,
                    wholeClasspath = true
                )
            }
        }

        // Configure evaluation to support provided properties (for module support)
        evaluationConfig = ScriptEvaluationConfiguration {
            // Enable implicit receivers for module support
            implicitReceivers()
        }

        logger.info(
            "Kotlin sandbox initialized | Security: 3-layer (static + compile-time + runtime) | " +
                    "Whitelist: {} patterns | Blacklist: {} patterns | Framework allowed: {} patterns",
            sandboxConfig.classWhitelist.size, sandboxConfig.classBlacklist.size, allowedPatterns.size
        )
    }

    override fun getSupportedLanguages(): List<String> {
        return ScriptLanguage.KOTLIN.aliases
    }

    override fun supports(language: String): Boolean {
        return ScriptLanguage.KOTLIN.matches(language)
    }

    override fun compile(script: String, context: ScriptContext): CompiledScript {
        logger.debug("Compiling Kotlin script | Length: {} chars", script.length)

        // Layer 1: Static security check (if enabled)
        if (sandboxConfig.isEnabled) {
            StaticSecurityChecker.checkScript(script, sandboxConfig.classBlacklist, sandboxConfig.classWhitelist)
        }

        // Compile with RestrictedClassLoader (early detection)
        val scriptSource = script.toScriptSource()
        val compilationResult = runBlocking {
            scriptingHost.compiler(scriptSource, restrictedCompilationConfiguration)
        }

        // Check compilation result and detect security violations
        when (compilationResult) {
            is ResultWithDiagnostics.Failure -> {
                val errorMessage = compilationResult.reports
                    .filter { it.severity == ScriptDiagnostic.Severity.ERROR }
                    .joinToString("\n") {
                        "${it.message}${if (it.exception != null) ": ${it.exception}" else ""}"
                    }

                // Check if this is a security violation (class loading denied)
                if (isSecurityViolation(errorMessage)) {
                    val securityMessage = extractSecurityMessage(errorMessage)
                    logger.error("Compilation failed | Reason: Security violation | Details: {}", securityMessage)
                    throw ScriptSecurityException(securityMessage)
                }

                // Check if this is an unresolved reference error (variables will be provided at execution time)
                if (errorMessage.contains("Unresolved reference")) {
                    logger.debug("Compilation deferred | Reason: Unresolved references (variables will be provided at execution)")

                    // Return CompiledScript that will compile+execute with context variables
                    return DefaultCompiledScript(
                        ScriptLanguage.KOTLIN,
                        script,
                        context,
                        CompileFunction { src, ctx ->
                            // 延迟编译：直接返回脚本源码，真正的编译在执行时进行
                            src
                        },
                        ExecuteFunction { compiled, ctx ->
                            // Layer 3: Execute in isolated thread with RestrictedClassLoader
                            // This will recompile with provided variables
                            executeWithConfiguredIsolation(compiled as String, ctx)
                        }
                    )
                }

                logger.error("Compilation failed | Reason: Syntax or type error | Details: {}", errorMessage)
                throw ScriptCompilationException("Failed to compile Kotlin script: $errorMessage")
            }

            is ResultWithDiagnostics.Success -> {
                logger.debug("Compilation successful | Script ready for execution")

                // Return CompiledScript wrapper for execution
                return DefaultCompiledScript(
                    ScriptLanguage.KOTLIN,
                    script,
                    context,
                    CompileFunction { src, ctx ->
                        // 真正的编译：使用 Kotlin script compiler
                        val scriptSource = src.toScriptSource()
                        val compilationResult = runBlocking {
                            scriptingHost.compiler(scriptSource, restrictedCompilationConfiguration)
                        }

                        // 返回编译结果
                        when (compilationResult) {
                            is ResultWithDiagnostics.Success -> compilationResult.value
                            is ResultWithDiagnostics.Failure -> {
                                val errorMsg = compilationResult.reports
                                    .filter { it.severity == ScriptDiagnostic.Severity.ERROR }
                                    .joinToString("\n") { "${it.message}" }
                                throw ScriptCompilationException("Compilation failed: $errorMsg")
                            }
                        }
                    },
                    ExecuteFunction { compiled, ctx ->
                        // Layer 3: Execute in isolated thread with RestrictedClassLoader
                        executeWithConfiguredIsolation(script, ctx)
                    }
                )
            }
        }
    }


    /**
     * Execute script in isolated thread with RestrictedClassLoader (Layer 3 security)
     *
     * <p>This method provides runtime security by:</p>
     * <ul>
     *   <li>🧵 Executing in dedicated thread from shared pool</li>
     *   <li>🔒 Setting thread's ContextClassLoader to RestrictedClassLoader</li>
     *   <li>🛡️ Preventing thread pollution of main thread</li>
     *   <li>✅ Ensuring proper cleanup even on exceptions</li>
     *   <li>🔍 Auto-detecting ScriptModule implementations for dynamic class loading</li>
     * </ul>
     */
    private fun executeWithConfiguredIsolation(script: String, context: ScriptContext?): Any? {
        try {
            val scriptSource = script.toScriptSource()

            // Create compilation and evaluation configurations with provided properties
            val (compilationConfig, evaluationConfig) = if (context != null && context.variables != null && context.variables.isNotEmpty()) {
                createConfigurationsWithVariables(context.variables)
            } else {
                Pair(restrictedCompilationConfiguration, this.evaluationConfig)
            }

            // Check if we need to create a custom RestrictedClassLoader for ScriptModule instances
            val effectiveClassLoader = if (context != null && context.variables != null) {
                createClassLoaderWithModuleSupport(context.variables)
            } else {
                restrictedClassLoader
            }

            val evalResult = executeWithIsolation(context, {
                runBlocking {
                    scriptingHost.eval(scriptSource, compilationConfig, evaluationConfig)
                }
            }, effectiveClassLoader)

            // Process evaluation result with security checks
            return processEvaluationResult(evalResult)
        } finally {
            // Clean up thread-local variables to prevent memory leaks
            clearVariables()
        }
    }

    /**
     * Create a RestrictedClassLoader with support for custom ScriptModule implementations
     *
     * <p>This method detects ScriptModule implementations in the context variables and
     * automatically adds their class patterns to the allowed list. This enables users
     * to register custom modules without explicitly configuring the whitelist.</p>
     *
     * @param variables Context variables that may contain ScriptModule instances
     * @return RestrictedClassLoader with enhanced allowed patterns
     */
    private fun createClassLoaderWithModuleSupport(variables: Map<String, Any>): RestrictedClassLoader {
        // Check if any variables are ScriptModule implementations
        val moduleClasses = variables.values
            .filter { it is io.github.koyan9.lingonexus.api.module.spi.ScriptModule }
            .map { it.javaClass.name }
            .toSet()

        if (moduleClasses.isEmpty()) {
            // No custom modules, use the default RestrictedClassLoader
            return restrictedClassLoader
        }

        // Create enhanced allowed patterns including custom module classes
        val enhancedAllowedPatterns = mutableSetOf<String>()

        // Add base allowed patterns
        enhancedAllowedPatterns.add("java.**")
        enhancedAllowedPatterns.add("javax.**")
        enhancedAllowedPatterns.add("jdk.**")
        enhancedAllowedPatterns.add("sun.**")
        enhancedAllowedPatterns.add("com.sun.**")
        enhancedAllowedPatterns.add("kotlin.**")
        enhancedAllowedPatterns.add("org.jetbrains.kotlin.**")
        enhancedAllowedPatterns.add("Script")
        enhancedAllowedPatterns.add("Script$*")
        enhancedAllowedPatterns.add("Line_*")
        enhancedAllowedPatterns.add("io.github.koyan9.lingonexus.**")

        // Add custom module class patterns
        moduleClasses.forEach { className ->
            enhancedAllowedPatterns.add(className)
            logger.debug("Custom ScriptModule detected | Class: {} | Status: Added to allowed patterns", className)
        }

        logger.info(
            "Enhanced ClassLoader created | Custom modules: {} | Classes: {}",
            moduleClasses.size, moduleClasses.joinToString(", ")
        )

        // Create a new RestrictedClassLoader with enhanced patterns
        return RestrictedClassLoader(
            sandboxConfig.isEnabled,
            sandboxConfig.classWhitelist,
            sandboxConfig.classBlacklist,
            enhancedAllowedPatterns,
            Thread.currentThread().contextClassLoader
        )
    }

    /**
     * Create compilation and evaluation configurations with provided properties for variables
     *
     * <p>This method uses Kotlin script API's providedProperties mechanism to inject variables
     * in a type-safe way. This allows the Kotlin compiler to properly resolve method calls
     * on injected objects, including ScriptModule instances.</p>
     *
     * @param variables Map of variable names to values
     * @return Pair of compilation configuration and evaluation configuration
     */
    private fun createConfigurationsWithVariables(
        variables: Map<String, Any>
    ): Pair<ScriptCompilationConfiguration, ScriptEvaluationConfiguration> {
        // Build provided properties for compilation (type declarations)
        val compilationProperties = mutableMapOf<String, KotlinType>()

        // Build provided properties for evaluation (actual values)
        val evaluationProperties = mutableMapOf<String, Any?>()

        variables.forEach { (name, value) ->
            // Add to evaluation properties (actual value)
            evaluationProperties[name] = value

            // Add to compilation properties (type declaration)
            // Handle anonymous classes and interfaces by using the first interface or superclass
            val kClass = value::class
            val typeClass = if (kClass.java.isAnonymousClass || kClass.java.isSynthetic) {
                // For anonymous classes, use the first interface or superclass
                val interfaces = kClass.java.interfaces
                if (interfaces.isNotEmpty()) {
                    interfaces[0].kotlin
                } else {
                    kClass.java.superclass?.kotlin ?: kClass
                }
            } else {
                kClass
            }

            compilationProperties[name] = KotlinType(typeClass)
        }

        // Create new compilation configuration with provided properties
        val newCompilationConfig = ScriptCompilationConfiguration(restrictedCompilationConfiguration) {
            providedProperties(compilationProperties)
        }

        // Create new evaluation configuration with provided properties
        val newEvaluationConfig = ScriptEvaluationConfiguration(evaluationConfig) {
            providedProperties(evaluationProperties)
        }

        logger.debug("Configuration created | Provided properties: {} variables", variables.size)
        return Pair(newCompilationConfig, newEvaluationConfig)
    }

    /**
     * Process evaluation result with comprehensive security violation detection
     */
    private fun processEvaluationResult(evalResult: ResultWithDiagnostics<EvaluationResult>): Any? {
        when (evalResult) {
            is ResultWithDiagnostics.Failure -> {
                val errorMessage = evalResult.reports
                    .filter { it.severity == ScriptDiagnostic.Severity.ERROR }
                    .joinToString("\n") {
                        "${it.message}${if (it.exception != null) ": ${it.exception}" else ""}"
                    }

                // Check for security violations
                if (isSecurityViolation(errorMessage)) {
                    val securityMessage = extractSecurityMessage(errorMessage)
                    logger.error(
                        "Execution failed | Reason: Security violation at runtime | Details: {}",
                        securityMessage
                    )
                    throw ScriptSecurityException(securityMessage)
                }

                logger.error("Execution failed | Reason: Runtime error | Details: {}", errorMessage)
                throw ScriptRuntimeException("Failed to execute Kotlin script: $errorMessage")
            }

            is ResultWithDiagnostics.Success -> {
                val resultValue = evalResult.value.returnValue
                return when (resultValue) {
                    is ResultValue.Value -> resultValue.value
                    is ResultValue.Unit -> Unit
                    is ResultValue.Error -> {
                        val errorMsg = resultValue.error.toString()

                        // Check for security violations in execution errors
                        if (isSecurityViolation(errorMsg)) {
                            val securityMessage = extractSecurityMessage(errorMsg)
                            logger.error("Execution error | Reason: Security violation | Details: {}", securityMessage)
                            throw ScriptSecurityException(securityMessage)
                        }

                        logger.error("Execution error | Reason: Script logic error | Details: {}", resultValue.error)
                        throw ScriptRuntimeException("Script execution error: ${resultValue.error}")
                    }

                    else -> resultValue
                }
            }
        }
    }

    /**
     * Detect security violations in error messages
     *
     * <p>Detects various patterns indicating security violations:</p>
     * <ul>
     *   <li>Class not in whitelist</li>
     *   <li>Class access denied by blacklist</li>
     *   <li>ClassNotFoundException for restricted classes</li>
     *   <li>NoClassDefFoundError for restricted classes</li>
     * </ul>
     */
    private fun isSecurityViolation(errorMessage: String): Boolean {
        val securityKeywords = listOf(
            "class not in whitelist",
            "class access denied",
            "classnotfoundexception",
            "noclassdeffounderror",
            "securityexception",
            "accesscontrolexception",
            "access denied by blacklist"
        )

        val messageLower = errorMessage.lowercase()
        return securityKeywords.any { keyword -> messageLower.contains(keyword) }
    }

    /**
     * Extract user-friendly security violation message
     */
    private fun extractSecurityMessage(errorMessage: String): String {
        val messageLower = errorMessage.lowercase()

        return when {
            messageLower.contains("not in whitelist") -> {
                // Extract class name if possible
                val classNamePattern = Regex("not in whitelist: ([\\w.]+)")
                val match = classNamePattern.find(errorMessage)
                if (match != null) {
                    "Script contains non-whitelisted class: ${match.groupValues[1]}"
                } else {
                    "Script contains non-whitelisted class"
                }
            }

            messageLower.contains("class access denied") -> {
                // Extract class name if possible
                val classNamePattern = Regex("class access denied.*?: ([\\w.]+)")
                val match = classNamePattern.find(errorMessage)
                if (match != null) {
                    "Script contains blacklisted class: ${match.groupValues[1]}"
                } else {
                    "Script contains blacklisted class"
                }
            }

            messageLower.contains("classnotfoundexception") -> {
                "Attempt to load restricted class (ClassNotFoundException)"
            }

            messageLower.contains("noclassdeffounderror") -> {
                "Attempt to use restricted class (NoClassDefFoundError)"
            }

            else -> "Security violation: $errorMessage"
        }
    }
}

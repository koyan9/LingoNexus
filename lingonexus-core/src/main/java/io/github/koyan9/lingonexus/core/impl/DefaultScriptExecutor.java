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

package io.github.koyan9.lingonexus.core.impl;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.MetadataKeys;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.context.VariableManager;
import io.github.koyan9.lingonexus.api.exception.ExternalProcessCompatibilityException;
import io.github.koyan9.lingonexus.api.exception.ScriptSecurityException;
import io.github.koyan9.lingonexus.api.exception.ScriptTimeoutException;
import io.github.koyan9.lingonexus.api.executor.ScriptExecutor;
import io.github.koyan9.lingonexus.api.executor.ScriptTask;
import io.github.koyan9.lingonexus.api.executor.ThreadPoolManager;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.module.ModuleRegistry;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.result.ExecutionStatus;
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.result.ResultMetadataProfile;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicy;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicySupport;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.sandbox.SandboxManager;
import io.github.koyan9.lingonexus.api.sandbox.spi.ScriptSandbox;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;
import io.github.koyan9.lingonexus.api.security.ValidationResult;
import io.github.koyan9.lingonexus.api.statistics.EngineDiagnostics;
import io.github.koyan9.lingonexus.api.statistics.EngineStatistics;
import io.github.koyan9.lingonexus.api.statistics.ExternalProcessStatistics;
import io.github.koyan9.lingonexus.core.cache.CachedCompilationResult;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheManager;
import io.github.koyan9.lingonexus.core.executor.ExecutionPreparationService;
import io.github.koyan9.lingonexus.core.executor.ExecutionStatisticsCollector;
import io.github.koyan9.lingonexus.core.executor.PreparedExecution;
import io.github.koyan9.lingonexus.core.process.ExternalProcessExecutionRequest;
import io.github.koyan9.lingonexus.core.process.ExternalProcessExecutionRequestFactory;
import io.github.koyan9.lingonexus.core.process.ExternalProcessScriptExecutor;
import io.github.koyan9.lingonexus.core.process.ExternalProcessWorkerPool;
import io.github.koyan9.lingonexus.core.process.ExternalProcessWorkerPoolStatistics;
import io.github.koyan9.lingonexus.core.util.LingoNexusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Default script executor implementation with caching and async support.
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-01
 */
public class DefaultScriptExecutor implements ScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(DefaultScriptExecutor.class);

    private final EngineConfig config;
    private final SandboxManager sandboxManager;
    private final ModuleRegistry moduleRegistry;
    private final List<SecurityPolicy> securityPolicies;
    private final VariableManager variableManager;
    private final ScriptCacheManager cacheManager;
    private final ExecutorService executorService;
    private volatile ExternalProcessRuntime externalProcessRuntime;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final ExecutionPreparationService executionPreparationService;
    private final ExecutionStatisticsCollector statisticsCollector;

    public DefaultScriptExecutor(EngineConfig config, ScriptCacheManager cacheManager) {
        this.config = Objects.requireNonNull(config, "engineConfig cannot be null");
        this.sandboxManager = Objects.requireNonNull(config.getSandboxManager(), "sandboxManager cannot be null");
        this.moduleRegistry = config.getModuleRegistry();
        this.securityPolicies = config.getSecurityPolicies();
        this.variableManager = config.getVariableManager();
        this.cacheManager = cacheManager;
        this.externalProcessRuntime = shouldEagerInitializeExternalProcessRuntime()
                ? createExternalProcessRuntime()
                : null;

        this.executorService = config.getThreadPoolManager().getExecutor(
                ThreadPoolManager.PoolType.ASYNC_EXECUTOR,
                config.getExecutorConfig()
        );
        this.executionPreparationService = new ExecutionPreparationService(variableManager, moduleRegistry);
        this.statisticsCollector = new ExecutionStatisticsCollector();

        logger.info("DefaultScriptExecutor initialized with thread pool from ThreadPoolManager");
    }

    @Override
    public ScriptResult execute(String script, String language, ScriptContext context) {
        ensureActive();
        long requestStartNanos = System.nanoTime();
        Map<String, Object> resultMetadata = null;
        java.util.Set<ResultMetadataCategory> resultMetadataCategories = config.getResultMetadataCategories();

        try {
            logger.debug("Executing script: language={}, length={}", language, script != null ? script.length() : 0);

            Objects.requireNonNull(script, "Script cannot be null");
            Objects.requireNonNull(language, "Language cannot be null");
            Objects.requireNonNull(context, "Context cannot be null");

            if (!sandboxManager.supports(language)) {
                String error = "Unsupported language: " + language +
                        ". Supported languages: " + sandboxManager.getSupportedLanguages();
                logger.error(error);
                return ScriptResult.failure(error);
            }

            resultMetadataCategories = resolveResultMetadataCategories(context);
            resultMetadata = createInitialResultMetadata(
                    language,
                    config.getSandboxConfig().getIsolationMode().name(),
                    resultMetadataCategories
            );

            if (config.getSandboxConfig().getIsolationMode() == ExecutionIsolationMode.EXTERNAL_PROCESS) {
                PreparedExecution preparedExecution = executionPreparationService.prepareForExternalProcess(context);
                ScriptResult processResult = executeInExternalProcess(
                        script,
                        language,
                        context,
                        preparedExecution.getExecutionVariables()
                );
                ScriptResult finalProcessResult = finalizeExternalProcessResult(
                        processResult,
                        language,
                        requestStartNanos,
                        resultMetadataCategories
                );
                statisticsCollector.record(
                        language,
                        finalProcessResult.isSuccess(),
                        extractCacheHit(finalProcessResult.getMetadata()),
                        finalProcessResult.getExecutionTime()
                );
                return finalProcessResult;
            }

            resultMetadata = createInitialResultMetadata(
                    language,
                    config.getSandboxConfig().getIsolationMode().name(),
                    resultMetadataCategories
            );

            ScriptResult directResult = executeInProcess(
                    script,
                    language,
                    context,
                    requestStartNanos,
                    resultMetadata,
                    resultMetadataCategories
            );
            statisticsCollector.record(
                    language,
                    true,
                    extractCacheHit(directResult.getMetadata()),
                    directResult.getExecutionTime()
            );
            return directResult;

        } catch (Exception e) {
            long elapsedTime = nanosToMillis(System.nanoTime() - requestStartNanos);
            logger.error("Script execution failed: language={}, error={}", language, e.getMessage(), e);

            boolean requiresMetadataFiltering;
            if (resultMetadata == null) {
                resultMetadata = createErrorResultMetadata(
                        language,
                        resultMetadataCategories,
                        e,
                        false
                );
                requiresMetadataFiltering = e instanceof ExternalProcessCompatibilityException;
            } else {
                requiresMetadataFiltering = populateErrorResultMetadata(resultMetadata, e, false);
            }
            putTimingResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataKeys.EXECUTION_TIME, elapsedTime);
            putTimingResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataKeys.TOTAL_TIME, elapsedTime);
            if (requiresMetadataFiltering) {
                stripResultMetadataForCategories(resultMetadata, resultMetadataCategories);
            }

            ExecutionStatus status = determineExecutionStatus(e);
            statisticsCollector.record(language, false, extractCacheHit(resultMetadata), elapsedTime);
            return ScriptResult.of(status, null, e, elapsedTime, resultMetadata);
        }
    }

    @Override
    public CompletableFuture<ScriptResult> executeAsync(String script, String language, ScriptContext context) {
        if (isShutdown()) {
            return createFailedFuture(new IllegalStateException("Script executor has been shut down"));
        }

        String finalLanguage = language;
        if (language == null || language.trim().isEmpty()) {
            finalLanguage = config.getDefaultLanguage();
            logger.debug("Using default language: {}", finalLanguage);
        }

        logger.debug("Async execution requested: language={}", finalLanguage);

        final String langToUse = finalLanguage;
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(script, langToUse, context);
            } catch (Exception e) {
                logger.error("Async execution failed: {}", e.getMessage(), e);
                long executionTime = 0;
                ExecutionStatus status = determineExecutionStatus(e);

                java.util.Set<ResultMetadataCategory> resultMetadataCategories = resolveResultMetadataCategories(context);
                Map<String, Object> errorMetadata = createErrorResultMetadata(
                        langToUse,
                        resultMetadataCategories,
                        e,
                        true
                );

                return ScriptResult.of(status, null, e, executionTime, errorMetadata);
            }
        }, executorService);
    }

    @Override
    public List<ScriptResult> executeBatch(List<ScriptTask> tasks) {
        ensureActive();
        Objects.requireNonNull(tasks, "tasks cannot be null");
        logger.debug("Executing batch of {} scripts", tasks.size());

        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }

        List<ScriptResult> results = new ArrayList<ScriptResult>(tasks.size());
        List<Future<ScriptResult>> futures = new ArrayList<Future<ScriptResult>>(tasks.size());

        for (final ScriptTask task : tasks) {
            Future<ScriptResult> future = executorService.submit(() ->
                    execute(task.getScript(), task.getLanguage(), task.getContext()));
            futures.add(future);
        }

        long successCount = 0;
        long failureCount = 0;
        for (int i = 0; i < futures.size(); i++) {
            try {
                ScriptResult scriptResult = futures.get(i).get();
                if (scriptResult.isSuccess()) {
                    successCount++;
                } else {
                    failureCount++;
                }
                results.add(scriptResult);
            } catch (Exception e) {
                logger.error("Batch script execution failed at index {}: {}", i, e.getMessage(), e);
                results.add(ScriptResult.failure(e));
            }
        }
        logger.info("Batch execution completed: total={}, success={}, failed={}", tasks.size(), successCount, failureCount);
        return results;
    }

    @Override
    public List<String> getSupportedLanguages() {
        ensureActive();
        return sandboxManager.getSupportedLanguages();
    }

    @Override
    public void registerModule(ScriptModule module) {
        ensureActive();
        boolean moduleAllowed = LingoNexusUtil.isModuleAllowed(
                module,
                config.getExcludeScriptModules(),
                config.getAllowedScriptModules()
        );
        if (moduleAllowed) {
            moduleRegistry.registerModule(module);
        }
    }

    @Override
    public void unregisterModule(String moduleName) {
        ensureActive();
        moduleRegistry.unregisterModule(moduleName);
    }

    @Override
    public EngineStatistics getStatistics() {
        return statisticsCollector.snapshot();
    }

    @Override
    public EngineDiagnostics getDiagnostics() {
        ExternalProcessStatistics externalProcessStatistics = createExternalProcessStatistics();

        return new EngineDiagnostics(
                getStatistics(),
                cacheManager.getCacheSize(),
                config.getThreadPoolManager().getStatistics(ThreadPoolManager.PoolType.ASYNC_EXECUTOR),
                config.getThreadPoolManager().getStatistics(ThreadPoolManager.PoolType.ISOLATED_EXECUTOR),
                externalProcessStatistics,
                config.getSandboxConfig().getIsolationMode().name(),
                isShutdown()
        );
    }

    @Override
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        logger.info("Shutting down DefaultScriptExecutor");
        cacheManager.clearCache();
        shutdownExternalProcessRuntime();
        shutdownSandboxes();
        config.getThreadPoolManager().shutdownAll();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    private ExecutionStatus determineExecutionStatus(Exception e) {
        boolean timeoutDetected = false;
        Throwable current = e;
        while (current != null) {
            if (current instanceof ScriptSecurityException) {
                return ExecutionStatus.SECURITY_VIOLATION;
            }
            if (current instanceof ScriptTimeoutException || current instanceof TimeoutException) {
                timeoutDetected = true;
            }
            current = current.getCause();
        }
        if (timeoutDetected) {
            return ExecutionStatus.TIMEOUT;
        }
        if (e.getMessage() != null && e.getMessage().toLowerCase().contains("compile")) {
            return ExecutionStatus.COMPILATION_ERROR;
        }
        return ExecutionStatus.FAILURE;
    }

    private void shutdownSandboxes() {
        for (ScriptSandbox sandbox : sandboxManager.getAllSandboxes()) {
            try {
                sandbox.shutdown();
            } catch (Exception e) {
                logger.warn("Failed to shutdown sandbox {}: {}", sandbox.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    private void ensureActive() {
        if (isShutdown()) {
            throw new IllegalStateException("Script executor has been shut down");
        }
    }

    private CompletableFuture<ScriptResult> createFailedFuture(Throwable throwable) {
        CompletableFuture<ScriptResult> future = new CompletableFuture<ScriptResult>();
        future.completeExceptionally(throwable);
        return future;
    }

    private Boolean extractCacheHit(Map<String, Object> metadata) {
        if (!config.getCacheConfig().isEnabled() || metadata == null) {
            return null;
        }
        Object cacheHit = metadata.get(ResultMetadataKeys.CACHE_HIT);
        return cacheHit instanceof Boolean ? (Boolean) cacheHit : null;
    }

    private ScriptResult executeInExternalProcess(String script, String language, ScriptContext context,
                                                  Map<String, Object> executionVariables) {
        ExternalProcessRuntime runtime = getOrCreateExternalProcessRuntime();
        ExternalProcessExecutionRequest request = runtime.getRequestFactory().createRequest(
                script,
                language,
                context,
                executionVariables
        );
        return runtime.getExecutor().execute(request, config.getSandboxConfig().getTimeoutMs());
    }

    private ScriptResult executeInProcess(String script, String language, ScriptContext context,
                                          long requestStartNanos, Map<String, Object> resultMetadata,
                                          java.util.Set<ResultMetadataCategory> resultMetadataCategories) {
        PreparedDirectExecution preparedDirectExecution = prepareDirectExecution(
                language,
                context,
                resultMetadata,
                resultMetadataCategories
        );
        ScriptContext executionContext = preparedDirectExecution.getExecutionContext();
        long securityValidationTime = performSecurityValidation(
                script,
                language,
                executionContext,
                resultMetadata,
                resultMetadataCategories
        );
        putTimingResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataKeys.SECURITY_VALIDATION_TIME, securityValidationTime);

        CompiledExecution compiledExecution = compileInProcessScript(
                script,
                language,
                preparedDirectExecution.getSandbox(),
                executionContext
        );
        applyCompilationMetadata(resultMetadata, compiledExecution, resultMetadataCategories);

        InProcessExecutionResult executionResult = executeCompiledScript(
                compiledExecution.getCompiledScript(),
                executionContext,
                requestStartNanos,
                resultMetadata,
                resultMetadataCategories
        );
        logSuccessfulExecution(
                language,
                resultMetadata.get(ResultMetadataKeys.ISOLATION_MODE),
                compiledExecution.isCacheHit(),
                securityValidationTime,
                compiledExecution.getCacheWaitTimeMs(),
                compiledExecution.getCompileTimeMs(),
                executionResult.getQueueWaitTimeMs(),
                executionResult.getExecuteTimeMs(),
                executionResult.getWallTimeMs(),
                executionResult.getTotalTimeMs()
        );
        return ScriptResult.success(executionResult.getValue(), executionResult.getTotalTimeMs(), resultMetadata);
    }

    private PreparedDirectExecution prepareDirectExecution(String language, ScriptContext context,
                                                           Map<String, Object> resultMetadata,
                                                           java.util.Set<ResultMetadataCategory> resultMetadataCategories) {
        ScriptSandbox sandbox = sandboxManager.getSandbox(language).orElseThrow(
                new IllegalStateExceptionSupplier("No sandbox available for language: " + language)
        );
        PreparedExecution preparedExecution = executionPreparationService.prepareForInProcessExecution(context);
        putDiagnosticResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataCategory.MODULE, ResultMetadataKeys.MODULES_USED, preparedExecution.getModulesUsed());
        return new PreparedDirectExecution(sandbox, preparedExecution.getExecutionContext());
    }

    private CompiledExecution compileInProcessScript(String script, String language, ScriptSandbox sandbox,
                                                     ScriptContext executionContext) {
        if (config.getCacheConfig().isEnabled()) {
            String compilationContextSignature = resolveCompilationContextSignature(language, executionContext);
            CachedCompilationResult cachedResult = cacheManager.getOrCompile(
                    script,
                    language,
                    compilationContextSignature,
                    () -> sandbox.compile(script, executionContext)
            );
            if (cachedResult.isCacheHit()) {
                logger.debug("Cache hit for compiled script: language={}", language);
            } else {
                logger.debug(
                        "Compiled and cached script: language={}, compileTime={}ms, cacheWaitTime={}ms",
                        language,
                        cachedResult.getCompileTimeMs(),
                        cachedResult.getCacheWaitTimeMs()
                );
            }
            return new CompiledExecution(
                    cachedResult.getCompiledScript(),
                    cachedResult.getCompileTimeMs(),
                    cachedResult.getCacheWaitTimeMs(),
                    cachedResult.isCacheHit()
            );
        }

        long compileStartNanos = System.nanoTime();
        CompiledScript compiledScript = sandbox.compile(script, executionContext);
        long compileTime = nanosToMillis(System.nanoTime() - compileStartNanos);
        logger.debug("Compiled script (cache disabled): language={}", language);
        return new CompiledExecution(compiledScript, compileTime, 0L, false);
    }



    private String resolveCompilationContextSignature(String language, ScriptContext executionContext) {
        if (!io.github.koyan9.lingonexus.api.lang.ScriptLanguage.JAVA.matches(language)) {
            return null;
        }
        Map<String, Object> variables = executionContext != null ? executionContext.getVariables() : null;
        if (variables == null || variables.isEmpty()) {
            return "java-context:<empty>";
        }
        java.util.TreeMap<String, String> typeSignature = new java.util.TreeMap<String, String>();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            String name = entry.getKey();
            if (name == null) {
                continue;
            }
            Object value = entry.getValue();
            typeSignature.put(name, value != null ? value.getClass().getName() : Object.class.getName());
        }
        StringBuilder signature = new StringBuilder("java-context:");
        for (Map.Entry<String, String> entry : typeSignature.entrySet()) {
            signature.append(entry.getKey()).append('=').append(entry.getValue()).append(';');
        }
        return signature.toString();
    }

    private void applyCompilationMetadata(Map<String, Object> resultMetadata, CompiledExecution compiledExecution,
                                          java.util.Set<ResultMetadataCategory> resultMetadataCategories) {
        resultMetadata.put(ResultMetadataKeys.CACHE_HIT, compiledExecution.isCacheHit());
        putTimingResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataKeys.COMPILE_TIME, compiledExecution.getCompileTimeMs());
        putTimingResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataKeys.CACHE_WAIT_TIME, compiledExecution.getCacheWaitTimeMs());
    }

    private InProcessExecutionResult executeCompiledScript(CompiledScript compiledScript, ScriptContext executionContext,
                                                           long requestStartNanos, Map<String, Object> resultMetadata,
                                                           java.util.Set<ResultMetadataCategory> resultMetadataCategories) {
        long executeWallStartNanos = System.nanoTime();
        Object value = compiledScript.execute(executionContext);
        long executeWallTime = nanosToMillis(System.nanoTime() - executeWallStartNanos);

        long executeTime = getLongMetadata(executionContext, ResultMetadataKeys.EXECUTION_TIME, executeWallTime);
        long queueWaitTime = getLongMetadata(executionContext, ResultMetadataKeys.QUEUE_WAIT_TIME, 0L);
        long wallTime = getLongMetadata(executionContext, ResultMetadataKeys.WALL_TIME, executeWallTime);
        long totalTime = nanosToMillis(System.nanoTime() - requestStartNanos);

        putTimingResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataKeys.EXECUTION_TIME, executeTime);
        putTimingResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataKeys.QUEUE_WAIT_TIME, queueWaitTime);
        putTimingResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataKeys.WALL_TIME, wallTime);
        putTimingResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataKeys.TOTAL_TIME, totalTime);
        return new InProcessExecutionResult(value, executeTime, queueWaitTime, wallTime, totalTime);
    }

    private ExternalProcessStatistics createExternalProcessStatistics() {
        ExternalProcessRuntime runtime = externalProcessRuntime;
        if (runtime == null) {
            return new ExternalProcessStatistics(
                    config.getSandboxConfig().getExternalProcessPoolSize(),
                    0,
                    0,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    0L,
                    null,
                    Collections.<String>emptyList(),
                    Collections.<String>emptyList(),
                    0L,
                    null,
                    null,
                    null,
                    Collections.<String, Long>emptyMap()
            );
        }

        ExternalProcessWorkerPoolStatistics workerPoolStatistics = runtime.getExecutor().getStatistics();
        Map<String, Long> executorCacheStatistics = runtime.getExecutor().getExecutorCacheStatistics();
        ExternalProcessWorkerPool.ProtocolHandshakeSnapshot protocolHandshakeSnapshot = runtime.getExecutor().getProtocolHandshakeSnapshot();
        return new ExternalProcessStatistics(
                workerPoolStatistics.getMaxSize(),
                workerPoolStatistics.getCreatedWorkers(),
                workerPoolStatistics.getIdleWorkers(),
                workerPoolStatistics.getBorrowCount(),
                workerPoolStatistics.getReturnCount(),
                workerPoolStatistics.getDiscardCount(),
                workerPoolStatistics.getEvictionCount(),
                workerPoolStatistics.getStartupFailureCount(),
                workerPoolStatistics.getHealthCheckFailureCount(),
                getMapValue(executorCacheStatistics, "executorCacheSize"),
                getMapValue(executorCacheStatistics, "executorCacheHits"),
                getMapValue(executorCacheStatistics, "executorCacheMisses"),
                getMapValue(executorCacheStatistics, "executorCacheEvictions"),
                protocolHandshakeSnapshot.getProtocolVersion(),
                protocolHandshakeSnapshot.getSupportedTransportProtocolCapabilities(),
                protocolHandshakeSnapshot.getSupportedTransportSerializerContractIds(),
                runtime.getExecutor().getProtocolNegotiationFailureCount(),
                runtime.getExecutor().getLatestProtocolNegotiationFailureReason(),
                runtime.getExecutor().getLatestBorrowFailureReason(),
                runtime.getExecutor().getLatestWorkerExecutionFailureReason(),
                runtime.getExecutor().getFailureReasonCounts()
        );
    }

    private boolean shouldEagerInitializeExternalProcessRuntime() {
        return config.getSandboxConfig().getIsolationMode() == ExecutionIsolationMode.EXTERNAL_PROCESS;
    }

    private ExternalProcessRuntime getOrCreateExternalProcessRuntime() {
        ExternalProcessRuntime runtime = externalProcessRuntime;
        if (runtime != null) {
            return runtime;
        }

        synchronized (this) {
            runtime = externalProcessRuntime;
            if (runtime == null) {
                ensureActive();
                runtime = createExternalProcessRuntime();
                externalProcessRuntime = runtime;
            }
            return runtime;
        }
    }

    private ExternalProcessRuntime createExternalProcessRuntime() {
        return new ExternalProcessRuntime(
                new ExternalProcessScriptExecutor(
                        config.getSandboxConfig().getExternalProcessPoolSize(),
                        config.getSandboxConfig().getExternalProcessStartupRetries(),
                        config.getSandboxConfig().getExternalProcessPrewarmCount(),
                        config.getSandboxConfig().getExternalProcessIdleTtlMs()
                ),
                new ExternalProcessExecutionRequestFactory(config, moduleRegistry, securityPolicies)
        );
    }

    private void shutdownExternalProcessRuntime() {
        ExternalProcessRuntime runtime = externalProcessRuntime;
        if (runtime != null) {
            runtime.getExecutor().shutdown();
        }
    }

    private ScriptResult finalizeExternalProcessResult(ScriptResult processResult, String language,
                                                       long requestStartNanos,
                                                       java.util.Set<ResultMetadataCategory> resultMetadataCategories) {
        Map<String, Object> processMetadata = processResult.getMetadata();
        Map<String, Object> externalMetadata = processMetadata != null
                ? processMetadata
                : new HashMap<String, Object>(resolveMergedResultMetadataCapacity(null, null));
        externalMetadata.putIfAbsent(ResultMetadataKeys.SCRIPT_ENGINE, language);
        externalMetadata.put(ResultMetadataKeys.ISOLATION_MODE, ExecutionIsolationMode.EXTERNAL_PROCESS.name());
        putThreadResultMetadata(externalMetadata, resultMetadataCategories);

        long totalTime = nanosToMillis(System.nanoTime() - requestStartNanos);
        putTimingResultMetadata(externalMetadata, resultMetadataCategories, ResultMetadataKeys.TOTAL_TIME, totalTime);
        putTimingResultMetadata(externalMetadata, resultMetadataCategories, ResultMetadataKeys.WALL_TIME, totalTime);

        if (processResult.isSuccess()) {
            logSuccessfulExecution(
                    externalMetadata.get(ResultMetadataKeys.SCRIPT_ENGINE),
                    externalMetadata.get(ResultMetadataKeys.ISOLATION_MODE),
                    externalMetadata.get(ResultMetadataKeys.CACHE_HIT),
                    externalMetadata.get(ResultMetadataKeys.SECURITY_VALIDATION_TIME),
                    externalMetadata.get(ResultMetadataKeys.CACHE_WAIT_TIME),
                    externalMetadata.get(ResultMetadataKeys.COMPILE_TIME),
                    externalMetadata.get(ResultMetadataKeys.QUEUE_WAIT_TIME),
                    externalMetadata.get(ResultMetadataKeys.EXECUTION_TIME),
                    externalMetadata.get(ResultMetadataKeys.WALL_TIME),
                    externalMetadata.get(ResultMetadataKeys.TOTAL_TIME)
            );
            return ScriptResult.success(processResult.getValue(), totalTime, externalMetadata);
        }

        externalMetadata.put(ResultMetadataKeys.ERROR_MESSAGE, processResult.getErrorMessage());
        externalMetadata.putIfAbsent(ResultMetadataKeys.ERROR_TYPE, processResult.getStatus().name());
        stripResultMetadataForCategories(externalMetadata, resultMetadataCategories);
        return ScriptResult.of(
                processResult.getStatus(),
                null,
                processResult.getErrorMessage(),
                null,
                totalTime,
                externalMetadata
        );
    }

    private void logSuccessfulExecution(Object language, Object isolationMode, Object cacheHit,
                                        Object securityValidationTime, Object cacheWaitTime,
                                        Object compileTime, Object queueWaitTime,
                                        Object executeTime, Object wallTime, Object totalTime) {
        logger.info(
                "Script executed successfully: language={}, isolationMode={}, cacheHit={}, securityValidationTime={}ms, cacheWaitTime={}ms, compileTime={}ms, queueWaitTime={}ms, executeTime={}ms, wallTime={}ms, totalTime={}ms",
                language,
                isolationMode,
                cacheHit,
                securityValidationTime,
                cacheWaitTime,
                compileTime,
                queueWaitTime,
                executeTime,
                wallTime,
                totalTime
        );
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Validates a script against configured security policies.
     *
     * @param script script source
     * @param language script language
     * @param context execution context
     * @param resultMetadata metadata map that records validation details
     * @throws ScriptSecurityException when any security policy denies execution
     */
    private long performSecurityValidation(String script, String language, ScriptContext context,
                                          Map<String, Object> resultMetadata,
                                          java.util.Set<ResultMetadataCategory> resultMetadataCategories) {
        if (!config.getSandboxConfig().isEnabled() || securityPolicies == null || securityPolicies.isEmpty()) {
            return 0L;
        }

        long validationStartNanos = System.nanoTime();
        int securityChecksPassed = 0;
        for (SecurityPolicy policy : securityPolicies) {
            ValidationResult result = policy.validate(script, language, context, config.getSandboxConfig());
            if (!result.isValid()) {
                String error = String.format("Security validation failed [%s]: %s", policy.getName(), result.getReason());
                logger.error(error);
                throw new ScriptSecurityException(error);
            }
            securityChecksPassed++;
        }
        logger.debug("All security policies passed");
        putDiagnosticResultMetadata(resultMetadata, resultMetadataCategories, ResultMetadataCategory.SECURITY, ResultMetadataKeys.SECURITY_CHECKS, securityChecksPassed);
        return nanosToMillis(System.nanoTime() - validationStartNanos);
    }

    private Map<String, Object> createErrorResultMetadata(String language,
                                                        java.util.Set<ResultMetadataCategory> resultMetadataCategories,
                                                        Exception exception,
                                                        boolean async) {
        Map<String, Object> errorMetadata = createInitialResultMetadata(
                language,
                config.getSandboxConfig().getIsolationMode().name(),
                resultMetadataCategories
        );
        populateErrorResultMetadata(errorMetadata, exception, async);
        return errorMetadata;
    }

    private boolean populateErrorResultMetadata(Map<String, Object> resultMetadata,
                                                Exception exception,
                                                boolean async) {
        resultMetadata.put(ResultMetadataKeys.ERROR_TYPE, exception.getClass().getSimpleName());
        resultMetadata.put(ResultMetadataKeys.ERROR_MESSAGE, exception.getMessage());
        if (async) {
            resultMetadata.put("async", true);
        }
        if (exception instanceof ExternalProcessCompatibilityException) {
            populateExternalProcessCompatibilityMetadata(
                    resultMetadata,
                    (ExternalProcessCompatibilityException) exception
            );
            return true;
        }
        return false;
    }

    private Map<String, Object> createInitialResultMetadata(String language, String isolationMode,
                                                            java.util.Set<ResultMetadataCategory> resultMetadataCategories) {
        Map<String, Object> resultMetadata = new HashMap<String, Object>(resolveInitialResultMetadataCapacity(resultMetadataCategories));
        resultMetadata.put(ResultMetadataKeys.SCRIPT_ENGINE, language);
        resultMetadata.put(ResultMetadataKeys.ISOLATION_MODE, isolationMode);
        putThreadResultMetadata(resultMetadata, resultMetadataCategories);
        return resultMetadata;
    }

    private java.util.Set<ResultMetadataCategory> resolveResultMetadataCategories(ScriptContext context) {
        Map<String, Object> contextMetadata = context != null ? context.getMetadata() : null;
        if (contextMetadata == null || contextMetadata.isEmpty()) {
            return config.getResultMetadataCategories();
        }

        Object categoriesOverride = contextMetadata.get(MetadataKeys.RESULT_METADATA_CATEGORIES);
        java.util.Set<ResultMetadataCategory> parsedCategories = parseResultMetadataCategories(categoriesOverride);
        if (parsedCategories != null) {
            return parsedCategories;
        }

        Object policyOverride = contextMetadata.get(MetadataKeys.RESULT_METADATA_POLICY);
        if (policyOverride instanceof ResultMetadataPolicy) {
            return ((ResultMetadataPolicy) policyOverride).getCategories();
        }
        if (policyOverride != null) {
            String policyText = String.valueOf(policyOverride);
            ResultMetadataPolicy parsedPolicy = ResultMetadataPolicy.fromString(policyText);
            if (parsedPolicy != null) {
                return parsedPolicy.getCategories();
            }
            java.util.Set<ResultMetadataCategory> resolvedNamedPolicy = ResultMetadataPolicySupport.resolveNamedPolicy(
                    policyText,
                    config.getResultMetadataPolicyTemplates()
            );
            if (resolvedNamedPolicy != null) {
                return resolvedNamedPolicy;
            }
        }

        Object profileOverride = contextMetadata.get(MetadataKeys.RESULT_METADATA_PROFILE);
        if (profileOverride instanceof ResultMetadataProfile) {
            return ResultMetadataCategory.forProfile((ResultMetadataProfile) profileOverride);
        }
        if (profileOverride != null) {
            ResultMetadataProfile parsedProfile = ResultMetadataProfile.fromString(String.valueOf(profileOverride));
            if (parsedProfile != null) {
                return ResultMetadataCategory.forProfile(parsedProfile);
            }
        }

        Object override = contextMetadata.get(MetadataKeys.DETAILED_RESULT_METADATA_ENABLED);
        if (override instanceof Boolean) {
            return ResultMetadataCategory.forProfile(ResultMetadataProfile.fromDetailedEnabled((Boolean) override));
        }
        if (override != null) {
            return ResultMetadataCategory.forProfile(ResultMetadataProfile.fromDetailedEnabled(Boolean.parseBoolean(String.valueOf(override))));
        }
        return config.getResultMetadataCategories();
    }

    private java.util.Set<ResultMetadataCategory> parseResultMetadataCategories(Object override) {
        if (override == null) {
            return null;
        }
        java.util.EnumSet<ResultMetadataCategory> categories = java.util.EnumSet.noneOf(ResultMetadataCategory.class);
        if (override instanceof ResultMetadataCategory) {
            categories.add((ResultMetadataCategory) override);
            return categories;
        }
        if (override instanceof java.util.Collection) {
            for (Object item : (java.util.Collection<?>) override) {
                if (item == null) {
                    continue;
                }
                categories.add(item instanceof ResultMetadataCategory
                        ? (ResultMetadataCategory) item
                        : ResultMetadataCategory.valueOf(String.valueOf(item).trim().toUpperCase()));
            }
            return categories;
        }
        String text = String.valueOf(override).trim();
        if (text.isEmpty()) {
            return categories;
        }
        String[] parts = text.split(",");
        for (String part : parts) {
            String candidate = part != null ? part.trim() : "";
            if (!candidate.isEmpty()) {
                categories.add(ResultMetadataCategory.valueOf(candidate.toUpperCase()));
            }
        }
        return categories;
    }

    private void putThreadResultMetadata(Map<String, Object> resultMetadata,
                                         java.util.Set<ResultMetadataCategory> resultMetadataCategories) {
        if (resultMetadataCategories == null || !resultMetadataCategories.contains(ResultMetadataCategory.THREAD)) {
            return;
        }
        Thread currentThread = Thread.currentThread();
        resultMetadata.put(ResultMetadataKeys.THREAD_ID, currentThread.getId());
        resultMetadata.put(ResultMetadataKeys.THREAD_NAME, currentThread.getName());
    }

    private void putTimingResultMetadata(Map<String, Object> resultMetadata, java.util.Set<ResultMetadataCategory> resultMetadataCategories,
                                         String key, Object value) {
        if (resultMetadataCategories.contains(ResultMetadataCategory.TIMING)) {
            resultMetadata.put(key, value);
        }
    }

    private void putDiagnosticResultMetadata(Map<String, Object> resultMetadata, java.util.Set<ResultMetadataCategory> resultMetadataCategories,
                                             ResultMetadataCategory category, String key, Object value) {
        if (resultMetadataCategories.contains(category)) {
            resultMetadata.put(key, value);
        }
    }

    private int resolveInitialResultMetadataCapacity(java.util.Set<ResultMetadataCategory> resultMetadataCategories) {
        if (resultMetadataCategories == null || resultMetadataCategories.isEmpty()) {
            return 4;
        }
        if (resultMetadataCategories.size() >= 3) {
            return 16;
        }
        return 8;
    }

    private int resolveMergedResultMetadataCapacity(Map<String, Object> baseMetadata,
                                                    Map<String, Object> processMetadata) {
        int baseSize = baseMetadata != null ? baseMetadata.size() : 0;
        int processSize = processMetadata != null ? processMetadata.size() : 0;
        int expectedSize = baseSize + processSize + 4;
        if (expectedSize <= 0) {
            return 8;
        }
        return (int) ((expectedSize / 0.75f) + 1.0f);
    }

    private void stripResultMetadataForCategories(Map<String, Object> metadata, java.util.Set<ResultMetadataCategory> resultMetadataCategories) {
        if (!resultMetadataCategories.contains(ResultMetadataCategory.TIMING)) {
            metadata.remove(ResultMetadataKeys.COMPILE_TIME);
            metadata.remove(ResultMetadataKeys.EXECUTION_TIME);
            metadata.remove(ResultMetadataKeys.QUEUE_WAIT_TIME);
            metadata.remove(ResultMetadataKeys.SECURITY_VALIDATION_TIME);
            metadata.remove(ResultMetadataKeys.CACHE_WAIT_TIME);
            metadata.remove(ResultMetadataKeys.TOTAL_TIME);
            metadata.remove(ResultMetadataKeys.WALL_TIME);
        }
        if (!resultMetadataCategories.contains(ResultMetadataCategory.MODULE)) {
            metadata.remove(ResultMetadataKeys.MODULES_USED);
        }
        if (!resultMetadataCategories.contains(ResultMetadataCategory.SECURITY)) {
            metadata.remove(ResultMetadataKeys.SECURITY_CHECKS);
        }
        if (!resultMetadataCategories.contains(ResultMetadataCategory.THREAD)) {
            metadata.remove(ResultMetadataKeys.THREAD_ID);
            metadata.remove(ResultMetadataKeys.THREAD_NAME);
        }
        if (!resultMetadataCategories.contains(ResultMetadataCategory.ERROR_DIAGNOSTICS)) {
            metadata.remove(ResultMetadataKeys.ERROR_STAGE);
            metadata.remove(ResultMetadataKeys.ERROR_COMPONENT);
            metadata.remove(ResultMetadataKeys.ERROR_REASON);
        }
    }

    private long getLongMetadata(ScriptContext context, String key, long defaultValue) {
        Object value = context.getMetadata().get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return defaultValue;
    }

    private void populateExternalProcessCompatibilityMetadata(Map<String, Object> resultMetadata,
                                                              ExternalProcessCompatibilityException exception) {
        if (exception.getStage() != null) {
            resultMetadata.put(ResultMetadataKeys.ERROR_STAGE, exception.getStage());
        }
        if (exception.getComponent() != null) {
            resultMetadata.put(ResultMetadataKeys.ERROR_COMPONENT, exception.getComponent());
        }
        if (exception.getReason() != null) {
            resultMetadata.put(ResultMetadataKeys.ERROR_REASON, exception.getReason());
        }
    }

    private long getMapValue(Map<String, Long> map, String key) {
        if (map == null) {
            return 0L;
        }
        Long value = map.get(key);
        return value != null ? value : 0L;
    }

    private long nanosToMillis(long nanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    /**
     * Java 8 compatible supplier for IllegalStateException.
     */
    private static class IllegalStateExceptionSupplier implements Supplier<IllegalStateException> {
        private final String message;

        IllegalStateExceptionSupplier(String message) {
            this.message = message;
        }

        @Override
        public IllegalStateException get() {
            return new IllegalStateException(message);
        }
    }

    private static final class ExternalProcessRuntime {
        private final ExternalProcessScriptExecutor executor;
        private final ExternalProcessExecutionRequestFactory requestFactory;

        private ExternalProcessRuntime(ExternalProcessScriptExecutor executor,
                                       ExternalProcessExecutionRequestFactory requestFactory) {
            this.executor = executor;
            this.requestFactory = requestFactory;
        }

        private ExternalProcessScriptExecutor getExecutor() {
            return executor;
        }

        private ExternalProcessExecutionRequestFactory getRequestFactory() {
            return requestFactory;
        }
    }

    private static final class PreparedDirectExecution {
        private final ScriptSandbox sandbox;
        private final ScriptContext executionContext;

        private PreparedDirectExecution(ScriptSandbox sandbox, ScriptContext executionContext) {
            this.sandbox = sandbox;
            this.executionContext = executionContext;
        }

        private ScriptSandbox getSandbox() {
            return sandbox;
        }

        private ScriptContext getExecutionContext() {
            return executionContext;
        }
    }

    private static final class CompiledExecution {
        private final CompiledScript compiledScript;
        private final long compileTimeMs;
        private final long cacheWaitTimeMs;
        private final boolean cacheHit;

        private CompiledExecution(CompiledScript compiledScript, long compileTimeMs,
                                  long cacheWaitTimeMs, boolean cacheHit) {
            this.compiledScript = compiledScript;
            this.compileTimeMs = compileTimeMs;
            this.cacheWaitTimeMs = cacheWaitTimeMs;
            this.cacheHit = cacheHit;
        }

        private CompiledScript getCompiledScript() {
            return compiledScript;
        }

        private long getCompileTimeMs() {
            return compileTimeMs;
        }

        private long getCacheWaitTimeMs() {
            return cacheWaitTimeMs;
        }

        private boolean isCacheHit() {
            return cacheHit;
        }
    }

    private static final class InProcessExecutionResult {
        private final Object value;
        private final long executeTimeMs;
        private final long queueWaitTimeMs;
        private final long wallTimeMs;
        private final long totalTimeMs;

        private InProcessExecutionResult(Object value, long executeTimeMs,
                                         long queueWaitTimeMs, long wallTimeMs,
                                         long totalTimeMs) {
            this.value = value;
            this.executeTimeMs = executeTimeMs;
            this.queueWaitTimeMs = queueWaitTimeMs;
            this.wallTimeMs = wallTimeMs;
            this.totalTimeMs = totalTimeMs;
        }

        private Object getValue() {
            return value;
        }

        private long getExecuteTimeMs() {
            return executeTimeMs;
        }

        private long getQueueWaitTimeMs() {
            return queueWaitTimeMs;
        }

        private long getWallTimeMs() {
            return wallTimeMs;
        }

        private long getTotalTimeMs() {
            return totalTimeMs;
        }
    }
}

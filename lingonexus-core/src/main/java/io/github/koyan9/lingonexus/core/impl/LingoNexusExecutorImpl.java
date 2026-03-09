/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 *   License.  You may obtain a copy of the License at
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

import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.context.LingoNexusContext;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.ScriptExecutor;
import io.github.koyan9.lingonexus.api.executor.ScriptTask;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.statistics.EngineDiagnostics;
import io.github.koyan9.lingonexus.api.statistics.EngineStatistics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class LingoNexusExecutorImpl implements LingoNexusExecutor {

    private static final Logger logger = LoggerFactory.getLogger(LingoNexusExecutorImpl.class);

    private final EngineConfig config;
    private final ScriptExecutor executor;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public LingoNexusExecutorImpl(EngineConfig config, ScriptExecutor executor) {
        this.config = Objects.requireNonNull(config, "engineConfig cannot be null");
        this.executor = Objects.requireNonNull(executor, "executor cannot be null");
    }

    @Override
    public ScriptResult execute(String script, String language, ScriptContext context) {
        ensureActive();
        if (language == null || language.trim().isEmpty()) {
            language = config.getDefaultLanguage();
        }

        if (context == null) {
            context = ScriptContext.of(Map.of());
        }

        logger.debug("Executing script: language={}, size={} bytes", language, script != null ? script.length() : 0);

        return executor.execute(script, language, context);
    }

    @Override
    public CompletableFuture<ScriptResult> executeAsync(String script, String language, ScriptContext context) {
        ensureActive();
        if (language == null || language.trim().isEmpty()) {
            language = config.getDefaultLanguage();
            logger.debug("Using default language: {}", language);
        }

        if (context == null) {
            context = ScriptContext.of(Collections.<String, Object>emptyMap());
        }

        return executor.executeAsync(script, language, context);
    }

    @Override
    public List<ScriptResult> executeBatch(List<String> scripts, String language, ScriptContext context) {
        ensureActive();
        Objects.requireNonNull(scripts, "scripts cannot be null");
        if (language == null || language.trim().isEmpty()) {
            language = config.getDefaultLanguage();
        }
        if (context == null) {
            context = ScriptContext.of(Collections.<String, Object>emptyMap());
        }
        if (scripts.isEmpty()) {
            return Collections.emptyList();
        }

        int id = 1;
        List<ScriptTask> tasks = new ArrayList<ScriptTask>(scripts.size());
        for (String script : scripts) {
            tasks.add(ScriptTask.of(Integer.toString(id++), script, language, context));
        }

        return executor.executeBatch(tasks);
    }

    @Override
    public List<String> getSupportedLanguages() {
        ensureActive();
        return executor.getSupportedLanguages();
    }

    @Override
    public boolean isSupported(String language) {
        ensureActive();
        if (language == null || language.trim().isEmpty()) {
            return false;
        }
        List<String> supportedLanguages = getSupportedLanguages();
        for (String supported : supportedLanguages) {
            if (supported.equalsIgnoreCase(language)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void registerModule(ScriptModule module) {
        ensureActive();
        executor.registerModule(module);
    }

    @Override
    public void unregisterModule(String moduleName) {
        ensureActive();
        executor.unregisterModule(moduleName);
    }

    @Override
    public EngineStatistics getStatistics() {
        return executor.getStatistics();
    }

    @Override
    public EngineDiagnostics getDiagnostics() {
        return executor.getDiagnostics();
    }

    @Override
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        logger.info("Shutting down LingoNexusExecutor");
        executor.shutdown();
        detachFromGlobalContextIfCurrent();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get() || executor.isShutdown();
    }

    private void ensureActive() {
        if (isShutdown()) {
            throw new IllegalStateException("LingoNexusExecutor has been shut down");
        }
    }

    private void detachFromGlobalContextIfCurrent() {
        if (!LingoNexusContext.isInitialized()) {
            return;
        }

        try {
            if (LingoNexusContext.getExecutor() == this) {
                LingoNexusContext.clear();
            }
        } catch (IllegalStateException ignored) {
            logger.debug("Skip clearing global context because it is not initialized");
        }
    }
}

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
package io.github.koyan9.lingonexus.core.executor;

import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.context.VariableManager;
import io.github.koyan9.lingonexus.api.module.ModuleRegistry;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Prepares execution variables and runtime context for script execution.
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-03-06
 */
public class ExecutionPreparationService {

    private final VariableManager variableManager;
    private final ModuleRegistry moduleRegistry;
    private volatile long cachedGlobalVariablesVersion = Long.MIN_VALUE;
    private volatile Map<String, Object> cachedGlobalVariables = Collections.emptyMap();
    private final ReentrantLock globalVariablesLock = new ReentrantLock();
    private volatile long cachedModuleSnapshotVersion = Long.MIN_VALUE;
    private volatile ModuleSnapshot cachedModuleSnapshot = ModuleSnapshot.empty();
    private final ReentrantLock moduleSnapshotLock = new ReentrantLock();

    public ExecutionPreparationService(VariableManager variableManager, ModuleRegistry moduleRegistry) {
        this.variableManager = variableManager;
        this.moduleRegistry = moduleRegistry;
    }

    public PreparedExecution prepareForInProcessExecution(ScriptContext requestContext) {
        return prepare(requestContext, true);
    }

    public PreparedExecution prepareForExternalProcess(ScriptContext requestContext) {
        return prepare(requestContext, false);
    }

    private PreparedExecution prepare(ScriptContext requestContext, boolean includeModules) {
        Objects.requireNonNull(requestContext, "requestContext cannot be null");

        Map<String, Object> requestVariables = requestContext.getVariables();
        Map<String, Object> globalVariables = getGlobalVariablesSnapshot();
        ModuleSnapshot moduleSnapshot = includeModules ? getModuleSnapshot() : ModuleSnapshot.empty();

        PreparedExecution fastPathExecution = tryPrepareFastPath(
                requestContext,
                includeModules,
                requestVariables,
                globalVariables,
                moduleSnapshot
        );
        if (fastPathExecution != null) {
            return fastPathExecution;
        }

        if (includeModules) {
            return prepareMergedInProcessExecution(
                    requestContext,
                    requestVariables,
                    globalVariables,
                    moduleSnapshot
            );
        }

        Map<String, Object> executionVariables = new HashMap<String, Object>(
                calculateInitialCapacity(
                        globalVariables.size(),
                        requestVariables != null ? requestVariables.size() : 0,
                        0
                )
        );
        if (!globalVariables.isEmpty()) {
            executionVariables.putAll(globalVariables);
        }
        if (requestVariables != null && !requestVariables.isEmpty()) {
            executionVariables.putAll(requestVariables);
        }

        return new PreparedExecution(executionVariables, Collections.<String>emptyList(), null);
    }

    private PreparedExecution tryPrepareFastPath(ScriptContext requestContext,
                                                 boolean includeModules,
                                                 Map<String, Object> requestVariables,
                                                 Map<String, Object> globalVariables,
                                                 ModuleSnapshot moduleSnapshot) {
        if (globalVariables.isEmpty() && moduleSnapshot.isEmpty()) {
            Map<String, Object> executionVariables = requestVariables != null
                    ? requestVariables
                    : Collections.<String, Object>emptyMap();
            ScriptContext executionContext = includeModules ? requestContext : null;
            return new PreparedExecution(
                    executionVariables,
                    Collections.<String>emptyList(),
                    executionContext
            );
        }

        if (includeModules && globalVariables.isEmpty()
                && (requestVariables == null || requestVariables.isEmpty())
                && !moduleSnapshot.isEmpty()) {
            ScriptContext executionContext = ScriptContext.of(
                    moduleSnapshot.getModuleBindings(),
                    requestContext.getMetadata()
            );
            return new PreparedExecution(
                    executionContext.getVariables(),
                    moduleSnapshot.getModuleNames(),
                    executionContext
            );
        }

        return null;
    }

    private PreparedExecution prepareMergedInProcessExecution(ScriptContext requestContext,
                                                              Map<String, Object> requestVariables,
                                                              Map<String, Object> globalVariables,
                                                              ModuleSnapshot moduleSnapshot) {
        ScriptContext executionContext = createBaseExecutionContext(requestContext, requestVariables, globalVariables);
        Map<String, Object> executionVariables = executionContext.getVariables();

        if (!globalVariables.isEmpty()
                && requestVariables != null
                && !requestVariables.isEmpty()) {
            injectGlobalVariables(executionVariables, globalVariables);
        }

        List<String> modulesUsed = injectModules(executionVariables, moduleSnapshot);
        return new PreparedExecution(executionVariables, modulesUsed, executionContext);
    }

    // Build the execution context once, then merge globals/modules into the same backing map.
    private ScriptContext createBaseExecutionContext(ScriptContext requestContext,
                                                     Map<String, Object> requestVariables,
                                                     Map<String, Object> globalVariables) {
        if (requestVariables != null && !requestVariables.isEmpty()) {
            return ScriptContext.of(requestVariables, requestContext.getMetadata());
        }
        if (!globalVariables.isEmpty()) {
            return ScriptContext.of(globalVariables, requestContext.getMetadata());
        }
        return ScriptContext.of(Collections.<String, Object>emptyMap(), requestContext.getMetadata());
    }

    private void injectGlobalVariables(Map<String, Object> executionVariables,
                                       Map<String, Object> globalVariables) {
        if (globalVariables == null || globalVariables.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : globalVariables.entrySet()) {
            executionVariables.putIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, Object> getGlobalVariablesSnapshot() {
        if (variableManager == null) {
            return Collections.emptyMap();
        }

        long version = variableManager.getVersion();
        Map<String, Object> snapshot = cachedGlobalVariables;
        if (version == cachedGlobalVariablesVersion) {
            return snapshot;
        }

        if (!globalVariablesLock.tryLock()) {
            return snapshot;
        }
        try {
            if (version != cachedGlobalVariablesVersion) {
                Map<String, Object> variables = variableManager.getAllVariables();
                if (variables == null || variables.isEmpty()) {
                    cachedGlobalVariables = Collections.emptyMap();
                } else {
                    cachedGlobalVariables = Collections.unmodifiableMap(new HashMap<String, Object>(variables));
                }
                cachedGlobalVariablesVersion = version;
            }
            return cachedGlobalVariables;
        } finally {
            globalVariablesLock.unlock();
        }
    }

    private ModuleSnapshot getModuleSnapshot() {
        if (moduleRegistry == null) {
            return ModuleSnapshot.empty();
        }

        long version = moduleRegistry.getVersion();
        ModuleSnapshot snapshot = cachedModuleSnapshot;
        if (version == cachedModuleSnapshotVersion) {
            return snapshot;
        }

        if (!moduleSnapshotLock.tryLock()) {
            return snapshot;
        }
        try {
            if (version != cachedModuleSnapshotVersion) {
                cachedModuleSnapshot = buildModuleSnapshot(moduleRegistry.getAllModules());
                cachedModuleSnapshotVersion = version;
            }
            return cachedModuleSnapshot;
        } finally {
            moduleSnapshotLock.unlock();
        }
    }

    private ModuleSnapshot buildModuleSnapshot(List<ScriptModule> modules) {
        if (modules == null || modules.isEmpty()) {
            return ModuleSnapshot.empty();
        }

        Map<String, Object> moduleBindings = new HashMap<String, Object>(calculateInitialCapacity(0, 0, modules.size()));
        List<String> moduleNames = new ArrayList<String>(modules.size());
        for (ScriptModule module : modules) {
            if (module == null) {
                continue;
            }
            String moduleName = module.getName();
            if (moduleName == null || moduleName.trim().isEmpty()) {
                continue;
            }
            Object previousModule = moduleBindings.put(moduleName, module);
            if (previousModule == null) {
                moduleNames.add(moduleName);
            }
        }

        if (moduleBindings.isEmpty()) {
            return ModuleSnapshot.empty();
        }

        return new ModuleSnapshot(
                Collections.unmodifiableMap(moduleBindings),
                Collections.unmodifiableList(moduleNames)
        );
    }

    private List<String> injectModules(Map<String, Object> executionVariables, ModuleSnapshot moduleSnapshot) {
        if (moduleSnapshot.isEmpty()) {
            return Collections.emptyList();
        }

        for (Map.Entry<String, Object> entry : moduleSnapshot.getModuleBindings().entrySet()) {
            executionVariables.putIfAbsent(entry.getKey(), entry.getValue());
        }
        return moduleSnapshot.getModuleNames();
    }

    private int calculateInitialCapacity(int globalSize, int requestSize, int moduleSize) {
        int expectedSize = globalSize + requestSize + moduleSize;
        if (expectedSize <= 0) {
            return 16;
        }
        return (int) ((expectedSize / 0.75f) + 1.0f);
    }

    private static final class ModuleSnapshot {
        private static final ModuleSnapshot EMPTY = new ModuleSnapshot(
                Collections.<String, Object>emptyMap(),
                Collections.<String>emptyList()
        );

        private final Map<String, Object> moduleBindings;
        private final List<String> moduleNames;

        private ModuleSnapshot(Map<String, Object> moduleBindings, List<String> moduleNames) {
            this.moduleBindings = moduleBindings;
            this.moduleNames = moduleNames;
        }

        private static ModuleSnapshot empty() {
            return EMPTY;
        }

        private boolean isEmpty() {
            return moduleBindings.isEmpty();
        }

        private int getBindingCount() {
            return moduleBindings.size();
        }

        private Map<String, Object> getModuleBindings() {
            return moduleBindings;
        }

        private List<String> getModuleNames() {
            return moduleNames;
        }
    }
}

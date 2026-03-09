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
    private volatile long cachedModuleSnapshotVersion = Long.MIN_VALUE;
    private volatile ModuleSnapshot cachedModuleSnapshot = ModuleSnapshot.empty();

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

        Map<String, Object> executionVariables = new HashMap<String, Object>(
                calculateInitialCapacity(
                        globalVariables.size(),
                        requestVariables != null ? requestVariables.size() : 0,
                        moduleSnapshot.getBindingCount()
                )
        );
        if (!globalVariables.isEmpty()) {
            executionVariables.putAll(globalVariables);
        }
        if (requestVariables != null && !requestVariables.isEmpty()) {
            executionVariables.putAll(requestVariables);
        }

        List<String> modulesUsed = includeModules
                ? injectModules(executionVariables, moduleSnapshot)
                : Collections.<String>emptyList();
        ScriptContext executionContext = includeModules
                ? ScriptContext.of(executionVariables, requestContext.getMetadata())
                : null;

        return new PreparedExecution(executionVariables, modulesUsed, executionContext);
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

        synchronized (this) {
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

        synchronized (this) {
            if (version != cachedModuleSnapshotVersion) {
                cachedModuleSnapshot = buildModuleSnapshot(moduleRegistry.getAllModules());
                cachedModuleSnapshotVersion = version;
            }
            return cachedModuleSnapshot;
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
            if (!moduleBindings.containsKey(moduleName)) {
                moduleNames.add(moduleName);
            }
            moduleBindings.put(moduleName, module);
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

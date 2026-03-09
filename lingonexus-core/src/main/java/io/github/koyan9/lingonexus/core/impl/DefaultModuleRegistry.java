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

import io.github.koyan9.lingonexus.api.module.ModuleRegistry;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default module registry implementation.
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-01
 */
public class DefaultModuleRegistry implements ModuleRegistry {

    private static final Logger log = LoggerFactory.getLogger(DefaultModuleRegistry.class);

    private final Map<String, ScriptModule> modules;
    private final Map<String, Map<String, Object>> moduleFunctions;
    private final AtomicLong version;

    public DefaultModuleRegistry() {
        this.modules = new ConcurrentHashMap<String, ScriptModule>();
        this.moduleFunctions = new ConcurrentHashMap<String, Map<String, Object>>();
        this.version = new AtomicLong(0L);
    }

    @Override
    public void registerModule(ScriptModule module) {
        if (module == null) {
            throw new IllegalArgumentException("Module cannot be null");
        }
        String name = module.getName();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Module name cannot be null or empty");
        }
        modules.put(name, module);
        moduleFunctions.put(name, new HashMap<String, Object>(module.getFunctions()));
        version.incrementAndGet();
        log.debug("Registered module: {}", name);
    }

    @Override
    public void unregisterModule(String moduleName) {
        if (moduleName == null) {
            return;
        }
        ScriptModule removed = modules.remove(moduleName);
        if (removed != null) {
            moduleFunctions.remove(moduleName);
            version.incrementAndGet();
            log.debug("Unregistered module: {}", moduleName);
        }
    }

    @Override
    public Optional<ScriptModule> getModule(String moduleName) {
        return Optional.ofNullable(modules.get(moduleName));
    }

    @Override
    public List<ScriptModule> getAllModules() {
        return new ArrayList<ScriptModule>(modules.values());
    }

    @Override
    public Optional<Object> getFunction(String moduleName, String functionName) {
        if (moduleName == null || functionName == null) {
            return Optional.empty();
        }
        Map<String, Object> functions = moduleFunctions.get(moduleName);
        return Optional.ofNullable(functions != null ? functions.get(functionName) : null);
    }

    @Override
    public Map<String, Object> getAllFunctions() {
        Map<String, Object> allFunctions = new HashMap<String, Object>();
        for (Map<String, Object> moduleFuncs : moduleFunctions.values()) {
            if (moduleFuncs != null) {
                allFunctions.putAll(moduleFuncs);
            }
        }
        return allFunctions;
    }

    @Override
    public boolean hasModule(String moduleName) {
        return modules.containsKey(moduleName);
    }

    @Override
    public long getVersion() {
        return version.get();
    }
}

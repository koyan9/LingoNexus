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
package io.github.koyan9.lingonexus.api.module;

import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Module registry interface for managing script modules.
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public interface ModuleRegistry {

    /**
     * Registers a script module.
     *
     * @param module script module
     */
    void registerModule(ScriptModule module);

    /**
     * Unregisters a script module by name.
     *
     * @param moduleName module name
     */
    void unregisterModule(String moduleName);

    /**
     * Gets a module by name.
     *
     * @param moduleName module name
     * @return optional module instance
     */
    Optional<ScriptModule> getModule(String moduleName);

    /**
     * Gets all registered modules.
     *
     * @return module list snapshot
     */
    List<ScriptModule> getAllModules();

    /**
     * Gets a function from a module.
     *
     * @param moduleName module name
     * @param functionName function name
     * @return optional function object
     */
    Optional<Object> getFunction(String moduleName, String functionName);

    /**
     * Gets all module functions in a flattened view.
     *
     * @return function-name to function-object mapping
     */
    Map<String, Object> getAllFunctions();

    /**
     * Checks whether a module exists.
     *
     * @param moduleName module name
     * @return true when present
     */
    boolean hasModule(String moduleName);

    /**
     * Gets the current registry version.
     *
     * @return monotonically increasing version for module-structure changes
     */
    long getVersion();
}

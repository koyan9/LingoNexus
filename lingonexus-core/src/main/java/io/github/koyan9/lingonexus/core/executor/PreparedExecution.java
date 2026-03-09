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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Prepared execution data for a single script invocation.
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-03-06
 */
public class PreparedExecution {

    private final Map<String, Object> executionVariables;
    private final List<String> modulesUsed;
    private final ScriptContext executionContext;

    public PreparedExecution(Map<String, Object> executionVariables, List<String> modulesUsed,
                             ScriptContext executionContext) {
        this.executionVariables = Objects.requireNonNull(executionVariables, "executionVariables cannot be null");
        this.modulesUsed = modulesUsed != null ? modulesUsed : Collections.<String>emptyList();
        this.executionContext = executionContext;
    }

    public Map<String, Object> getExecutionVariables() {
        return executionVariables;
    }

    public List<String> getModulesUsed() {
        return modulesUsed;
    }

    public ScriptContext getExecutionContext() {
        return executionContext;
    }
}

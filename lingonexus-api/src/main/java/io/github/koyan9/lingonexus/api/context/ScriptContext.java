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
package io.github.koyan9.lingonexus.api.context;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Script execution context containing runtime variables and metadata.
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public class ScriptContext {

    private final Map<String, Object> variables;
    private final Map<String, Object> metadata;

    ScriptContext(Map<String, Object> variables, Map<String, Object> metadata) {
        this.variables = new ConcurrentHashMap<String, Object>(variables);
        this.metadata = metadata != null
                ? new ConcurrentHashMap<String, Object>(metadata)
                : new ConcurrentHashMap<String, Object>();
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }

    public void setVariable(String name, Object value) {
        variables.put(name, value);
    }

    public void setMetadata(String name, Object value) {
        metadata.put(name, value);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static ScriptContextBuilder builder() {
        return new ScriptContextBuilder();
    }

    public static ScriptContext of(Map<String, Object> variables) {
        return of(variables, null);
    }

    public static ScriptContext of(Map<String, Object> variables, Map<String, Object> metadata) {
        return new ScriptContext(
                variables != null ? variables : Collections.<String, Object>emptyMap(),
                metadata
        );
    }
}
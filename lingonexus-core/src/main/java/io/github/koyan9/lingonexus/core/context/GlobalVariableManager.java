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

package io.github.koyan9.lingonexus.core.context;

import io.github.koyan9.lingonexus.api.context.VariableManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 全局变量管理器
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-01
 */
public class GlobalVariableManager implements VariableManager {

    private final ConcurrentHashMap<String, Object> variables;
    private final AtomicLong version;

    public GlobalVariableManager() {
        this.variables = new ConcurrentHashMap<String, Object>();
        this.version = new AtomicLong(0);
    }

    @Override
    public void setVariable(String name, Object value) {
        variables.put(name, value);
        version.incrementAndGet();
    }

    @Override
    public Object getVariable(String name) {
        return variables.get(name);
    }

    @Override
    public boolean hasVariable(String name) {
        return variables.containsKey(name);
    }

    @Override
    public void removeVariable(String name) {
        variables.remove(name);
        version.incrementAndGet();
    }

    @Override
    public void clear() {
        variables.clear();
        version.incrementAndGet();
    }

    @Override
    public long getVersion() {
        return version.get();
    }

    @Override
    public Map<String, Object> getAllVariables() {
        return new HashMap<String, Object>(variables);
    }
}

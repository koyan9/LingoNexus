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

import java.util.HashMap;
import java.util.Map;

/**
 * 脚本上下文构建器
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public class ScriptContextBuilder {

    private final Map<String, Object> variables = new HashMap<String, Object>();
    private final Map<String, Object> metadata = new HashMap<String, Object>();

    public ScriptContextBuilder put(String key, Object value) {
        variables.put(key, value);
        return this;
    }

    public ScriptContextBuilder putAll(Map<String, Object> variables) {
        if (variables != null) {
            this.variables.putAll(variables);
        }
        return this;
    }

    public ScriptContextBuilder putMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }

    public ScriptContextBuilder putAllMetadata(Map<String, Object> metadata) {
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
        return this;
    }

    public ScriptContext build() {
        return new ScriptContext(variables, metadata);
    }
}

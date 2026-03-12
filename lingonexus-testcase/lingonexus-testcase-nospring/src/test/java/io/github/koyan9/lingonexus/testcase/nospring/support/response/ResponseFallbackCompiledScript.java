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
package io.github.koyan9.lingonexus.testcase.nospring.support.response;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.context.ScriptContext;

import java.util.LinkedHashMap;
import java.util.Map;

final class ResponseFallbackCompiledScript implements CompiledScript {

    private final String source;

    ResponseFallbackCompiledScript(String source) {
        this.source = source;
    }

    @Override
    public Object execute(ScriptContext context) {
        if (source.contains("non-json-leaf")) {
            Map<String, Object> value = new LinkedHashMap<String, Object>();
            value.put("status", "ok");
            value.put("converted", new Object());
            return value;
        }
        if (source.contains("bad-key")) {
            Map<Object, Object> value = new LinkedHashMap<Object, Object>();
            value.put(Integer.valueOf(1), "bad-key");
            return value;
        }
        if (source.contains("bad-metadata")) {
            return "ok";
        }
        return "fallback-default";
    }

    @Override
    public String getLanguage() {
        return "response-fallback-test";
    }

    @Override
    public String getSource() {
        return source;
    }
}

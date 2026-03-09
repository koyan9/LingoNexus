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
package io.github.koyan9.lingonexus.api.executor;

import io.github.koyan9.lingonexus.api.context.ScriptContext;

/**
 * 脚本执行任务
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public class ScriptTask {

    private final String script;
    private final String language;
    private final ScriptContext context;
    private final String id;

    private ScriptTask(Builder builder) {
        this.script = builder.script;
        this.language = builder.language;
        this.context = builder.context;
        this.id = builder.id;
    }

    public String getScript() {
        return script;
    }

    public String getLanguage() {
        return language;
    }

    public ScriptContext getContext() {
        return context;
    }

    public String getId() {
        return id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ScriptTask of(String id, String script, String language, ScriptContext context) {
        return builder()
                .id(id)
                .script(script)
                .language(language)
                .context(context)
                .build();
    }

    public static class Builder {
        private String script;
        private String language;
        private ScriptContext context;
        private String id;

        public Builder script(String script) {
            this.script = script;
            return this;
        }

        public Builder language(String language) {
            this.language = language;
            return this;
        }

        public Builder context(ScriptContext context) {
            this.context = context;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public ScriptTask build() {
            return new ScriptTask(this);
        }
    }
}

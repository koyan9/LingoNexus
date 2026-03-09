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

package io.github.koyan9.lingonexus.core.cache;

import java.util.Objects;

/**
 * 脚本缓存键
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-01
 */
public class ScriptCacheKey {

    private final String script;
    private final String language;
    private final String compilationContextSignature;

    public ScriptCacheKey(String script, String language) {
        this(script, language, null);
    }

    public ScriptCacheKey(String script, String language, String compilationContextSignature) {
        this.script = Objects.requireNonNull(script, "script cannot be null");
        this.language = Objects.requireNonNull(language, "language cannot be null");
        this.compilationContextSignature = compilationContextSignature;
    }

    public String getScript() {
        return script;
    }

    public String getLanguage() {
        return language;
    }

    public String getCompilationContextSignature() {
        return compilationContextSignature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScriptCacheKey that = (ScriptCacheKey) o;
        return script.equals(that.script)
                && language.equals(that.language)
                && Objects.equals(compilationContextSignature, that.compilationContextSignature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(script, language, compilationContextSignature);
    }

    @Override
    public String toString() {
        return "ScriptCacheKey{language='" + language + "', compilationContextSignature='" + compilationContextSignature + "', script='" + script + "'}";
    }
}

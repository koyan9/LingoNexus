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

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;

/**
 * Wrapper for compiled-script cache lookup results.
 */
public class CachedCompilationResult {

    private final CompiledScript compiledScript;
    private final boolean cacheHit;
    private final long compileTimeMs;
    private final long cacheWaitTimeMs;
    private final long acquireTimeMs;

    public CachedCompilationResult(CompiledScript compiledScript, boolean cacheHit,
                                   long compileTimeMs, long cacheWaitTimeMs, long acquireTimeMs) {
        this.compiledScript = compiledScript;
        this.cacheHit = cacheHit;
        this.compileTimeMs = compileTimeMs;
        this.cacheWaitTimeMs = cacheWaitTimeMs;
        this.acquireTimeMs = acquireTimeMs;
    }

    public CompiledScript getCompiledScript() {
        return compiledScript;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public long getCompileTimeMs() {
        return compileTimeMs;
    }

    public long getCacheWaitTimeMs() {
        return cacheWaitTimeMs;
    }

    public long getAcquireTimeMs() {
        return acquireTimeMs;
    }
}

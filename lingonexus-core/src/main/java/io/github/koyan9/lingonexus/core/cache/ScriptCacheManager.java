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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Script cache manager.
 */
public class ScriptCacheManager {

    private static final Logger log = LoggerFactory.getLogger(ScriptCacheManager.class);

    private final CacheProvider<ScriptCacheKey, CompiledScript> compiledCache;

    public ScriptCacheManager(CacheProvider<ScriptCacheKey, CompiledScript> compiledCache) {
        this.compiledCache = compiledCache;
    }

    public CompiledScript getCompiledScript(String script, String language) {
        return getCompiledScript(script, language, null);
    }

    public CompiledScript getCompiledScript(String script, String language, String compilationContextSignature) {
        ScriptCacheKey key = new ScriptCacheKey(script, language, compilationContextSignature);
        CompiledScript cached = compiledCache.get(key);
        if (cached != null) {
            log.debug("Cache hit for compiled script: language={}", language);
            return cached;
        }
        log.debug("Cache miss for compiled script: language={}", language);
        return null;
    }

    /**
     * Gets a compiled script from cache or compiles atomically when missing.
     */
    public CachedCompilationResult getOrCompile(String script, String language, Supplier<CompiledScript> compileFunction) {
        return getOrCompile(script, language, null, compileFunction);
    }

    public CachedCompilationResult getOrCompile(String script, String language, String compilationContextSignature, Supplier<CompiledScript> compileFunction) {
        ScriptCacheKey key = new ScriptCacheKey(script, language, compilationContextSignature);
        AtomicBoolean compiledNow = new AtomicBoolean(false);
        AtomicLong actualCompileTimeNanos = new AtomicLong(0L);
        long acquireStartNanos = System.nanoTime();

        CompiledScript compiledScript = compiledCache.computeIfAbsent(key, cacheKey -> {
            compiledNow.set(true);
            long compileStartNanos = System.nanoTime();
            try {
                return compileFunction.get();
            } finally {
                actualCompileTimeNanos.set(System.nanoTime() - compileStartNanos);
            }
        });

        long acquireTimeNanos = System.nanoTime() - acquireStartNanos;
        boolean cacheHit = !compiledNow.get();
        long compileTimeMs = TimeUnit.NANOSECONDS.toMillis(actualCompileTimeNanos.get());
        long cacheWaitTimeNanos = cacheHit
                ? acquireTimeNanos
                : Math.max(0L, acquireTimeNanos - actualCompileTimeNanos.get());
        long cacheWaitTimeMs = TimeUnit.NANOSECONDS.toMillis(cacheWaitTimeNanos);
        long acquireTimeMs = TimeUnit.NANOSECONDS.toMillis(acquireTimeNanos);

        if (cacheHit) {
            log.debug("Cache hit for compiled script: language={}", language);
        } else {
            log.debug("Compiled and cached script: language={}", language);
        }
        return new CachedCompilationResult(compiledScript, cacheHit, compileTimeMs, cacheWaitTimeMs, acquireTimeMs);
    }

    public void cacheCompiledScript(String script, String language, CompiledScript compiledScript) {
        cacheCompiledScript(script, language, null, compiledScript);
    }

    public void cacheCompiledScript(String script, String language, String compilationContextSignature, CompiledScript compiledScript) {
        if (compiledScript == null) {
            return;
        }
        ScriptCacheKey key = new ScriptCacheKey(script, language, compilationContextSignature);
        compiledCache.put(key, compiledScript);
        log.debug("Cached compiled script: language={}", language);
    }

    public void clearCache() {
        compiledCache.clear();
        log.debug("Cleared compiled script cache");
    }

    public long getCacheSize() {
        return compiledCache.size();
    }
}

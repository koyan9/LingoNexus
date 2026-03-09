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

package io.github.koyan9.lingonexus.core.impl;

import io.github.koyan9.lingonexus.api.sandbox.SandboxManager;
import io.github.koyan9.lingonexus.api.sandbox.spi.ScriptSandbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认沙箱管理器实现
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-01
 */
public class DefaultSandboxManager implements SandboxManager {

    private static final Logger log = LoggerFactory.getLogger(DefaultSandboxManager.class);

    private final Map<String, ScriptSandbox> sandboxes;

    public DefaultSandboxManager() {
        this.sandboxes = new ConcurrentHashMap<String, ScriptSandbox>();
    }

    @Override
    public void registerSandbox(ScriptSandbox sandbox) {
        List<String> languages = sandbox.getSupportedLanguages();
        if (languages == null || languages.isEmpty()) {
            throw new IllegalArgumentException("Sandbox must support at least one language");
        }
        for (String language : languages) {
            registerSandbox(language, sandbox);
        }
    }

    public void registerSandbox(String language, ScriptSandbox sandbox) {
        if (sandbox == null) {
            throw new IllegalArgumentException("Sandbox cannot be null");
        }
        if (language == null || language.trim().isEmpty()) {
            throw new IllegalArgumentException("Language cannot be null or empty");
        }
        sandboxes.put(language.toLowerCase(), sandbox);
        log.debug("Registered sandbox for language: {}", language);
    }

    @Override
    public void unregisterSandbox(String language) {
        if (language == null) {
            return;
        }
        ScriptSandbox removed = sandboxes.remove(language.toLowerCase());
        if (removed != null) {
            log.debug("Unregistered sandbox for language: {}", language);
        }
    }

    @Override
    public Optional<ScriptSandbox> getSandbox(String language) {
        if (language == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sandboxes.get(language.toLowerCase()));
    }

    @Override
    public List<ScriptSandbox> getAllSandboxes() {
        return new ArrayList<ScriptSandbox>(sandboxes.values().stream().distinct().toList());
    }

    @Override
    public List<String> getSupportedLanguages() {
        return new ArrayList<String>(sandboxes.keySet());
    }

    @Override
    public boolean supports(String language) {
        if (language == null) {
            return false;
        }
        return sandboxes.containsKey(language.toLowerCase());
    }
}

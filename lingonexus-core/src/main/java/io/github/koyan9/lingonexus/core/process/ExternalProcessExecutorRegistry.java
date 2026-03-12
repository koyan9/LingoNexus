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
package io.github.koyan9.lingonexus.core.process;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.exception.ExternalProcessCompatibilityException;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reuses worker-local executors keyed by configuration signature.
 */
public final class ExternalProcessExecutorRegistry {

    private static final int SIGNATURE_CAPACITY = 32;
    private static final int DESCRIPTOR_ENTRY_CAPACITY = 4;
    private static final String STAGE_WORKER_EXECUTION = "worker_execution";
    private static final String COMPONENT_EXTERNAL_WORKER = "external-worker";
    private static final String REASON_SECURITY_POLICY_DESCRIPTOR_LOAD_FAILED = "security_policy_descriptor_load_failed";
    private static final String REASON_SCRIPT_MODULE_DESCRIPTOR_LOAD_FAILED = "script_module_descriptor_load_failed";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final LinkedHashMap<String, CachedExecutorEntry> EXECUTORS = new LinkedHashMap<String, CachedExecutorEntry>(16, 0.75f, true);
    private static final AtomicLong cacheHits = new AtomicLong(0);
    private static final AtomicLong cacheMisses = new AtomicLong(0);
    private static final AtomicLong cacheEvictions = new AtomicLong(0);

    private ExternalProcessExecutorRegistry() {
    }

    public static synchronized LingoNexusExecutor getOrCreate(ExternalProcessExecutionRequest request) {
        try {
            String signature = buildSignature(request);
            evictExpired(request.getExternalProcessExecutorCacheIdleTtlMs());

            CachedExecutorEntry cached = EXECUTORS.get(signature);
            if (cached != null) {
                cached.touch();
                cacheHits.incrementAndGet();
                return cached.getExecutor();
            }

            cacheMisses.incrementAndGet();
            LingoNexusExecutor executor = createExecutor(request);
            enforceCapacity(request.getExternalProcessExecutorCacheMaxSize());
            EXECUTORS.put(signature, new CachedExecutorEntry(executor));
            return executor;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create worker-local executor", e);
        }
    }

    public static synchronized void shutdownAll() {
        for (CachedExecutorEntry entry : EXECUTORS.values()) {
            try {
                entry.getExecutor().close();
            } catch (Exception ignored) {
            }
        }
        EXECUTORS.clear();
    }

    public static synchronized Map<String, Long> getStatistics() {
        Map<String, Long> statistics = new HashMap<String, Long>();
        statistics.put("executorCacheSize", (long) EXECUTORS.size());
        statistics.put("executorCacheHits", cacheHits.get());
        statistics.put("executorCacheMisses", cacheMisses.get());
        statistics.put("executorCacheEvictions", cacheEvictions.get());
        return statistics;
    }

    private static void enforceCapacity(int maxSize) {
        int limit = Math.max(1, maxSize);
        while (EXECUTORS.size() >= limit) {
            Iterator<Map.Entry<String, CachedExecutorEntry>> iterator = EXECUTORS.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            Map.Entry<String, CachedExecutorEntry> eldest = iterator.next();
            iterator.remove();
            closeQuietly(eldest.getValue().getExecutor());
            cacheEvictions.incrementAndGet();
        }
    }

    private static void evictExpired(long idleTtlMs) {
        if (idleTtlMs <= 0) {
            return;
        }
        long now = System.nanoTime();
        long ttlNanos = TimeUnit.MILLISECONDS.toNanos(idleTtlMs);
        Iterator<Map.Entry<String, CachedExecutorEntry>> iterator = EXECUTORS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, CachedExecutorEntry> entry = iterator.next();
            if (now - entry.getValue().getLastAccessNanos() > ttlNanos) {
                closeQuietly(entry.getValue().getExecutor());
                iterator.remove();
                cacheEvictions.incrementAndGet();
            }
        }
    }

    private static String buildSignature(ExternalProcessExecutionRequest request) throws Exception {
        Map<String, Object> signature = new LinkedHashMap<String, Object>(SIGNATURE_CAPACITY);
        signature.put("defaultLanguage", request.getDefaultLanguage());
        signature.put("cacheEnabled", request.isCacheEnabled());
        signature.put("cacheMaxSize", request.getCacheMaxSize());
        signature.put("cacheExpireAfterWriteMs", request.getCacheExpireAfterWriteMs());
        signature.put("cacheExpireAfterAccessMs", request.getCacheExpireAfterAccessMs());
        signature.put("sandboxEnabled", request.isSandboxEnabled());
        signature.put("maxScriptSize", request.getMaxScriptSize());
        signature.put("enableEngineCache", request.isEnableEngineCache());
        signature.put("classWhitelist", request.getClassWhitelist() != null ? request.getClassWhitelist() : Collections.emptySet());
        signature.put("classBlacklist", request.getClassBlacklist() != null ? request.getClassBlacklist() : Collections.emptySet());
        signature.put("excludeScriptModules", request.getExcludeScriptModules() != null ? request.getExcludeScriptModules() : Collections.emptySet());
        signature.put("allowedScriptModules", request.getAllowedScriptModules() != null ? request.getAllowedScriptModules() : Collections.emptySet());
        signature.put("allowedSandboxImplementations", request.getAllowedSandboxImplementations() != null ? request.getAllowedSandboxImplementations() : Collections.emptySet());
        signature.put("allowedSandboxLanguages", request.getAllowedSandboxLanguages() != null ? request.getAllowedSandboxLanguages() : Collections.emptySet());
        signature.put("allowedSandboxHostAccessModes", request.getAllowedSandboxHostAccessModes() != null ? request.getAllowedSandboxHostAccessModes() : Collections.emptySet());
        signature.put("allowedSandboxHostRestrictionModes", request.getAllowedSandboxHostRestrictionModes() != null ? request.getAllowedSandboxHostRestrictionModes() : Collections.emptySet());
        signature.put("requiredSandboxHostRestrictionFlags", request.getRequiredSandboxHostRestrictionFlags() != null ? request.getRequiredSandboxHostRestrictionFlags() : Collections.emptySet());
        signature.put("allowedSandboxResultTransportModes", request.getAllowedSandboxResultTransportModes() != null ? request.getAllowedSandboxResultTransportModes() : Collections.emptySet());
        signature.put("allowedSandboxTransportSerializerModes", request.getAllowedSandboxTransportSerializerModes() != null ? request.getAllowedSandboxTransportSerializerModes() : Collections.emptySet());
        signature.put("allowedSandboxTransportPayloadProfiles", request.getAllowedSandboxTransportPayloadProfiles() != null ? request.getAllowedSandboxTransportPayloadProfiles() : Collections.emptySet());
        signature.put("requiredSandboxTransportProtocolCapabilities", request.getRequiredSandboxTransportProtocolCapabilities() != null ? request.getRequiredSandboxTransportProtocolCapabilities() : Collections.emptySet());
        signature.put("requiredSandboxTransportSerializerContractIds", request.getRequiredSandboxTransportSerializerContractIds() != null ? request.getRequiredSandboxTransportSerializerContractIds() : Collections.emptySet());
        signature.put("requireEngineCacheCapableSandbox", request.isRequireEngineCacheCapableSandbox());
        signature.put("requireExternalProcessCompatibleSandbox", request.isRequireExternalProcessCompatibleSandbox());
        signature.put("requireJsonSafeExternalResult", request.isRequireJsonSafeExternalResult());
        signature.put("requireJsonSafeExternalMetadata", request.isRequireJsonSafeExternalMetadata());
        signature.put("resultMetadataProfile", request.getResultMetadataProfile().name());
        signature.put("resultMetadataCategories", request.getResultMetadataCategories() != null ? request.getResultMetadataCategories() : Collections.emptySet());
        signature.put("customSecurityPolicies", request.getCustomSecurityPolicies() != null ? normalizeDescriptors(request.getCustomSecurityPolicies()) : Collections.emptyList());
        signature.put("dynamicModules", request.getDynamicModules() != null ? normalizeDescriptors(request.getDynamicModules()) : Collections.emptyList());
        return OBJECT_MAPPER.writeValueAsString(signature);
    }

    private static LingoNexusExecutor createExecutor(ExternalProcessExecutionRequest request) {
        List<ExternalProcessExtensionDescriptor> customSecurityPolicyDescriptors = defaultDescriptors(request.getCustomSecurityPolicies());
        List<SecurityPolicy> customPolicies = new ArrayList<SecurityPolicy>(customSecurityPolicyDescriptors.size());
        for (ExternalProcessExtensionDescriptor descriptor : customSecurityPolicyDescriptors) {
            try {
                customPolicies.add(ExternalProcessExtensionSupport.instantiate(descriptor, SecurityPolicy.class));
            } catch (RuntimeException e) {
                throw compatibilityFailure(
                        "EXTERNAL_PROCESS failed to reconstruct SecurityPolicy " + descriptor.getClassName(),
                        e,
                        REASON_SECURITY_POLICY_DESCRIPTOR_LOAD_FAILED
                );
            }
        }

        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.fromStringOrThrow(request.getDefaultLanguage()))
                .cacheConfig(CacheConfig.builder()
                        .enabled(request.isCacheEnabled())
                        .maxSize(request.getCacheMaxSize())
                        .expireAfterWriteMs(request.getCacheExpireAfterWriteMs())
                        .expireAfterAccessMs(request.getCacheExpireAfterAccessMs())
                        .build())
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(request.isSandboxEnabled())
                        .maxScriptSize(request.getMaxScriptSize())
                        .timeoutMs(0L)
                        .enableEngineCache(request.isEnableEngineCache())
                        .isolationMode(ExecutionIsolationMode.DIRECT)
                        .classWhitelist(defaultSet(request.getClassWhitelist()))
                        .classBlacklist(defaultSet(request.getClassBlacklist()))
                        .build())
                .excludeScriptModule(defaultSet(request.getExcludeScriptModules()).toArray(new String[0]))
                .allowedScriptModule(defaultSet(request.getAllowedScriptModules()).toArray(new String[0]))
                .allowedSandboxImplementation(defaultSet(request.getAllowedSandboxImplementations()).toArray(new String[0]))
                .allowedSandboxLanguage(defaultSet(request.getAllowedSandboxLanguages()).toArray(new String[0]))
                .allowedSandboxHostAccessMode(defaultHostAccessModes(request.getAllowedSandboxHostAccessModes()).toArray(new io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode[0]))
                .allowedSandboxHostRestrictionMode(defaultHostRestrictionModes(request.getAllowedSandboxHostRestrictionModes()).toArray(new io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode[0]))
                .requireSandboxHostRestrictionFlag(defaultHostRestrictionFlags(request.getRequiredSandboxHostRestrictionFlags()).toArray(new io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag[0]))
                .allowedSandboxResultTransportMode(defaultResultTransportModes(request.getAllowedSandboxResultTransportModes()).toArray(new io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode[0]))
                .allowedSandboxTransportSerializerMode(defaultTransportSerializerModes(request.getAllowedSandboxTransportSerializerModes()).toArray(new io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode[0]))
                .allowedSandboxTransportPayloadProfile(defaultTransportPayloadProfiles(request.getAllowedSandboxTransportPayloadProfiles()).toArray(new io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile[0]))
                .requireSandboxTransportProtocolCapability(defaultTransportProtocolCapabilities(request.getRequiredSandboxTransportProtocolCapabilities()).toArray(new io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability[0]))
                .requireSandboxTransportSerializerContractId(defaultTransportSerializerContractIds(request.getRequiredSandboxTransportSerializerContractIds()).toArray(new String[0]))
                .requireEngineCacheCapableSandbox(request.isRequireEngineCacheCapableSandbox())
                .requireExternalProcessCompatibleSandbox(request.isRequireExternalProcessCompatibleSandbox())
                .requireJsonSafeExternalResult(request.isRequireJsonSafeExternalResult())
                .requireJsonSafeExternalMetadata(request.isRequireJsonSafeExternalMetadata())
                .resultMetadataProfile(request.getResultMetadataProfile())
                .resultMetadataCategories(request.getResultMetadataCategories())
                .securityPolicies(customPolicies)
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);
        for (ExternalProcessExtensionDescriptor descriptor : defaultDescriptors(request.getDynamicModules())) {
            try {
                executor.registerModule(ExternalProcessExtensionSupport.instantiate(descriptor, ScriptModule.class));
            } catch (RuntimeException e) {
                throw compatibilityFailure(
                        "EXTERNAL_PROCESS failed to reconstruct ScriptModule " + descriptor.getClassName(),
                        e,
                        REASON_SCRIPT_MODULE_DESCRIPTOR_LOAD_FAILED
                );
            }
        }
        return executor;
    }

    private static ExternalProcessCompatibilityException compatibilityFailure(String message,
                                                                              RuntimeException cause,
                                                                              String reason) {
        String finalMessage = message;
        if (cause != null) {
            String causeMessage = cause.getMessage();
            String rootMessage = null;
            Throwable cursor = cause;
            while (cursor != null) {
                String candidate = cursor.getMessage();
                if (candidate != null && !candidate.trim().isEmpty()) {
                    rootMessage = candidate;
                }
                cursor = cursor.getCause();
            }
            if (causeMessage != null && !causeMessage.trim().isEmpty()) {
                finalMessage = message + ": " + causeMessage;
                if (rootMessage != null && !rootMessage.trim().isEmpty()
                        && (causeMessage == null || !causeMessage.contains(rootMessage))) {
                    finalMessage = finalMessage + " (" + rootMessage + ")";
                }
            } else if (rootMessage != null && !rootMessage.trim().isEmpty()) {
                finalMessage = message + ": " + rootMessage;
            }
        }
        return new ExternalProcessCompatibilityException(
                finalMessage,
                cause,
                STAGE_WORKER_EXECUTION,
                COMPONENT_EXTERNAL_WORKER,
                reason
        );
    }

    private static List<Map<String, Object>> normalizeDescriptors(List<ExternalProcessExtensionDescriptor> descriptors) {
        if (descriptors == null || descriptors.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> normalized = new ArrayList<Map<String, Object>>(descriptors.size());
        for (ExternalProcessExtensionDescriptor descriptor : descriptors) {
            Map<String, Object> item = new HashMap<String, Object>(DESCRIPTOR_ENTRY_CAPACITY);
            item.put("className", descriptor.getClassName());
            item.put("descriptor", descriptor.getDescriptor());
            normalized.add(item);
        }
        return normalized;
    }

    private static java.util.Set<String> defaultSet(java.util.Set<String> source) {
        return source != null ? source : Collections.<String>emptySet();
    }


    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode> defaultHostAccessModes(java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode> source) {
        return source != null ? source : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode>emptySet();
    }

    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode> defaultHostRestrictionModes(java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode> source) {
        return source != null ? source : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode>emptySet();
    }


    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag> defaultHostRestrictionFlags(java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag> source) {
        return source != null ? source : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag>emptySet();
    }

    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode> defaultResultTransportModes(java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode> source) {
        return source != null ? source : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode>emptySet();
    }


    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode> defaultTransportSerializerModes(java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode> source) {
        return source != null ? source : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode>emptySet();
    }

    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile> defaultTransportPayloadProfiles(java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile> source) {
        return source != null ? source : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile>emptySet();
    }


    private static java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability> defaultTransportProtocolCapabilities(java.util.Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability> source) {
        return source != null ? source : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability>emptySet();
    }

    private static java.util.Set<String> defaultTransportSerializerContractIds(java.util.Set<String> source) {
        return source != null ? source : Collections.<String>emptySet();
    }

    private static List<ExternalProcessExtensionDescriptor> defaultDescriptors(List<ExternalProcessExtensionDescriptor> source) {
        return source != null ? source : Collections.<ExternalProcessExtensionDescriptor>emptyList();
    }

    private static void closeQuietly(LingoNexusExecutor executor) {
        if (executor == null) {
            return;
        }
        try {
            executor.close();
        } catch (Exception ignored) {
        }
    }

    private static class CachedExecutorEntry {
        private final LingoNexusExecutor executor;
        private volatile long lastAccessNanos;

        CachedExecutorEntry(LingoNexusExecutor executor) {
            this.executor = executor;
            this.lastAccessNanos = System.nanoTime();
        }

        LingoNexusExecutor getExecutor() {
            return executor;
        }

        long getLastAccessNanos() {
            return lastAccessNanos;
        }

        void touch() {
            this.lastAccessNanos = System.nanoTime();
        }
    }
}

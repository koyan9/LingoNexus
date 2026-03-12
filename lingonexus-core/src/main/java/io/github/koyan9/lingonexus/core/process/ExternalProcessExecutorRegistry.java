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

    private static final int DESCRIPTOR_ENTRY_CAPACITY = 4;
    private static final String STAGE_WORKER_EXECUTION = "worker_execution";
    private static final String COMPONENT_EXTERNAL_WORKER = "external-worker";
    private static final String REASON_SECURITY_POLICY_DESCRIPTOR_LOAD_FAILED = "security_policy_descriptor_load_failed";
    private static final String REASON_SCRIPT_MODULE_DESCRIPTOR_LOAD_FAILED = "script_module_descriptor_load_failed";
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
        StringBuilder signature = new StringBuilder(512);
        appendField(signature, "defaultLanguage", request.getDefaultLanguage());
        appendField(signature, "cacheEnabled", request.isCacheEnabled());
        appendField(signature, "cacheMaxSize", request.getCacheMaxSize());
        appendField(signature, "cacheExpireAfterWriteMs", request.getCacheExpireAfterWriteMs());
        appendField(signature, "cacheExpireAfterAccessMs", request.getCacheExpireAfterAccessMs());
        appendField(signature, "sandboxEnabled", request.isSandboxEnabled());
        appendField(signature, "maxScriptSize", request.getMaxScriptSize());
        appendField(signature, "enableEngineCache", request.isEnableEngineCache());
        appendField(signature, "classWhitelist", defaultSet(request.getClassWhitelist()));
        appendField(signature, "classBlacklist", defaultSet(request.getClassBlacklist()));
        appendField(signature, "excludeScriptModules", defaultSet(request.getExcludeScriptModules()));
        appendField(signature, "allowedScriptModules", defaultSet(request.getAllowedScriptModules()));
        appendField(signature, "allowedSandboxImplementations", defaultSet(request.getAllowedSandboxImplementations()));
        appendField(signature, "allowedSandboxLanguages", defaultSet(request.getAllowedSandboxLanguages()));
        appendField(signature, "allowedSandboxHostAccessModes", defaultHostAccessModes(request.getAllowedSandboxHostAccessModes()));
        appendField(signature, "allowedSandboxHostRestrictionModes", defaultHostRestrictionModes(request.getAllowedSandboxHostRestrictionModes()));
        appendField(signature, "requiredSandboxHostRestrictionFlags", defaultHostRestrictionFlags(request.getRequiredSandboxHostRestrictionFlags()));
        appendField(signature, "allowedSandboxResultTransportModes", defaultResultTransportModes(request.getAllowedSandboxResultTransportModes()));
        appendField(signature, "allowedSandboxTransportSerializerModes", defaultTransportSerializerModes(request.getAllowedSandboxTransportSerializerModes()));
        appendField(signature, "allowedSandboxTransportPayloadProfiles", defaultTransportPayloadProfiles(request.getAllowedSandboxTransportPayloadProfiles()));
        appendField(signature, "requiredSandboxTransportProtocolCapabilities", defaultTransportProtocolCapabilities(request.getRequiredSandboxTransportProtocolCapabilities()));
        appendField(signature, "requiredSandboxTransportSerializerContractIds", defaultTransportSerializerContractIds(request.getRequiredSandboxTransportSerializerContractIds()));
        appendField(signature, "requireEngineCacheCapableSandbox", request.isRequireEngineCacheCapableSandbox());
        appendField(signature, "requireExternalProcessCompatibleSandbox", request.isRequireExternalProcessCompatibleSandbox());
        appendField(signature, "requireJsonSafeExternalResult", request.isRequireJsonSafeExternalResult());
        appendField(signature, "requireJsonSafeExternalMetadata", request.isRequireJsonSafeExternalMetadata());
        appendField(signature, "resultMetadataProfile", request.getResultMetadataProfile().name());
        appendField(signature, "resultMetadataCategories", request.getResultMetadataCategories());
        appendField(signature, "customSecurityPolicies", normalizeDescriptors(request.getCustomSecurityPolicies()));
        appendField(signature, "dynamicModules", normalizeDescriptors(request.getDynamicModules()));
        return signature.toString();
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

    private static void appendField(StringBuilder signature, String key, Object value) {
        appendToken(signature, key);
        appendToken(signature, canonicalValue(value));
        signature.append('|');
    }

    private static void appendToken(StringBuilder signature, String token) {
        if (token == null) {
            signature.append("-1:");
            return;
        }
        signature.append(token.length()).append(':').append(token);
    }

    private static String canonicalValue(Object value) {
        if (value == null) {
            return "N";
        }
        if (value instanceof String) {
            return "S:" + value;
        }
        if (value instanceof Boolean) {
            return "B:" + value;
        }
        if (value instanceof Number) {
            return "N:" + value;
        }
        if (value instanceof Enum<?>) {
            return "E:" + ((Enum<?>) value).name();
        }
        if (value instanceof Map<?, ?>) {
            return canonicalMap((Map<?, ?>) value);
        }
        if (value instanceof java.util.Collection<?>) {
            return canonicalCollection((java.util.Collection<?>) value);
        }
        if (value.getClass().isArray()) {
            return canonicalArray(value);
        }
        return "O:" + String.valueOf(value);
    }

    private static String canonicalMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return "M{}";
        }
        List<KeyValue> entries = new ArrayList<KeyValue>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            entries.add(new KeyValue(String.valueOf(entry.getKey()), entry.getValue()));
        }
        entries.sort(java.util.Comparator.comparing(keyValue -> keyValue.key));
        StringBuilder builder = new StringBuilder();
        builder.append("M{");
        for (KeyValue entry : entries) {
            appendToken(builder, entry.key);
            appendToken(builder, canonicalValue(entry.value));
            builder.append(';');
        }
        builder.append('}');
        return builder.toString();
    }

    private static String canonicalCollection(java.util.Collection<?> collection) {
        if (collection == null || collection.isEmpty()) {
            return "L[]";
        }
        java.util.List<String> items = new ArrayList<String>(collection.size());
        for (Object item : collection) {
            items.add(canonicalValue(item));
        }
        if (collection instanceof java.util.Set) {
            java.util.Collections.sort(items);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("L[");
        for (String item : items) {
            appendToken(builder, item);
            builder.append(',');
        }
        builder.append(']');
        return builder.toString();
    }

    private static String canonicalArray(Object array) {
        int length = java.lang.reflect.Array.getLength(array);
        if (length == 0) {
            return "L[]";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("L[");
        for (int index = 0; index < length; index++) {
            appendToken(builder, canonicalValue(java.lang.reflect.Array.get(array, index)));
            builder.append(',');
        }
        builder.append(']');
        return builder.toString();
    }

    private static final class KeyValue {
        private final String key;
        private final Object value;

        private KeyValue(String key, Object value) {
            this.key = key;
            this.value = value;
        }
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

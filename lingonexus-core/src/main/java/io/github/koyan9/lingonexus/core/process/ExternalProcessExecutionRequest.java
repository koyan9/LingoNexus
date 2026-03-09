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

import io.github.koyan9.lingonexus.api.result.ResultMetadataProfile;
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Serializable request payload for one-shot external process execution.
 */
public class ExternalProcessExecutionRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum RequestType {
        EXECUTE,
        HEALTHCHECK
    }

    private final RequestType requestType;
    private final String script;
    private final String language;
    private final String defaultLanguage;
    private final boolean cacheEnabled;
    private final long cacheMaxSize;
    private final long cacheExpireAfterWriteMs;
    private final long cacheExpireAfterAccessMs;
    private final int externalProcessExecutorCacheMaxSize;
    private final long externalProcessExecutorCacheIdleTtlMs;
    private final boolean sandboxEnabled;
    private final int maxScriptSize;
    private final boolean enableEngineCache;
    private final Set<String> classWhitelist;
    private final Set<String> classBlacklist;
    private final Set<String> excludeScriptModules;
    private final Set<String> allowedScriptModules;
    private final Set<String> allowedSandboxImplementations;
    private final Set<String> allowedSandboxLanguages;
    private final Set<SandboxHostAccessMode> allowedSandboxHostAccessModes;
    private final Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes;
    private final Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags;
    private final Set<SandboxResultTransportMode> allowedSandboxResultTransportModes;
    private final Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes;
    private final Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles;
    private final Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities;
    private final Set<String> requiredSandboxTransportSerializerContractIds;
    private final boolean requireEngineCacheCapableSandbox;
    private final boolean requireExternalProcessCompatibleSandbox;
    private final boolean requireJsonSafeExternalResult;
    private final boolean requireJsonSafeExternalMetadata;
    private final ResultMetadataProfile resultMetadataProfile;
    private final Set<ResultMetadataCategory> resultMetadataCategories;
    private final List<ExternalProcessExtensionDescriptor> customSecurityPolicies;
    private final List<ExternalProcessExtensionDescriptor> dynamicModules;
    private final Map<String, Object> variables;
    private final Map<String, Object> metadata;

    public ExternalProcessExecutionRequest(String script, String language, String defaultLanguage,
                                           boolean cacheEnabled, long cacheMaxSize, long cacheExpireAfterWriteMs, long cacheExpireAfterAccessMs,
                                           int externalProcessExecutorCacheMaxSize, long externalProcessExecutorCacheIdleTtlMs,
                                           boolean sandboxEnabled, int maxScriptSize, boolean enableEngineCache,
                                           Set<String> classWhitelist, Set<String> classBlacklist,
                                           Set<String> excludeScriptModules, Set<String> allowedScriptModules,
                                           Set<String> allowedSandboxImplementations, Set<String> allowedSandboxLanguages,
                                           Set<SandboxHostAccessMode> allowedSandboxHostAccessModes,
                                           Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes,
                                           Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags,
                                           Set<SandboxResultTransportMode> allowedSandboxResultTransportModes,
                                           Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes,
                                           Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles,
                                           Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities,
                                           Set<String> requiredSandboxTransportSerializerContractIds,
                                           boolean requireEngineCacheCapableSandbox, boolean requireExternalProcessCompatibleSandbox,
                                           boolean requireJsonSafeExternalResult, boolean requireJsonSafeExternalMetadata,
                                           ResultMetadataProfile resultMetadataProfile,
                                           Set<ResultMetadataCategory> resultMetadataCategories,
                                           List<ExternalProcessExtensionDescriptor> customSecurityPolicies,
                                           List<ExternalProcessExtensionDescriptor> dynamicModules,
                                           Map<String, Object> variables, Map<String, Object> metadata) {
        this(RequestType.EXECUTE, script, language, defaultLanguage,
                cacheEnabled, cacheMaxSize, cacheExpireAfterWriteMs, cacheExpireAfterAccessMs,
                externalProcessExecutorCacheMaxSize, externalProcessExecutorCacheIdleTtlMs,
                sandboxEnabled, maxScriptSize, enableEngineCache,
                classWhitelist, classBlacklist, excludeScriptModules, allowedScriptModules, allowedSandboxImplementations, allowedSandboxLanguages,
                allowedSandboxHostAccessModes, allowedSandboxHostRestrictionModes, requiredSandboxHostRestrictionFlags, allowedSandboxResultTransportModes, allowedSandboxTransportSerializerModes, allowedSandboxTransportPayloadProfiles, requiredSandboxTransportProtocolCapabilities, requiredSandboxTransportSerializerContractIds,
                requireEngineCacheCapableSandbox, requireExternalProcessCompatibleSandbox, requireJsonSafeExternalResult, requireJsonSafeExternalMetadata, resultMetadataProfile, resultMetadataCategories,
                customSecurityPolicies, dynamicModules, variables, metadata);
    }

    public ExternalProcessExecutionRequest(RequestType requestType, String script, String language, String defaultLanguage,
                                           boolean cacheEnabled, long cacheMaxSize, long cacheExpireAfterWriteMs, long cacheExpireAfterAccessMs,
                                           int externalProcessExecutorCacheMaxSize, long externalProcessExecutorCacheIdleTtlMs,
                                           boolean sandboxEnabled, int maxScriptSize, boolean enableEngineCache,
                                           Set<String> classWhitelist, Set<String> classBlacklist,
                                           Set<String> excludeScriptModules, Set<String> allowedScriptModules,
                                           Set<String> allowedSandboxImplementations, Set<String> allowedSandboxLanguages,
                                           Set<SandboxHostAccessMode> allowedSandboxHostAccessModes,
                                           Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes,
                                           Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags,
                                           Set<SandboxResultTransportMode> allowedSandboxResultTransportModes,
                                           Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes,
                                           Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles,
                                           Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities,
                                           Set<String> requiredSandboxTransportSerializerContractIds,
                                           boolean requireEngineCacheCapableSandbox, boolean requireExternalProcessCompatibleSandbox,
                                           boolean requireJsonSafeExternalResult, boolean requireJsonSafeExternalMetadata,
                                           ResultMetadataProfile resultMetadataProfile,
                                           Set<ResultMetadataCategory> resultMetadataCategories,
                                           List<ExternalProcessExtensionDescriptor> customSecurityPolicies,
                                           List<ExternalProcessExtensionDescriptor> dynamicModules,
                                           Map<String, Object> variables, Map<String, Object> metadata) {
        this.requestType = requestType;
        this.script = script;
        this.language = language;
        this.defaultLanguage = defaultLanguage;
        this.cacheEnabled = cacheEnabled;
        this.cacheMaxSize = cacheMaxSize;
        this.cacheExpireAfterWriteMs = cacheExpireAfterWriteMs;
        this.cacheExpireAfterAccessMs = cacheExpireAfterAccessMs;
        this.externalProcessExecutorCacheMaxSize = externalProcessExecutorCacheMaxSize;
        this.externalProcessExecutorCacheIdleTtlMs = externalProcessExecutorCacheIdleTtlMs;
        this.sandboxEnabled = sandboxEnabled;
        this.maxScriptSize = maxScriptSize;
        this.enableEngineCache = enableEngineCache;
        this.classWhitelist = classWhitelist;
        this.classBlacklist = classBlacklist;
        this.excludeScriptModules = excludeScriptModules;
        this.allowedScriptModules = allowedScriptModules;
        this.allowedSandboxImplementations = allowedSandboxImplementations;
        this.allowedSandboxLanguages = allowedSandboxLanguages;
        this.allowedSandboxHostAccessModes = allowedSandboxHostAccessModes;
        this.allowedSandboxHostRestrictionModes = allowedSandboxHostRestrictionModes;
        this.requiredSandboxHostRestrictionFlags = requiredSandboxHostRestrictionFlags;
        this.allowedSandboxResultTransportModes = allowedSandboxResultTransportModes;
        this.allowedSandboxTransportSerializerModes = allowedSandboxTransportSerializerModes;
        this.allowedSandboxTransportPayloadProfiles = allowedSandboxTransportPayloadProfiles;
        this.requiredSandboxTransportProtocolCapabilities = requiredSandboxTransportProtocolCapabilities;
        this.requiredSandboxTransportSerializerContractIds = requiredSandboxTransportSerializerContractIds;
        this.requireEngineCacheCapableSandbox = requireEngineCacheCapableSandbox;
        this.requireExternalProcessCompatibleSandbox = requireExternalProcessCompatibleSandbox;
        this.requireJsonSafeExternalResult = requireJsonSafeExternalResult;
        this.requireJsonSafeExternalMetadata = requireJsonSafeExternalMetadata;
        this.resultMetadataProfile = resultMetadataProfile != null ? resultMetadataProfile : ResultMetadataProfile.FULL;
        this.resultMetadataCategories = resultMetadataCategories != null ? resultMetadataCategories : ResultMetadataCategory.forProfile(this.resultMetadataProfile);
        this.customSecurityPolicies = customSecurityPolicies;
        this.dynamicModules = dynamicModules;
        this.variables = variables;
        this.metadata = metadata;
    }

    public static ExternalProcessExecutionRequest healthCheck() {
        return new ExternalProcessExecutionRequest(
                RequestType.HEALTHCHECK,
                null,
                null,
                null,
                false,
                0L,
                0L,
                0L,
                1,
                0L,
                false,
                0,
                false,
                java.util.Collections.<String>emptySet(),
                java.util.Collections.<String>emptySet(),
                java.util.Collections.<String>emptySet(),
                java.util.Collections.<String>emptySet(),
                java.util.Collections.<String>emptySet(),
                java.util.Collections.<String>emptySet(),
                java.util.Collections.<SandboxHostAccessMode>emptySet(),
                java.util.Collections.<SandboxHostRestrictionMode>emptySet(),
                java.util.Collections.<SandboxHostRestrictionFlag>emptySet(),
                java.util.Collections.<SandboxResultTransportMode>emptySet(),
                java.util.Collections.<SandboxTransportSerializerMode>emptySet(),
                java.util.Collections.<SandboxTransportPayloadProfile>emptySet(),
                java.util.Collections.<SandboxTransportProtocolCapability>emptySet(),
                java.util.Collections.<String>emptySet(),
                false,
                false,
                false,
                false,
                ResultMetadataProfile.FULL,
                java.util.Collections.<ResultMetadataCategory>emptySet(),
                java.util.Collections.<ExternalProcessExtensionDescriptor>emptyList(),
                java.util.Collections.<ExternalProcessExtensionDescriptor>emptyList(),
                java.util.Collections.<String, Object>emptyMap(),
                java.util.Collections.<String, Object>emptyMap()
        );
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public String getScript() {
        return script;
    }

    public String getLanguage() {
        return language;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public long getCacheExpireAfterWriteMs() {
        return cacheExpireAfterWriteMs;
    }

    public long getCacheExpireAfterAccessMs() {
        return cacheExpireAfterAccessMs;
    }

    public int getExternalProcessExecutorCacheMaxSize() {
        return externalProcessExecutorCacheMaxSize;
    }

    public long getExternalProcessExecutorCacheIdleTtlMs() {
        return externalProcessExecutorCacheIdleTtlMs;
    }

    public boolean isSandboxEnabled() {
        return sandboxEnabled;
    }

    public int getMaxScriptSize() {
        return maxScriptSize;
    }

    public boolean isEnableEngineCache() {
        return enableEngineCache;
    }

    public Set<String> getClassWhitelist() {
        return classWhitelist;
    }

    public Set<String> getClassBlacklist() {
        return classBlacklist;
    }

    public Set<String> getExcludeScriptModules() {
        return excludeScriptModules;
    }

    public Set<String> getAllowedScriptModules() {
        return allowedScriptModules;
    }

    public Set<String> getAllowedSandboxImplementations() {
        return allowedSandboxImplementations;
    }

    public Set<String> getAllowedSandboxLanguages() {
        return allowedSandboxLanguages;
    }

    public Set<SandboxHostAccessMode> getAllowedSandboxHostAccessModes() {
        return allowedSandboxHostAccessModes;
    }

    public Set<SandboxHostRestrictionMode> getAllowedSandboxHostRestrictionModes() {
        return allowedSandboxHostRestrictionModes;
    }

    public Set<SandboxHostRestrictionFlag> getRequiredSandboxHostRestrictionFlags() {
        return requiredSandboxHostRestrictionFlags;
    }

    public Set<SandboxResultTransportMode> getAllowedSandboxResultTransportModes() {
        return allowedSandboxResultTransportModes;
    }

    public Set<SandboxTransportSerializerMode> getAllowedSandboxTransportSerializerModes() {
        return allowedSandboxTransportSerializerModes;
    }

    public Set<SandboxTransportPayloadProfile> getAllowedSandboxTransportPayloadProfiles() {
        return allowedSandboxTransportPayloadProfiles;
    }

    public Set<SandboxTransportProtocolCapability> getRequiredSandboxTransportProtocolCapabilities() {
        return requiredSandboxTransportProtocolCapabilities;
    }

    public Set<String> getRequiredSandboxTransportSerializerContractIds() {
        return requiredSandboxTransportSerializerContractIds;
    }

    public boolean isRequireEngineCacheCapableSandbox() {
        return requireEngineCacheCapableSandbox;
    }

    public boolean isRequireExternalProcessCompatibleSandbox() {
        return requireExternalProcessCompatibleSandbox;
    }

    public boolean isRequireJsonSafeExternalResult() {
        return requireJsonSafeExternalResult;
    }

    public boolean isRequireJsonSafeExternalMetadata() {
        return requireJsonSafeExternalMetadata;
    }

    public boolean isDetailedResultMetadataEnabled() {
        return resultMetadataCategories != null && !resultMetadataCategories.isEmpty();
    }

    public ResultMetadataProfile getResultMetadataProfile() {
        return resultMetadataProfile;
    }

    public Set<ResultMetadataCategory> getResultMetadataCategories() {
        return resultMetadataCategories;
    }

    public List<ExternalProcessExtensionDescriptor> getCustomSecurityPolicies() {
        return customSecurityPolicies;
    }

    public List<ExternalProcessExtensionDescriptor> getDynamicModules() {
        return dynamicModules;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}

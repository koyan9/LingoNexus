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
package io.github.koyan9.lingonexus.api.config;

import io.github.koyan9.lingonexus.api.context.VariableManager;
import io.github.koyan9.lingonexus.api.executor.ThreadPoolManager;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.module.ModuleRegistry;
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.result.ResultMetadataProfile;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicy;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyRegistry;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicySupport;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyTemplate;
import io.github.koyan9.lingonexus.api.sandbox.SandboxManager;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 引擎配置 - 控制脚本引擎的整体行为
 *
 * <p>EngineConfig 是 LingoNexus 脚本引擎的顶层配置类，包含以下配置：</p>
 * <ul>
 *   <li><b>defaultLanguage</b>: 默认脚本语言（如 "groovy", "javascript"）</li>
 *   <li><b>cacheConfig</b>: ScriptCacheManager 的缓存配置</li>
 *   <li><b>sandboxConfig</b>: 沙箱安全限制和引擎内部缓存配置</li>
 *   <li><b>executorConfig</b>: 线程池配置</li>
 *   <li><b>sandboxManager</b>: 沙箱管理器（运行时对象）</li>
 *   <li><b>moduleRegistry</b>: 模块注册表（运行时对象）</li>
 *   <li><b>securityPolicy</b>: 安全策略（运行时对象）</li>
 *   <li><b>variableManager</b>: 变量管理器（运行时对象）</li>
 *   <li><b>excludeScriptModules</b>: 排除的 module 列表（支持类全限定名或 module name）</li>
 *   <li><b>allowedScriptModules</b>: 允许的 module 列表（支持类全限定名或 module name）</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public class EngineConfig {

    private final String defaultLanguage;
    private SandboxManager sandboxManager;
    private ModuleRegistry moduleRegistry;
    private final CacheConfig cacheConfig;
    private final SandboxConfig sandboxConfig;
    private final ExecutorConfig executorConfig;
    private final ExecutorConfig isolatedExecutorConfig;
    private final ThreadPoolManager threadPoolManager;
    private final List<SecurityPolicy> securityPolicies;
    private final VariableManager variableManager;
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
    private final ResultMetadataPolicy resultMetadataPolicy;
    private final String resultMetadataPolicyName;
    private final Map<String, ResultMetadataPolicyTemplate> resultMetadataPolicyTemplates;
    private final Set<ResultMetadataCategory> resultMetadataCategories;

    private EngineConfig(Builder builder) {
        this.defaultLanguage = builder.defaultLanguage;
        this.cacheConfig = builder.cacheConfig;
        this.sandboxConfig = builder.sandboxConfig;
        this.executorConfig = builder.executorConfig;
        this.isolatedExecutorConfig = builder.isolatedExecutorConfig;
        this.threadPoolManager = builder.threadPoolManager;
        this.securityPolicies = builder.securityPolicies;
        this.variableManager = builder.variableManager;
        this.excludeScriptModules = builder.excludeScriptModules;
        this.allowedScriptModules = builder.allowedScriptModules;
        this.allowedSandboxImplementations = builder.allowedSandboxImplementations;
        this.allowedSandboxLanguages = builder.allowedSandboxLanguages;
        this.allowedSandboxHostAccessModes = builder.allowedSandboxHostAccessModes;
        this.allowedSandboxHostRestrictionModes = builder.allowedSandboxHostRestrictionModes;
        this.requiredSandboxHostRestrictionFlags = builder.requiredSandboxHostRestrictionFlags;
        this.allowedSandboxResultTransportModes = builder.allowedSandboxResultTransportModes;
        this.allowedSandboxTransportSerializerModes = builder.allowedSandboxTransportSerializerModes;
        this.allowedSandboxTransportPayloadProfiles = builder.allowedSandboxTransportPayloadProfiles;
        this.requiredSandboxTransportProtocolCapabilities = builder.requiredSandboxTransportProtocolCapabilities;
        this.requiredSandboxTransportSerializerContractIds = builder.requiredSandboxTransportSerializerContractIds;
        this.requireEngineCacheCapableSandbox = builder.requireEngineCacheCapableSandbox;
        this.requireExternalProcessCompatibleSandbox = builder.requireExternalProcessCompatibleSandbox;
        this.requireJsonSafeExternalResult = builder.requireJsonSafeExternalResult;
        this.requireJsonSafeExternalMetadata = builder.requireJsonSafeExternalMetadata;
        this.resultMetadataProfile = builder.resultMetadataProfile;
        this.resultMetadataPolicy = builder.resultMetadataPolicy;
        this.resultMetadataPolicyName = ResultMetadataPolicySupport.normalizePolicyName(builder.resultMetadataPolicyName);
        this.resultMetadataPolicyTemplates = ResultMetadataPolicySupport.mergeTemplates(
                builder.resultMetadataPolicyRegistry != null ? builder.resultMetadataPolicyRegistry.snapshotTemplates() : java.util.Collections.<String, ResultMetadataPolicyTemplate>emptyMap(),
                builder.resultMetadataPolicyTemplates
        );
        this.resultMetadataCategories = builder.resultMetadataCategoriesExplicitlyConfigured
                ? (builder.resultMetadataCategories.isEmpty()
                    ? java.util.Collections.<ResultMetadataCategory>emptySet()
                    : java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<ResultMetadataCategory>(builder.resultMetadataCategories)))
                : this.resultMetadataPolicyName != null
                    ? Builder.resolveConfiguredNamedPolicy(this.resultMetadataPolicyName, this.resultMetadataPolicyTemplates)
                : builder.resultMetadataPolicy != null
                    ? builder.resultMetadataPolicy.getCategories()
                    : ResultMetadataCategory.forProfile(builder.resultMetadataProfile);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public SandboxManager getSandboxManager() {
        return sandboxManager;
    }

    public ModuleRegistry getModuleRegistry() {
        return moduleRegistry;
    }

    public CacheConfig getCacheConfig() {
        return cacheConfig;
    }

    public SandboxConfig getSandboxConfig() {
        return sandboxConfig;
    }

    public ExecutorConfig getExecutorConfig() {
        return executorConfig;
    }

    public ExecutorConfig getIsolatedExecutorConfig() {
        return isolatedExecutorConfig;
    }

    public ThreadPoolManager getThreadPoolManager() {
        return threadPoolManager;
    }

    public List<SecurityPolicy> getSecurityPolicies() {
        return securityPolicies;
    }

    public VariableManager getVariableManager() {
        return variableManager;
    }

    public void setModuleRegistry(ModuleRegistry moduleRegistry) {
        this.moduleRegistry = moduleRegistry;
    }

    public void setSandboxManager(SandboxManager sandboxManager) {
        this.sandboxManager = sandboxManager;
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
        return !resultMetadataCategories.isEmpty();
    }

    public ResultMetadataProfile getResultMetadataProfile() {
        return resultMetadataProfile;
    }

    public ResultMetadataPolicy getResultMetadataPolicy() {
        return resultMetadataPolicy;
    }

    public String getResultMetadataPolicyName() {
        return resultMetadataPolicyName;
    }

    public Map<String, ResultMetadataPolicyTemplate> getResultMetadataPolicyTemplates() {
        return resultMetadataPolicyTemplates;
    }

    public Set<ResultMetadataCategory> getResultMetadataCategories() {
        return resultMetadataCategories;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String defaultLanguage = ScriptLanguage.getDefault().getId();
        private CacheConfig cacheConfig = CacheConfig.builder().build();
        private SandboxConfig sandboxConfig = SandboxConfig.builder().build();
        private ExecutorConfig executorConfig = ExecutorConfig.builder().build();
        private ExecutorConfig isolatedExecutorConfig;
        private ThreadPoolManager threadPoolManager = ThreadPoolManager.createIndependentManager();
        private List<SecurityPolicy> securityPolicies;
        private VariableManager variableManager;
        private Set<String> excludeScriptModules;
        private Set<String> allowedScriptModules;
        private Set<String> allowedSandboxImplementations;
        private Set<String> allowedSandboxLanguages;
        private Set<SandboxHostAccessMode> allowedSandboxHostAccessModes;
        private Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes;
        private Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags;
        private Set<SandboxResultTransportMode> allowedSandboxResultTransportModes;
        private Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes;
        private Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles;
        private Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities;
        private Set<String> requiredSandboxTransportSerializerContractIds;
        private boolean requireEngineCacheCapableSandbox;
        private boolean requireExternalProcessCompatibleSandbox;
        private boolean requireJsonSafeExternalResult;
        private boolean requireJsonSafeExternalMetadata;
        private ResultMetadataProfile resultMetadataProfile = ResultMetadataProfile.FULL;
        private ResultMetadataPolicy resultMetadataPolicy;
        private String resultMetadataPolicyName;
        private ResultMetadataPolicyRegistry resultMetadataPolicyRegistry;
        private Map<String, ResultMetadataPolicyTemplate> resultMetadataPolicyTemplates = new java.util.LinkedHashMap<String, ResultMetadataPolicyTemplate>();
        private Set<ResultMetadataCategory> resultMetadataCategories = java.util.EnumSet.noneOf(ResultMetadataCategory.class);
        private boolean resultMetadataCategoriesExplicitlyConfigured;

        public Builder defaultLanguage(String defaultLanguage) {
            this.defaultLanguage = defaultLanguage;
            return this;
        }

        public Builder cacheConfig(CacheConfig cacheConfig) {
            this.cacheConfig = cacheConfig;
            return this;
        }

        public Builder sandboxConfig(SandboxConfig sandboxConfig) {
            this.sandboxConfig = sandboxConfig;
            return this;
        }

        public Builder executorConfig(ExecutorConfig executorConfig) {
            this.executorConfig = executorConfig;
            return this;
        }

        public Builder isolatedExecutorConfig(ExecutorConfig isolatedExecutorConfig) {
            this.isolatedExecutorConfig = isolatedExecutorConfig;
            return this;
        }

        public Builder threadPoolManager(ThreadPoolManager threadPoolManager) {
            if (threadPoolManager != null) {
                this.threadPoolManager = threadPoolManager;
            }
            return this;
        }

        public Builder securityPolicies(List<SecurityPolicy> securityPolicies) {
            this.securityPolicies = securityPolicies;
            return this;
        }

        public Builder excludeScriptModules(Set<String> excludeScriptModules) {
            this.excludeScriptModules = excludeScriptModules;
            return this;
        }

        public Builder allowedScriptModules(Set<String> allowedScriptModules) {
            this.allowedScriptModules = allowedScriptModules;
            return this;
        }

        public Builder allowedSandboxImplementations(Set<String> allowedSandboxImplementations) {
            this.allowedSandboxImplementations = allowedSandboxImplementations;
            return this;
        }

        public Builder allowedSandboxLanguages(Set<String> allowedSandboxLanguages) {
            this.allowedSandboxLanguages = allowedSandboxLanguages;
            return this;
        }

        public Builder allowedSandboxHostAccessModes(Set<SandboxHostAccessMode> allowedSandboxHostAccessModes) {
            this.allowedSandboxHostAccessModes = allowedSandboxHostAccessModes;
            return this;
        }


        public Builder allowedSandboxHostRestrictionModes(Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes) {
            this.allowedSandboxHostRestrictionModes = allowedSandboxHostRestrictionModes;
            return this;
        }


        public Builder requiredSandboxHostRestrictionFlags(Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags) {
            this.requiredSandboxHostRestrictionFlags = requiredSandboxHostRestrictionFlags;
            return this;
        }

        public Builder allowedSandboxResultTransportModes(Set<SandboxResultTransportMode> allowedSandboxResultTransportModes) {
            this.allowedSandboxResultTransportModes = allowedSandboxResultTransportModes;
            return this;
        }


        public Builder allowedSandboxTransportSerializerModes(Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes) {
            this.allowedSandboxTransportSerializerModes = allowedSandboxTransportSerializerModes;
            return this;
        }

        public Builder allowedSandboxTransportPayloadProfiles(Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles) {
            this.allowedSandboxTransportPayloadProfiles = allowedSandboxTransportPayloadProfiles;
            return this;
        }


        public Builder requiredSandboxTransportProtocolCapabilities(Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities) {
            this.requiredSandboxTransportProtocolCapabilities = requiredSandboxTransportProtocolCapabilities;
            return this;
        }

        public Builder requiredSandboxTransportSerializerContractIds(Set<String> requiredSandboxTransportSerializerContractIds) {
            this.requiredSandboxTransportSerializerContractIds = requiredSandboxTransportSerializerContractIds;
            return this;
        }

        public Builder requireEngineCacheCapableSandbox(boolean requireEngineCacheCapableSandbox) {
            this.requireEngineCacheCapableSandbox = requireEngineCacheCapableSandbox;
            return this;
        }

        public Builder requireExternalProcessCompatibleSandbox(boolean requireExternalProcessCompatibleSandbox) {
            this.requireExternalProcessCompatibleSandbox = requireExternalProcessCompatibleSandbox;
            return this;
        }


        public Builder requireJsonSafeExternalResult(boolean requireJsonSafeExternalResult) {
            this.requireJsonSafeExternalResult = requireJsonSafeExternalResult;
            return this;
        }

        public Builder requireJsonSafeExternalMetadata(boolean requireJsonSafeExternalMetadata) {
            this.requireJsonSafeExternalMetadata = requireJsonSafeExternalMetadata;
            return this;
        }

        public Builder detailedResultMetadataEnabled(boolean detailedResultMetadataEnabled) {
            this.resultMetadataProfile = ResultMetadataProfile.fromDetailedEnabled(detailedResultMetadataEnabled);
            this.resultMetadataPolicy = null;
            this.resultMetadataPolicyName = null;
            this.resultMetadataCategoriesExplicitlyConfigured = false;
            return this;
        }

        public Builder resultMetadataProfile(ResultMetadataProfile resultMetadataProfile) {
            if (resultMetadataProfile != null) {
                this.resultMetadataProfile = resultMetadataProfile;
                this.resultMetadataPolicy = null;
                this.resultMetadataPolicyName = null;
                this.resultMetadataCategoriesExplicitlyConfigured = false;
            }
            return this;
        }

        public Builder resultMetadataPolicy(ResultMetadataPolicy resultMetadataPolicy) {
            if (resultMetadataPolicy != null) {
                this.resultMetadataPolicy = resultMetadataPolicy;
                this.resultMetadataProfile = resultMetadataPolicy.getProfile();
                this.resultMetadataPolicyName = null;
                this.resultMetadataCategoriesExplicitlyConfigured = false;
            }
            return this;
        }

        public Builder resultMetadataPolicyName(String resultMetadataPolicyName) {
            this.resultMetadataPolicyName = resultMetadataPolicyName;
            this.resultMetadataPolicy = null;
            this.resultMetadataCategoriesExplicitlyConfigured = false;
            return this;
        }

        public Builder resultMetadataPolicyTemplate(ResultMetadataPolicyTemplate resultMetadataPolicyTemplate) {
            if (resultMetadataPolicyTemplate != null) {
                this.resultMetadataPolicyTemplates.put(resultMetadataPolicyTemplate.getName(), resultMetadataPolicyTemplate);
            }
            return this;
        }

        public Builder resultMetadataPolicyRegistry(ResultMetadataPolicyRegistry resultMetadataPolicyRegistry) {
            this.resultMetadataPolicyRegistry = resultMetadataPolicyRegistry;
            return this;
        }

        public Builder resultMetadataCategories(Set<ResultMetadataCategory> resultMetadataCategories) {
            this.resultMetadataCategories = java.util.EnumSet.noneOf(ResultMetadataCategory.class);
            if (resultMetadataCategories != null) {
                this.resultMetadataCategories.addAll(resultMetadataCategories);
            }
            this.resultMetadataPolicy = null;
            this.resultMetadataPolicyName = null;
            this.resultMetadataCategoriesExplicitlyConfigured = true;
            return this;
        }

        public Builder resultMetadataCategory(ResultMetadataCategory... resultMetadataCategories) {
            this.resultMetadataCategories = java.util.EnumSet.noneOf(ResultMetadataCategory.class);
            if (resultMetadataCategories != null) {
                for (ResultMetadataCategory resultMetadataCategory : resultMetadataCategories) {
                    if (resultMetadataCategory != null) {
                        this.resultMetadataCategories.add(resultMetadataCategory);
                    }
                }
            }
            this.resultMetadataPolicy = null;
            this.resultMetadataPolicyName = null;
            this.resultMetadataCategoriesExplicitlyConfigured = true;
            return this;
        }

        private static Set<ResultMetadataCategory> resolveConfiguredNamedPolicy(String resultMetadataPolicyName,
                                                                                Map<String, ResultMetadataPolicyTemplate> resultMetadataPolicyTemplates) {
            Set<ResultMetadataCategory> resolved = ResultMetadataPolicySupport.resolveNamedPolicy(resultMetadataPolicyName, resultMetadataPolicyTemplates);
            if (resolved == null) {
                throw new IllegalArgumentException("Unknown result metadata policy name: " + resultMetadataPolicyName);
            }
            return resolved;
        }

        /**
         *
         * @param variableManager Global variable manager
         * @return Builder
         */
        public Builder variableManager(VariableManager variableManager) {
            this.variableManager = variableManager;
            return this;
        }

        public EngineConfig build() {
            return new EngineConfig(this);
        }
    }
}

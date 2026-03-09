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
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.result.ResultMetadataProfile;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicy;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyRegistry;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicySupport;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyTemplate;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.ScriptSandbox;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 引擎配置 - 控制脚本引擎的整体行为
 *
 * <p>LingoNexusConfig 是 LingoNexus 最外层配置类，包含以下配置：</p>
 * <ul>
 *   <li><b>defaultLanguage</b>: 默认脚本语言（如 "groovy", "javascript"）</li>
 *   <li><b>cacheConfig</b>: ScriptCacheManager 的缓存配置</li>
 *   <li><b>sandboxConfig</b>: 沙箱安全限制和引擎内部缓存配置</li>
 *   <li><b>executorConfig</b>: 线程池配置</li>
 *   <li><b>securityPolicies</b>: 安全策略列表（责任链模式，运行时对象）</li>
 *   <li><b>variableManager</b>: 变量管理器（运行时对象）</li>
 *   <li><b>excludeScriptModules</b>: 排除的 module（包括内置）列表（高优先级），支持类全限定名、模块名和通配符</li>
 *   <li><b>allowedScriptModules</b>: 允许的 module（包括内置）列表，支持类全限定名、模块名和通配符</li>
 * </ul>
 *
 * <p><b>模块配置说明：</b></p>
 * <ul>
 *   <li>支持类全限定名：如 "io.github.koyan9.lingonexus.utils.MathModule"</li>
 *   <li>支持模块名：如 "math"</li>
 *   <li>支持通配符：如 "io.github.koyan9.lingonexus.utils.*"</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public class LingoNexusConfig {

    private final String defaultLanguage;
    private final CacheConfig cacheConfig;
    private final SandboxConfig sandboxConfig;
    private final ExecutorConfig executorConfig;
    private final ExecutorConfig isolatedExecutorConfig;
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

    private LingoNexusConfig(LingoNexusConfig.Builder builder) {
        this.defaultLanguage = builder.defaultLanguage;
        this.cacheConfig = builder.cacheConfig;
        this.sandboxConfig = builder.sandboxConfig;
        this.executorConfig = builder.executorConfig;
        this.isolatedExecutorConfig = builder.isolatedExecutorConfig;
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
                    : java.util.Collections.unmodifiableSet(java.util.EnumSet.copyOf(builder.resultMetadataCategories)))
                : this.resultMetadataPolicyName != null
                    ? Builder.resolveConfiguredNamedPolicy(this.resultMetadataPolicyName, this.resultMetadataPolicyTemplates)
                : builder.resultMetadataPolicy != null
                    ? builder.resultMetadataPolicy.getCategories()
                    : ResultMetadataCategory.forProfile(builder.resultMetadataProfile);
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
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

    public List<SecurityPolicy> getSecurityPolicies() {
        return securityPolicies;
    }

    public VariableManager getVariableManager() {
        return variableManager;
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

    public static LingoNexusConfig.Builder builder() {
        return new LingoNexusConfig.Builder();
    }

    public static class Builder {
        private String defaultLanguage = ScriptLanguage.getDefault().getId();
        private CacheConfig cacheConfig = CacheConfig.builder().build();
        private SandboxConfig sandboxConfig = SandboxConfig.builder().build();
        private ExecutorConfig executorConfig = ExecutorConfig.builder().build();
        private ExecutorConfig isolatedExecutorConfig;
        private final List<SecurityPolicy> securityPolicies = new ArrayList<>();
        private VariableManager variableManager;
        private final Set<String> excludeScriptModules = new HashSet<>();
        private final Set<String> allowedScriptModules = new HashSet<>();
        private final Set<String> allowedSandboxImplementations = new HashSet<>();
        private final Set<String> allowedSandboxLanguages = new HashSet<>();
        private final Set<SandboxHostAccessMode> allowedSandboxHostAccessModes = new HashSet<>();
        private final Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes = new HashSet<>();
        private final Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags = new HashSet<>();
        private final Set<SandboxResultTransportMode> allowedSandboxResultTransportModes = new HashSet<>();
        private final Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes = new HashSet<>();
        private final Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles = new HashSet<>();
        private final Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities = new HashSet<>();
        private final Set<String> requiredSandboxTransportSerializerContractIds = new HashSet<>();
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

        public LingoNexusConfig.Builder defaultLanguage(ScriptLanguage defaultLanguage) {
            this.defaultLanguage = defaultLanguage.getId();
            return this;
        }

        public LingoNexusConfig.Builder cacheConfig(CacheConfig cacheConfig) {
            if (cacheConfig != null) {
                this.cacheConfig = cacheConfig;
            }
            return this;
        }

        public LingoNexusConfig.Builder sandboxConfig(SandboxConfig sandboxConfig) {
            if (sandboxConfig != null) {
                this.sandboxConfig = sandboxConfig;
            }
            return this;
        }

        public LingoNexusConfig.Builder executorConfig(ExecutorConfig executorConfig) {
            if (executorConfig != null) {
                this.executorConfig = executorConfig;
            }
            return this;
        }

        public LingoNexusConfig.Builder isolatedExecutorConfig(ExecutorConfig isolatedExecutorConfig) {
            this.isolatedExecutorConfig = isolatedExecutorConfig;
            return this;
        }

        /**
         * 添加单个安全策略
         *
         * @param securityPolicy 安全策略
         * @return Builder
         */
        public LingoNexusConfig.Builder addSecurityPolicy(SecurityPolicy securityPolicy) {
            if (securityPolicy != null) {
                this.securityPolicies.add(securityPolicy);
            }
            return this;
        }

        /**
         * 设置安全策略列表（会替换现有策略）
         *
         * @param securityPolicies 安全策略列表
         * @return Builder
         */
        public LingoNexusConfig.Builder securityPolicies(List<SecurityPolicy> securityPolicies) {
            if (securityPolicies != null) {
                this.securityPolicies.addAll(securityPolicies);
            }
            return this;
        }

        /**
         * 排除指定的模块（通过类）
         * <p>为了向后兼容保留此方法，内部会转换为类全限定名</p>
         *
         * @param moduleClass 模块类
         * @return Builder
         */
        @SafeVarargs
        public final LingoNexusConfig.Builder excludeScriptModule(Class<? extends ScriptModule>... moduleClass) {
            if (moduleClass != null && moduleClass.length > 0) {
                for (Class<? extends ScriptModule> clazz : moduleClass) {
                    this.excludeScriptModules.add(clazz.getName());
                }
            }
            return this;
        }

        /**
         * 排除指定的模块（通过字符串）
         * <p>支持类全限定名、模块名和通配符</p>
         *
         * @param moduleNames 模块名称（类全限定名、模块名或通配符）
         * @return Builder
         */
        public LingoNexusConfig.Builder excludeScriptModule(String... moduleNames) {
            if (moduleNames != null && moduleNames.length > 0) {
                this.excludeScriptModules.addAll(List.of(moduleNames));
            }
            return this;
        }

        /**
         * 允许指定的模块（通过类）
         * <p>为了向后兼容保留此方法，内部会转换为类全限定名</p>
         *
         * @param moduleClass 模块类
         * @return Builder
         */
        @SafeVarargs
        public final LingoNexusConfig.Builder allowedScriptModule(Class<? extends ScriptModule>... moduleClass) {
            if (moduleClass != null && moduleClass.length > 0) {
                for (Class<? extends ScriptModule> clazz : moduleClass) {
                    this.allowedScriptModules.add(clazz.getName());
                }
            }
            return this;
        }

        /**
         * 允许指定的模块（通过字符串）
         * <p>支持类全限定名、模块名和通配符</p>
         *
         * @param moduleNames 模块名称（类全限定名、模块名或通配符）
         * @return Builder
         */
        public LingoNexusConfig.Builder allowedScriptModule(String... moduleNames) {
            if (moduleNames != null && moduleNames.length > 0) {
                this.allowedScriptModules.addAll(List.of(moduleNames));
            }
            return this;
        }


        @SafeVarargs
        public final LingoNexusConfig.Builder allowedSandboxImplementation(Class<? extends ScriptSandbox>... sandboxClasses) {
            if (sandboxClasses != null && sandboxClasses.length > 0) {
                for (Class<? extends ScriptSandbox> sandboxClass : sandboxClasses) {
                    if (sandboxClass != null) {
                        this.allowedSandboxImplementations.add(sandboxClass.getName());
                    }
                }
            }
            return this;
        }

        public LingoNexusConfig.Builder allowedSandboxImplementation(String... sandboxClassNames) {
            if (sandboxClassNames != null && sandboxClassNames.length > 0) {
                this.allowedSandboxImplementations.addAll(List.of(sandboxClassNames));
            }
            return this;
        }


        public LingoNexusConfig.Builder allowedSandboxLanguage(ScriptLanguage... languages) {
            if (languages != null && languages.length > 0) {
                for (ScriptLanguage language : languages) {
                    if (language != null) {
                        this.allowedSandboxLanguages.add(language.getId());
                    }
                }
            }
            return this;
        }

        public LingoNexusConfig.Builder allowedSandboxLanguage(String... languageIds) {
            if (languageIds != null && languageIds.length > 0) {
                for (String languageId : languageIds) {
                    if (languageId != null && !languageId.trim().isEmpty()) {
                        this.allowedSandboxLanguages.add(languageId.trim().toLowerCase());
                    }
                }
            }
            return this;
        }


        public LingoNexusConfig.Builder allowedSandboxHostAccessMode(SandboxHostAccessMode... hostAccessModes) {
            if (hostAccessModes != null && hostAccessModes.length > 0) {
                for (SandboxHostAccessMode hostAccessMode : hostAccessModes) {
                    if (hostAccessMode != null) {
                        this.allowedSandboxHostAccessModes.add(hostAccessMode);
                    }
                }
            }
            return this;
        }


        public LingoNexusConfig.Builder allowedSandboxHostRestrictionMode(SandboxHostRestrictionMode... hostRestrictionModes) {
            if (hostRestrictionModes != null && hostRestrictionModes.length > 0) {
                for (SandboxHostRestrictionMode hostRestrictionMode : hostRestrictionModes) {
                    if (hostRestrictionMode != null) {
                        this.allowedSandboxHostRestrictionModes.add(hostRestrictionMode);
                    }
                }
            }
            return this;
        }


        public LingoNexusConfig.Builder requireSandboxHostRestrictionFlag(SandboxHostRestrictionFlag... hostRestrictionFlags) {
            if (hostRestrictionFlags != null && hostRestrictionFlags.length > 0) {
                for (SandboxHostRestrictionFlag hostRestrictionFlag : hostRestrictionFlags) {
                    if (hostRestrictionFlag != null) {
                        this.requiredSandboxHostRestrictionFlags.add(hostRestrictionFlag);
                    }
                }
            }
            return this;
        }

        public LingoNexusConfig.Builder allowedSandboxResultTransportMode(SandboxResultTransportMode... resultTransportModes) {
            if (resultTransportModes != null && resultTransportModes.length > 0) {
                for (SandboxResultTransportMode resultTransportMode : resultTransportModes) {
                    if (resultTransportMode != null) {
                        this.allowedSandboxResultTransportModes.add(resultTransportMode);
                    }
                }
            }
            return this;
        }


        public LingoNexusConfig.Builder allowedSandboxTransportSerializerMode(SandboxTransportSerializerMode... serializerModes) {
            if (serializerModes != null && serializerModes.length > 0) {
                for (SandboxTransportSerializerMode serializerMode : serializerModes) {
                    if (serializerMode != null) {
                        this.allowedSandboxTransportSerializerModes.add(serializerMode);
                    }
                }
            }
            return this;
        }

        public LingoNexusConfig.Builder allowedSandboxTransportPayloadProfile(SandboxTransportPayloadProfile... payloadProfiles) {
            if (payloadProfiles != null && payloadProfiles.length > 0) {
                for (SandboxTransportPayloadProfile payloadProfile : payloadProfiles) {
                    if (payloadProfile != null) {
                        this.allowedSandboxTransportPayloadProfiles.add(payloadProfile);
                    }
                }
            }
            return this;
        }


        public LingoNexusConfig.Builder requireSandboxTransportProtocolCapability(SandboxTransportProtocolCapability... protocolCapabilities) {
            if (protocolCapabilities != null && protocolCapabilities.length > 0) {
                for (SandboxTransportProtocolCapability protocolCapability : protocolCapabilities) {
                    if (protocolCapability != null) {
                        this.requiredSandboxTransportProtocolCapabilities.add(protocolCapability);
                    }
                }
            }
            return this;
        }

        public LingoNexusConfig.Builder requireSandboxTransportSerializerContractId(String... contractIds) {
            if (contractIds != null && contractIds.length > 0) {
                for (String contractId : contractIds) {
                    if (contractId != null && !contractId.trim().isEmpty()) {
                        this.requiredSandboxTransportSerializerContractIds.add(contractId.trim());
                    }
                }
            }
            return this;
        }

        public LingoNexusConfig.Builder requireEngineCacheCapableSandbox(boolean requireEngineCacheCapableSandbox) {
            this.requireEngineCacheCapableSandbox = requireEngineCacheCapableSandbox;
            return this;
        }

        public LingoNexusConfig.Builder requireExternalProcessCompatibleSandbox(boolean requireExternalProcessCompatibleSandbox) {
            this.requireExternalProcessCompatibleSandbox = requireExternalProcessCompatibleSandbox;
            return this;
        }


        public LingoNexusConfig.Builder requireJsonSafeExternalResult(boolean requireJsonSafeExternalResult) {
            this.requireJsonSafeExternalResult = requireJsonSafeExternalResult;
            return this;
        }

        public LingoNexusConfig.Builder requireJsonSafeExternalMetadata(boolean requireJsonSafeExternalMetadata) {
            this.requireJsonSafeExternalMetadata = requireJsonSafeExternalMetadata;
            return this;
        }

        public LingoNexusConfig.Builder detailedResultMetadataEnabled(boolean detailedResultMetadataEnabled) {
            this.resultMetadataProfile = ResultMetadataProfile.fromDetailedEnabled(detailedResultMetadataEnabled);
            this.resultMetadataPolicy = null;
            this.resultMetadataPolicyName = null;
            this.resultMetadataCategoriesExplicitlyConfigured = false;
            return this;
        }

        public LingoNexusConfig.Builder resultMetadataProfile(ResultMetadataProfile resultMetadataProfile) {
            if (resultMetadataProfile != null) {
                this.resultMetadataProfile = resultMetadataProfile;
                this.resultMetadataPolicy = null;
                this.resultMetadataPolicyName = null;
                this.resultMetadataCategoriesExplicitlyConfigured = false;
            }
            return this;
        }

        public LingoNexusConfig.Builder resultMetadataPolicy(ResultMetadataPolicy resultMetadataPolicy) {
            if (resultMetadataPolicy != null) {
                this.resultMetadataPolicy = resultMetadataPolicy;
                this.resultMetadataProfile = resultMetadataPolicy.getProfile();
                this.resultMetadataPolicyName = null;
                this.resultMetadataCategoriesExplicitlyConfigured = false;
            }
            return this;
        }

        public LingoNexusConfig.Builder resultMetadataPolicyName(String resultMetadataPolicyName) {
            this.resultMetadataPolicyName = resultMetadataPolicyName;
            this.resultMetadataPolicy = null;
            this.resultMetadataCategoriesExplicitlyConfigured = false;
            return this;
        }

        public LingoNexusConfig.Builder resultMetadataPolicyTemplate(ResultMetadataPolicyTemplate resultMetadataPolicyTemplate) {
            if (resultMetadataPolicyTemplate != null) {
                this.resultMetadataPolicyTemplates.put(resultMetadataPolicyTemplate.getName(), resultMetadataPolicyTemplate);
            }
            return this;
        }

        public LingoNexusConfig.Builder resultMetadataPolicyRegistry(ResultMetadataPolicyRegistry resultMetadataPolicyRegistry) {
            this.resultMetadataPolicyRegistry = resultMetadataPolicyRegistry;
            return this;
        }

        public LingoNexusConfig.Builder resultMetadataCategories(Set<ResultMetadataCategory> resultMetadataCategories) {
            this.resultMetadataCategories = java.util.EnumSet.noneOf(ResultMetadataCategory.class);
            if (resultMetadataCategories != null) {
                this.resultMetadataCategories.addAll(resultMetadataCategories);
            }
            this.resultMetadataPolicy = null;
            this.resultMetadataPolicyName = null;
            this.resultMetadataCategoriesExplicitlyConfigured = true;
            return this;
        }

        public LingoNexusConfig.Builder resultMetadataCategory(ResultMetadataCategory... resultMetadataCategories) {
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
        public LingoNexusConfig.Builder variableManager(VariableManager variableManager) {
            this.variableManager = variableManager;
            return this;
        }

        public LingoNexusConfig build() {
            return new LingoNexusConfig(this);
        }
    }

}

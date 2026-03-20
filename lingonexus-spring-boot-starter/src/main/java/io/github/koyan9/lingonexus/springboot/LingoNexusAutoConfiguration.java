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

package io.github.koyan9.lingonexus.springboot;

import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.ExecutorConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyRegistry;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyTemplate;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import io.github.koyan9.lingonexus.core.context.GlobalVariableManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Set;

/**
 * LingoNexus Spring Boot 自动配置
 *
 * <p>参考 {@link io.github.koyan9.lingonexus.core.LingoNexusBuilder} 的设计，
 * 通过 {@link LingoNexusConfig} 统一配置，简化 Bean 注册。</p>
 *
 * <p>通过 {@code lingonexus.enabled=false} 可以禁用自动配置。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
@Configuration
@EnableConfigurationProperties(LingoNexusProperties.class)
@ConditionalOnProperty(prefix = LingoNexusConstants.CONFIG_PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class LingoNexusAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(LingoNexusAutoConfiguration.class);

    /**
     * 创建 LingoNexusConfig 配置对象
     * <p>将 Spring Boot 配置属性转换为 LingoNexusConfig</p>
     */
    @Bean
    @ConditionalOnMissingBean
    public LingoNexusConfig lingoNexusConfig(LingoNexusProperties properties) {
        // 创建 GlobalVariableManager 并注入配置的全局变量
        GlobalVariableManager variableManager = new GlobalVariableManager();
        Map<String, Object> globalVariables = properties.getGlobalVariables();
        if (globalVariables != null && !globalVariables.isEmpty()) {
            for (Map.Entry<String, Object> entry : globalVariables.entrySet()) {
                variableManager.setVariable(entry.getKey(), entry.getValue());
                logger.debug("Registered global variable: {} = {}", entry.getKey(), entry.getValue());
            }
            logger.info("Loaded {} global variables from configuration", globalVariables.size());
        }

        ResultMetadataPolicyRegistry resultMetadataPolicyRegistry = createResultMetadataPolicyRegistry(properties.getMetadata());

        LingoNexusConfig.Builder builder = LingoNexusConfig.builder()
                .defaultLanguage(properties.getDefaultLanguage() != null
                        ? ScriptLanguage.fromString(properties.getDefaultLanguage())
                        : ScriptLanguage.getDefault())
                .cacheConfig(CacheConfig.builder()
                        .enabled(properties.getCache().isEnabled())
                        .maxSize(properties.getCache().getMaxSize())
                        .expireAfterWriteMs(properties.getCache().getExpireAfterWriteMs())
                        .expireAfterAccessMs(properties.getCache().getExpireAfterAccessMs())
                        .build())
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(properties.getSandbox().isEnabled())
                        .maxScriptSize(properties.getSandbox().getMaxScriptSize())
                        .timeoutMs(properties.getSandbox().getTimeoutMs())
                        .enableEngineCache(properties.getSandbox().isEnableEngineCache())
                        .isolationMode(properties.getSandbox().getIsolationMode())
                        .externalProcessPoolSize(properties.getSandbox().getExternalProcessPoolSize())
                        .externalProcessStartupRetries(properties.getSandbox().getExternalProcessStartupRetries())
                        .externalProcessPrewarmCount(properties.getSandbox().getExternalProcessPrewarmCount())
                        .externalProcessIdleTtlMs(properties.getSandbox().getExternalProcessIdleTtlMs())
                        .externalProcessBorrowTimeoutMs(properties.getSandbox().getExternalProcessBorrowTimeoutMs())
                        .externalProcessExecutorCacheMaxSize(properties.getSandbox().getExternalProcessExecutorCacheMaxSize())
                        .externalProcessExecutorCacheIdleTtlMs(properties.getSandbox().getExternalProcessExecutorCacheIdleTtlMs())
                        .classWhitelist(properties.getSandbox().getClassWhitelist())
                        .classBlacklist(properties.getSandbox().getClassBlacklist())
                        .build())
                .executorConfig(ExecutorConfig.builder()
                        .corePoolSize(properties.getExecutor().getCorePoolSize())
                        .maxPoolSize(properties.getExecutor().getMaxPoolSize())
                        .keepAliveTimeSeconds(properties.getExecutor().getKeepAliveTimeSeconds())
                        .queueCapacity(properties.getExecutor().getQueueCapacity())
                        .threadNamePrefix(properties.getExecutor().getThreadNamePrefix())
                        .rejectionPolicy(properties.getExecutor().getRejectionPolicy())
                        .build())
                .isolatedExecutorConfig(ExecutorConfig.builder()
                        .corePoolSize(properties.getIsolatedExecutor().getCorePoolSize())
                        .maxPoolSize(properties.getIsolatedExecutor().getMaxPoolSize())
                        .keepAliveTimeSeconds(properties.getIsolatedExecutor().getKeepAliveTimeSeconds())
                        .queueCapacity(properties.getIsolatedExecutor().getQueueCapacity())
                        .threadNamePrefix(properties.getIsolatedExecutor().getThreadNamePrefix())
                        .rejectionPolicy(properties.getIsolatedExecutor().getRejectionPolicy())
                        .build())
                .excludeScriptModule(properties.getExcludeScriptModules() != null
                        ? properties.getExcludeScriptModules().toArray(new String[0])
                        : new String[0])
                .allowedScriptModule(properties.getAllowedScriptModules() != null
                        ? properties.getAllowedScriptModules().toArray(new String[0])
                        : new String[0])
                .resultMetadataProfile(properties.getMetadata().getProfile())
                .resultMetadataPolicy(properties.getMetadata().getPolicy())
                .resultMetadataPolicyName(properties.getMetadata().getPolicyName())
                .resultMetadataCategories(properties.getMetadata().getCategories())
                .resultMetadataPolicyRegistry(resultMetadataPolicyRegistry)
                .variableManager(variableManager);

        Set<String> allowedSandboxImplementations = properties.getAllowedSandboxImplementations();
        if (allowedSandboxImplementations != null && !allowedSandboxImplementations.isEmpty()) {
            builder.allowedSandboxImplementation(allowedSandboxImplementations.toArray(new String[0]));
        }

        Set<String> allowedSandboxLanguages = properties.getAllowedSandboxLanguages();
        if (allowedSandboxLanguages != null && !allowedSandboxLanguages.isEmpty()) {
            builder.allowedSandboxLanguage(allowedSandboxLanguages.toArray(new String[0]));
        }

        Set<SandboxHostAccessMode> allowedSandboxHostAccessModes = properties.getAllowedSandboxHostAccessModes();
        if (allowedSandboxHostAccessModes != null && !allowedSandboxHostAccessModes.isEmpty()) {
            builder.allowedSandboxHostAccessMode(allowedSandboxHostAccessModes.toArray(new SandboxHostAccessMode[0]));
        }

        Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes = properties.getAllowedSandboxHostRestrictionModes();
        if (allowedSandboxHostRestrictionModes != null && !allowedSandboxHostRestrictionModes.isEmpty()) {
            builder.allowedSandboxHostRestrictionMode(allowedSandboxHostRestrictionModes.toArray(new SandboxHostRestrictionMode[0]));
        }

        Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags = properties.getRequiredSandboxHostRestrictionFlags();
        if (requiredSandboxHostRestrictionFlags != null && !requiredSandboxHostRestrictionFlags.isEmpty()) {
            builder.requireSandboxHostRestrictionFlag(requiredSandboxHostRestrictionFlags.toArray(new SandboxHostRestrictionFlag[0]));
        }

        Set<SandboxResultTransportMode> allowedSandboxResultTransportModes = properties.getAllowedSandboxResultTransportModes();
        if (allowedSandboxResultTransportModes != null && !allowedSandboxResultTransportModes.isEmpty()) {
            builder.allowedSandboxResultTransportMode(allowedSandboxResultTransportModes.toArray(new SandboxResultTransportMode[0]));
        }

        Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes = properties.getAllowedSandboxTransportSerializerModes();
        if (allowedSandboxTransportSerializerModes != null && !allowedSandboxTransportSerializerModes.isEmpty()) {
            builder.allowedSandboxTransportSerializerMode(allowedSandboxTransportSerializerModes.toArray(new SandboxTransportSerializerMode[0]));
        }

        Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles = properties.getAllowedSandboxTransportPayloadProfiles();
        if (allowedSandboxTransportPayloadProfiles != null && !allowedSandboxTransportPayloadProfiles.isEmpty()) {
            builder.allowedSandboxTransportPayloadProfile(allowedSandboxTransportPayloadProfiles.toArray(new SandboxTransportPayloadProfile[0]));
        }

        Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities = properties.getRequiredSandboxTransportProtocolCapabilities();
        if (requiredSandboxTransportProtocolCapabilities != null && !requiredSandboxTransportProtocolCapabilities.isEmpty()) {
            builder.requireSandboxTransportProtocolCapability(requiredSandboxTransportProtocolCapabilities.toArray(new SandboxTransportProtocolCapability[0]));
        }

        Set<String> requiredSandboxTransportSerializerContractIds = properties.getRequiredSandboxTransportSerializerContractIds();
        if (requiredSandboxTransportSerializerContractIds != null && !requiredSandboxTransportSerializerContractIds.isEmpty()) {
            builder.requireSandboxTransportSerializerContractId(requiredSandboxTransportSerializerContractIds.toArray(new String[0]));
        }

        builder.requireEngineCacheCapableSandbox(properties.isRequireEngineCacheCapableSandbox());
        builder.requireExternalProcessCompatibleSandbox(properties.isRequireExternalProcessCompatibleSandbox());
        builder.requireJsonSafeExternalResult(properties.isRequireJsonSafeExternalResult());
        builder.requireJsonSafeExternalMetadata(properties.isRequireJsonSafeExternalMetadata());

        return builder.build();
    }

    /**
     * 创建 LingoNexusExecutor 主入口
     * <p>使用 {@link LingoNexusBuilder} 构建，自动发现并注册 Sandbox 和 Module</p>
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public LingoNexusExecutor lingoNexusExecutor(LingoNexusConfig config) {
        logger.info("Initializing LingoNexus with Spring Boot auto-configuration");

        // 使用 LingoNexusBuilder 创建实例，它会自动发现并注册 Sandbox 和 Module
        return LingoNexusBuilder.loadInstance(config);
    }

    private ResultMetadataPolicyRegistry createResultMetadataPolicyRegistry(LingoNexusProperties.MetadataProperties metadataProperties) {
        ResultMetadataPolicyRegistry registry = ResultMetadataPolicyRegistry.create();
        if (metadataProperties == null || metadataProperties.getPolicyTemplates() == null) {
            return registry;
        }
        for (Map.Entry<String, LingoNexusProperties.PolicyTemplateProperties> entry : metadataProperties.getPolicyTemplates().entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                continue;
            }
            LingoNexusProperties.PolicyTemplateProperties value = entry.getValue();
            ResultMetadataPolicyTemplate template = ResultMetadataPolicyTemplate.builder()
                    .name(entry.getKey())
                    .parentPolicyName(value != null ? value.getParentPolicyName() : null)
                    .categories(value != null ? value.getCategories() : null)
                    .build();
            registry.registerTemplate(template);
        }
        return registry;
    }
}

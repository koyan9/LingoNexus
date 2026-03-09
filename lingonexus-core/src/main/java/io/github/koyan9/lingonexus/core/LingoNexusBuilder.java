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

package io.github.koyan9.lingonexus.core;

import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.context.LingoNexusContext;
import io.github.koyan9.lingonexus.api.executor.ThreadPoolManager;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxProviderMetadata;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.ScriptSandbox;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;
import io.github.koyan9.lingonexus.api.util.SPILoader;
import io.github.koyan9.lingonexus.core.cache.CaffeineCacheProvider;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheManager;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import io.github.koyan9.lingonexus.core.impl.DefaultSandboxManager;
import io.github.koyan9.lingonexus.core.impl.DefaultScriptExecutor;
import io.github.koyan9.lingonexus.core.impl.LingoNexusExecutorImpl;
import io.github.koyan9.lingonexus.core.security.BuiltInPolicy;
import io.github.koyan9.lingonexus.core.util.LingoNexusUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * LingoNexus 闁哄瀚紓鎾诲闯?- 闁绘瑯鍓涢悵娑樜熼垾宕囩闁稿繈鍎辫ぐ娑㈡晬鐏炵偓锟ラ梻鍥ｅ亾 Spring 濞撴碍绻嗙粋?
 *
 * <p>濞达綀娉曢弫銈囩矆鏉炴壆浼?</p>
 * <pre>{@code
 * // 1. 闁告帗绋戠紓鎾绘煀瀹ュ洨鏋?
 * LingoNexusConfig config = LingoNexusConfig.builder()
 *     .defaultLanguage(ScriptLanguage.GROOVY)
 *     .cacheConfig(CacheConfig.builder().enabled(true).build())
 *     .sandboxConfig(SandboxConfig.builder().timeoutMs(5000).build())
 *     .build();
 *
 * // 2. 闁哄瀚紓鎾诲礂閵夈儱缍撻柟绗涘棭鏀介柛? * LingoNexusExecutor engine = LingoNexusBuilder.loadInstance(config);
 * }</pre>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class LingoNexusBuilder {

    private static final Logger logger = LoggerFactory.getLogger(LingoNexusBuilder.class);

    private static volatile LingoNexusExecutor lingoNexusExecutor;
    private static volatile ThreadPoolManager singletonThreadPoolManager;

    private LingoNexusBuilder() {
    }

    /**
     * 闁哄瀚紓?LingoNexusExecutor 閻庡湱鍋樼欢?
     *
     * @return 閻庣懓鏈弳锝夋煀瀹ュ洨鏋傞柣銊ュ閸撳ジ寮甸鈧槐鈺呭箼?     * @throws Exception 濠碘€冲€归悘澶愬嫉椤忓浂鍟庣紓?engineConfig 闁瑰瓨鐗楅悗顖炴焻?Sandbox/Module 濠㈡儼绮剧憴?
     */
    public static LingoNexusExecutor loadInstance(LingoNexusConfig lingoNexusConfig) {
        if (lingoNexusExecutor == null) {
            synchronized (LingoNexusBuilder.class) {
                if (lingoNexusExecutor == null) {
                    lingoNexusExecutor = createExecutor(lingoNexusConfig, true);
                }
            }
        }
        return lingoNexusExecutor;
    }

    /**
     * Reset the singleton instance (for testing purposes only)
     * <p>This method should only be used in test environments to reset the singleton
     * instance between tests with different configurations.</p>
     */
    public static synchronized void resetInstance() {
        if (lingoNexusExecutor != null) {
            lingoNexusExecutor.close();
        }
        if (singletonThreadPoolManager != null) {
            singletonThreadPoolManager = null;
        }
        lingoNexusExecutor = null;
        LingoNexusContext.clear();
    }

    /**
     * 闁告帗绋戠紓鎾诲棘閹殿喗鐣?LingoNexusExecutor 閻庡湱鍋樼欢銉╂晬閸儲濮滈柛妤佹磻缁躲儵鏁?     * <p>婵絽绻戦鑲╂嫬閸愵亝鏆忛梺顔藉灊缁变即宕氬☉妯肩处濞戞挴鍋撳☉鎿冧簻閸欏繘寮幍顔界暠閻庡湱鍋樼欢銉╂晬瀹€鍕ㄥ亾閸屾粍鏆忓ù婊冮叄濞撳墎鎲版担鍝ユ殮闁稿繈鍔戝▓褏绮嬮懡銈嗙暠闁革妇鍎ゅ▍娆撴晬?/p>
     * <ul>
     *   <li>濞戞挸绉撮幃鎾垛偓鍦仒缁躲儲鎷呯捄銊︽殢濞戞挸绉撮幃鎾绘儍閸曨垰甯崇紓?/li>
     *   <li>濞戞挸绉撮幃鎾垛偓鍦仒缁躲儲绋婄€ｎ喗锛熼柣銊ュ缁憋妇鈧稒锚閻ｎ剟宕楅妸鈺傤吘缂?/li>
     *   <li>濞戞挸绉撮幃鎾垛偓鍦仒缁躲儲绋婄€ｎ喗锛熼柣銊ュ鑶╅柛褎顨嗛弫鐐哄礃鐏炵晫鏆氶柛蹇嬪姂濞堁呯矉?/li>
     *   <li>濞戞挸绉撮幃鎾垛偓鍦仒缁躲儲绋婄€ｎ喗锛熼柣銊ュ閻瑧绮绘潏鈺佺畾濠⒀冨暙閻ｎ剟宕楅妸鈺傤吘缂?/li>
     * </ul>
     *
     * <p>濞达綀娉曢弫銈囩矆鏉炴壆浼?</p>
     * <pre>{@code
     * // 闁告帗绋戠紓鎾寸▔閵堝嫰鍤嬮悗鐟拌嫰閸欏繘姊鹃弮鍌ょ€查柣銊ュ閻ゅ嫭绗?     * LingoNexusConfig config1 = LingoNexusConfig.builder()
     *     .defaultLanguage(ScriptLanguage.GROOVY)
     *     .build();
     * LingoNexusExecutor engine1 = LingoNexusBuilder.createNewInstance(config1);
     *
     * LingoNexusConfig config2 = LingoNexusConfig.builder()
     *     .defaultLanguage(ScriptLanguage.JAVASCRIPT)
     *     .build();
     * LingoNexusExecutor engine2 = LingoNexusBuilder.createNewInstance(config2);
     *
     * // engine1 闁?engine2 閻庣懓鑻崣蹇涙⒕閺冨偆鐎查柨娑樺缁ㄧ増绋夊鍛殯闁?     * }</pre>
     *
     * @param lingoNexusConfig 闂佹澘绉堕悿鍡欌偓鐢殿攰閽?
     * @return 闁哄倹澹嗗▓?LingoNexusExecutor 閻庡湱鍋樼欢?
     * @throws IllegalStateException 濠碘€冲€归悘澶愭煀瀹ュ洨鏋傚☉?null
     */
    public static LingoNexusExecutor createNewInstance(LingoNexusConfig lingoNexusConfig) {
        return createExecutor(lingoNexusConfig, false);
    }

    /**
     * 闁哄瀚紓?LingoNexusExecutor 閻庡湱鍋樼欢?
     *
     * @return 閻庣懓鏈弳锝夋煀瀹ュ洨鏋傞柣銊ュ閸撳ジ寮甸鈧槐鈺呭箼?     */
    private static LingoNexusExecutor createExecutor(LingoNexusConfig lingoNexusConfig, boolean trackSingletonManager) {
        // 濡ょ姴鐭侀惁澶愬矗閸屾稒娈?
        if (lingoNexusConfig == null) {
            throw new IllegalStateException("EngineConfig is required. Please call engineConfig() method first.");
        }

        ThreadPoolManager threadPoolManager = ThreadPoolManager.createIndependentManager();
        if (trackSingletonManager) {
            singletonThreadPoolManager = threadPoolManager;
        }

        List<SecurityPolicy> securityPolicies = new ArrayList<>(lingoNexusConfig.getSecurityPolicies());
        addDefaultSecurityPolicies(securityPolicies);

        EngineConfig.Builder finalConfigBuilder = EngineConfig.builder()
                .defaultLanguage(lingoNexusConfig.getDefaultLanguage())
                .securityPolicies(securityPolicies)
                .variableManager(lingoNexusConfig.getVariableManager())
                .cacheConfig(lingoNexusConfig.getCacheConfig())
                .sandboxConfig(lingoNexusConfig.getSandboxConfig())
                .executorConfig(lingoNexusConfig.getExecutorConfig())
                .isolatedExecutorConfig(lingoNexusConfig.getIsolatedExecutorConfig())
                .threadPoolManager(threadPoolManager)
                .excludeScriptModules(lingoNexusConfig.getExcludeScriptModules())
                .allowedScriptModules(lingoNexusConfig.getAllowedScriptModules())
                .allowedSandboxImplementations(lingoNexusConfig.getAllowedSandboxImplementations())
                .allowedSandboxLanguages(lingoNexusConfig.getAllowedSandboxLanguages())
                .allowedSandboxHostAccessModes(lingoNexusConfig.getAllowedSandboxHostAccessModes())
                .allowedSandboxHostRestrictionModes(lingoNexusConfig.getAllowedSandboxHostRestrictionModes())
                .requiredSandboxHostRestrictionFlags(lingoNexusConfig.getRequiredSandboxHostRestrictionFlags())
                .allowedSandboxResultTransportModes(lingoNexusConfig.getAllowedSandboxResultTransportModes())
                .allowedSandboxTransportSerializerModes(lingoNexusConfig.getAllowedSandboxTransportSerializerModes())
                .allowedSandboxTransportPayloadProfiles(lingoNexusConfig.getAllowedSandboxTransportPayloadProfiles())
                .requiredSandboxTransportProtocolCapabilities(lingoNexusConfig.getRequiredSandboxTransportProtocolCapabilities())
                .requiredSandboxTransportSerializerContractIds(lingoNexusConfig.getRequiredSandboxTransportSerializerContractIds())
                .requireEngineCacheCapableSandbox(lingoNexusConfig.isRequireEngineCacheCapableSandbox())
                .requireExternalProcessCompatibleSandbox(lingoNexusConfig.isRequireExternalProcessCompatibleSandbox())
                .requireJsonSafeExternalResult(lingoNexusConfig.isRequireJsonSafeExternalResult())
                .requireJsonSafeExternalMetadata(lingoNexusConfig.isRequireJsonSafeExternalMetadata())
                .resultMetadataProfile(lingoNexusConfig.getResultMetadataProfile())
                .resultMetadataPolicy(lingoNexusConfig.getResultMetadataPolicy())
                .resultMetadataPolicyName(lingoNexusConfig.getResultMetadataPolicyName())
                .resultMetadataCategories(lingoNexusConfig.getResultMetadataCategories())
                .resultMetadataPolicyRegistry(io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyRegistry.create().registerTemplates(lingoNexusConfig.getResultMetadataPolicyTemplates()))
                ;
        EngineConfig finalConfig = finalConfigBuilder.build();

        DefaultSandboxManager sandboxManager = createDefaultSandboxManagerInstance(finalConfig);

        DefaultModuleRegistry moduleRegistry = createDefaultModuleRegistryInstance(finalConfig);

        finalConfig.setModuleRegistry(moduleRegistry);
        finalConfig.setSandboxManager(sandboxManager);

        // 闁哄瀚紓?ScriptCacheManager
        ScriptCacheManager cacheManager = new ScriptCacheManager(new CaffeineCacheProvider<>(finalConfig.getCacheConfig()));
        // 闁哄瀚紓?ScriptExecutor
        DefaultScriptExecutor executor = new DefaultScriptExecutor(finalConfig, cacheManager);
        // 闁哄瀚紓?LingoNexusExecutor
        LingoNexusExecutor facade = new LingoNexusExecutorImpl(finalConfig, executor);

        // 閻犱礁澧介悿鍡涘礂閵娿儳婀板☉鎾筹梗缁楀懘寮?        LingoNexusContext.setContext(lingoNexusConfig, facade);

        logger.info("LingoNexus initialized successfully: defaultLanguage={}, cacheEnabled={}, sandboxEnabled={}, sandboxes={}, modules={}",
                finalConfig.getDefaultLanguage(), finalConfig.getCacheConfig().isEnabled(), finalConfig.getSandboxConfig().isEnabled(),
                sandboxManager.getAllSandboxes().size(), moduleRegistry.getAllModules().size());
        return facade;
    }


    /**
     * 闂侇偅淇虹换鍐矗瀹ュ懐娈搁柛鎺撶☉缂?Sandbox 閻庡湱鍋樼欢?
     */
    private static DefaultSandboxManager createDefaultSandboxManagerInstance(EngineConfig engineConfig) {
        DefaultSandboxManager sandboxManager = new DefaultSandboxManager();
        Set<String> allowedSandboxImplementations = engineConfig.getAllowedSandboxImplementations();
        Set<String> allowedSandboxLanguages = normalizeSandboxLanguages(engineConfig.getAllowedSandboxLanguages());
        Set<SandboxHostAccessMode> allowedSandboxHostAccessModes = normalizeHostAccessModes(engineConfig.getAllowedSandboxHostAccessModes());
        Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes = normalizeHostRestrictionModes(engineConfig.getAllowedSandboxHostRestrictionModes());
        Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags = normalizeHostRestrictionFlags(engineConfig.getRequiredSandboxHostRestrictionFlags());
        Set<SandboxResultTransportMode> allowedSandboxResultTransportModes = normalizeResultTransportModes(engineConfig.getAllowedSandboxResultTransportModes());
        Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes = normalizeTransportSerializerModes(engineConfig.getAllowedSandboxTransportSerializerModes());
        Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles = normalizeTransportPayloadProfiles(engineConfig.getAllowedSandboxTransportPayloadProfiles());
        Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities = normalizeTransportProtocolCapabilities(engineConfig.getRequiredSandboxTransportProtocolCapabilities(), engineConfig.getSandboxConfig().getIsolationMode() == io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode.EXTERNAL_PROCESS);
        Set<String> requiredSandboxTransportSerializerContractIds = normalizeTransportSerializerContractIds(engineConfig.getRequiredSandboxTransportSerializerContractIds());
        boolean requireEngineCacheCapableSandbox = engineConfig.isRequireEngineCacheCapableSandbox();
        boolean requireExternalProcessCompatibleSandbox = engineConfig.isRequireExternalProcessCompatibleSandbox()
                || engineConfig.getSandboxConfig().getIsolationMode() == io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode.EXTERNAL_PROCESS;
        boolean requireJsonSafeExternalResult = engineConfig.isRequireJsonSafeExternalResult()
                || engineConfig.getSandboxConfig().getIsolationMode() == io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode.EXTERNAL_PROCESS;
        boolean requireJsonSafeExternalMetadata = engineConfig.isRequireJsonSafeExternalMetadata();
        Set<ScriptSandbox> importScriptSandboxes = SPILoader.load(
                ScriptSandbox.class,
                (implClass, supplier) -> {
                    if (allowedSandboxImplementations != null && !allowedSandboxImplementations.isEmpty()
                            && !allowedSandboxImplementations.contains(implClass.getName())) {
                        return null;
                    }
                    SandboxProviderMetadata metadata = implClass.getAnnotation(SandboxProviderMetadata.class);
                    if (metadata != null && !matchesCapabilities(metadata, allowedSandboxLanguages, allowedSandboxHostAccessModes, allowedSandboxHostRestrictionModes, requiredSandboxHostRestrictionFlags, allowedSandboxResultTransportModes, allowedSandboxTransportSerializerModes, allowedSandboxTransportPayloadProfiles, requiredSandboxTransportProtocolCapabilities, requiredSandboxTransportSerializerContractIds, requireEngineCacheCapableSandbox, requireExternalProcessCompatibleSandbox, requireJsonSafeExternalResult, requireJsonSafeExternalMetadata)) {
                        return null;
                    }
                    if (allowedSandboxLanguages.isEmpty() && allowedSandboxHostAccessModes.isEmpty()
                            && allowedSandboxHostRestrictionModes.isEmpty() && requiredSandboxHostRestrictionFlags.isEmpty() && allowedSandboxResultTransportModes.isEmpty()
                            && allowedSandboxTransportSerializerModes.isEmpty() && allowedSandboxTransportPayloadProfiles.isEmpty()
                            && requiredSandboxTransportProtocolCapabilities.isEmpty() && requiredSandboxTransportSerializerContractIds.isEmpty()
                            && !requireEngineCacheCapableSandbox && !requireExternalProcessCompatibleSandbox
                            && !requireJsonSafeExternalResult && !requireJsonSafeExternalMetadata) {
                        return supplier.get();
                    }

                    ScriptSandbox sandbox = supplier.get();
                    if (!allowedSandboxLanguages.isEmpty() && !supportsAnyLanguage(sandbox.getSupportedLanguages(), allowedSandboxLanguages)) {
                        return null;
                    }
                    if ((requireEngineCacheCapableSandbox || requireExternalProcessCompatibleSandbox || !allowedSandboxHostAccessModes.isEmpty()
                            || !allowedSandboxHostRestrictionModes.isEmpty() || !requiredSandboxHostRestrictionFlags.isEmpty() || !allowedSandboxResultTransportModes.isEmpty()
                            || !allowedSandboxTransportSerializerModes.isEmpty() || !allowedSandboxTransportPayloadProfiles.isEmpty()
                            || !requiredSandboxTransportProtocolCapabilities.isEmpty() || !requiredSandboxTransportSerializerContractIds.isEmpty()
                            || requireJsonSafeExternalResult || requireJsonSafeExternalMetadata) && metadata == null) {
                        return null;
                    }
                    return sandbox;
                },
                engineConfig
        );
        registerPreferredSandboxes(
                sandboxManager,
                importScriptSandboxes,
                allowedSandboxLanguages,
                allowedSandboxHostAccessModes,
                allowedSandboxHostRestrictionModes,
                requiredSandboxHostRestrictionFlags,
                allowedSandboxResultTransportModes,
                allowedSandboxTransportSerializerModes,
                allowedSandboxTransportPayloadProfiles,
                requiredSandboxTransportProtocolCapabilities,
                requiredSandboxTransportSerializerContractIds,
                requireEngineCacheCapableSandbox,
                requireExternalProcessCompatibleSandbox,
                requireJsonSafeExternalResult,
                requireJsonSafeExternalMetadata
        );
        return sandboxManager;
    }

    private static void registerPreferredSandboxes(DefaultSandboxManager sandboxManager,
                                                   Set<ScriptSandbox> sandboxes,
                                                   Set<String> allowedSandboxLanguages,
                                                   Set<SandboxHostAccessMode> allowedSandboxHostAccessModes,
                                                   Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes,
                                                   Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags,
                                                   Set<SandboxResultTransportMode> allowedSandboxResultTransportModes,
                                                   Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes,
                                                   Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles,
                                                   Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities,
                                                   Set<String> requiredSandboxTransportSerializerContractIds,
                                                   boolean requireEngineCacheCapableSandbox,
                                                   boolean requireExternalProcessCompatibleSandbox,
                                                   boolean requireJsonSafeExternalResult,
                                                   boolean requireJsonSafeExternalMetadata) {
        java.util.Map<String, SandboxRegistrationCandidate> selected = new java.util.LinkedHashMap<String, SandboxRegistrationCandidate>();
        for (ScriptSandbox sandbox : sandboxes) {
            SandboxProviderMetadata metadata = sandbox.getClass().getAnnotation(SandboxProviderMetadata.class);
            if (metadata == null && hasCapabilityConstraints(
                    allowedSandboxHostAccessModes,
                    allowedSandboxHostRestrictionModes,
                    requiredSandboxHostRestrictionFlags,
                    allowedSandboxResultTransportModes,
                    allowedSandboxTransportSerializerModes,
                    allowedSandboxTransportPayloadProfiles,
                    requiredSandboxTransportProtocolCapabilities,
                    requiredSandboxTransportSerializerContractIds,
                    requireEngineCacheCapableSandbox,
                    requireExternalProcessCompatibleSandbox,
                    requireJsonSafeExternalResult,
                    requireJsonSafeExternalMetadata)) {
                continue;
            }
            if (metadata != null && !matchesCapabilities(
                    metadata,
                    allowedSandboxLanguages,
                    allowedSandboxHostAccessModes,
                    allowedSandboxHostRestrictionModes,
                    requiredSandboxHostRestrictionFlags,
                    allowedSandboxResultTransportModes,
                    allowedSandboxTransportSerializerModes,
                    allowedSandboxTransportPayloadProfiles,
                    requiredSandboxTransportProtocolCapabilities,
                    requiredSandboxTransportSerializerContractIds,
                    requireEngineCacheCapableSandbox,
                    requireExternalProcessCompatibleSandbox,
                    requireJsonSafeExternalResult,
                    requireJsonSafeExternalMetadata)) {
                continue;
            }
            int priority = metadata != null ? metadata.priority() : 0;
            String providerId = resolveProviderId(sandbox, metadata);
            for (String language : sandbox.getSupportedLanguages()) {
                String normalizedLanguage = normalizeLanguage(language);
                if (normalizedLanguage == null) {
                    continue;
                }
                if (!allowedSandboxLanguages.isEmpty() && !allowedSandboxLanguages.contains(normalizedLanguage)) {
                    continue;
                }
                SandboxRegistrationCandidate candidate = new SandboxRegistrationCandidate(
                        normalizedLanguage,
                        sandbox,
                        priority,
                        providerId,
                        sandbox.getClass().getName()
                );
                SandboxRegistrationCandidate current = selected.get(normalizedLanguage);
                if (current == null || candidate.isPreferredOver(current)) {
                    selected.put(normalizedLanguage, candidate);
                }
            }
        }
        for (SandboxRegistrationCandidate candidate : selected.values()) {
            sandboxManager.registerSandbox(candidate.language, candidate.sandbox);
        }
    }

    private static String resolveProviderId(ScriptSandbox sandbox, SandboxProviderMetadata metadata) {
        if (metadata != null && metadata.providerId() != null && !metadata.providerId().trim().isEmpty()) {
            return metadata.providerId().trim();
        }
        return sandbox.getClass().getName();
    }

    private static String normalizeLanguage(String language) {
        if (language == null || language.trim().isEmpty()) {
            return null;
        }
        return language.trim().toLowerCase();
    }


    private static boolean hasCapabilityConstraints(Set<SandboxHostAccessMode> allowedSandboxHostAccessModes,
                                                    Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes,
                                                    Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags,
                                                    Set<SandboxResultTransportMode> allowedSandboxResultTransportModes,
                                                    Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes,
                                                    Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles,
                                                    Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities,
                                                    Set<String> requiredSandboxTransportSerializerContractIds,
                                                    boolean requireEngineCacheCapableSandbox,
                                                    boolean requireExternalProcessCompatibleSandbox,
                                                    boolean requireJsonSafeExternalResult,
                                                    boolean requireJsonSafeExternalMetadata) {
        return !allowedSandboxHostAccessModes.isEmpty()
                || !allowedSandboxHostRestrictionModes.isEmpty()
                || !requiredSandboxHostRestrictionFlags.isEmpty()
                || !allowedSandboxResultTransportModes.isEmpty()
                || !allowedSandboxTransportSerializerModes.isEmpty()
                || !allowedSandboxTransportPayloadProfiles.isEmpty()
                || !requiredSandboxTransportProtocolCapabilities.isEmpty()
                || !requiredSandboxTransportSerializerContractIds.isEmpty()
                || requireEngineCacheCapableSandbox
                || requireExternalProcessCompatibleSandbox
                || requireJsonSafeExternalResult
                || requireJsonSafeExternalMetadata;
    }

    private static final class SandboxRegistrationCandidate {
        private final String language;
        private final ScriptSandbox sandbox;
        private final int priority;
        private final String providerId;
        private final String className;

        private SandboxRegistrationCandidate(String language, ScriptSandbox sandbox, int priority,
                                             String providerId, String className) {
            this.language = language;
            this.sandbox = sandbox;
            this.priority = priority;
            this.providerId = providerId;
            this.className = className;
        }

        private boolean isPreferredOver(SandboxRegistrationCandidate current) {
            if (priority != current.priority) {
                return priority > current.priority;
            }
            int providerIdCompare = providerId.compareTo(current.providerId);
            if (providerIdCompare != 0) {
                return providerIdCompare < 0;
            }
            return className.compareTo(current.className) < 0;
        }
    }

    private static Set<SandboxHostAccessMode> normalizeHostAccessModes(Set<SandboxHostAccessMode> hostAccessModes) {
        if (hostAccessModes == null || hostAccessModes.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return new LinkedHashSet<SandboxHostAccessMode>(hostAccessModes);
    }

    private static Set<SandboxHostRestrictionMode> normalizeHostRestrictionModes(Set<SandboxHostRestrictionMode> hostRestrictionModes) {
        if (hostRestrictionModes == null || hostRestrictionModes.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return new LinkedHashSet<SandboxHostRestrictionMode>(hostRestrictionModes);
    }


    private static Set<SandboxHostRestrictionFlag> normalizeHostRestrictionFlags(Set<SandboxHostRestrictionFlag> hostRestrictionFlags) {
        if (hostRestrictionFlags == null || hostRestrictionFlags.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return new LinkedHashSet<SandboxHostRestrictionFlag>(hostRestrictionFlags);
    }

    private static Set<SandboxResultTransportMode> normalizeResultTransportModes(Set<SandboxResultTransportMode> resultTransportModes) {
        if (resultTransportModes == null || resultTransportModes.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return new LinkedHashSet<SandboxResultTransportMode>(resultTransportModes);
    }


    private static Set<SandboxTransportSerializerMode> normalizeTransportSerializerModes(Set<SandboxTransportSerializerMode> serializerModes) {
        if (serializerModes == null || serializerModes.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return new LinkedHashSet<SandboxTransportSerializerMode>(serializerModes);
    }

    private static Set<SandboxTransportPayloadProfile> normalizeTransportPayloadProfiles(Set<SandboxTransportPayloadProfile> payloadProfiles) {
        if (payloadProfiles == null || payloadProfiles.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        return new LinkedHashSet<SandboxTransportPayloadProfile>(payloadProfiles);
    }


    private static Set<SandboxTransportProtocolCapability> normalizeTransportProtocolCapabilities(Set<SandboxTransportProtocolCapability> protocolCapabilities,
                                                                                                  boolean externalProcess) {
        LinkedHashSet<SandboxTransportProtocolCapability> normalized = new LinkedHashSet<SandboxTransportProtocolCapability>();
        if (protocolCapabilities != null) {
            normalized.addAll(protocolCapabilities);
        }
        if (externalProcess) {
            normalized.add(SandboxTransportProtocolCapability.JSON_FRAMED);
        }
        return normalized;
    }

    private static Set<String> normalizeTransportSerializerContractIds(Set<String> contractIds) {
        if (contractIds == null || contractIds.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        for (String contractId : contractIds) {
            if (contractId != null && !contractId.trim().isEmpty()) {
                normalized.add(contractId.trim());
            }
        }
        return normalized;
    }

    private static boolean matchesCapabilities(SandboxProviderMetadata metadata,
                                               Set<String> allowedSandboxLanguages,
                                               Set<SandboxHostAccessMode> allowedSandboxHostAccessModes,
                                               Set<SandboxHostRestrictionMode> allowedSandboxHostRestrictionModes,
                                               Set<SandboxHostRestrictionFlag> requiredSandboxHostRestrictionFlags,
                                               Set<SandboxResultTransportMode> allowedSandboxResultTransportModes,
                                               Set<SandboxTransportSerializerMode> allowedSandboxTransportSerializerModes,
                                               Set<SandboxTransportPayloadProfile> allowedSandboxTransportPayloadProfiles,
                                               Set<SandboxTransportProtocolCapability> requiredSandboxTransportProtocolCapabilities,
                                               Set<String> requiredSandboxTransportSerializerContractIds,
                                               boolean requireEngineCacheCapableSandbox,
                                               boolean requireExternalProcessCompatibleSandbox,
                                               boolean requireJsonSafeExternalResult,
                                               boolean requireJsonSafeExternalMetadata) {
        if (!allowedSandboxLanguages.isEmpty() && !supportsAnyLanguage(metadata.languages(), allowedSandboxLanguages)) {
            return false;
        }
        if (!allowedSandboxHostAccessModes.isEmpty() && !allowedSandboxHostAccessModes.contains(metadata.hostAccessMode())) {
            return false;
        }
        if (!allowedSandboxHostRestrictionModes.isEmpty() && !allowedSandboxHostRestrictionModes.contains(metadata.hostRestrictionMode())) {
            return false;
        }
        if (!requiredSandboxHostRestrictionFlags.isEmpty() && !containsAllHostRestrictionFlags(metadata.hostRestrictionFlags(), requiredSandboxHostRestrictionFlags)) {
            return false;
        }
        if (!allowedSandboxResultTransportModes.isEmpty() && !allowedSandboxResultTransportModes.contains(metadata.resultTransportMode())) {
            return false;
        }
        if (!allowedSandboxTransportSerializerModes.isEmpty() && !allowedSandboxTransportSerializerModes.contains(metadata.transportSerializerMode())) {
            return false;
        }
        if (!allowedSandboxTransportPayloadProfiles.isEmpty() && !allowedSandboxTransportPayloadProfiles.contains(metadata.transportPayloadProfile())) {
            return false;
        }
        if (!containsAllTransportProtocolCapabilities(metadata.transportProtocolCapabilities(), requiredSandboxTransportProtocolCapabilities)) {
            return false;
        }
        if (!containsAllTransportSerializerContractIds(metadata.transportSerializerContractIds(), requiredSandboxTransportSerializerContractIds)) {
            return false;
        }
        if (requireEngineCacheCapableSandbox && !metadata.supportsEngineCache()) {
            return false;
        }
        if (requireExternalProcessCompatibleSandbox && !metadata.externalProcessCompatible()) {
            return false;
        }
        if (requireJsonSafeExternalResult && !supportsJsonSafeResult(metadata.resultTransportMode())) {
            return false;
        }
        if (requireJsonSafeExternalMetadata && !supportsJsonSafeMetadata(metadata.resultTransportMode())) {
            return false;
        }
        return true;
    }

    private static boolean supportsJsonSafeResult(SandboxResultTransportMode resultTransportMode) {
        return resultTransportMode == SandboxResultTransportMode.JSON_SAFE_RESULT
                || resultTransportMode == SandboxResultTransportMode.JSON_SAFE_RESULT_AND_METADATA;
    }

    private static boolean supportsJsonSafeMetadata(SandboxResultTransportMode resultTransportMode) {
        return resultTransportMode == SandboxResultTransportMode.JSON_SAFE_RESULT_AND_METADATA;
    }


    private static boolean containsAllHostRestrictionFlags(SandboxHostRestrictionFlag[] providerFlags,
                                                           Set<SandboxHostRestrictionFlag> requiredFlags) {
        if (requiredFlags.isEmpty()) {
            return true;
        }
        if (providerFlags == null || providerFlags.length == 0) {
            return false;
        }
        java.util.Set<SandboxHostRestrictionFlag> provided = new java.util.LinkedHashSet<SandboxHostRestrictionFlag>();
        java.util.Collections.addAll(provided, providerFlags);
        return provided.containsAll(requiredFlags);
    }


    private static boolean containsAllTransportProtocolCapabilities(SandboxTransportProtocolCapability[] providerCapabilities,
                                                                    Set<SandboxTransportProtocolCapability> requiredCapabilities) {
        if (requiredCapabilities.isEmpty()) {
            return true;
        }
        if (providerCapabilities == null || providerCapabilities.length == 0) {
            return false;
        }
        java.util.Set<SandboxTransportProtocolCapability> provided = new java.util.LinkedHashSet<SandboxTransportProtocolCapability>();
        java.util.Collections.addAll(provided, providerCapabilities);
        return provided.containsAll(requiredCapabilities);
    }

    private static boolean containsAllTransportSerializerContractIds(String[] providerContractIds,
                                                                     Set<String> requiredContractIds) {
        if (requiredContractIds.isEmpty()) {
            return true;
        }
        if (providerContractIds == null || providerContractIds.length == 0) {
            return false;
        }
        java.util.Set<String> provided = new java.util.LinkedHashSet<String>();
        java.util.Collections.addAll(provided, providerContractIds);
        return provided.containsAll(requiredContractIds);
    }

    private static Set<String> normalizeSandboxLanguages(Set<String> languages) {
        if (languages == null || languages.isEmpty()) {
            return java.util.Collections.emptySet();
        }
        Set<String> normalized = new LinkedHashSet<String>();
        for (String language : languages) {
            if (language != null && !language.trim().isEmpty()) {
                normalized.add(language.trim().toLowerCase());
            }
        }
        return normalized;
    }

    private static boolean supportsAnyLanguage(String[] providerLanguages, Set<String> allowedSandboxLanguages) {
        if (providerLanguages == null || providerLanguages.length == 0) {
            return false;
        }
        for (String language : providerLanguages) {
            if (language != null && allowedSandboxLanguages.contains(language.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean supportsAnyLanguage(List<String> providerLanguages, Set<String> allowedSandboxLanguages) {
        if (providerLanguages == null || providerLanguages.isEmpty()) {
            return false;
        }
        for (String language : providerLanguages) {
            if (language != null && allowedSandboxLanguages.contains(language.trim().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 闂侇偅淇虹换鍐矗瀹ュ懐娈搁柛鎺撶☉缂?Module 閻庡湱鍋樼欢?
     */
    private static DefaultModuleRegistry createDefaultModuleRegistryInstance(EngineConfig finalConfig) {
        DefaultModuleRegistry moduleRegistry = new DefaultModuleRegistry();
        // 闁煎浜滄慨鈺佲枖閵娿儱鏂€閻庡湱鍋熼獮鍥ㄧ?ScriptModule 闁规亽鍎辫ぐ娑㈡儍閸曨叀顫?
        ServiceLoader<ScriptModule> importScriptModules = ServiceLoader.load(ScriptModule.class);
        for (ScriptModule scriptModule : importScriptModules) {
            boolean moduleAllowed = LingoNexusUtil.isModuleAllowed(scriptModule, finalConfig.getExcludeScriptModules(), finalConfig.getAllowedScriptModules());
            if (moduleAllowed) {
                moduleRegistry.registerModule(scriptModule);
            }
        }
        return moduleRegistry;
    }

    /**
     * 闁告帗绋戠紓鎾搭渶濡鍚囬悗鐟邦槸閸欏繒绮甸弽顐ｆ闁告帗顨夐妴鍐晬閸繂绻侀柛鎺曟硾閸炲绱旈鍓ф憸闁伙絻鍎荤槐?
     * <p>缂佹稒鐗滈弳鎰板箥瑜戦、鎴炪亜閸濆嫮纰嶉柨?/p>
     * <ol>
     *     <li>ScriptSizePolicy - 闁煎瓨纰嶅﹢鐗堝緞瑜嶉惃顒勬⒔閹邦剙鐓?/li>
     * </ol>
     *
     */
    private static void addDefaultSecurityPolicies(List<SecurityPolicy> securityPolicies) {
        StringBuilder stringBuilder = new StringBuilder();
        for (BuiltInPolicy builtInPolicy : BuiltInPolicy.values()) {
            securityPolicies.add(builtInPolicy.getSecurityPolicy());
            stringBuilder.append(", ").append(builtInPolicy.getPolicyName());
        }
        logger.debug("Add default security policies:" + stringBuilder.substring(1));
    }

}

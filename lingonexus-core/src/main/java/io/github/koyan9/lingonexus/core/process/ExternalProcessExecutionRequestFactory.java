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

import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.MetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.exception.ExternalProcessCompatibilityException;
import io.github.koyan9.lingonexus.api.module.ModuleRegistry;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicySupport;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;
import io.github.koyan9.lingonexus.core.security.BuiltInPolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Builds external-process execution requests and caches stable extension descriptors.
 */
public class ExternalProcessExecutionRequestFactory {

    private static final String STAGE_REQUEST_VALIDATION = "request_validation";
    private static final String COMPONENT_EXTERNAL_PROCESS_COMPATIBILITY = "external-process-compatibility";
    private static final String REASON_REQUEST_PAYLOAD_NOT_JSON_SAFE = "request_payload_not_json_safe";
    private static final String REASON_SECURITY_POLICY_NOT_EXTERNAL_PROCESS_COMPATIBLE =
            "security_policy_not_external_process_compatible";
    private static final String REASON_SECURITY_POLICY_DESCRIPTOR_NOT_JSON_SAFE =
            "security_policy_descriptor_not_json_safe";
    private static final String REASON_SCRIPT_MODULE_NOT_EXTERNAL_PROCESS_COMPATIBLE =
            "script_module_not_external_process_compatible";
    private static final String REASON_SCRIPT_MODULE_DESCRIPTOR_NOT_JSON_SAFE =
            "script_module_descriptor_not_json_safe";

    private final EngineConfig config;
    private final ModuleRegistry moduleRegistry;
    private final List<SecurityPolicy> securityPolicies;
    private final Set<String> builtInPolicyClasses;
    private volatile boolean customSecurityPolicyDescriptorsInitialized = false;
    private volatile List<ExternalProcessExtensionDescriptor> cachedCustomSecurityPolicyDescriptors = Collections.emptyList();
    private volatile long cachedDynamicModuleDescriptorVersion = Long.MIN_VALUE;
    private volatile List<ExternalProcessExtensionDescriptor> cachedDynamicModuleDescriptors = Collections.emptyList();

    public ExternalProcessExecutionRequestFactory(EngineConfig config,
                                                  ModuleRegistry moduleRegistry,
                                                  List<SecurityPolicy> securityPolicies) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.moduleRegistry = moduleRegistry;
        this.securityPolicies = securityPolicies != null
                ? Collections.unmodifiableList(new ArrayList<SecurityPolicy>(securityPolicies))
                : Collections.<SecurityPolicy>emptyList();
        this.builtInPolicyClasses = resolveBuiltInPolicyClasses();
    }

    public ExternalProcessExecutionRequest createRequest(String script, String language,
                                                         ScriptContext context,
                                                         Map<String, Object> executionVariables) {
        List<ExternalProcessExtensionDescriptor> customSecurityPolicyDescriptors = getCustomSecurityPolicyDescriptors();
        List<ExternalProcessExtensionDescriptor> dynamicModuleDescriptors = getDynamicModuleDescriptors();
        Map<String, Object> requestMetadata = createRequestMetadata(context);
        Map<String, Object> normalizedExecutionVariables = normalizeRequestPayload(executionVariables, "$.variables");
        return new ExternalProcessExecutionRequest(
                script,
                language,
                config.getDefaultLanguage(),
                config.getCacheConfig().isEnabled(),
                config.getCacheConfig().getMaxSize(),
                config.getCacheConfig().getExpireAfterWriteMs(),
                config.getCacheConfig().getExpireAfterAccessMs(),
                config.getSandboxConfig().getExternalProcessExecutorCacheMaxSize(),
                config.getSandboxConfig().getExternalProcessExecutorCacheIdleTtlMs(),
                config.getSandboxConfig().isEnabled(),
                config.getSandboxConfig().getMaxScriptSize(),
                config.getSandboxConfig().isEnableEngineCache(),
                config.getSandboxConfig().getClassWhitelist(),
                config.getSandboxConfig().getClassBlacklist(),
                config.getExcludeScriptModules() != null ? config.getExcludeScriptModules() : Collections.<String>emptySet(),
                config.getAllowedScriptModules() != null ? config.getAllowedScriptModules() : Collections.<String>emptySet(),
                config.getAllowedSandboxImplementations() != null ? config.getAllowedSandboxImplementations() : Collections.<String>emptySet(),
                Collections.singleton(language.toLowerCase()),
                config.getAllowedSandboxHostAccessModes() != null ? config.getAllowedSandboxHostAccessModes() : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode>emptySet(),
                config.getAllowedSandboxHostRestrictionModes() != null ? config.getAllowedSandboxHostRestrictionModes() : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode>emptySet(),
                config.getRequiredSandboxHostRestrictionFlags() != null ? config.getRequiredSandboxHostRestrictionFlags() : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag>emptySet(),
                config.getAllowedSandboxResultTransportModes() != null ? config.getAllowedSandboxResultTransportModes() : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode>emptySet(),
                config.getAllowedSandboxTransportSerializerModes() != null ? config.getAllowedSandboxTransportSerializerModes() : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode>emptySet(),
                config.getAllowedSandboxTransportPayloadProfiles() != null ? config.getAllowedSandboxTransportPayloadProfiles() : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile>emptySet(),
                config.getRequiredSandboxTransportProtocolCapabilities() != null ? config.getRequiredSandboxTransportProtocolCapabilities() : Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability>emptySet(),
                config.getRequiredSandboxTransportSerializerContractIds() != null ? config.getRequiredSandboxTransportSerializerContractIds() : Collections.<String>emptySet(),
                config.isRequireEngineCacheCapableSandbox(),
                true,
                true,
                config.isRequireJsonSafeExternalMetadata(),
                config.getResultMetadataProfile(),
                config.getResultMetadataCategories() != null ? config.getResultMetadataCategories() : Collections.<ResultMetadataCategory>emptySet(),
                customSecurityPolicyDescriptors,
                dynamicModuleDescriptors,
                normalizedExecutionVariables,
                requestMetadata
        );
    }

    private Map<String, Object> createRequestMetadata(ScriptContext context) {
        if (context == null || context.getMetadata() == null || context.getMetadata().isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Object> metadata = new HashMap<String, Object>(context.getMetadata());
        if (!metadata.containsKey(MetadataKeys.RESULT_METADATA_CATEGORIES)) {
            Object policyOverride = metadata.get(MetadataKeys.RESULT_METADATA_POLICY);
            if (policyOverride != null) {
                java.util.Set<ResultMetadataCategory> resolved = ResultMetadataPolicySupport.resolveNamedPolicy(
                        String.valueOf(policyOverride),
                        config.getResultMetadataPolicyTemplates()
                );
                if (resolved != null) {
                    List<String> categories = new ArrayList<String>(resolved.size());
                    for (ResultMetadataCategory category : resolved) {
                        categories.add(category.name());
                    }
                    metadata.put(MetadataKeys.RESULT_METADATA_CATEGORIES, categories);
                }
            }
        }
        return normalizeRequestPayload(metadata, "$.metadata");
    }

    private Map<String, Object> normalizeRequestPayload(Map<String, Object> payload, String path) {
        if (payload == null || payload.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            return JsonSafeValueNormalizer.normalizeMap(payload, path, false);
        } catch (IllegalArgumentException e) {
            throw compatibilityFailure(
                    "External process mode requires JSON-safe request payload: " + e.getMessage(),
                    e,
                    REASON_REQUEST_PAYLOAD_NOT_JSON_SAFE
            );
        }
    }

    List<ExternalProcessExtensionDescriptor> getCustomSecurityPolicyDescriptors() {
        if (customSecurityPolicyDescriptorsInitialized) {
            return cachedCustomSecurityPolicyDescriptors;
        }

        synchronized (this) {
            if (!customSecurityPolicyDescriptorsInitialized) {
                cachedCustomSecurityPolicyDescriptors = buildCustomSecurityPolicyDescriptors();
                customSecurityPolicyDescriptorsInitialized = true;
            }
            return cachedCustomSecurityPolicyDescriptors;
        }
    }

    List<ExternalProcessExtensionDescriptor> getDynamicModuleDescriptors() {
        if (moduleRegistry == null) {
            return Collections.emptyList();
        }

        long version = moduleRegistry.getVersion();
        List<ExternalProcessExtensionDescriptor> snapshot = cachedDynamicModuleDescriptors;
        if (version == cachedDynamicModuleDescriptorVersion) {
            return snapshot;
        }

        synchronized (this) {
            if (version != cachedDynamicModuleDescriptorVersion) {
                cachedDynamicModuleDescriptors = buildDynamicModuleDescriptors(moduleRegistry.getAllModules());
                cachedDynamicModuleDescriptorVersion = version;
            }
            return cachedDynamicModuleDescriptors;
        }
    }

    private List<ExternalProcessExtensionDescriptor> buildCustomSecurityPolicyDescriptors() {
        if (securityPolicies.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExternalProcessExtensionDescriptor> result = new ArrayList<ExternalProcessExtensionDescriptor>();
        for (SecurityPolicy securityPolicy : securityPolicies) {
            if (securityPolicy == null || builtInPolicyClasses.contains(securityPolicy.getClass().getName())) {
                continue;
            }
            ExternalProcessExtensionDescriptor descriptor = createSecurityPolicyDescriptor(securityPolicy);
            result.add(descriptor);
        }
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(result);
    }

    private List<ExternalProcessExtensionDescriptor> buildDynamicModuleDescriptors(List<ScriptModule> modules) {
        if (modules == null || modules.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExternalProcessExtensionDescriptor> result = new ArrayList<ExternalProcessExtensionDescriptor>();
        for (ScriptModule module : modules) {
            if (module == null) {
                continue;
            }
            ExternalProcessExtensionDescriptor descriptor = createDynamicModuleDescriptor(module);
            result.add(descriptor);
        }
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(result);
    }

    private Set<String> resolveBuiltInPolicyClasses() {
        Set<String> result = new HashSet<String>();
        for (BuiltInPolicy builtInPolicy : BuiltInPolicy.values()) {
            result.add(builtInPolicy.getSecurityPolicy().getClass().getName());
        }
        return Collections.unmodifiableSet(result);
    }

    private ExternalProcessExtensionDescriptor createSecurityPolicyDescriptor(SecurityPolicy securityPolicy) {
        try {
            ExternalProcessExtensionDescriptor descriptor = ExternalProcessExtensionSupport.createDescriptor(securityPolicy);
            ExternalProcessExtensionSupport.validateCompatibility(
                    securityPolicy.getClass(),
                    SecurityPolicy.class,
                    "SecurityPolicy",
                    descriptor.getDescriptor() != null && !descriptor.getDescriptor().isEmpty()
            );
            return descriptor;
        } catch (IllegalArgumentException e) {
            throw compatibilityFailure(
                    "EXTERNAL_PROCESS requires a JSON-safe SecurityPolicy descriptor: "
                            + securityPolicy.getClass().getName() + ". " + e.getMessage(),
                    e,
                    REASON_SECURITY_POLICY_DESCRIPTOR_NOT_JSON_SAFE
            );
        } catch (IllegalStateException e) {
            throw compatibilityFailure(
                    "EXTERNAL_PROCESS does not support SecurityPolicy "
                            + securityPolicy.getClass().getName() + ": " + e.getMessage(),
                    e,
                    REASON_SECURITY_POLICY_NOT_EXTERNAL_PROCESS_COMPATIBLE
            );
        }
    }

    private ExternalProcessExtensionDescriptor createDynamicModuleDescriptor(ScriptModule module) {
        try {
            ExternalProcessExtensionDescriptor descriptor = ExternalProcessExtensionSupport.createDescriptor(module);
            ExternalProcessExtensionSupport.validateCompatibility(
                    module.getClass(),
                    ScriptModule.class,
                    "ScriptModule",
                    descriptor.getDescriptor() != null && !descriptor.getDescriptor().isEmpty()
            );
            return descriptor;
        } catch (IllegalArgumentException e) {
            throw compatibilityFailure(
                    "EXTERNAL_PROCESS requires a JSON-safe ScriptModule descriptor: "
                            + module.getClass().getName() + ". " + e.getMessage(),
                    e,
                    REASON_SCRIPT_MODULE_DESCRIPTOR_NOT_JSON_SAFE
            );
        } catch (IllegalStateException e) {
            throw compatibilityFailure(
                    "EXTERNAL_PROCESS does not support ScriptModule "
                            + module.getClass().getName() + ": " + e.getMessage(),
                    e,
                    REASON_SCRIPT_MODULE_NOT_EXTERNAL_PROCESS_COMPATIBLE
            );
        }
    }

    private ExternalProcessCompatibilityException compatibilityFailure(String message,
                                                                       Throwable cause,
                                                                       String reason) {
        return new ExternalProcessCompatibilityException(
                message,
                cause,
                STAGE_REQUEST_VALIDATION,
                COMPONENT_EXTERNAL_PROCESS_COMPATIBILITY,
                reason
        );
    }
}

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
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.exception.ExternalProcessCompatibilityException;
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyTemplate;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.process.ExternalProcessDescriptorConsumer;
import io.github.koyan9.lingonexus.api.process.ExternalProcessDescriptorProvider;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;
import io.github.koyan9.lingonexus.api.security.ValidationResult;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import io.github.koyan9.lingonexus.core.security.BuiltInPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("External Process Execution Request Factory Feature Tests")
class ExternalProcessExecutionRequestFactoryFeatureTest {

    @Test
    @DisplayName("Should reuse custom security policy descriptors and filter built-ins")
    void shouldReuseCustomSecurityPolicyDescriptorsAndFilterBuiltIns() {
        ExternalProcessExecutionRequestFactory factory = new ExternalProcessExecutionRequestFactory(
                createEngineConfig(),
                new DefaultModuleRegistry(),
                Arrays.<SecurityPolicy>asList(
                        BuiltInPolicy.SCRIPT_SIZE_POLICY.getSecurityPolicy(),
                        new DescriptorSecurityPolicy("blocked")
                )
        );

        List<ExternalProcessExtensionDescriptor> first = factory.getCustomSecurityPolicyDescriptors();
        List<ExternalProcessExtensionDescriptor> second = factory.getCustomSecurityPolicyDescriptors();

        assertSame(first, second);
        assertEquals(1, first.size());
        assertEquals(DescriptorSecurityPolicy.class.getName(), first.get(0).getClassName());
    }

    @Test
    @DisplayName("Should reuse dynamic module descriptors until registry changes")
    void shouldReuseDynamicModuleDescriptorsUntilRegistryChanges() {
        DefaultModuleRegistry moduleRegistry = new DefaultModuleRegistry();
        moduleRegistry.registerModule(new DescriptorModule("alpha", 2));

        ExternalProcessExecutionRequestFactory factory = new ExternalProcessExecutionRequestFactory(
                createEngineConfig(),
                moduleRegistry,
                Collections.<SecurityPolicy>emptyList()
        );

        List<ExternalProcessExtensionDescriptor> first = factory.getDynamicModuleDescriptors();
        List<ExternalProcessExtensionDescriptor> second = factory.getDynamicModuleDescriptors();

        assertSame(first, second);
        assertEquals(1, first.size());

        moduleRegistry.registerModule(new DescriptorModule("beta", 3));
        List<ExternalProcessExtensionDescriptor> third = factory.getDynamicModuleDescriptors();

        assertEquals(2, third.size());
        assertEquals(2, third.get(0).getDescriptor().size());
    }

    @Test
    @DisplayName("Should populate external request with cached extension descriptors")
    void shouldPopulateExternalRequestWithCachedExtensionDescriptors() {
        DefaultModuleRegistry moduleRegistry = new DefaultModuleRegistry();
        moduleRegistry.registerModule(new DescriptorModule("alpha", 2));
        ExternalProcessExecutionRequestFactory factory = new ExternalProcessExecutionRequestFactory(
                createEngineConfig(),
                moduleRegistry,
                Collections.<SecurityPolicy>singletonList(new DescriptorSecurityPolicy("blocked"))
        );

        ExternalProcessExecutionRequest request = factory.createRequest(
                "return value",
                "groovy",
                ScriptContext.of(Collections.<String, Object>singletonMap("value", 42)),
                Collections.<String, Object>singletonMap("value", 42)
        );

        assertEquals(1, request.getCustomSecurityPolicies().size());
        assertEquals(1, request.getDynamicModules().size());
        assertEquals("groovy", request.getAllowedSandboxLanguages().iterator().next());
    }


    @Test
    @DisplayName("Should expand named metadata policy without mutating request metadata")
    void shouldExpandNamedMetadataPolicyWithoutMutatingRequestMetadata() {
        EngineConfig config = EngineConfig.builder()
                .defaultLanguage("groovy")
                .cacheConfig(CacheConfig.builder().enabled(true).build())
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(1000L)
                        .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                        .build())
                .resultMetadataPolicyTemplate(ResultMetadataPolicyTemplate.builder()
                        .name("module-only")
                        .category(ResultMetadataCategory.MODULE)
                        .build())
                .build();
        ExternalProcessExecutionRequestFactory factory = new ExternalProcessExecutionRequestFactory(
                config,
                new DefaultModuleRegistry(),
                Collections.<SecurityPolicy>emptyList()
        );
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("trace", "abc");
        metadata.put("resultMetadataPolicy", "module-only");

        ExternalProcessExecutionRequest request = factory.createRequest(
                "return 1",
                "groovy",
                ScriptContext.of(Collections.<String, Object>emptyMap(), metadata),
                Collections.<String, Object>emptyMap()
        );

        assertEquals("module-only", metadata.get("resultMetadataPolicy"));
        assertTrue(!metadata.containsKey("resultMetadataCategories"));
        assertEquals(Collections.<Object>singletonList("MODULE"), request.getMetadata().get("resultMetadataCategories"));
        assertEquals("abc", request.getMetadata().get("trace"));
    }

    @Test
    @DisplayName("Should normalize request variables and metadata into JSON-safe structures")
    void shouldNormalizeRequestVariablesAndMetadataIntoJsonSafeStructures() {
        ExternalProcessExecutionRequestFactory factory = new ExternalProcessExecutionRequestFactory(
                createEngineConfig(),
                new DefaultModuleRegistry(),
                Collections.<SecurityPolicy>emptyList()
        );
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("status", SampleState.ACTIVE);
        variables.put("numbers", new int[]{4, 2});

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("state", SampleState.BLOCKED);
        metadata.put("trace", new String[]{"a", "b"});

        ExternalProcessExecutionRequest request = factory.createRequest(
                "return numbers[0]",
                "groovy",
                ScriptContext.of(variables, metadata),
                variables
        );

        assertEquals("ACTIVE", request.getVariables().get("status"));
        assertTrue(request.getVariables().get("numbers") instanceof List);
        assertEquals(Arrays.<Object>asList(4, 2), request.getVariables().get("numbers"));
        assertEquals("BLOCKED", request.getMetadata().get("state"));
        assertEquals(Arrays.<Object>asList("a", "b"), request.getMetadata().get("trace"));
    }

    @Test
    @DisplayName("Should classify incompatible security policy extensions before dispatch")
    void shouldClassifyIncompatibleSecurityPolicyExtensionsBeforeDispatch() {
        ExternalProcessExecutionRequestFactory factory = new ExternalProcessExecutionRequestFactory(
                createEngineConfig(),
                new DefaultModuleRegistry(),
                Collections.<SecurityPolicy>singletonList(new IncompatibleDescriptorSecurityPolicy("blocked"))
        );

        ExternalProcessCompatibilityException exception = assertThrows(
                ExternalProcessCompatibilityException.class,
                () -> factory.createRequest(
                        "return 1",
                        "groovy",
                        ScriptContext.of(Collections.<String, Object>emptyMap()),
                        Collections.<String, Object>emptyMap()
                )
        );

        assertEquals("request_validation", exception.getStage());
        assertEquals("external-process-compatibility", exception.getComponent());
        assertEquals("security_policy_not_external_process_compatible", exception.getReason());
        assertTrue(exception.getMessage().contains(IncompatibleDescriptorSecurityPolicy.class.getName()));
    }

    @Test
    @DisplayName("Should classify incompatible script modules before dispatch")
    void shouldClassifyIncompatibleScriptModulesBeforeDispatch() {
        DefaultModuleRegistry moduleRegistry = new DefaultModuleRegistry();
        moduleRegistry.registerModule(new IncompatibleDescriptorModule("brokenModule"));

        ExternalProcessExecutionRequestFactory factory = new ExternalProcessExecutionRequestFactory(
                createEngineConfig(),
                moduleRegistry,
                Collections.<SecurityPolicy>emptyList()
        );

        ExternalProcessCompatibilityException exception = assertThrows(
                ExternalProcessCompatibilityException.class,
                () -> factory.createRequest(
                        "return 1",
                        "groovy",
                        ScriptContext.of(Collections.<String, Object>emptyMap()),
                        Collections.<String, Object>emptyMap()
                )
        );

        assertEquals("request_validation", exception.getStage());
        assertEquals("external-process-compatibility", exception.getComponent());
        assertEquals("script_module_not_external_process_compatible", exception.getReason());
        assertTrue(exception.getMessage().contains(IncompatibleDescriptorModule.class.getName()));
    }

    private EngineConfig createEngineConfig() {
        return EngineConfig.builder()
                .defaultLanguage("groovy")
                .cacheConfig(CacheConfig.builder().enabled(true).build())
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(1000L)
                        .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                        .build())
                .build();
    }

    public static class DescriptorSecurityPolicy implements SecurityPolicy,
            ExternalProcessDescriptorProvider, ExternalProcessDescriptorConsumer {

        private String blockedWord;

        public DescriptorSecurityPolicy() {
            this("default");
        }

        public DescriptorSecurityPolicy(String blockedWord) {
            this.blockedWord = blockedWord;
        }

        @Override
        public ValidationResult validate(String script, String language, ScriptContext context,
                                         io.github.koyan9.lingonexus.api.config.SandboxConfig config) {
            return ValidationResult.success();
        }

        @Override
        public String getName() {
            return "DescriptorSecurityPolicy";
        }

        @Override
        public Map<String, Object> toExternalProcessDescriptor() {
            return Collections.<String, Object>singletonMap("blockedWord", blockedWord);
        }

        @Override
        public void loadExternalProcessDescriptor(Map<String, Object> descriptor) {
            Object value = descriptor.get("blockedWord");
            if (value != null) {
                blockedWord = String.valueOf(value);
            }
        }
    }

    public static class DescriptorModule implements ScriptModule,
            ExternalProcessDescriptorProvider, ExternalProcessDescriptorConsumer {

        private String name;
        private int multiplier;

        public DescriptorModule() {
            this("default", 1);
        }

        public DescriptorModule(String name, int multiplier) {
            this.name = name;
            this.multiplier = multiplier;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<String, Object> getFunctions() {
            Map<String, Object> functions = new HashMap<String, Object>();
            functions.put("multiply", null);
            return functions;
        }

        @Override
        public boolean hasFunction(String functionName) {
            return "multiply".equals(functionName);
        }

        @Override
        public Map<String, Object> toExternalProcessDescriptor() {
            Map<String, Object> descriptor = new HashMap<String, Object>();
            descriptor.put("name", name);
            descriptor.put("multiplier", multiplier);
            return descriptor;
        }

        @Override
        public void loadExternalProcessDescriptor(Map<String, Object> descriptor) {
            Object moduleName = descriptor.get("name");
            Object moduleMultiplier = descriptor.get("multiplier");
            if (moduleName != null) {
                name = String.valueOf(moduleName);
            }
            if (moduleMultiplier instanceof Number) {
                multiplier = ((Number) moduleMultiplier).intValue();
            }
        }
    }

    public static class IncompatibleDescriptorSecurityPolicy implements SecurityPolicy,
            ExternalProcessDescriptorProvider {

        private final String blockedWord;

        public IncompatibleDescriptorSecurityPolicy(String blockedWord) {
            this.blockedWord = blockedWord;
        }

        @Override
        public ValidationResult validate(String script, String language, ScriptContext context,
                                         io.github.koyan9.lingonexus.api.config.SandboxConfig config) {
            return ValidationResult.success();
        }

        @Override
        public String getName() {
            return "IncompatibleDescriptorSecurityPolicy";
        }

        @Override
        public Map<String, Object> toExternalProcessDescriptor() {
            return Collections.<String, Object>singletonMap("blockedWord", blockedWord);
        }
    }

    public static class IncompatibleDescriptorModule implements ScriptModule,
            ExternalProcessDescriptorProvider {

        private final String name;

        public IncompatibleDescriptorModule(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<String, Object> getFunctions() {
            return Collections.<String, Object>singletonMap("echo", null);
        }

        @Override
        public boolean hasFunction(String functionName) {
            return "echo".equals(functionName);
        }

        @Override
        public Map<String, Object> toExternalProcessDescriptor() {
            return Collections.<String, Object>singletonMap("name", name);
        }
    }

    private enum SampleState {
        ACTIVE,
        BLOCKED
    }
}

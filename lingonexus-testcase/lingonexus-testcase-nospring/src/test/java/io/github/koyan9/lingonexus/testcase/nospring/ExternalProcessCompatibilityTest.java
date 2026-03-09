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
package io.github.koyan9.lingonexus.testcase.nospring;

import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.process.ExternalProcessDescriptorConsumer;
import io.github.koyan9.lingonexus.api.process.ExternalProcessDescriptorProvider;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;
import io.github.koyan9.lingonexus.api.security.ValidationResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("External Process Compatibility Tests")
class ExternalProcessCompatibilityTest {

    @Test
    @DisplayName("Should support descriptor-based custom security policy reconstruction")
    void shouldSupportDescriptorBasedCustomSecurityPolicyReconstruction() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .build())
                        .addSecurityPolicy(new DescriptorWordPolicy("custom-block"))
                        .build()
        );

        try {
            ScriptResult result = executor.execute("return 'custom-block'", "groovy", ScriptContext.of(Collections.emptyMap()));
            assertFalse(result.isSuccess());
            assertTrue(result.getErrorMessage().contains("custom-block"));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should support descriptor-based runtime module reconstruction")
    void shouldSupportDescriptorBasedRuntimeModuleReconstruction() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .build())
                        .build()
        );

        try {
            executor.registerModule(new StatefulExternalModule(3));
            ScriptResult result = executor.execute(
                    "return statefulExternal.multiply(value)",
                    "groovy",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", 14))
            );
            assertTrue(result.isSuccess());
            assertEquals(42, ((Number) result.getValue()).intValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should support static factory module reconstruction without no-arg constructor")
    void shouldSupportStaticFactoryModuleReconstructionWithoutNoArgConstructor() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .build())
                        .build()
        );

        try {
            executor.registerModule(FactoryBackedModule.of(5));
            ScriptResult result = executor.execute(
                    "return factoryModule.multiply(value)",
                    "groovy",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", 9))
            );
            assertTrue(result.isSuccess());
            assertEquals(45, ((Number) result.getValue()).intValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should expose failure stage metadata for incompatible external-process payload")
    void shouldExposeFailureStageMetadataForIncompatibleExternalProcessPayload() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .build())
                        .build()
        );

        try {
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("value", new Object());
            ScriptResult result = executor.execute("return value", "groovy", ScriptContext.of(vars));

            assertFalse(result.isSuccess());
            assertEquals("request_validation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-process-compatibility", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("ExternalProcessCompatibilityException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertEquals("request_payload_not_json_safe", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should expose failure reason for nested incompatible variable payload")
    void shouldExposeFailureReasonForNestedIncompatibleVariablePayload() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .build())
                        .build()
        );

        try {
            Map<String, Object> vars = new HashMap<String, Object>();
            Map<String, Object> nested = new HashMap<String, Object>();
            nested.put("items", Arrays.<Object>asList("ok", new Object()));
            vars.put("payload", nested);

            ScriptResult result = executor.execute("return payload", "groovy", ScriptContext.of(vars));

            assertFalse(result.isSuccess());
            assertEquals("request_validation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-process-compatibility", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("request_payload_not_json_safe", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertTrue(result.getErrorMessage().contains("$.variables.payload.items[1]"));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should expose failure reason for nested incompatible metadata payload")
    void shouldExposeFailureReasonForNestedIncompatibleMetadataPayload() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .build())
                        .build()
        );

        try {
            Map<String, Object> metadata = new HashMap<String, Object>();
            metadata.put("trace", Arrays.<Object>asList("ok", new Object()));

            ScriptResult result = executor.execute(
                    "return 1",
                    "groovy",
                    ScriptContext.of(Collections.<String, Object>emptyMap(), metadata)
            );

            assertFalse(result.isSuccess());
            assertEquals("request_validation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-process-compatibility", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("request_payload_not_json_safe", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertTrue(result.getErrorMessage().contains("$.metadata.trace[1]"));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should expose structured failure metadata for incompatible custom security policy")
    void shouldExposeStructuredFailureMetadataForIncompatibleCustomSecurityPolicy() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .build())
                        .addSecurityPolicy(new IncompatibleDescriptorPolicy("blocked"))
                        .build()
        );

        try {
            ScriptResult result = executor.execute("return 1", "groovy", ScriptContext.of(Collections.emptyMap()));

            assertFalse(result.isSuccess());
            assertEquals("request_validation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-process-compatibility", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("security_policy_not_external_process_compatible", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("ExternalProcessCompatibilityException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertTrue(result.getErrorMessage().contains(IncompatibleDescriptorPolicy.class.getName()));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should stringify unsupported response leaf values instead of failing external-process execution")
    void shouldStringifyUnsupportedResponseLeafValuesInsteadOfFailingExternalProcessExecution() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage("response-fallback-test")
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .build())
                        .build()
        );

        try {
            ScriptResult result = executor.execute("non-json-leaf", "response-fallback-test", ScriptContext.of(Collections.emptyMap()));

            assertTrue(result.isSuccess());
            @SuppressWarnings("unchecked")
            Map<String, Object> value = (Map<String, Object>) result.getValue();
            assertEquals("ok", value.get("status"));
            assertTrue(String.valueOf(value.get("converted")).startsWith("java.lang.Object@"));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should expose structured failure metadata when response payload remains transport-incompatible")
    void shouldExposeStructuredFailureMetadataWhenResponsePayloadRemainsTransportIncompatible() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage("response-fallback-test")
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .build())
                        .build()
        );

        try {
            ScriptResult result = executor.execute("bad-key", "response-fallback-test", ScriptContext.of(Collections.emptyMap()));

            assertFalse(result.isSuccess());
            assertEquals("worker_execution", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("response_payload_not_json_safe", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("ExternalProcessCompatibilityException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertTrue(result.getErrorMessage().contains("response payload compatible with JSON transport"));
        } finally {
            executor.close();
        }
    }


    public static class DescriptorWordPolicy implements SecurityPolicy,
            ExternalProcessDescriptorProvider, ExternalProcessDescriptorConsumer {

        private String blockedWord = "default-block";

        public DescriptorWordPolicy() {
        }

        public DescriptorWordPolicy(String blockedWord) {
            this.blockedWord = blockedWord;
        }

        @Override
        public ValidationResult validate(String script, String language, ScriptContext context,
                                         io.github.koyan9.lingonexus.api.config.SandboxConfig config) {
            if (script != null && script.contains(blockedWord)) {
                return ValidationResult.failure("blocked keyword detected: " + blockedWord);
            }
            return ValidationResult.success();
        }

        @Override
        public String getName() {
            return "DescriptorWordPolicy";
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

    public static class IncompatibleDescriptorPolicy implements SecurityPolicy,
            ExternalProcessDescriptorProvider {

        private final String blockedWord;

        public IncompatibleDescriptorPolicy(String blockedWord) {
            this.blockedWord = blockedWord;
        }

        @Override
        public ValidationResult validate(String script, String language, ScriptContext context,
                                         io.github.koyan9.lingonexus.api.config.SandboxConfig config) {
            return ValidationResult.success();
        }

        @Override
        public String getName() {
            return "IncompatibleDescriptorPolicy";
        }

        @Override
        public Map<String, Object> toExternalProcessDescriptor() {
            return Collections.<String, Object>singletonMap("blockedWord", blockedWord);
        }
    }

    public static class StatefulExternalModule implements ScriptModule,
            ExternalProcessDescriptorProvider, ExternalProcessDescriptorConsumer {

        private int multiplier = 1;

        public StatefulExternalModule() {
        }

        public StatefulExternalModule(int multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public String getName() {
            return "statefulExternal";
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

        public int multiply(int value) {
            return value * multiplier;
        }

        @Override
        public Map<String, Object> toExternalProcessDescriptor() {
            return Collections.<String, Object>singletonMap("multiplier", multiplier);
        }

        @Override
        public void loadExternalProcessDescriptor(Map<String, Object> descriptor) {
            Object value = descriptor.get("multiplier");
            if (value instanceof Number) {
                multiplier = ((Number) value).intValue();
            }
        }
    }

    public static class FactoryBackedModule implements ScriptModule, ExternalProcessDescriptorProvider {

        private final int multiplier;

        private FactoryBackedModule(int multiplier) {
            this.multiplier = multiplier;
        }

        public static FactoryBackedModule of(int multiplier) {
            return new FactoryBackedModule(multiplier);
        }

        public static FactoryBackedModule fromExternalProcessDescriptor(Map<String, Object> descriptor) {
            Object value = descriptor.get("multiplier");
            int multiplier = value instanceof Number ? ((Number) value).intValue() : 1;
            return new FactoryBackedModule(multiplier);
        }

        @Override
        public String getName() {
            return "factoryModule";
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

        public int multiply(int value) {
            return value * multiplier;
        }

        @Override
        public Map<String, Object> toExternalProcessDescriptor() {
            return Collections.<String, Object>singletonMap("multiplier", multiplier);
        }
    }
}

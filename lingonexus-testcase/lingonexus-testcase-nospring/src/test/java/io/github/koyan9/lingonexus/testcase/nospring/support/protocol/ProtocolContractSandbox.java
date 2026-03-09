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
package io.github.koyan9.lingonexus.testcase.nospring.support.protocol;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.sandbox.AbstractScriptSandbox;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxProviderMetadata;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode;

import java.util.Collections;
import java.util.List;

@SandboxProviderMetadata(
        providerId = "protocol-contract",
        priority = 10,
        hostAccessMode = SandboxHostAccessMode.JVM_CLASSLOADER,
        hostRestrictionMode = SandboxHostRestrictionMode.STRICT,
        externalProcessCompatible = false,
        resultTransportMode = SandboxResultTransportMode.ANY,
        transportSerializerMode = SandboxTransportSerializerMode.CUSTOM_SERIALIZER_REQUIRED,
        transportPayloadProfile = SandboxTransportPayloadProfile.LARGE_PAYLOAD_FRIENDLY,
        transportProtocolCapabilities = {SandboxTransportProtocolCapability.CUSTOM_SERIALIZER_CONTRACT},
        transportSerializerContractIds = {"artifact-tool-v1"},
        languages = {"protocol-test"}
)
public class ProtocolContractSandbox extends AbstractScriptSandbox {

    public ProtocolContractSandbox(EngineConfig engineConfig) {
        super(engineConfig);
    }

    @Override
    public CompiledScript compile(String script, ScriptContext context) {
        return new ProtocolCompiledScript("PROTOCOL_CONTRACT", script);
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList("protocol-test");
    }

    @Override
    public boolean supports(String language) {
        return "protocol-test".equalsIgnoreCase(language);
    }
}

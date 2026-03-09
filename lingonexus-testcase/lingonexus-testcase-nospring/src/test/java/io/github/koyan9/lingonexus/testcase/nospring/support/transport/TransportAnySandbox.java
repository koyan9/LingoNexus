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
package io.github.koyan9.lingonexus.testcase.nospring.support.transport;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.sandbox.AbstractScriptSandbox;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxProviderMetadata;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode;

import java.util.Collections;
import java.util.List;

@SandboxProviderMetadata(
        providerId = "transport-any",
        priority = 100,
        hostAccessMode = SandboxHostAccessMode.POLYGLOT_HOST,
        hostRestrictionMode = SandboxHostRestrictionMode.RELAXED,
        supportsEngineCache = false,
        externalProcessCompatible = false,
        resultTransportMode = SandboxResultTransportMode.ANY,
        transportSerializerMode = SandboxTransportSerializerMode.JSON_FRAMED,
        transportPayloadProfile = SandboxTransportPayloadProfile.STANDARD_PAYLOAD,
        languages = {"transport-test"}
)
public class TransportAnySandbox extends AbstractScriptSandbox {

    public TransportAnySandbox(EngineConfig engineConfig) {
        super(engineConfig);
    }

    @Override
    public CompiledScript compile(String script, ScriptContext context) {
        return new TransportCompiledScript("ANY", script);
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList("transport-test");
    }

    @Override
    public boolean supports(String language) {
        return "transport-test".equalsIgnoreCase(language);
    }
}

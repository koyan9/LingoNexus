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
package io.github.koyan9.lingonexus.testcase.nospring.support.priority;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.sandbox.AbstractScriptSandbox;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxProviderMetadata;

import java.util.Collections;
import java.util.List;

@SandboxProviderMetadata(
        providerId = "priority-high",
        priority = 100,
        hostAccessMode = SandboxHostAccessMode.JVM_CLASSLOADER,
        hostRestrictionMode = SandboxHostRestrictionMode.STRICT,
        supportsEngineCache = false,
        externalProcessCompatible = true,
        resultTransportMode = SandboxResultTransportMode.JSON_SAFE_RESULT_AND_METADATA,
        languages = {"priority-test"}
)
public class HighPriorityPriorityTestSandbox extends AbstractScriptSandbox {

    public HighPriorityPriorityTestSandbox(EngineConfig engineConfig) {
        super(engineConfig);
    }

    @Override
    public CompiledScript compile(String script, ScriptContext context) {
        return new ProviderCompiledScript("HIGH", script);
    }

    @Override
    public List<String> getSupportedLanguages() {
        return Collections.singletonList("priority-test");
    }

    @Override
    public boolean supports(String language) {
        return "priority-test".equalsIgnoreCase(language);
    }
}

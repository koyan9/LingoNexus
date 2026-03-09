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

package io.github.koyan9.lingonexus.testcase.nospring.script.javaexr;

import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.exception.ScriptRuntimeException;
import io.github.koyan9.lingonexus.script.javaexpr.JavaExprSandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for JavaExprSandbox to verify RestrictedClassLoader integration
 */
class JavaExprSandboxSecurityTest {

    private JavaExprSandbox sandbox;

    @BeforeEach
    void setUp() {
        SandboxConfig sandboxConfig = SandboxConfig.builder()
                .addToBlacklist("java.io.*")
                .build();
        EngineConfig engineConfig = EngineConfig.builder()
                .sandboxConfig(sandboxConfig)
                .build();
        sandbox = new JavaExprSandbox(engineConfig);
    }

}

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

import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.core.executor.ExecutionPreparationService;
import io.github.koyan9.lingonexus.core.executor.PreparedExecution;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Execution Preparation Service Feature Tests")
class ExecutionPreparationServiceFeatureTest {

    @Test
    @DisplayName("Should reuse module snapshot until registry changes")
    void shouldReuseModuleSnapshotUntilRegistryChanges() {
        DefaultModuleRegistry moduleRegistry = new DefaultModuleRegistry();
        moduleRegistry.registerModule(new TestModule("alpha"));

        ExecutionPreparationService service = new ExecutionPreparationService(null, moduleRegistry);
        PreparedExecution first = service.prepareForInProcessExecution(ScriptContext.of(Collections.emptyMap()));
        PreparedExecution second = service.prepareForInProcessExecution(ScriptContext.of(Collections.emptyMap()));

        assertSame(first.getModulesUsed(), second.getModulesUsed());
        assertEquals(1, first.getModulesUsed().size());
        assertTrue(first.getExecutionVariables().containsKey("alpha"));

        moduleRegistry.registerModule(new TestModule("beta"));
        PreparedExecution third = service.prepareForInProcessExecution(ScriptContext.of(Collections.emptyMap()));

        assertNotSame(first.getModulesUsed(), third.getModulesUsed());
        assertEquals(2, third.getModulesUsed().size());
        assertTrue(third.getModulesUsed().contains("alpha"));
        assertTrue(third.getModulesUsed().contains("beta"));
        assertTrue(third.getExecutionVariables().containsKey("alpha"));
        assertTrue(third.getExecutionVariables().containsKey("beta"));
    }

    @Test
    @DisplayName("Should preserve request variables over module bindings")
    void shouldPreserveRequestVariablesOverModuleBindings() {
        DefaultModuleRegistry moduleRegistry = new DefaultModuleRegistry();
        moduleRegistry.registerModule(new TestModule("alpha"));

        ExecutionPreparationService service = new ExecutionPreparationService(null, moduleRegistry);
        PreparedExecution execution = service.prepareForInProcessExecution(
                ScriptContext.of(Collections.<String, Object>singletonMap("alpha", "request-value"))
        );

        assertEquals("request-value", execution.getExecutionVariables().get("alpha"));
        assertEquals(1, execution.getModulesUsed().size());
        assertTrue(execution.getModulesUsed().contains("alpha"));
    }

    private static class TestModule implements ScriptModule {
        private final String name;

        private TestModule(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<String, Object> getFunctions() {
            return Collections.emptyMap();
        }

        @Override
        public boolean hasFunction(String functionName) {
            return false;
        }
    }
}
